package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ============================================================
// CuandoTocaRutinaTest — «cuándo toca» cada rutina del programa en Entrenar (GP-216, tablero 14).
// Los programas son cíclicos y la API manda las rutinas en el orden en que tocan desde hoy: la
// primera es la de hoy, la segunda la siguiente y a partir de ahí, «en N entrenos». No hay
// «mañana» ni «el jueves»: nadie sabe qué días entrenará la persona.
// ============================================================
public class CuandoTocaRutinaTest {

    @Test
    public void laPrimeraEsLaDeHoy() {
        CuandoTocaRutina c = CuandoTocaRutina.de(0);
        assertEquals(CuandoTocaRutina.Tipo.HOY, c.tipo);
    }

    @Test
    public void laSegundaEsLaSiguiente() {
        assertEquals(CuandoTocaRutina.Tipo.SIGUIENTE, CuandoTocaRutina.de(1).tipo);
    }

    @Test
    public void despuesEsEnNEntrenos() {
        CuandoTocaRutina c = CuandoTocaRutina.de(3);
        assertEquals(CuandoTocaRutina.Tipo.EN_N, c.tipo);
        assertEquals(3, c.n);
    }

    @Test
    public void unIndiceNegativoSeTrataComoHoy() {
        assertEquals(CuandoTocaRutina.Tipo.HOY, CuandoTocaRutina.de(-2).tipo);
    }
}
