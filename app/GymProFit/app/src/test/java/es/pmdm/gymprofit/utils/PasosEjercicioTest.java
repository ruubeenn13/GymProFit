package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

// ============================================================
// PasosEjercicioTest — «Cómo se hace» de la ficha del ejercicio (GP-216, tablero 30): las
// instrucciones del catálogo llegan en un bloque y se enseñan como pasos numerados.
// ============================================================
public class PasosEjercicioTest {

    @Test
    public void unPasoPorLinea() {
        assertEquals(Arrays.asList("Túmbate en el banco.", "Agarra la barra."),
                PasosEjercicio.de("Túmbate en el banco.\nAgarra la barra."));
    }

    @Test
    public void unParrafoSeParteEnFrases() {
        assertEquals(Arrays.asList("Túmbate en el banco.", "Agarra la barra.", "Baja con control."),
                PasosEjercicio.de("Túmbate en el banco. Agarra la barra. Baja con control."));
    }

    @Test
    public void quitaLaNumeracionPropiaParaNoDuplicarla() {
        assertEquals(Arrays.asList("Túmbate.", "Agarra la barra.", "Baja."),
                PasosEjercicio.de("1. Túmbate.\n2) Agarra la barra.\n3 - Baja."));
    }

    @Test
    public void unDecimalNoSeConfundeConUnaFrase() {
        assertEquals(Arrays.asList("Usa una barra de 2.5 kg por lado."),
                PasosEjercicio.de("Usa una barra de 2.5 kg por lado."));
    }

    @Test
    public void lineasVaciasNoDanPasos() {
        assertEquals(Arrays.asList("Uno.", "Dos."), PasosEjercicio.de("Uno.\n\n  \nDos."));
    }

    @Test
    public void sinTextoNoHayPasos() {
        assertTrue(PasosEjercicio.de(null).isEmpty());
        assertTrue(PasosEjercicio.de("   ").isEmpty());
    }
}
