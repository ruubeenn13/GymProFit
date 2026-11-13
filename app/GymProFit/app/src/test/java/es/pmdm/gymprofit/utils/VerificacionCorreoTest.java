package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// VerificacionCorreoTest — GP-045, lo que decide la pantalla del código y el aviso de Perfil
//
// Qué cuenta como código para mandarlo, cuándo sale el aviso (nunca con una API que no
// dice el estado: un aviso que no se va sería peor que ninguno), y qué hace la pantalla
// con cada respuesta de la API.
// ============================================================
public class VerificacionCorreoTest {

    @Test
    public void elCodigoSonSeisDigitos() {
        assertTrue(VerificacionCorreo.codigoCompleto("012345"));
        assertTrue(VerificacionCorreo.codigoCompleto(" 012345 "));
        assertFalse(VerificacionCorreo.codigoCompleto("12345"));
        assertFalse(VerificacionCorreo.codigoCompleto("1234567"));
        assertFalse(VerificacionCorreo.codigoCompleto("12a456"));
        assertFalse(VerificacionCorreo.codigoCompleto(""));
        assertFalse(VerificacionCorreo.codigoCompleto(null));
    }

    @Test
    public void elAvisoSaleSoloSiLaApiDiceQueNoEstaVerificado() {
        assertTrue(VerificacionCorreo.mostrarAviso(Boolean.FALSE));
        assertFalse(VerificacionCorreo.mostrarAviso(Boolean.TRUE));
        // Una API sin el campo (anterior a GP-045) no dice nada: no se avisa.
        assertFalse(VerificacionCorreo.mostrarAviso(null));
    }

    @Test
    public void reenviarDistingueEnviadoYaVerificadoYFallo() {
        assertEquals(VerificacionCorreo.Reenvio.ENVIADO, VerificacionCorreo.reenvio(204));
        assertEquals(VerificacionCorreo.Reenvio.ENVIADO, VerificacionCorreo.reenvio(200));
        // 409: ya estaba verificado (otro móvil, la web). No es un error para quien lo pide.
        assertEquals(VerificacionCorreo.Reenvio.YA_VERIFICADO, VerificacionCorreo.reenvio(409));
        assertEquals(VerificacionCorreo.Reenvio.FALLO, VerificacionCorreo.reenvio(429));
        assertEquals(VerificacionCorreo.Reenvio.FALLO, VerificacionCorreo.reenvio(-1));
        assertEquals(VerificacionCorreo.Reenvio.FALLO, VerificacionCorreo.reenvio(500));
    }

    @Test
    public void verificarDistingueVerificadoCodigoMaloYFallo() {
        assertEquals(VerificacionCorreo.Canje.VERIFICADO, VerificacionCorreo.canje(200));
        // 400: código equivocado, caducado o agotado. Va al campo, no a un toast.
        assertEquals(VerificacionCorreo.Canje.CODIGO_MALO, VerificacionCorreo.canje(400));
        assertEquals(VerificacionCorreo.Canje.FALLO, VerificacionCorreo.canje(429));
        assertEquals(VerificacionCorreo.Canje.FALLO, VerificacionCorreo.canje(-1));
        assertEquals(VerificacionCorreo.Canje.FALLO, VerificacionCorreo.canje(503));
    }

    // --- Diseño nuevo: lo que la app lleva por su cuenta ---------------------
    // La API da el mismo 400 a todo fallo (DEC-066), así que la vida del código y los
    // intentos que quedan los cuenta el móvil desde que se emitió.

    private static final long MIN = 60_000L;

    @Test
    public void elCodigoCaducaALosQuinceMinutosOAlQuintoFallo() {
        long t0 = 1_000_000L;
        assertFalse(VerificacionCorreo.caducado(t0, 0, t0 + 14 * MIN));
        assertTrue(VerificacionCorreo.caducado(t0, 0, t0 + 15 * MIN));
        assertFalse(VerificacionCorreo.caducado(t0, 4, t0));
        assertTrue(VerificacionCorreo.caducado(t0, 5, t0));
        // Sin saber cuándo se mandó (desde Perfil, días después), no se promete un código vivo.
        assertTrue(VerificacionCorreo.caducado(0, 0, t0));
    }

    @Test
    public void loQueFaltaParaCaducar() {
        long t0 = 1_000_000L;
        assertEquals(15 * MIN, VerificacionCorreo.msHastaCaducar(t0, t0));
        assertEquals(MIN, VerificacionCorreo.msHastaCaducar(t0, t0 + 14 * MIN));
        assertEquals(0, VerificacionCorreo.msHastaCaducar(t0, t0 + 20 * MIN));
    }

    @Test
    public void intentosQueQuedan() {
        assertEquals(4, VerificacionCorreo.intentosRestantes(1));
        assertEquals(1, VerificacionCorreo.intentosRestantes(4));
        assertEquals(0, VerificacionCorreo.intentosRestantes(7));
    }

    @Test
    public void alPegarSeQuedanSoloLasCifras() {
        assertEquals("123456", VerificacionCorreo.soloCifras("123 456"));
        assertEquals("123456", VerificacionCorreo.soloCifras("Tu código: 123-456."));
        assertEquals("123456", VerificacionCorreo.soloCifras("1234567"));
        assertEquals("", VerificacionCorreo.soloCifras(null));
    }

    @Test
    public void pegarSoloSiHaySeisCifras() {
        assertTrue(VerificacionCorreo.seisCifras("123456"));
        assertTrue(VerificacionCorreo.seisCifras(" 123 456 "));
        assertFalse(VerificacionCorreo.seisCifras("12345"));
        assertFalse(VerificacionCorreo.seisCifras("1234567"));
        assertFalse(VerificacionCorreo.seisCifras("hola"));
        assertFalse(VerificacionCorreo.seisCifras(null));
    }

    @Test
    public void laCuentaAtrasEnMinutosYSegundos() {
        assertEquals("0:42", VerificacionCorreo.cuentaAtras(42));
        assertEquals("1:05", VerificacionCorreo.cuentaAtras(65));
        assertEquals("0:00", VerificacionCorreo.cuentaAtras(-3));
    }

    // GP-267: la respuesta al reenvío que se pide al entrar desde Perfil.
    @Test
    public void alEntrarDesdePerfil_el429EsLaCuentaAtrasYNoUnFallo() {
        assertEquals(VerificacionCorreo.Apertura.ENVIADO, VerificacionCorreo.apertura(204));
        assertEquals(VerificacionCorreo.Apertura.YA_VERIFICADO, VerificacionCorreo.apertura(409));
        assertEquals(VerificacionCorreo.Apertura.ESPERA, VerificacionCorreo.apertura(429));
        assertEquals(VerificacionCorreo.Apertura.FALLO, VerificacionCorreo.apertura(-1));
        assertEquals(VerificacionCorreo.Apertura.FALLO, VerificacionCorreo.apertura(500));
    }
}
