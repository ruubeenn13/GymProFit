package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * GP-225: en «Tus últimas veces», la diferencia con la sesión anterior. Con 0 kg sale el chip «—»
 * (antes no salía nada y parecía que faltaba el dato); sin anterior o por tiempo, nada.
 */
public class DiferenciaVezTest {

    @Test
    public void subirBajarEIgual() {
        assertEquals(DiferenciaVez.Que.MAS, DiferenciaVez.de(12.5, false));
        assertEquals(DiferenciaVez.Que.MENOS, DiferenciaVez.de(-5.0, false));
        assertEquals(DiferenciaVez.Que.IGUAL, DiferenciaVez.de(0.0, false));
        // Lo que se redondea a 0 kg es igual: «+0 kg» no dice nada.
        assertEquals(DiferenciaVez.Que.IGUAL, DiferenciaVez.de(0.4, false));
        assertEquals(DiferenciaVez.Que.IGUAL, DiferenciaVez.de(-0.4, false));
    }

    @Test
    public void sinAnteriorOPorTiempoNada() {
        assertEquals(DiferenciaVez.Que.NADA, DiferenciaVez.de(null, false));
        assertEquals(DiferenciaVez.Que.NADA, DiferenciaVez.de(10.0, true));
    }
}
