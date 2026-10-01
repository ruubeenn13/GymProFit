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

    @Test
    public void unNombreEnMayusculasDeTituloSeQuedaComoEsta() {
        // El catálogo en inglés viene así; «barbell Bench Press» se leía roto en «Tu plan».
        Locale en = Locale.ENGLISH;
        assertEquals("Barbell Bench Press - Medium Grip", Enumeracion.enMedio("Barbell Bench Press - Medium Grip", en));
        assertEquals("Bent Over Barbell Row", Enumeracion.enMedio("Bent Over Barbell Row", en));
        assertEquals("pullups", Enumeracion.enMedio("Pullups", en));
        assertEquals("press militar de pie", Enumeracion.enMedio("Press militar de pie", new Locale("es")));
    }
}
