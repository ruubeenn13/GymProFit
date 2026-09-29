package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import es.pmdm.gymprofit.model.record.Record;

// ============================================================
// MarcasTiempoTest — las marcas por tiempo (GP-125, lote 1.2.1).
// Se escriben en minutos y segundos, y ya no pasan por marcas de peso: antes todo lo
// que no fuera de repeticiones se tomaba por kilos y la plancha salía como «0 kg».
// ============================================================
public class MarcasTiempoTest {

    @Test
    public void minutos_y_segundos() {
        assertEquals("0:45", Marcas.tiempo(45));
        assertEquals("1:05", Marcas.tiempo(65));
        assertEquals("12:00", Marcas.tiempo(720));
        assertEquals("0:00", Marcas.tiempo(-3));
    }

    @Test
    public void una_marca_por_tiempo_no_es_de_peso() {
        Record plancha = Record.deTiempo(3, "Plancha", 65, 45);
        assertTrue(plancha.esDeTiempo());
        assertFalse(plancha.esDePeso());
        assertTrue("trae su «antes»", plancha.tieneAnterior());
        assertEquals(Integer.valueOf(45), plancha.getSegundosAnterior());
        assertFalse("una primera marca no trae «antes»", Record.deTiempo(3, "Plancha", 40, null).tieneAnterior());
    }

    @Test
    public void las_de_peso_y_repeticiones_siguen_igual() {
        Record press = new Record(1, "Press", null, null, Record.TIPO_PESO, 80.0, 5, null);
        Record dominadas = new Record(2, "Dominadas", null, null, Record.TIPO_REPETICIONES, null, 12, null);
        assertTrue(press.esDePeso());
        assertFalse(press.esDeTiempo());
        assertFalse(dominadas.esDePeso());
        assertFalse(dominadas.esDeTiempo());
    }
}
