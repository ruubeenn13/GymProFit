package es.pmdm.gymprofit.ui;

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
// LogroMedallonTest — GP-079: el glifo del logro contra su medallón.
//
// El medallón y el glifo salen de la paleta (colors.xml y values-night):
// bloqueado = gp_text_secondary sobre gp_surface_2; conseguido = gp_gold sobre
// gp_gold_container, porque lo conseguido es oro (DEC-018). WCAG 1.4.11 pide 3:1 a un gráfico que informa.
// Si alguien toca esos cuatro tokens, esto lo dice antes que una captura.
// ============================================================
public class LogroMedallonTest {

    private static final Path RES = Paths.get("src", "main", "res");

    @Test
    public void bloqueado_pasa_3_a_1_en_los_dos_temas() throws IOException {
        comprobar("gp_text_secondary", "gp_surface_2");
    }

    @Test
    public void conseguido_pasa_3_a_1_en_los_dos_temas() throws IOException {
        comprobar("gp_gold", "gp_gold_container");
    }

    private static void comprobar(String glifo, String medallon) throws IOException {
        String claro = leer("values/colors.xml");
        String oscuro = leer("values-night/colors.xml");
        double rc = MedidorNivelTest.contraste(color(claro, glifo), color(claro, medallon));
        double ro = MedidorNivelTest.contraste(color(oscuro, glifo), color(oscuro, medallon));
        assertTrue(glifo + " sobre " + medallon + " en claro da " + rc, rc >= 3.0);
        assertTrue(glifo + " sobre " + medallon + " en oscuro da " + ro, ro >= 3.0);
    }

    private static String color(String xml, String nombre) {
        Matcher m = Pattern.compile("<color name=\"" + nombre + "\">(#[0-9A-Fa-f]{6})</color>").matcher(xml);
        assertTrue("no está " + nombre, m.find());
        return m.group(1);
    }

    private static String leer(String ruta) throws IOException {
        return new String(Files.readAllBytes(RES.resolve(ruta)), StandardCharsets.UTF_8);
    }
}
