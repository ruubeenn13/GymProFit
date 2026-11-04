package es.pmdm.gymprofit.utils;

import static es.pmdm.gymprofit.utils.VistaKilos.Llegada.FALLO;
import static es.pmdm.gymprofit.utils.VistaKilos.Llegada.OK;
import static es.pmdm.gymprofit.utils.VistaKilos.Llegada.PENDIENTE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import es.pmdm.gymprofit.utils.VistaKilos.Que;

// ============================================================
// VistaKilosTest — qué enseña «Kilos movidos» según el orden en que llegan las sesiones y los
// kilos (8311d31: una cuenta recién creada veía ocho barras a cero en vez de su vacío).
// ============================================================
public class VistaKilosTest {

    private static final boolean OCHO_SEMANAS = false;
    private static final boolean TODO = true;

    @Test
    public void cuentaNuevaConLasSesionesPrimeroYLosKilosDespues() {
        // Llegan las sesiones, sin ningún entreno: el vacío.
        assertEquals(Que.VACIO_CUENTA_NUEVA, VistaKilos.decidir(OCHO_SEMANAS, OK, true, PENDIENTE, false));
        // Después llegan los kilos, ocho semanas a cero: sigue el vacío (el fallo del release).
        assertEquals(Que.VACIO_CUENTA_NUEVA, VistaKilos.decidir(OCHO_SEMANAS, OK, true, OK, false));
    }

    @Test
    public void cuentaNuevaConLosKilosPrimeroYLasSesionesDespues() {
        // Llegan los kilos a cero sin saber aún si hay entrenos: no se pintan barras.
        assertEquals(Que.CARGANDO, VistaKilos.decidir(OCHO_SEMANAS, PENDIENTE, false, OK, false));
        // Llegan las sesiones, sin ningún entreno: el vacío.
        assertEquals(Que.VACIO_CUENTA_NUEVA, VistaKilos.decidir(OCHO_SEMANAS, OK, true, OK, false));
    }

    @Test
    public void conEntrenosAcabaEnLasBarrasEnLosDosOrdenes() {
        assertEquals(Que.CARGANDO, VistaKilos.decidir(OCHO_SEMANAS, OK, false, PENDIENTE, false));
        assertEquals(Que.BARRAS, VistaKilos.decidir(OCHO_SEMANAS, OK, false, OK, false));
        assertEquals(Que.CARGANDO, VistaKilos.decidir(OCHO_SEMANAS, PENDIENTE, false, OK, false));
        assertEquals(Que.BARRAS, VistaKilos.decidir(OCHO_SEMANAS, OK, false, OK, false));
    }

    @Test
    public void todoEsperaALaListaYEnseñaSuFallo() {
        assertEquals(Que.CARGANDO, VistaKilos.decidir(TODO, PENDIENTE, false, PENDIENTE, false));
        assertEquals(Que.ERROR_SESIONES, VistaKilos.decidir(TODO, FALLO, false, PENDIENTE, false));
        assertFalse("sin la lista no sabe cuántas semanas pedir", VistaKilos.pedirKilos(TODO, PENDIENTE, false));
        assertFalse(VistaKilos.pedirKilos(TODO, FALLO, false));
        assertTrue(VistaKilos.pedirKilos(TODO, OK, false));
    }

    @Test
    public void sinTodoUnFalloDeLaListaNoTapaLosKilos() {
        assertTrue("4 y 8 semanas se piden a la vez que la lista", VistaKilos.pedirKilos(OCHO_SEMANAS, PENDIENTE, false));
        assertEquals(Que.BARRAS, VistaKilos.decidir(OCHO_SEMANAS, FALLO, false, OK, false));
    }

    @Test
    public void aUnaCuentaNuevaNoSeLePidenKilos() {
        assertFalse(VistaKilos.pedirKilos(OCHO_SEMANAS, OK, true));
        assertFalse(VistaKilos.pedirKilos(TODO, OK, true));
    }

    @Test
    public void elFalloYElVacioDeLosKilos() {
        assertEquals(Que.ERROR_KILOS, VistaKilos.decidir(OCHO_SEMANAS, OK, false, FALLO, false));
        assertEquals(Que.VACIO, VistaKilos.decidir(OCHO_SEMANAS, OK, false, OK, true));
    }
}
