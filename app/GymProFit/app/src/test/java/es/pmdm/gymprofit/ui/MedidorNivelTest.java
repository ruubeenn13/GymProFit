package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
// MedidorNivelTest — GP-078: el medidor de nivel del onboarding.
//
// Lee los recursos tal cual están en disco (la prueba corre con el módulo
// como directorio de trabajo) y fija lo que el arreglo prometió:
//   · cada ic_nivel_* tiene cuatro segmentos y N de ellos llenos;
//   · lleno contra vacío llega a 3:1 (WCAG 1.4.11) en claro y en oscuro.
// El segundo es el que se rompería sin avisar si alguien toca la paleta.
// ============================================================
public class MedidorNivelTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final String[] NIVELES = {
            "ic_nivel_principiante", "ic_nivel_intermedio", "ic_nivel_avanzado", "ic_nivel_experto" };

    @Test
    public void cada_nivel_llena_tantos_segmentos_como_su_posicion() throws IOException {
        for (int i = 0; i < NIVELES.length; i++) {
            List<String> colores = coloresDeRelleno(leer(RES.resolve("drawable/" + NIVELES[i] + ".xml")));
            assertEquals(NIVELES[i] + ": cuatro segmentos", 4, colores.size());
            int llenos = 0;
            for (String c : colores) if (c.equals("?attr/colorPrimary")) llenos++;
            assertEquals(NIVELES[i] + ": segmentos llenos", i + 1, llenos);
            for (String c : colores) {
                assertTrue(NIVELES[i] + ": color inesperado " + c,
                        c.equals("?attr/colorPrimary") || c.equals("@color/gp_meter_track"));
            }
        }
    }

    @Test
    public void lleno_contra_vacio_pasa_3_a_1_en_claro() throws IOException {
        assertContraste("values");
    }

    @Test
    public void lleno_contra_vacio_pasa_3_a_1_en_oscuro() throws IOException {
        assertContraste("values-night");
    }

    private static void assertContraste(String carpeta) throws IOException {
        String colores = leer(RES.resolve(carpeta + "/colors.xml"));
        double r = contraste(color(colores, "gp_primary"), color(colores, "gp_meter_track"));
        assertTrue(carpeta + ": lleno contra vacío da " + r, r >= 3.0);
    }

    private static List<String> coloresDeRelleno(String xml) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("android:fillColor=\"([^\"]+)\"").matcher(xml);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    private static String color(String xml, String nombre) {
        Matcher m = Pattern.compile("<color name=\"" + nombre + "\">(#[0-9A-Fa-f]{6})</color>").matcher(xml);
        assertTrue("no está " + nombre, m.find());
        return m.group(1);
    }

    // Contraste WCAG 2.x entre dos colores #RRGGBB.
    static double contraste(String a, String b) {
        double la = luminancia(a), lb = luminancia(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminancia(String hex) {
        double[] c = new double[3];
        for (int i = 0; i < 3; i++) {
            double v = Integer.parseInt(hex.substring(1 + 2 * i, 3 + 2 * i), 16) / 255.0;
            c[i] = v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
