package es.pmdm.gymprofit.ui.activities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// CambiarCorreoActivityTest — cuándo lo escrito es otro correo (GP-083, GP-105).
//
// cambiaEmail() decide si lo escrito en Ajustes › Correo es otro correo o el mismo
// que ya tiene la cuenta; con el mismo, la pantalla lo dice en el campo en vez de
// gastar una petición. Venía de editar perfil, de donde salió el correo en GP-105.
// ============================================================
public class CambiarCorreoActivityTest {

    @Test
    public void otro_correo_es_un_cambio() {
        assertTrue(CambiarCorreoActivity.cambiaEmail("ana@test.local", "otra@test.local"));
    }

    @Test
    public void el_mismo_correo_no_es_un_cambio() {
        assertFalse(CambiarCorreoActivity.cambiaEmail("ana@test.local", "ana@test.local"));
    }

    @Test
    public void solo_mayusculas_o_espacios_no_es_un_cambio() {
        // La restricción única de la base no distingue mayúsculas: no es otro correo.
        assertFalse(CambiarCorreoActivity.cambiaEmail("ana@test.local", "  ANA@test.local "));
    }

    @Test
    public void sin_perfil_cargado_no_hay_cambio_que_pedir() {
        // Sin el correo original no se sabe si es el mismo: decide la API.
        assertFalse(CambiarCorreoActivity.cambiaEmail(null, "ana@test.local"));
    }
}
