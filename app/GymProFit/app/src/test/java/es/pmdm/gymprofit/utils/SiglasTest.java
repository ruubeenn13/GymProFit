package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ============================================================
// SiglasTest — las dos letras de cada día en la tira de la semana de Entrenar y del programa
// (GP-216, tableros 14 y 31): «Torso A» es TA, «Empuje» es EM.
// ============================================================
public class SiglasTest {

    @Test
    public void dosPalabrasDanSusIniciales() {
        assertEquals("TA", Siglas.de("Torso A"));
        assertEquals("PB", Siglas.de("pierna b"));
    }

    @Test
    public void unaPalabraDaSusDosPrimerasLetras() {
        assertEquals("EM", Siglas.de("Empuje"));
        assertEquals("TI", Siglas.de("Tirón"));
    }

    @Test
    public void unNumeroCuentaComoPalabra() {
        assertEquals("D1", Siglas.de("Día 1"));
    }

    @Test
    public void masDeDosPalabrasLaPrimeraYLaUltima() {
        assertEquals("FC", Siglas.de("Full body completo"));
    }

    @Test
    public void lasRutinasDeUnMismoProgramaNoSeConfunden() {
        assertEquals("FA", Siglas.de("Full body A"));
        assertEquals("FB", Siglas.de("Full body B"));
        assertEquals("FC", Siglas.de("Full body C"));
    }

    @Test
    public void sinNombreUnGuion() {
        assertEquals("—", Siglas.de(null));
        assertEquals("—", Siglas.de("   "));
    }
}
