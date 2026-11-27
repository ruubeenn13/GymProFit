package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// ============================================================
// TextoQueEncogeTest — GP-285, lote 1b: el rótulo de «Hoy toca» va en una línea y encoge si no
// cabe, sin romper la pantalla.
//
// El ajuste de AppCompat lanza una excepción si el máximo no es mayor que el mínimo. A letra
// 1,0 el rótulo mide lo mismo que su mínimo (13 sp y 13 dp son los mismos píxeles): puesto en
// el layout, Inicio no llegaba a abrirse con la letra normal y solo funcionaba con letra
// grande. Por eso se activa desde código y solo con margen.
// ============================================================
public class TextoQueEncogeTest {

    private static final Path MAIN = Paths.get("src", "main");

    @Test
    public void a_letra_normal_no_hay_margen_y_no_se_activa() {
        // 13 sp a letra 1,0 y 13 dp, en un móvil de 420 ppp: 34 px los dos.
        assertFalse(TextoQueEncoge.hayMargen(34, 34));
        // Con la letra del sistema más pequeña, tampoco: el texto ya está por debajo del mínimo.
        assertFalse(TextoQueEncoge.hayMargen(29, 34));
    }

    @Test
    public void con_letra_grande_si_hay_margen() {
        // 13 sp a letra 1,3: 44 px frente a los 34 del mínimo.
        assertTrue(TextoQueEncoge.hayMargen(44, 34));
        assertTrue(TextoQueEncoge.hayMargen(35, 34));
    }

    @Test
    public void sin_minimo_no_se_activa() {
        assertFalse(TextoQueEncoge.hayMargen(44, 0));
    }

    // El layout no lleva el ajuste: ahí no se puede condicionar a que haya margen.
    @Test
    public void inicio_no_pone_el_ajuste_en_el_layout() throws IOException {
        String inicio = new String(Files.readAllBytes(MAIN.resolve("res/layout/fragment_inicio.xml")), StandardCharsets.UTF_8);
        assertFalse("autoSize en el layout rompe Inicio a letra 1,0", inicio.contains("app:autoSize"));
        String fragmento = new String(Files.readAllBytes(MAIN.resolve(
                "java/es/pmdm/gymprofit/ui/fragments/InicioFragment.java")), StandardCharsets.UTF_8);
        assertTrue(fragmento.contains("TextoQueEncoge.enUnaLinea("));
    }
}
