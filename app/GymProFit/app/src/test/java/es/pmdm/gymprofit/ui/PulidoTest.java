package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// PulidoTest — los bajos del lote de pulido (GP-225) que se pueden fijar leyendo res/ y el
// código: cada regla es un fallo que el revisor de UI midió en la app.
// ============================================================
public class PulidoTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Path JAVA = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit");

    // GP-065: la hoja de seguir un programa (y la de elegir rutina) salía en el lavanda de
    // Material, el colorSurfaceContainerLow que el tema no define.
    @Test
    public void lasHojasInferioresVanEnLaSuperficieDeLaApp() throws IOException {
        String temas = leer(RES.resolve("values/themes.xml"));
        assertTrue(temas.contains("<item name=\"bottomSheetDialogTheme\">@style/ThemeOverlay.GymProFit.Hoja</item>"));
        assertTrue(temas.contains("<item name=\"bottomSheetStyle\">@style/Widget.GymProFit.Hoja</item>"));
        assertTrue(temas.contains("<item name=\"backgroundTint\">?attr/colorSurface</item>"));
    }

    // Los separadores de las listas de Nutrición, Añadir y Comida, hasta el texto (70 dp).
    @Test
    public void losSeparadoresLleganHastaElTexto() throws IOException {
        for (String[] l : new String[][]{ { "item_alimento_busqueda", "rayaFila" },
                { "item_comida_reciente", "rayaReciente" }, { "item_sueles", "rayaSueles" } }) {
            String xml = leer(RES.resolve("layout/" + l[0] + ".xml"));
            assertTrue(l[0], Pattern.compile("android:id=\"@\\+id/" + l[1] + "\"\\s+style=\"@style/Widget.GymProFit.Separador\"")
                    .matcher(xml).find());
        }
        String fila = leer(RES.resolve("drawable/bg_fila_comida.xml"));
        assertTrue("la raya de la línea de una comida, desde el texto",
                fila.contains("android:left=\"@dimen/fila_sangria_separador\""));
        String b = leer(RES.resolve("layout/item_alimento_busqueda.xml"));
        assertTrue("la fila de Añadir: 16 a los lados y el texto a 14 del icono",
                b.contains("android:paddingStart=\"@dimen/espacio_16\"") && b.contains("@dimen/fila_icono_a_texto"));
    }

    @Test
    public void losMasSonBotonesParaTalkBack() throws IOException {
        String busqueda = leer(JAVA.resolve("ui/adapters/BusquedaAlimentoAdapter.java"));
        assertEquals(2, cuenta(busqueda, "RolBoton.aplicar(mas);"));
        assertTrue(leer(JAVA.resolve("ui/activities/ComidaActivity.java")).contains("RolBoton.aplicar(mas);"));
    }

    @Test
    public void elBuscadorUsaSusMedidas() throws IOException {
        String a = leer(RES.resolve("layout/activity_anadir_alimento.xml"));
        assertFalse("sin el radio a mano", a.contains("boxCornerRadiusTopStart=\"26dp\""));
        assertEquals(4, cuenta(a, "=\"@dimen/buscador_radio\""));
        assertTrue(a.contains("android:minHeight=\"@dimen/buscador_alto\""));
        assertTrue(leer(RES.resolve("layout/activity_ejercicios.xml")).contains("android:layout_height=\"@dimen/buscador_alto\""));
        assertTrue(leer(RES.resolve("drawable/bg_buscador_pildora.xml")).contains("@dimen/buscador_radio"));
    }

    @Test
    public void laInsigniaTieneSuToken() throws IOException {
        assertTrue(leer(RES.resolve("values/colors.xml")).contains("<color name=\"gp_revisado\">"));
        assertTrue(leer(RES.resolve("layout/activity_ficha_alimento.xml")).contains("app:tint=\"@color/gp_revisado\""));
        assertTrue(leer(JAVA.resolve("ui/nutricion/Insignia.java")).contains("R.color.gp_revisado"));
    }

    // Tablero 25: la cara de Profi y el título a 17 sp, no la estrella y el rótulo.
    @Test
    public void loQueTeEncajaConProfi() throws IOException {
        String c = leer(RES.resolve("layout/item_encaja_cabecera.xml"));
        assertTrue(c.contains("@drawable/profi_cara"));
        assertFalse(c.contains("ic_ms_star_shine_fill"));
        assertTrue(c.contains("android:textSize=\"@dimen/text_titulo_profi\""));
        assertTrue(c.contains("android:accessibilityHeading=\"true\""));
        assertTrue(leer(RES.resolve("values/dimens.xml")).contains("<dimen name=\"text_titulo_profi\">17sp</dimen>"));
    }

    @Test
    public void elBocadilloTieneColaYBorde() throws IOException {
        assertTrue(leer(RES.resolve("drawable/bg_burbuja_profi.xml"))
                .contains("class=\"es.pmdm.gymprofit.ui.widget.BurbujaProfi\""));
        for (String l : new String[]{ "fragment_inicio", "activity_tus_datos", "view_profi_copiar" }) {
            String xml = leer(RES.resolve("layout/" + l + ".xml"));
            // El hueco de la cola: el relleno izquierdo crece lo que mide.
            assertTrue(l, xml.contains("android:paddingStart=\"@dimen/espacio_24\""));
        }
        // El tema de la pantalla llega en applyTheme: sin él, en oscuro el bocadillo salía blanco
        // (visto en el emulador al hacerlo).
        String burbuja = leer(JAVA.resolve("ui/widget/BurbujaProfi.java"));
        assertTrue(burbuja.contains("public boolean canApplyTheme()") && burbuja.contains("public void applyTheme("));
    }

    @Test
    public void laFichaDelAlimento() throws IOException {
        for (String t : new String[]{ "values", "values-en" }) {
            Matcher m = Pattern.compile("<string name=\"ficha_cifras_a11y\">([^<]+)</string>")
                    .matcher(leer(RES.resolve(t + "/strings.xml")));
            assertTrue(m.find());
            assertEquals(t + ": las tres cifras de macros dicen sus gramos", 3,
                    cuenta(m.group(1), t.equals("values") ? "gramos" : "grams"));
        }
        assertTrue("el corazón con su margen", Pattern.compile(
                "android:id=\"@\\+id/btnFavoritoFicha\"[^>]*android:layout_marginEnd=\"@dimen/espacio_16\"",
                Pattern.DOTALL).matcher(leer(RES.resolve("layout/activity_ficha_alimento.xml"))).find());
    }

    // La capa de foco de Material cambiaba el naranja del botón; el anillo ya lo pone FocoVisible.
    @Test
    public void losBotonesNoLlevanCapaDeFoco() throws IOException {
        String temas = leer(RES.resolve("values/themes.xml"));
        assertTrue(temas.contains("<item name=\"rippleColor\">@color/boton_capa</item>"));
        assertTrue("los fondos del sistema son los de la app",
                temas.contains("<item name=\"android:selectableItemBackground\">@drawable/fondo_pulsable</item>"));
        String capa = leer(RES.resolve("color/boton_capa.xml"));
        assertFalse(capa.contains("state_focused"));
        assertTrue(leer(RES.resolve("drawable/bg_dia_elegido.xml")).contains("<ripple"));
    }

    @Test
    public void entrenarDiceLoMismoEnTodasPartes() throws IOException {
        assertTrue(leer(RES.resolve("values/strings.xml")).contains("<string name=\"rutinas_principiante\">Principiante</string>"));
        assertTrue("la actividad no mueve el selector de experiencia",
                leer(JAVA.resolve("ui/activities/TusDatosActivity.java"))
                        .contains("AltoReservado.para(findViewById(R.id.tvActividadTexto), ACTIVIDAD_DESC)"));
    }

    @Test
    public void medidasYLasListasDeVerTodo() throws IOException {
        String m = leer(RES.resolve("layout/fragment_medidas.xml"));
        assertFalse("los chevrones sin alpha", m.contains("android:alpha=\"0.4\""));
        assertEquals(8, cuenta(m, "app:tint=\"@color/gp_stroke\""));
        for (String l : new String[]{ "fragment_records", "fragment_logros", "fragment_historial" }) {
            assertTrue(l, leer(RES.resolve("layout/" + l + ".xml")).contains("android:paddingStart=\"@dimen/margen_pantalla\""));
        }
        assertTrue(m.contains("android:paddingStart=\"@dimen/margen_pantalla\""));
        assertTrue("la línea del peso, azul", leer(JAVA.resolve("ui/fragments/MedidasFragment.java"))
                .contains("R.color.gp_linea_peso"));
    }

    // DESIGN.md: la cabecera de las secundarias mide 56 dp; ?attr/actionBarSize de Material 3 da 64.
    @Test
    public void laCabeceraMide56() throws IOException {
        assertTrue(leer(RES.resolve("values/dimens.xml")).contains("<dimen name=\"cabecera_alto\">56dp</dimen>"));
        String temas = leer(RES.resolve("values/themes.xml"));
        int i = temas.indexOf("<style name=\"Widget.GymProFit.Cabecera\" ");
        String estilo = temas.substring(i, temas.indexOf("</style>", i));
        assertTrue(estilo.contains("<item name=\"android:minHeight\">@dimen/cabecera_alto</item>"));
    }

    @Test
    public void elTecladoYTalkBack() throws IOException {
        String main = leer(JAVA.resolve("ui/activities/MainActivity.java"));
        assertTrue("con teclado, al cambiar de pestaña el foco va a su contenido", main.contains("focoAlContenido(index);"));
        assertTrue("el desplegable de la rutina dice su nombre y lo elegido",
                leer(JAVA.resolve("ui/activities/RegistrarSesionActivity.java")).contains("R.string.sesiones_rutina_a11y"));
    }

    // Primera pasada del revisor: el escáner de Añadir (y todo MaterialButton con un estilo de
    // Material puesto a mano) cambiaba el naranja con el foco. La capa se apaga en su raíz.
    @Test
    public void ningunComponenteDeMaterialLlevaCapaDeFoco() throws IOException {
        // Material las redefine en values-v21, v28, v33 y v34: cada carpeta tapa a la de values.
        for (String carpeta : new String[]{ "values", "values-v21", "values-v28", "values-v33", "values-v34" }) {
            String f = leer(RES.resolve(carpeta + "/material_foco.xml"));
            for (String n : new String[]{ "m3_sys_state_focus_state_layer_opacity", "mtrl_high_ripple_focused_alpha",
                    "mtrl_low_ripple_focused_alpha", "m3_control_ripple_color_selector_focused_alpha" }) {
                assertTrue(carpeta + ": " + n, Pattern.compile("name=\"" + n + "\"[^>]*>0\\.0<").matcher(f).find());
            }
        }
    }

    @Test
    public void primeraPasadaDelRevisorEnNutricion() throws IOException {
        // El anillo de un día no toca al de al lado: el fondo va 2 dp hacia dentro y la celda sigue
        // con 2 dp de margen (con 4, en la segunda pasada, bajó a 47 dp de ancho, menos de 48).
        String dia = leer(RES.resolve("layout/item_dia_semana.xml"));
        assertTrue(dia.contains("android:layout_marginStart=\"2dp\""));
        assertTrue(leer(RES.resolve("drawable/bg_dia_elegido.xml")).contains("android:left=\"2dp\" android:right=\"2dp\""));
        assertTrue(leer(RES.resolve("drawable/bg_dia_pulsable.xml")).contains("android:left=\"2dp\" android:right=\"2dp\""));
        assertTrue("la capa al pulsar un día, redondeada", leer(JAVA.resolve("ui/fragments/NutricionFragment.java"))
                .contains("celda.setBackgroundResource(R.drawable.bg_dia_pulsable);"));
        assertTrue(leer(RES.resolve("drawable/bg_dia_pulsable.xml")).contains("@dimen/radio_miniatura"));
        assertTrue("el reparto de la ficha, con aire para el anillo", Pattern.compile(
                "android:id=\"@\\+id/repartoFicha\"[^>]*android:padding=\"@dimen/aire_anillo\"", Pattern.DOTALL)
                .matcher(leer(RES.resolve("layout/activity_ficha_alimento.xml"))).find());
        String en = leer(RES.resolve("values-en/strings.xml"));
        assertTrue("la fecha del bocadillo, como el resto en inglés",
                en.contains("<string name=\"reciente_fecha_patron\">MMMM d</string>"));
        assertTrue(en.contains("<string name=\"nutricion_grasas\">Fat</string>"));
        String est = leer(JAVA.resolve("ui/activities/EstadisticasNutricionActivity.java"));
        assertTrue("un solo día no es «entre 379 y 379»", est.contains("R.string.estadisticas_kcal_a11y_igual"));
        assertTrue(est.contains("RolBoton.aplicar(findViewById(R.id.btnPrev))"));
        assertTrue(leer(JAVA.resolve("ui/activities/AnadirAlimentoActivity.java")).contains("RolBoton.aplicar(etiqueta);"));
        assertTrue(leer(JAVA.resolve("ui/activities/ComidaActivity.java"))
                .contains("RolBoton.aplicar(tarjeta.findViewById(R.id.btnNoCopiar))"));
    }

    // Segunda pasada: los fondos pulsables propios se teñían con el foco (colorControlHighlight), y el
    // anillo cruzaba con sus esquinas el texto de los bloques que lo toman sin aire.
    @Test
    public void segundaPasadaDelRevisorEnNutricion() throws IOException {
        java.util.List<String> conCapa = new java.util.ArrayList<>();
        try (java.util.stream.Stream<Path> s = Files.list(RES.resolve("drawable"))) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (leer(p).contains("?attr/colorControlHighlight")) conCapa.add(p.getFileName().toString());
            }
        }
        assertTrue("fondos con capa de foco: " + conCapa, conCapa.isEmpty());
        for (String[] l : new String[][]{ { "view_profi_copiar", "tvMensajeProfi" }, { "item_sueles", "textosSueles" },
                { "item_encaja", "barraEncaja" } }) {
            assertTrue(l[1], Pattern.compile("android:id=\"@\\+id/" + l[1] + "\"[^>]*android:padding=\"@dimen/aire_anillo\"",
                    Pattern.DOTALL).matcher(leer(RES.resolve("layout/" + l[0] + ".xml"))).find());
        }
    }

    // Tercera pasada: el aire de las ideas quitaba ancho al texto por la derecha; los anillos del
    // mensaje de Profi, de «Ahora no» y de la unidad de la ficha se pegaban a lo de al lado.
    @Test
    public void terceraPasadaDelRevisorEnNutricion() throws IOException {
        assertTrue(Pattern.compile("android:id=\"@\\+id/textosSueles\"[^>]*android:layout_marginEnd=\"@dimen/aire_anillo_fuera\"",
                Pattern.DOTALL).matcher(leer(RES.resolve("layout/item_sueles.xml"))).find());
        String p = leer(RES.resolve("layout/view_profi_copiar.xml"));
        assertTrue(Pattern.compile("android:id=\"@\\+id/btnSiCopiar\"[^>]*android:layout_marginTop=\"@dimen/espacio_8\"",
                Pattern.DOTALL).matcher(p).find());
        assertTrue(Pattern.compile("android:id=\"@\\+id/btnNoCopiar\"[^>]*android:layout_marginTop=\"@dimen/espacio_8\"",
                Pattern.DOTALL).matcher(p).find());
        assertTrue(Pattern.compile("android:id=\"@\\+id/btnUnidadVarias\"[^>]*android:layout_marginTop=\"@dimen/espacio_16\"",
                Pattern.DOTALL).matcher(leer(RES.resolve("layout/activity_ficha_alimento.xml"))).find());
        assertTrue("las ideas cuentan esta comida como está en la pantalla", leer(JAVA.resolve("ui/activities/ComidaActivity.java"))
                .contains("DiaNutricion.deConEsta(comidasDelDia, tipoComida, lineas, meta)"));
    }

    // Revisor en el grupo Entrenar (primera pasada).
    @Test
    public void primeraPasadaDelRevisorEnEntrenar() throws IOException {
        // El tooltip conoce su gráfica (si no, no sabe su área y se salía por la derecha).
        for (String f : new String[]{ "ui/activities/DetalleEjercicioActivity.java", "ui/activities/EstadisticasNutricionActivity.java",
                "ui/fragments/MedidasFragment.java" }) {
            String c = leer(JAVA.resolve(f));
            assertFalse(f, c.contains("setMarker(new ChartMarker"));
            assertTrue(f, c.contains("ChartMarker.poner("));
        }
        assertTrue("con un punto solo, sin tooltip que tape su valor",
                leer(JAVA.resolve("utils/PuntosGrafica.java")).contains("ds.setHighlightEnabled(false);"));
        // El menú del desplegable, distinguible del fondo y con la elegida marcada.
        assertTrue(leer(RES.resolve("values/themes.xml")).contains("<item name=\"android:popupBackground\">@drawable/bg_menu_desplegable</item>"));
        assertTrue(leer(JAVA.resolve("ui/activities/RegistrarSesionActivity.java")).contains("R.layout.item_desplegable"));
        assertTrue(leer(RES.resolve("layout/item_desplegable.xml")).contains("android:checkMarkTint=\"@color/marca_elegida\""));
        // Los chips de Programas, los de la app y no el lavanda de Material.
        String prog = leer(RES.resolve("layout/activity_programas.xml"));
        assertFalse(prog.contains("Widget.Material3.Chip"));
        // La hoja de seguir programa: el aviso en superficie 2 (su alto, en la segunda pasada).
        assertTrue(leer(RES.resolve("layout/dialog_seguir_programa.xml")).contains("@drawable/bg_aviso_hoja"));
        // La pista del selector se vuelve a medir con la píldora de ahora.
        assertTrue(leer(JAVA.resolve("ui/widget/SelectorPildora.java")).contains("post(this::requestLayout);"));
        assertTrue("la cola apunta a la cara en Tus datos", leer(RES.resolve("layout/activity_tus_datos.xml"))
                .contains("android:gravity=\"center_vertical\""));
        assertTrue(leer(RES.resolve("values/strings.xml")).contains("<string name=\"ficha_atras\">Volver</string>"));
        assertTrue("Registrar sesión a margen_pantalla", leer(RES.resolve("layout/activity_registrar_sesion.xml"))
                .contains("android:paddingStart=\"@dimen/margen_pantalla\""));
    }

    // Segunda pasada en Entrenar.
    @Test
    public void segundaPasadaDelRevisorEnEntrenar() throws IOException {
        assertFalse("los chips de días de Programas, sin el lavanda de Material",
                leer(RES.resolve("layout/item_chip_filtro.xml")).contains("Widget.Material3.Chip"));
        String hoja = leer(JAVA.resolve("ui/widget/SeguirProgramaHoja.java"));
        assertFalse("la hoja sube desde abajo", hoja.contains("setFitToContents(false)"));
        assertTrue("y no cambia de alto al elegir minutos", hoja.contains("raiz.setMinimumHeight("));
        String ficha = leer(JAVA.resolve("ui/activities/DetalleEjercicioActivity.java"));
        assertFalse("un solo entreno no es un récord: sin dorado", ficha.contains("ds.setCircleColor(oro);"));
        assertTrue("sin cambios, Tus datos enseña el objetivo guardado",
                leer(JAVA.resolve("ui/activities/TusDatosActivity.java")).contains("plan.cifras(hayCambios(), hayAntes)"));
        assertTrue("«Guardar sesión» a margen_pantalla", Pattern.compile(
                "android:id=\"@\\+id/btnGuardar\"[^>]*android:layout_marginStart=\"@dimen/margen_pantalla\"", Pattern.DOTALL)
                .matcher(leer(RES.resolve("layout/activity_registrar_sesion.xml"))).find());
    }

    // Revisor en el grupo Progreso (primera pasada).
    @Test
    public void primeraPasadaDelRevisorEnProgreso() throws IOException {
        String estilo = leer(JAVA.resolve("utils/ChartStyler.java"));
        assertFalse("las líneas de dato no se curvan", estilo.contains("HORIZONTAL_BEZIER"));
        String m = leer(RES.resolve("layout/fragment_medidas.xml"));
        int pista = m.indexOf("android:id=\"@+id/tvGraficaPista\"");
        int cierre = m.indexOf("</FrameLayout>", m.indexOf("android:id=\"@+id/chartPeso\""));
        assertTrue("la pista del punto único va sobre la gráfica, dentro de su marco", pista > 0 && pista < cierre);
        String l = leer(JAVA.resolve("ui/progreso/ListasProgreso.java"));
        assertTrue("dorado solo el récord más reciente, como en la lista", l.contains("boolean dorado = i == 0;"));
        assertTrue(leer(RES.resolve("layout/item_record.xml")).contains("android:minHeight=\"@dimen/fila_alto_min\""));
        assertTrue(leer(JAVA.resolve("ui/adapters/SesionAdapter.java")).contains("RolBoton.aplicar(btnEliminar)"));
    }

    // Tercera pasada en Progreso: con un punto, sin cuadrícula que cruce su cifra, la cifra en la
    // condensada de la app, y la ficha dice en su descripción las cifras que dibuja.
    @Test
    public void terceraPasadaDelRevisorEnProgreso() throws IOException {
        String p = leer(JAVA.resolve("utils/PuntosGrafica.java"));
        assertTrue(p.contains("ds.setValueTypeface(ChartStyler.fuente(ctx));"));
        assertTrue(p.contains("c.getAxisLeft().setDrawGridLines(que == Que.LINEA);"));
        assertTrue(leer(JAVA.resolve("ui/fragments/MedidasFragment.java")).contains("PuntosGrafica.cuadricula(chartPeso, que);"));
        assertTrue(leer(JAVA.resolve("ui/activities/DetalleEjercicioActivity.java")).contains("PuntosGrafica.cuadricula(chart,"));
        for (String t : new String[]{ "values", "values-en" }) {
            Matcher m = Pattern.compile("<string name=\"detalle_progresion_a11y\">([^<]+)</string>")
                    .matcher(leer(RES.resolve(t + "/strings.xml")));
            assertTrue(m.find());
            assertTrue(t + ": de qué cifra a qué cifra", m.group(1).contains("%4$s") && m.group(1).contains("%5$s"));
        }
    }

    private static int cuenta(String s, String trozo) {
        int n = 0;
        for (int i = s.indexOf(trozo); i >= 0; i = s.indexOf(trozo, i + 1)) n++;
        return n;
    }

    static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
