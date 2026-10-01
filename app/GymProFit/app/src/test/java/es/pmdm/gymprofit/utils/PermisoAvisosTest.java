package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// PermisoAvisosTest — qué hace el botón de «¿Te avisamos?» según el permiso (GP-151).
// ============================================================
public class PermisoAvisosTest {

    private static final int T = 33; // Android 13, el primero que pide el permiso
    private static final int S = 32;

    @Test
    public void concedidoSoloGuarda() {
        assertEquals(PermisoAvisos.Estado.CONCEDIDO, PermisoAvisos.estado(T, true, false, true));
        assertEquals(PermisoAvisos.Estado.CONCEDIDO, PermisoAvisos.estado(S, true, false, false));
    }

    @Test
    public void negadoUnaVezSePuedeVolverAPedir() {
        // Lo que deja la 1.4.0 tras un «No permitir»: Android explica, así que deja pedir.
        assertEquals(PermisoAvisos.Estado.PEDIBLE, PermisoAvisos.estado(T, false, true, false));
        assertEquals(PermisoAvisos.Estado.PEDIBLE, PermisoAvisos.estado(T, false, true, true));
    }

    @Test
    public void sinPedirloNuncaSePide() {
        assertEquals(PermisoAvisos.Estado.PEDIBLE, PermisoAvisos.estado(T, false, false, false));
    }

    @Test
    public void pedidoYSinExplicacionYaNoSePuedePedir() {
        assertEquals(PermisoAvisos.Estado.BLOQUEADO, PermisoAvisos.estado(T, false, false, true));
    }

    @Test
    public void antesDeAndroid13ApagadasSonCosaDeLosAjustes() {
        // No hay permiso que pedir: si están apagadas, las apagó alguien en los ajustes.
        assertEquals(PermisoAvisos.Estado.BLOQUEADO, PermisoAvisos.estado(S, false, false, false));
    }

    @Test
    public void unaRespuestaInstantaneaEsQueAndroidNoEnsenoNada() {
        assertTrue(PermisoAvisos.sinDialogo(false, false, 120));
        assertFalse("con el diálogo a la vista se tarda más", PermisoAvisos.sinDialogo(false, false, 2500));
        assertFalse("concedido no es bloqueo", PermisoAvisos.sinDialogo(true, false, 50));
        assertFalse("si aún explica, deja pedir otra vez", PermisoAvisos.sinDialogo(false, true, 50));
    }

    @Test
    public void seEnsenaUnaVez() {
        assertTrue(PermisoAvisos.tocaPreguntar(false));
        assertFalse(PermisoAvisos.tocaPreguntar(true));
    }
}
