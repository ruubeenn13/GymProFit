package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// LuzQuietaWebTest — la luz de las páginas secundarias no deja ningún texto por debajo de AA
// (fase 4 de «Un día»).
//
// Cada hoja (lectura.css, beta.css, perdido.css) pone una luz de ventana quieta detrás de la
// cabecera y el titular: bandas de --luz con opacidad --luz-alfa sobre --fondo. Donde la banda
// es más clara, el fondo deja de ser --fondo, y el texto suelto que cae encima tiene que seguir
// en 4,5:1. Se comprueba en cada tema que la hoja tiene (oscuro y, si lo tiene, claro), con el
// color de cada banda en su punto más claro y los tres colores de texto: --texto, --texto-2 y
// --enlace. Subir --luz-alfa por encima del límite hace fallar el test.
// ============================================================
public class LuzQuietaWebTest {

    private static final String[] HOJAS = {"lectura.css", "beta.css", "perdido.css"};
    private static final String[] TEXTOS = {"--texto", "--texto-2", "--enlace"};
    private static final Pattern CLARO = Pattern.compile("@media \\(prefers-color-scheme: light\\) \\{\\s*:root \\{([^}]*)\\}");

    @Test
    public void cadaTextoSigueEnAASobreLaLuz() throws IOException {
        int temas = 0;
        for (String hoja : HOJAS) {
            String css = leer(hoja);
            Map<String, String> oscuro = variables(bloque(css));
            comprobar(hoja + " (oscuro)", oscuro);
            temas++;
            Matcher claro = CLARO.matcher(css);
            if (claro.find()) {
                Map<String, String> vars = new HashMap<>(oscuro);
                vars.putAll(variables(claro.group(1)));
                comprobar(hoja + " (claro)", vars);
                temas++;
            }
        }
        assertTrue("faltan temas: " + temas, temas >= 5);
    }

    @Test
    public void elTestVeUnaLuzDemasiadoFuerte() {
        Map<String, String> vars = new HashMap<>();
        vars.put("--fondo", "#0B0C0E");
        vars.put("--luz", "255 178 122");
        vars.put("--luz-alfa", ".5");
        vars.put("--texto", "#F2F2F3");
        vars.put("--texto-2", "#A1A9B6");
        vars.put("--enlace", "#FF6A00");
        try {
            comprobar("prueba", vars);
        } catch (AssertionError esperado) {
            return;
        }
        throw new AssertionError("una luz al 50 % debería dejar el enlace por debajo de 4,5:1");
    }

    private static void comprobar(String nombre, Map<String, String> v) {
        double[] fondo = hex(v.get("--fondo"));
        String[] luz = v.get("--luz").trim().split("\\s+");
        double alfa = Double.parseDouble(v.get("--luz-alfa"));
        double[] mezcla = new double[3];
        for (int i = 0; i < 3; i++) mezcla[i] = fondo[i] * (1 - alfa) + Double.parseDouble(luz[i]) * alfa;
        for (String t : TEXTOS) {
            double ratio = contraste(hex(v.get(t)), mezcla);
            assertTrue(String.format("%s: %s da %.2f:1 sobre la luz (α %.2f)", nombre, t, ratio, alfa), ratio >= 4.5);
        }
    }

    private static Map<String, String> variables(String bloque) {
        Map<String, String> vars = new HashMap<>();
        Matcher m = Pattern.compile("(--[a-z0-9-]+):\\s*([^;]+);").matcher(bloque);
        while (m.find()) vars.put(m.group(1), m.group(2).trim());
        return vars;
    }

    /** El primer :root de la hoja: el tema oscuro, que es el de partida. */
    private static String bloque(String css) {
        int i = css.indexOf(":root {");
        assertTrue("la hoja no tiene :root", i >= 0);
        return css.substring(i, css.indexOf('}', i));
    }

    private static double[] hex(String color) {
        assertTrue("no es un color #RRGGBB: " + color, color != null && color.matches("#[0-9A-Fa-f]{6}"));
        return new double[]{Integer.parseInt(color.substring(1, 3), 16),
                Integer.parseInt(color.substring(3, 5), 16), Integer.parseInt(color.substring(5, 7), 16)};
    }

    private static double contraste(double[] a, double[] b) {
        double la = luminancia(a);
        double lb = luminancia(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminancia(double[] rgb) {
        double[] k = {0.2126, 0.7152, 0.0722};
        double l = 0;
        for (int i = 0; i < 3; i++) {
            double v = rgb[i] / 255;
            l += k[i] * (v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4));
        }
        return l;
    }

    private static String leer(String hoja) throws IOException {
        Path p = WebDist.web().resolve("src/estilos").resolve(hoja);
        assertTrue("no existe web/src/estilos/" + hoja, Files.isRegularFile(p));
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
