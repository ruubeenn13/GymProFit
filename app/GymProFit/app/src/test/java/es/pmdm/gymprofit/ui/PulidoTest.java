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

    private static int cuenta(String s, String trozo) {
        int n = 0;
        for (int i = s.indexOf(trozo); i >= 0; i = s.indexOf(trozo, i + 1)) n++;
        return n;
    }

    static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
