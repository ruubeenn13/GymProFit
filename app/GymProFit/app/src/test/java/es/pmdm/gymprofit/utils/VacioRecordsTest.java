package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * GP-229: Récords sin récords dice una cosa sin entrenos y otra con ellos. La primera vez que se
 * hace un ejercicio es el punto de partida, no un récord.
 */
public class VacioRecordsTest {

    @Test
    public void sinEntrenos() {
        assertEquals(VacioRecords.Que.SIN_ENTRENOS, VacioRecords.de(true, false, 0));
    }

    @Test
    public void conEntrenosYSinRecords() {
        assertEquals(VacioRecords.Que.CON_ENTRENOS, VacioRecords.de(true, false, 1));
        assertEquals(VacioRecords.Que.CON_ENTRENOS, VacioRecords.de(true, false, 12));
    }

    @Test
    public void sinLaListaDeSesionesSeEspera() {
        assertEquals(VacioRecords.Que.CARGANDO, VacioRecords.de(false, false, 0));
    }

    // Sin poder saber si hay entrenos, el texto que vale en los dos casos.
    @Test
    public void conLaListaCaidaElTextoQueValeSiempre() {
        assertEquals(VacioRecords.Que.SIN_SABER, VacioRecords.de(true, true, 0));
    }
}
