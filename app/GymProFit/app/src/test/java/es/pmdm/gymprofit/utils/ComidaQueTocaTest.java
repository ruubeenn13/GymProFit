package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ============================================================
// ComidaQueTocaTest — GP-105: la comida que toca por la hora, en sus bordes.
// Antes de las 10:00 desayuno; hasta las 12:59 almuerzo; hasta las 16:29 comida;
// hasta las 19:59 merienda; después, cena.
// ============================================================
public class ComidaQueTocaTest {

    @Test
    public void cada_franja_en_sus_bordes() {
        assertEquals("DESAYUNO", ComidaQueToca.segunHora(0, 0));
        assertEquals("DESAYUNO", ComidaQueToca.segunHora(9, 59));
        assertEquals("ALMUERZO", ComidaQueToca.segunHora(10, 0));
        assertEquals("ALMUERZO", ComidaQueToca.segunHora(12, 59));
        assertEquals("COMIDA", ComidaQueToca.segunHora(13, 0));
        assertEquals("COMIDA", ComidaQueToca.segunHora(16, 29));
        assertEquals("MERIENDA", ComidaQueToca.segunHora(16, 30));
        assertEquals("MERIENDA", ComidaQueToca.segunHora(19, 59));
        assertEquals("CENA", ComidaQueToca.segunHora(20, 0));
        assertEquals("CENA", ComidaQueToca.segunHora(23, 59));
    }
}
