package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import es.pmdm.gymprofit.model.record.Record;

// ============================================================
// PrimerasMarcasTest — «N ejercicios hechos por primera vez» cuenta ejercicios, no
// marcas (GP-140). Un ejercicio nuevo puede dejar una primera marca de peso y otra de
// repeticiones, y el resumen decía «3» con dos ejercicios.
// ============================================================
public class PrimerasMarcasTest {

    @Test
    public void dos_marcas_del_mismo_ejercicio_cuentan_uno() {
        Record pesoPress = new Record(1, "Press", null, null, Record.TIPO_PESO, 60.0, 8, null);
        Record repsPress = new Record(1, "Press", null, null, Record.TIPO_REPETICIONES, null, 12, null);
        Record dominadas = new Record(2, "Dominadas", null, null, Record.TIPO_REPETICIONES, null, 10, null);

        assertEquals(2, Marcas.ejerciciosDistintos(Arrays.asList(pesoPress, repsPress, dominadas)));
    }

    @Test
    public void vacia_o_nula_es_cero() {
        assertEquals(0, Marcas.ejerciciosDistintos(Collections.emptyList()));
        assertEquals(0, Marcas.ejerciciosDistintos(null));
    }
}
