package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// LuzDelDiaWebTest — la luz de cada hora de «Un día» se nota y no rompe el contraste
// (web/PLAN-UN-DIA.md, «La luz tiene que notarse» y §5).
//
// Cada hora tiene en un-dia.css su color (--luz-<hora>: R G B) y su opacidad en el punto más
// claro (--luz-<hora>-alfa). Mezclado sobre el carbón #0B0C0E, ese punto:
// · no pasa de una luminancia relativa de 0,045: con eso text-secondary #A1A9B6 sigue por
//   encima de 4,5:1 encima de la luz;
// · y no baja de 0,030: la primera versión (4–10 %) se quedaba por debajo y en el móvil no
//   se veía. El tope no se sube para que se note: se cambia el color o la forma.
// ============================================================
public class LuzDelDiaWebTest {

    private static final double TOPE = 0.045;
    private static final double MINIMO = 0.030;
    private static final int[] CARBON = {0x0B, 0x0C, 0x0E};

    @Test
    public void cadaLuzSeNotaSinRomperElContraste() throws IOException {
        String css = new String(Files.readAllBytes(WebDist.web().resolve("src/estilos/un-dia.css")),
                StandardCharsets.UTF_8);
        Map<String, int[]> colores = new LinkedHashMap<>();
        Matcher c = Pattern.compile("--luz-([a-z]+):\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)\\s*;").matcher(css);
        while (c.find()) {
            colores.put(c.group(1), new int[]{Integer.parseInt(c.group(2)), Integer.parseInt(c.group(3)),
                    Integer.parseInt(c.group(4))});
        }
        assertTrue("faltan luces: hay " + colores.keySet(), colores.size() >= 3);
        for (Map.Entry<String, int[]> luz : colores.entrySet()) {
            Matcher a = Pattern.compile("--luz-" + luz.getKey() + "-alfa:\\s*([0-9.]+)\\s*;").matcher(css);
            assertTrue("la luz " + luz.getKey() + " no tiene opacidad", a.find());
            double alfa = Double.parseDouble(a.group(1));
            double l = luminancia(mezcla(luz.getValue(), alfa));
            assertTrue(String.format("la luz %s llega a %.4f: pasa del tope %.3f", luz.getKey(), l, TOPE), l <= TOPE);
            assertTrue(String.format("la luz %s se queda en %.4f: no se notaría (mínimo %.3f)", luz.getKey(), l, MINIMO),
                    l >= MINIMO);
        }
        // El peor texto sobre la luz más clara posible: text-secondary a 4,5:1 como poco.
        double contraste = (luminancia(new double[]{0xA1, 0xA9, 0xB6}) + 0.05) / (TOPE + 0.05);
        assertTrue("text-secondary sobre el tope baja de 4,5:1", contraste >= 4.5);
    }

    /** El color de la luz con su opacidad sobre el carbón, redondeado como lo pinta el navegador. */
    private static double[] mezcla(int[] luz, double alfa) {
        double[] r = new double[3];
        for (int i = 0; i < 3; i++) r[i] = Math.round(CARBON[i] + (luz[i] - CARBON[i]) * alfa);
        return r;
    }

    /** Luminancia relativa de WCAG 2. */
    private static double luminancia(double[] rgb) {
        double[] k = {0.2126, 0.7152, 0.0722};
        double l = 0;
        for (int i = 0; i < 3; i++) {
            double v = rgb[i] / 255;
            l += k[i] * (v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4));
        }
        return l;
    }
}
