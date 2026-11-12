package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import es.pmdm.gymprofit.R;

// ============================================================
// EstadoContenidoTest — GP-064: qué enseña cada estado de view_estado.
//
// La carga es solo la rueda; el error, su icono, su texto y «Reintentar», siempre; el
// vacío, su icono y su texto, y su acción solo si la pantalla tiene algo que proponer.
// Error y vacío no se pueden confundir: ni el mismo icono ni el mismo botón.
// ============================================================
public class EstadoContenidoTest {

    @Test
    public void la_carga_es_solo_la_rueda() {
        EstadoContenido e = EstadoContenido.cargando();
        assertTrue(e.visible);
        assertTrue(e.progreso);
        assertFalse(e.icono);
        assertFalse(e.titulo);
        assertFalse(e.texto);
        assertFalse(e.reintentar);
        assertFalse(e.accion);
    }

    @Test
    public void el_error_lleva_icono_texto_y_reintentar() {
        EstadoContenido e = EstadoContenido.error();
        assertFalse(e.progreso);
        assertTrue(e.icono);
        assertTrue(e.texto);
        assertTrue(e.reintentar);
        assertFalse(e.accion);
        assertFalse(e.titulo);
        assertEquals(R.drawable.ic_ms_cloud_off, e.iconoRes);
    }

    @Test
    public void el_vacio_sin_accion_no_lleva_boton() {
        EstadoContenido e = EstadoContenido.vacio(R.drawable.ic_ms_history, false, false);
        assertTrue(e.icono);
        assertTrue(e.texto);
        assertFalse(e.accion);
        assertFalse(e.reintentar);
        assertFalse(e.titulo);
        assertFalse(e.progreso);
        assertEquals(R.drawable.ic_ms_history, e.iconoRes);
    }

    @Test
    public void el_vacio_con_accion_y_titulo_los_ensena() {
        EstadoContenido e = EstadoContenido.vacio(R.drawable.ic_ms_trophy, true, true);
        assertTrue(e.titulo);
        assertTrue(e.accion);
        // El vacío nunca enseña «Reintentar»: no hay nada que reintentar.
        assertFalse(e.reintentar);
    }

    @Test
    public void el_vacio_no_usa_el_icono_del_error() {
        // Aunque una pantalla pidiera el icono del error para su vacío, se distinguen.
        EstadoContenido v = EstadoContenido.vacio(R.drawable.ic_ms_cloud_off, false, false);
        assertNotEquals(EstadoContenido.error().iconoRes, v.iconoRes);
    }

    @Test
    public void oculto_no_ensena_nada() {
        assertFalse(EstadoContenido.oculto().visible);
    }
}
