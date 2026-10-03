package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ============================================================
// ErrorFotoTest — el aviso al fallar la subida de la foto, por su causa (GP-188)
// ============================================================
public class ErrorFotoTest {

    @Test
    public void sin_respuesta_y_sin_red_es_sin_conexion() {
        assertEquals(ErrorFoto.Causa.SIN_CONEXION, ErrorFoto.causa(-1, false));
    }

    @Test
    public void sin_respuesta_con_red_no_es_sin_conexion() {
        // El servidor despertando: lo dice UiFeedback.
        assertEquals(ErrorFoto.Causa.OTRA, ErrorFoto.causa(-1, true));
    }

    @Test
    public void el_413_es_que_pesa_demasiado() {
        assertEquals(ErrorFoto.Causa.PESA_DEMASIADO, ErrorFoto.causa(413, true));
        assertEquals(ErrorFoto.Causa.PESA_DEMASIADO, ErrorFoto.causa(413, false));
    }

    @Test
    public void lo_demas_va_a_uifeedback() {
        assertEquals(ErrorFoto.Causa.OTRA, ErrorFoto.causa(500, true));
        assertEquals(ErrorFoto.Causa.OTRA, ErrorFoto.causa(400, true));
    }
}
