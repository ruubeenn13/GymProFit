package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
// MovimientoWebTest — la gramática de movimiento de la web (GP-205, web/DISENO.md).
//
// Nada se mueve sin parar (el pulso del botón y la luz que respiraba eran
// «movimiento de feria», lo que el propietario no quiere) y ninguna curva rebota:
// una cubic-bezier con un punto de control fuera de [0, 1] en el eje del tiempo
// de progreso se pasa de largo y vuelve. Y todas las animaciones viven dentro de
// prefers-reduced-motion: no-preference, para que con movimiento reducido la
// página esté quieta.
// ============================================================
public class MovimientoWebTest {

    private static final String[] HOJAS = {"portada.css", "beta.css", "estilo.css"};

    @Test
    public void nadaSeMueveSinParar() throws IOException {
        for (String hoja : HOJAS) {
            assertFalse(hoja + " tiene una animación infinita", leer(hoja).contains("infinite"));
        }
    }

    @Test
    public void ningunaCurvaRebota() throws IOException {
        Pattern curva = Pattern.compile("cubic-bezier\\(\\s*([-\\d.]+)\\s*,\\s*([-\\d.]+)\\s*,\\s*([-\\d.]+)\\s*,\\s*([-\\d.]+)\\s*\\)");
        int curvas = 0;
        for (String hoja : HOJAS) {
            Matcher m = curva.matcher(leer(hoja));
            while (m.find()) {
                curvas++;
                for (int y : new int[]{2, 4}) {
                    double valor = Double.parseDouble(m.group(y));
                    assertTrue(hoja + ": la curva " + m.group() + " rebota", valor >= 0 && valor <= 1);
                }
            }
        }
        assertTrue("no se ha encontrado ninguna curva", curvas >= 4);
    }

    @Test
    public void cadaAnimacionVaDentroDeMovimientoSinReducir() throws IOException {
        for (String hoja : new String[]{"portada.css", "beta.css"}) {
            String css = leer(hoja);
            // Lo que queda fuera del bloque de movimiento no puede animar nada
            String fuera = css;
            Matcher bloque = Pattern.compile("@media \\(prefers-reduced-motion: no-preference\\) \\{").matcher(css);
            while (bloque.find()) {
                int fin = cierre(css, bloque.end() - 1);
                fuera = fuera.replace(css.substring(bloque.start(), fin + 1), "");
            }
            assertTrue(hoja + " no tiene bloque de movimiento", !fuera.equals(css));
            assertFalse(hoja + " anima algo fuera de prefers-reduced-motion: no-preference",
                    Pattern.compile("(?<![-\\w])animation(-name)?\\s*:").matcher(fuera).find());
        }
    }

    // El índice de la llave que cierra la que está en «abre».
    private static int cierre(String css, int abre) {
        int nivel = 0;
        for (int i = abre; i < css.length(); i++) {
            if (css.charAt(i) == '{') nivel++;
            if (css.charAt(i) == '}' && --nivel == 0) return i;
        }
        throw new AssertionError("llave sin cerrar");
    }

    private static String leer(String nombre) throws IOException {
        Path raiz = Paths.get("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("web"))) raiz = raiz.getParent();
        assertNotNull("no se encuentra la carpeta web", raiz);
        return new String(Files.readAllBytes(raiz.resolve("web").resolve(nombre)), StandardCharsets.UTF_8);
    }
}
