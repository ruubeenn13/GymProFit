package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

// ============================================================
// EnumeracionTest — «Sentadilla, press de banca y remo con barra» (GP-103).
// ============================================================
public class EnumeracionTest {

    @Test
    public void uneConComasYLaConjuncionAntesDelUltimo() {
        assertEquals("lunes A, miércoles B y viernes C",
                Enumeracion.unir(Arrays.asList("lunes A", "miércoles B", "viernes C"), " y "));
        assertEquals("Monday A and Thursday B", Enumeracion.unir(Arrays.asList("Monday A", "Thursday B"), " and "));
        assertEquals("solo", Enumeracion.unir(Collections.singletonList("solo"), " y "));
        assertEquals("", Enumeracion.unir(Collections.emptyList(), " y "));
    }

    @Test
    public void enMedioDeLaFraseVaEnMinusculaSalvoLasSiglas() {
        Locale es = new Locale("es");
        assertEquals("press de banca", Enumeracion.enMedio("Press de banca", es));
        assertEquals("TRX remo", Enumeracion.enMedio("TRX remo", es));
        assertEquals("Lunes A", Enumeracion.alPrincipio("lunes A", es));
    }
}
