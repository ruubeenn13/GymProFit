package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// ConfirmarEliminarTest — la casilla de «Eliminar cuenta» (GP-216, tablero 21): el botón
// se habilita al marcarla, y con la contraseña vacía lo que se pide es la contraseña.
// ============================================================
public class ConfirmarEliminarTest {

    @Test
    public void sinMarcarLaCasillaElBotonEstaApagadoAunqueHayaContrasena() {
        assertFalse(ConfirmarEliminar.botonHabilitado(false, false));
        assertFalse(ConfirmarEliminar.botonHabilitado(false, true));
    }

    @Test
    public void marcadaLaCasillaSeHabilita() {
        assertTrue(ConfirmarEliminar.botonHabilitado(true, false));
    }

    @Test
    public void mientrasSeBorraNoSePuedePulsarDeNuevo() {
        assertFalse(ConfirmarEliminar.botonHabilitado(true, true));
    }

    @Test
    public void alPulsarSinContrasenaSePideLaContrasena() {
        assertEquals(ConfirmarEliminar.Paso.PEDIR_CLAVE, ConfirmarEliminar.alPulsar(true, ""));
        assertEquals(ConfirmarEliminar.Paso.PEDIR_CLAVE, ConfirmarEliminar.alPulsar(true, "   "));
        assertEquals(ConfirmarEliminar.Paso.PEDIR_CLAVE, ConfirmarEliminar.alPulsar(true, null));
    }

    @Test
    public void alPulsarConContrasenaYCasillaSeBorra() {
        assertEquals(ConfirmarEliminar.Paso.BORRAR, ConfirmarEliminar.alPulsar(true, "mi clave"));
    }

    @Test
    public void sinCasillaNuncaSeBorra() {
        assertEquals(ConfirmarEliminar.Paso.NADA, ConfirmarEliminar.alPulsar(false, "mi clave"));
    }
}
