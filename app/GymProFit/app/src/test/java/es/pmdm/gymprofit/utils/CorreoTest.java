package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ============================================================
// CorreoTest — GP-105: Ajustes enseña el correo enmascarado.
// ============================================================
public class CorreoTest {

    @Test
    public void deja_la_primera_letra_y_el_dominio() {
        assertEquals("r***@gmail.com", Correo.enmascarar("ruben@gmail.com"));
        assertEquals("a***@gymprofit.app", Correo.enmascarar(" a@gymprofit.app "));
    }

    @Test
    public void sin_correo_o_sin_arroba_no_se_enseña_entero() {
        assertEquals("", Correo.enmascarar(null));
        assertEquals("", Correo.enmascarar(""));
        assertEquals("x***", Correo.enmascarar("xyz"));
    }
}
