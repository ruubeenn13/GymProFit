package es.pmdm.gymprofit.ui;

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

// ============================================================
// EstadisticasYFormulariosTest — lo que el revisor de UI encontró en Estadísticas de nutrición,
// Registrar sesión, Entrenar y los vasos del agua (GP-228, GP-227, grupo B).
//
// Estadísticas pintaba el eje de las calorías desde el mínimo (139 kcal parecían diez veces 135),
// sus tres gráficas no existían para TalkBack y sus colores eran hex a fuego (el texto blanco sobre
// el turquesa daba 1,86:1). Aquí se fija el código y se miden los colores en los dos temas.
// ============================================================
public class EstadisticasYFormulariosTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Path UI = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit", "ui");
    private static final String[] TEMAS = { "values", "values-night" };

    @Test
    public void lasCaloriasEmpiezanEnCeroYLasGraficasSeDicen() throws IOException {
        String a = leer(UI.resolve("activities/EstadisticasNutricionActivity.java"));
        assertTrue("el eje empieza en 0", a.contains("chartKcal.getAxisLeft().setAxisMinimum(0f);"));
        assertTrue("y llega al objetivo", a.contains("Math.max(maxKcal, conObjetivo ? objetivoKcal : 0)"));
        assertTrue(a.contains("chartKcal.setContentDescription("));
        assertTrue(a.contains("chartMacros.setContentDescription("));
        assertTrue(a.contains("gridHeatmap.setContentDescription("));
        assertTrue("sin colores a fuego", !a.contains("Color.parseColor("));
        assertTrue(a.contains("R.color.gp_macro_proteinas") && a.contains("R.color.gp_macro_carbos")
                && a.contains("R.color.gp_macro_grasas"));
        assertTrue("un período vacío no dice «aún no has registrado»",
                a.contains("tvVacia.setText(R.string.estadisticas_periodo_vacio);")
                        && a.contains("tvMacrosVacia.setText(R.string.estadisticas_periodo_vacio);"));
        assertTrue("un hueco por día del período, no solo los días con comidas",
                a.contains("entradas.add(new BarEntry(d - dia0, r.getCalorias()));")
                        && a.contains("chartKcal.getXAxis().setAxisMaximum(lenDias - 0.5f);"));
        assertTrue("la descripción dice el período, no el primer y último día con comidas",
                a.contains("getString(R.string.estadisticas_kcal_a11y, inicioPeriodo, finPeriodo,"));
        for (String idioma : new String[]{ "values", "values-en" }) {
            String xml = leer(RES.resolve(idioma + "/strings.xml"));
            for (String n : new String[]{ "estadisticas_kcal_a11y", "estadisticas_macros_a11y", "estadisticas_mapa_a11y",
                    "estadisticas_periodo_vacio" }) {
                assertTrue(idioma + ": falta " + n, xml.contains("<string name=\"" + n + "\">"));
            }
        }
    }

    // GP-225: los gramos ya no van dentro de las porciones del donut (no cabían y, con letra 1,3,
    // montaban sobre el agujero), sino en la leyenda; con ellos se fue el color del texto sobre
    // cada macro que pedía GP-228.
    @Test
    public void elDonutDiceLosGramosEnLaLeyenda() throws IOException {
        String estilo = leer(Paths.get("src", "main", "java", "es", "pmdm", "gymprofit", "utils", "ChartStyler.java"));
        assertTrue("sin valores en las porciones", estilo.contains("ds.setDrawValues(false);")
                && !estilo.contains("INSIDE_SLICE"));
        String a = leer(Paths.get("src", "main", "java", "es", "pmdm", "gymprofit", "ui", "activities",
                "EstadisticasNutricionActivity.java"));
        assertTrue(a.contains("new PieEntry((float) prot, leyendaMacro(R.string.macro_prot, prot))"));
        for (String idioma : new String[]{ "values", "values-en" }) {
            assertTrue(idioma, leer(RES.resolve(idioma + "/strings.xml"))
                    .contains("<string name=\"estadisticas_macro_leyenda\">%1$s · %2$s</string>"));
        }
        // Las tarjetas de las cifras, a la altura de su fila: en código, porque un match_parent en
        // la fila de pesos las aplastaba a 3 px.
        String l = leer(RES.resolve("layout/activity_estadisticas_nutricion.xml"));
        assertFalse(l.contains("android:layout_height=\"match_parent\" android:layout_weight=\"1\""));
        assertTrue(a.contains("AltoIgual.fila(findViewById(R.id.cardKpiMedia), findViewById(R.id.cardKpiDias))"));
    }

    @Test
    public void lasCeldasDelMapaPasanTresAUnoContraLaTarjeta() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String tema : TEMAS) {
            for (String c : new String[]{ "gp_success", "gp_stroke" }) {
                double r = MedidorNivelTest.contraste(color(tema, c), color(tema, "gp_surface"));
                if (r < 3.0) fallos.add(String.format("%s: %s sobre la tarjeta = %.2f", tema, c, r));
            }
        }
        if (!fallos.isEmpty()) fail(String.join("\n", fallos));
    }

    @Test
    public void registrarSesionNombraSusControles() throws IOException {
        String l = leer(RES.resolve("layout/activity_registrar_sesion.xml"));
        assertTrue("el rótulo nombra el desplegable", l.contains("android:labelFor=\"@+id/spRutina\""));
        assertTrue("el calendario tiene nombre", l.contains("app:endIconContentDescription=\"@string/sesiones_fecha_elegir\""));
        assertTrue("las estrellas vacías a 3:1", l.contains("android:progressBackgroundTint=\"@color/gp_stroke\""));
    }

    @Test
    public void elNombreDeEntrenoLibreEmpiezaPorLoQueSeVe() throws IOException {
        assertTrue(leer(RES.resolve("layout/fragment_entrenar.xml"))
                .contains("android:contentDescription=\"@string/entrenar_entreno_libre_a11y\""));
        for (String idioma : new String[]{ "values", "values-en" }) {
            String xml = leer(RES.resolve(idioma + "/strings.xml"));
            String visible = valor(xml, "entrenar_entreno_libre"), nombre = valor(xml, "entrenar_entreno_libre_a11y");
            assertTrue(idioma + ": «" + nombre + "» no empieza por «" + visible + "»", nombre.startsWith(visible));
        }
    }

    @Test
    public void lasTarjetasPulsablesDeHoyDicenQueAbren() throws IOException {
        String inicio = leer(UI.resolve("fragments/InicioFragment.java"));
        assertTrue(inicio.contains("card.setContentDescription(getString(R.string.abrir_rutina_a11y, r.getNombre()));"));
        assertTrue(inicio.contains("card.setContentDescription(getString(R.string.abrir_resumen_a11y, titulo.getText()));"));
        String programa = leer(UI.resolve("widget/TuProgramaVista.java"));
        assertTrue(programa.contains("bloque.setContentDescription(ctx.getString(R.string.abrir_rutina_a11y, hoy.getNombre()));"));
        assertTrue("el carrusel de Hoy toca no es una parada vacía (por código: el constructor lo hace enfocable)",
                inicio.contains("view.findViewById(R.id.scrollHoyEjercicios).setFocusable(false);"));
    }

    @Test
    public void losVasosUsanElFondoDelTemaYNoSonParadas() throws IOException {
        String n = leer(UI.resolve("fragments/NutricionFragment.java"));
        assertTrue("el atributo del tema, no el de la plataforma",
                !n.contains("android.R.attr.selectableItemBackgroundBorderless"));
        assertTrue(n.contains("celda.setFocusable(false);"));
    }

    private static String valor(String xml, String nombre) {
        Matcher m = Pattern.compile("<string name=\"" + nombre + "\">([^<]*)</string>").matcher(xml);
        assertTrue("falta " + nombre, m.find());
        return m.group(1);
    }

    private static String color(String tema, String nombre) throws IOException {
        String v = buscar(leer(RES.resolve(tema + "/colors.xml")), nombre);
        if (v == null) v = buscar(leer(RES.resolve("values/colors.xml")), nombre);
        assertTrue("no está " + nombre, v != null);
        return v;
    }

    private static String buscar(String xml, String nombre) {
        Matcher m = Pattern.compile("<color name=\"" + nombre + "\">(#[0-9A-Fa-f]{6})</color>").matcher(xml);
        return m.find() ? m.group(1) : null;
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
