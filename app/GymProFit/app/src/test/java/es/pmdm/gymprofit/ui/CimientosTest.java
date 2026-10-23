package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// ============================================================
// CimientosTest — GP-210 paso 2 / GP-216 / GP-214: los cimientos de DESIGN.md son los de res/.
//
// Espaciado (4, 8, 12, 16, 20, 24, 28, 36), radios (12, 16, 20, 24, 28), tarjeta estándar y
// protagonista, cabecera de sección, fila de lista, avatar y roles de texto: cada número que
// DESIGN.md promete tiene que estar en dimens.xml y en los estilos, y ningún layout vuelve a
// escribir a mano un radio, un tamaño de texto ni el chip suelto donde las opciones se excluyen.
// ============================================================
public class CimientosTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Path JAVA = Paths.get("src", "main", "java");

    // ── Escalas ─────────────────────────────────────────────────────────────

    @Test
    public void el_espaciado_es_el_de_la_escala() throws IOException {
        String d = dimens();
        for (int dp : new int[]{4, 8, 12, 16, 20, 24, 28, 36}) {
            assertEquals("espacio_" + dp, dp + "dp", dimen(d, "espacio_" + dp));
        }
        assertEquals("margen lateral de pantalla", "20dp", dimen(d, "margen_pantalla"));
        assertEquals("entre secciones en las pestañas", "36dp", dimen(d, "separacion_pestana"));
        assertEquals("entre secciones en las subpantallas", "28dp", dimen(d, "separacion_subpantalla"));
        assertEquals("título de sección a contenido", "14dp", dimen(d, "titulo_a_contenido"));
    }

    @Test
    public void los_radios_son_los_de_la_escala() throws IOException {
        String d = dimens();
        assertEquals("12dp", dimen(d, "radio_chip"));
        assertEquals("16dp", dimen(d, "radio_miniatura"));
        assertEquals("20dp", dimen(d, "radio_tarjeta_pequena"));
        assertEquals("24dp", dimen(d, "radio_tarjeta"));
        assertEquals("28dp", dimen(d, "radio_protagonista"));
    }

    @Test
    public void los_roles_de_texto_son_los_de_DESIGN() throws IOException {
        String d = dimens();
        assertEquals("título de pantalla", "34sp", dimen(d, "text_display_large"));
        assertEquals("sección", "20sp", dimen(d, "text_title"));
        assertEquals("cuerpo", "16sp", dimen(d, "text_body"));
        assertEquals("fila", "16sp", dimen(d, "text_fila"));
        assertEquals("apoyo", "14sp", dimen(d, "text_caption"));
        assertEquals("apoyo pequeño", "13sp", dimen(d, "text_eyebrow"));
        assertEquals("enlace de sección", "15sp", dimen(d, "text_enlace_seccion"));
        assertEquals("cifra pequeña", "26sp", dimen(d, "text_cifra_pequena"));
        assertEquals("cifra grande", "48sp", dimen(d, "text_cifra_grande"));
        String s = leer(RES.resolve("values/estilos_sistema.xml"));
        for (String rol : new String[]{"Seccion", "Cuerpo", "Apoyo", "Enlace", "Cifra.Pequena", "Cifra.Grande"}) {
            assertTrue("falta TextAppearance.GymProFit." + rol, s.contains("name=\"TextAppearance.GymProFit." + rol + "\""));
        }
        // La sección es Barlow SemiBold, no la condensada.
        Matcher m = Pattern.compile("name=\"TextAppearance\\.GymProFit\\.Seccion\"[\\s\\S]*?</style>").matcher(s);
        assertTrue(m.find());
        assertTrue("la sección va en Barlow", m.group().contains("@font/barlow<"));
        assertTrue("la sección va en 600", m.group().contains(">600<"));
    }

    // ── Tarjetas ────────────────────────────────────────────────────────────

    @Test
    public void la_tarjeta_estandar_es_de_24_sin_elevacion_y_con_borde_solo_en_claro() throws IOException {
        String t = leer(RES.resolve("values/themes.xml"));
        String card = estilo(t, "Widget.GymProFit.Card");
        assertTrue("radio 24", card.contains("<item name=\"cardCornerRadius\">@dimen/radio_tarjeta</item>"));
        assertTrue("elevación 0", card.contains("<item name=\"cardElevation\">0dp</item>"));
        assertTrue("borde con outlineVariant", card.contains("<item name=\"strokeColor\">?attr/colorOutlineVariant</item>"));
        assertTrue("el ancho del borde sale del tema", card.contains("<item name=\"strokeWidth\">@dimen/tarjeta_borde</item>"));
        assertEquals("borde de 1 dp en claro", "1dp", dimen(dimens(), "tarjeta_borde"));
        assertEquals("sin borde en oscuro", "0dp", dimen(leer(RES.resolve("values-night/dimens.xml")), "tarjeta_borde"));
        // La superficie estándar es #1B1E24 en oscuro.
        assertTrue(leer(RES.resolve("values-night/colors.xml")).contains("<color name=\"gp_surface\">#1B1E24</color>"));
        assertFalse("Card.Estructura se retiró", leer(RES.resolve("values/estilos_estructura.xml")).contains("Card.Estructura"));
    }

    @Test
    public void la_tarjeta_protagonista_es_3A2110_de_28_con_su_texto() throws IOException {
        String c = leer(RES.resolve("values/colors.xml"));
        assertTrue(c.contains("<color name=\"gp_hero\">#3A2110</color>"));
        assertTrue(c.contains("<color name=\"gp_on_hero\">#FFF4E8</color>"));
        assertTrue(c.contains("<color name=\"gp_on_hero_variant\">#FFD3B0</color>"));
        String s = leer(RES.resolve("values/estilos_sistema.xml"));
        String p = estilo(s, "Widget.GymProFit.Card.Protagonista");
        assertTrue(p.contains("@dimen/radio_protagonista"));
        assertTrue(p.contains("<item name=\"cardBackgroundColor\">@color/gp_hero</item>"));
        assertTrue("«Hoy toca» es la tarjeta protagonista",
                leer(RES.resolve("layout/fragment_inicio.xml"))
                        .contains("android:id=\"@+id/cardHoyToca\"\n                style=\"@style/Widget.GymProFit.Card.Protagonista\""));
    }

    @Test
    public void ninguna_tarjeta_escribe_su_radio_ni_su_elevacion() throws IOException {
        List<String> fallos = new ArrayList<>();
        Pattern etiqueta = Pattern.compile("<com\\.google\\.android\\.material\\.card\\.MaterialCardView\\b[^>]*>");
        Pattern literal = Pattern.compile("cardCornerRadius=\"\\d+dp\"|cardElevation=\"\\d+dp\"");
        for (Path f : ficheros(RES.resolve("layout"), ".xml")) {
            Matcher m = etiqueta.matcher(leer(f));
            while (m.find()) {
                Matcher l = literal.matcher(m.group());
                while (l.find()) fallos.add(f.getFileName() + ": " + l.group());
            }
        }
        if (!fallos.isEmpty()) fail("Radios o elevaciones escritos a mano en tarjetas (usa el estilo o @dimen/radio_*):\n"
                + String.join("\n", fallos));
    }

    @Test
    public void ningun_boton_escribe_su_radio_a_mano() throws IOException {
        List<String> fallos = buscar(RES.resolve("layout"), Pattern.compile("app:cornerRadius=\"\\d+dp\""));
        if (!fallos.isEmpty()) fail("Botones con radio escrito (la forma sale del estilo, mitad de la altura):\n"
                + String.join("\n", fallos));
    }

    @Test
    public void ningun_layout_escribe_un_sp() throws IOException {
        List<String> fallos = buscar(RES.resolve("layout"), Pattern.compile("android:textSize=\"[0-9.]+sp\""));
        if (!fallos.isEmpty()) fail("Tamaños de texto escritos a mano (usa @dimen/text_*):\n" + String.join("\n", fallos));
    }

    // ── Cabecera, fila, avatar ──────────────────────────────────────────────

    @Test
    public void la_cabecera_de_seccion_y_la_fila_de_lista_son_las_de_DESIGN() throws IOException {
        String d = dimens();
        assertEquals("enlace de 48 dp", "48dp", dimen(d, "seccion_enlace_alto"));
        assertEquals("fila mínima de 64", "64dp", dimen(d, "fila_alto_min"));
        assertEquals("padding horizontal 16", "16dp", dimen(d, "fila_padding_horizontal"));
        assertEquals("círculo del icono 40", "40dp", dimen(d, "fila_icono_circulo"));
        assertEquals("sangría del separador 70", "70dp", dimen(d, "fila_sangria_separador"));
        assertEquals("separador de 1 dp", "1dp", dimen(d, "separador_alto"));
        String s = leer(RES.resolve("values/estilos_sistema.xml"));
        assertTrue(s.contains("name=\"Widget.GymProFit.CabeceraSeccion.Enlace\""));
        assertTrue(s.contains("name=\"Widget.GymProFit.Fila.Icono\""));
        assertTrue(estilo(s, "Widget.GymProFit.Fila.Icono").contains("@drawable/bg_fila_icono"));
        // El círculo del icono es surface_2 (#252A31 en oscuro) y el separador también.
        assertTrue(leer(RES.resolve("drawable/bg_fila_icono.xml")).contains("?attr/colorSurfaceVariant"));
        assertTrue(leer(RES.resolve("values-night/colors.xml")).contains("<color name=\"gp_surface_2\">#252A31</color>"));
        // Las filas que ya existían usan los mismos números.
        String e = leer(RES.resolve("values/estilos_estructura.xml"));
        assertTrue(estilo(e, "Widget.GymProFit.FilaAccion").contains("@dimen/fila_alto_min"));
        assertTrue(estilo(e, "Widget.GymProFit.FilaAjuste").contains("@dimen/fila_alto_min"));
        assertTrue(leer(RES.resolve("drawable/div_grupo_ajustes.xml")).contains("@dimen/fila_sangria_separador"));
    }

    @Test
    public void el_avatar_de_cabecera_es_de_48_con_borde_naranja_de_2() throws IOException {
        String d = dimens();
        assertEquals("48dp", dimen(d, "avatar_cabecera"));
        assertEquals("2dp", dimen(d, "avatar_cabecera_borde"));
        String bg = leer(RES.resolve("drawable/bg_avatar_cabecera.xml"));
        assertTrue(bg.contains("@color/gp_hero"));
        assertTrue(bg.contains("?attr/colorPrimary"));
        assertTrue(bg.contains("@dimen/avatar_cabecera_borde"));
        String v = leer(RES.resolve("layout/view_avatar.xml"));
        assertTrue(v.contains("@drawable/bg_avatar_cabecera"));
        assertTrue(v.contains("TextAppearance.GymProFit.Avatar"));
        // Progreso no pisa el fondo del avatar: la inicial va en #FFF4E8 y sobre otro fondo se perdía en claro.
        assertFalse(leer(JAVA.resolve("es/pmdm/gymprofit/ui/fragments/ProgresoFragment.java")).contains("avatar.setBackgroundResource"));
    }

    // ── Expressive ──────────────────────────────────────────────────────────

    @Test
    public void los_botones_principales_cambian_de_forma_de_26_a_12() throws IOException {
        String d = dimens();
        assertEquals("26dp", dimen(d, "boton_radio_reposo"));
        assertEquals("12dp", dimen(d, "boton_radio_pulsado"));
        // «Empezar» (Inicio, «Hoy» de Entrenar y el detalle de la rutina), «Hecho» (dos) y el «+» de la barra.
        String[][] botones = {
                {"layout/fragment_inicio.xml", "btnHoyPrincipal"},
                {"layout/activity_detalle_rutina.xml", "btnEntrenarRutina"},
                {"layout/view_tu_programa.xml", "btnEmpezarHoy"},
                {"layout/activity_resumen_sesion.xml", "btnHechoResumen"},
                {"layout/activity_anadir_alimento.xml", "btnHecho"},
        };
        for (String[] b : botones) {
            String x = leer(RES.resolve(b[0]));
            Matcher m = Pattern.compile("<es\\.pmdm\\.gymprofit\\.ui\\.widget\\.BotonExpresivo[^>]*android:id=\"@\\+id/" + b[1] + "\"").matcher(x);
            assertTrue(b[1] + " tiene que ser un BotonExpresivo", m.find());
        }
        assertTrue(leer(JAVA.resolve("es/pmdm/gymprofit/ui/widget/BarraNavegacion.java")).contains("BotonExpresivo.alPulsar(btnAcciones)"));
        assertTrue(leer(RES.resolve("drawable/bg_boton_acciones.xml")).contains("@dimen/boton_radio_reposo"));
        // El tiempo es el del toque, no un número escrito.
        String c = leer(JAVA.resolve("es/pmdm/gymprofit/ui/widget/BotonExpresivo.java"));
        assertTrue(c.contains("Movimiento.TOQUE"));
        assertFalse("BotonExpresivo no escribe milisegundos", Pattern.compile("setDuration\\(\\s*\\d").matcher(c).find());
    }

    @Test
    public void las_opciones_que_se_excluyen_van_en_botones_conectados() throws IOException {
        String[][] grupos = {
                {"layout/activity_estadisticas_nutricion.xml", "chipsPeriodo"},
                {"layout/activity_crear_rutina.xml", "chipGroupNivel"},
                {"layout/activity_editar_rutina.xml", "chipGroupNivelEditar"},
                {"layout/fragment_medidas.xml", "chipsMetrica"},
                {"layout/fragment_medidas.xml", "chipsRango"},
                {"layout/dialog_seguir_programa.xml", "chipsMinutos"},
        };
        for (String[] g : grupos) {
            String x = leer(RES.resolve(g[0]));
            Matcher m = Pattern.compile("<com\\.google\\.android\\.material\\.button\\.MaterialButtonToggleGroup[^>]*android:id=\"@\\+id/" + g[1] + "\"").matcher(x);
            assertTrue(g[1] + " en " + g[0] + " tiene que ser un grupo de botones conectados", m.find());
        }
        String s = leer(RES.resolve("values/estilos_sistema.xml"));
        assertTrue(s.contains("parent=\"Widget.Material3Expressive.MaterialButtonToggleGroup\""));
    }

    @Test
    public void la_espera_corta_dentro_de_un_boton_o_una_tarjeta_usa_el_indicador_con_forma() throws IOException {
        for (String f : new String[]{"layout/activity_guarda_plan.xml", "layout/activity_sesion_en_vivo.xml", "layout/fragment_paso_plan.xml"}) {
            assertTrue(f, leer(RES.resolve(f)).contains("com.google.android.material.loadingindicator.LoadingIndicator"));
        }
    }

    // ── Restos ──────────────────────────────────────────────────────────────

    @Test
    public void las_animaciones_de_pestanas_sin_uso_estan_borradas() throws IOException {
        try (Stream<Path> s = Files.list(RES.resolve("anim"))) {
            List<String> tab = s.map(p -> p.getFileName().toString()).filter(n -> n.startsWith("tab_")).collect(Collectors.toList());
            assertTrue("sobran res/anim/tab_*: " + tab, tab.isEmpty());
        }
    }

    // GP-214 y los bajos del revisor del paso 3.
    @Test
    public void los_bajos_del_revisor_estan_arreglados() throws IOException {
        // «Último récord»: la marca no se parte, o el «10» quedaba solo con letra 1,3.
        String inicio = leer(JAVA.resolve("es/pmdm/gymprofit/ui/inicio/InicioProgreso.java"));
        assertTrue("la marca del récord lleva espacios duros", inicio.contains("replace(' ', '\\u00A0')"));
        // «SERIE» y «ANTERIOR» ya no se pegan: la columna de la serie mide 48 dp.
        assertEquals("48dp", dimen(dimens(), "envivo_col_serie"));
        // La cabecera de la sesión en vivo usa su propio título de fila, no el de 22 sp de la barra.
        assertTrue(leer(RES.resolve("layout/activity_sesion_en_vivo.xml")).contains("Widget.GymProFit.Cabecera.Sesion"));
        assertTrue(leer(RES.resolve("values/themes.xml")).contains("TextAppearance.GymProFit.Seccion.Fila"));
    }

    // GP-219 y el pendiente del encargo: los campos de la sesión en vivo tienen nombre y «SERIE»
    // se separa de «ANTERIOR» con la escala de espaciado.
    @Test
    public void los_campos_de_la_sesion_en_vivo_tienen_nombre_y_las_columnas_respiran() throws IOException {
        String es = leer(RES.resolve("values/strings.xml"));
        String en = leer(RES.resolve("values-en/strings.xml"));
        assertTrue(es.contains("<string name=\"envivo_kg_a11y\">Kilos, serie %1$d</string>"));
        assertTrue(es.contains("<string name=\"envivo_reps_a11y\">Repeticiones, serie %1$d</string>"));
        assertTrue(en.contains("<string name=\"envivo_kg_a11y\">Kilos, set %1$d</string>"));
        assertTrue(en.contains("<string name=\"envivo_reps_a11y\">Reps, set %1$d</string>"));
        for (String f : new String[]{"layout/item_envivo_serie.xml", "layout/item_envivo_ejercicio.xml"}) {
            Matcher m = Pattern.compile("android:id=\"@\\+id/(tvAnterior|tvRotuloAnterior)\"[\\s\\S]*?/>").matcher(leer(RES.resolve(f)));
            assertTrue(f, m.find());
            assertTrue(f + ": «Anterior» separado de «Serie» con la escala",
                    m.group().contains("android:paddingStart=\"@dimen/espacio_8\""));
        }
    }

    @Test
    public void DESIGN_recoge_los_cimientos() throws IOException {
        Path design = Paths.get("..", "DESIGN.md");
        String x = leer(design);
        for (String clave : new String[]{"## Cimientos", "espacio_36", "radio_protagonista", "Tarjeta protagonista",
                "Cabecera de sección", "Fila de lista", "BotonExpresivo", "Botones conectados", "Transiciones"}) {
            assertTrue("DESIGN.md no recoge «" + clave + "»", x.contains(clave));
        }
    }

    // ── Ayudas ──────────────────────────────────────────────────────────────

    private static String dimens() throws IOException {
        return leer(RES.resolve("values/dimens.xml"));
    }

    private static String dimen(String xml, String nombre) {
        Matcher m = Pattern.compile("<dimen name=\"" + Pattern.quote(nombre) + "\">([^<]+)</dimen>").matcher(xml);
        assertTrue("no está el dimen " + nombre, m.find());
        return m.group(1).trim();
    }

    private static String estilo(String xml, String nombre) {
        Matcher m = Pattern.compile("<style name=\"" + Pattern.quote(nombre) + "\"[\\s\\S]*?</style>").matcher(xml);
        assertTrue("no está el estilo " + nombre, m.find());
        return m.group();
    }

    private static List<String> buscar(Path raiz, Pattern p) throws IOException {
        List<String> r = new ArrayList<>();
        for (Path f : ficheros(raiz, ".xml")) {
            Matcher m = p.matcher(leer(f));
            while (m.find()) r.add(f.getFileName() + ": " + m.group());
        }
        return r;
    }

    private static List<Path> ficheros(Path raiz, String sufijo) throws IOException {
        try (Stream<Path> s = Files.walk(raiz)) {
            return s.filter(p -> p.toString().endsWith(sufijo)).collect(Collectors.toList());
        }
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
