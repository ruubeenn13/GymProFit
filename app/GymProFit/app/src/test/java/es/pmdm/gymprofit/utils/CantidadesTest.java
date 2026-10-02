package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// CantidadesTest — cómo se escribe una cantidad (GP-172, lote 1.6.2)
// Con la unidad de la API: «1 rebanada (28 g)», «2 rebanadas (56 g)», «1,5 rebanadas
// (42 g)». Sin unidad, como en la 1.6.1: «2 × media taza (120 g)». Por gramos, «150 g».
// Los formatos se leen de strings.xml, en ES y en EN, como los usa la app.
// ============================================================
public class CantidadesTest {

    private static final Locale ES_ES = new Locale("es", "ES");
    private static final Cantidades.Formatos ES = formatos("values");
    private static final Cantidades.Formatos EN = formatos("values-en");

    private static Cantidades.Formatos formatos(String carpeta) {
        try {
            String xml = new String(Files.readAllBytes(Paths.get("src", "main", "res", carpeta, "strings.xml")),
                    StandardCharsets.UTF_8);
            return new Cantidades.Formatos(texto(xml, "cantidad_una_racion"), texto(xml, "cantidad_raciones"),
                    texto(xml, "cantidad_con_unidad"), texto(xml, "cantidad_gramos"));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static String texto(String xml, String nombre) {
        Matcher m = Pattern.compile("name=\"" + nombre + "\">([^<]*)<").matcher(xml);
        if (!m.find()) throw new AssertionError("falta " + nombre);
        return m.group(1).replace("\\u00A0", "\u00A0").replace("\\'", "'");
    }

    private static String sinDuro(String s) {
        return s.replace('\u00A0', ' ');
    }

    @Test
    public void con_unidad_en_singular_y_en_plural() {
        assertEquals("1 rebanada (28 g)", sinDuro(Cantidades.texto(ES, ES_ES, "1 rebanada", "rebanada", "rebanadas", 1.0, 28)));
        assertEquals("2 rebanadas (56 g)", sinDuro(Cantidades.texto(ES, ES_ES, "1 rebanada", "rebanada", "rebanadas", 2.0, 56)));
        assertEquals("1,5 rebanadas (42 g)", sinDuro(Cantidades.texto(ES, ES_ES, "1 rebanada", "rebanada", "rebanadas", 1.5, 42)));
    }

    @Test
    public void en_ingles() {
        assertEquals("1 slice (28 g)", sinDuro(Cantidades.texto(EN, Locale.ENGLISH, "1 slice", "slice", "slices", 1.0, 28)));
        assertEquals("2 slices (56 g)", sinDuro(Cantidades.texto(EN, Locale.ENGLISH, "1 slice", "slice", "slices", 2.0, 56)));
        assertEquals("1.5 slices (42 g)", sinDuro(Cantidades.texto(EN, Locale.ENGLISH, "1 slice", "slice", "slices", 1.5, 42)));
    }

    @Test
    public void por_gramos() {
        assertEquals("150 g", sinDuro(Cantidades.texto(ES, ES_ES, null, null, null, null, 150)));
        // Una línea con ración pero sin cuántas (una build vieja la guardó): sus gramos.
        assertEquals("150 g", sinDuro(Cantidades.texto(ES, ES_ES, "1 rebanada", "rebanada", "rebanadas", null, 150)));
        // Los gramos, redondeados.
        assertEquals("43 g", sinDuro(Cantidades.texto(ES, ES_ES, null, null, null, null, 42.6)));
    }

    @Test
    public void sin_unidad_como_en_la_161() {
        assertEquals("2 × media taza (120 g)", sinDuro(Cantidades.texto(ES, ES_ES, "Media taza", null, null, 2.0, 120)));
        assertEquals("Media taza (60 g)", sinDuro(Cantidades.texto(ES, ES_ES, "Media taza", null, null, 1.0, 60)));
        // Una API anterior a la 1.6.2, sin el campo unidad: lo de siempre.
        assertEquals("2 × rebanada (56 g)", sinDuro(Cantidades.texto(ES, ES_ES, "1 rebanada", null, null, 2.0, 56)));
        assertEquals("1 rebanada (28 g)", sinDuro(Cantidades.texto(ES, ES_ES, "1 rebanada", null, null, 1.0, 28)));
    }

    @Test
    public void el_espacio_antes_de_g_es_duro() {
        assertEquals("2 rebanadas (56\u00A0g)", Cantidades.texto(ES, ES_ES, "1 rebanada", "rebanada", "rebanadas", 2.0, 56));
    }
}
