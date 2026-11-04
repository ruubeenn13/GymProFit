package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Locale;

// ============================================================
// NombreMedidaTest — el número y el singular o plural del nombre de una fila de Medidas (GP-227).
// ============================================================
public class NombreMedidaTest {

    private static final Locale ES = new Locale("es", "ES");

    @Test
    public void elNumeroEnElIdiomaDeLaApp() {
        assertEquals("79,2", NombreMedida.numero(79.2, ES));
        assertEquals("79.2", NombreMedida.numero(79.2, Locale.US));
    }

    @Test
    public void unDecimalComoMuchoYSinComaCero() {
        assertEquals("84", NombreMedida.numero(84.0, ES));
        assertEquals("18,5", NombreMedida.numero(18.46, ES));
    }

    @Test
    public void soloUnUnoJustoEsSingular() {
        assertEquals(1, NombreMedida.cantidad(1.0));
        assertEquals(1, NombreMedida.cantidad(1.04));   // se dice «1»
        assertEquals(2, NombreMedida.cantidad(1.5));
        assertEquals(2, NombreMedida.cantidad(0.5));
        assertEquals(2, NombreMedida.cantidad(79.2));
    }
}
