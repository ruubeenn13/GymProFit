package es.pmdm.gymprofit.ui.activities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// EditarPerfilActivityTest — cuándo se pide la contraseña para el correo (GP-083).
//
// cambiaEmail() decide si lo escrito va por PUT /usuarios/me/email, con contraseña,
// o si el perfil se guarda sin tocar el correo. Si dijera «no cambia» cuando sí
// cambia, el correo nuevo se perdería en silencio: el PATCH ya no lo lleva.
// ============================================================
public class EditarPerfilActivityTest {

    @Test
    public void otro_correo_es_un_cambio() {
        assertTrue(EditarPerfilActivity.cambiaEmail("ana@test.local", "otra@test.local"));
    }

    @Test
    public void el_mismo_correo_no_es_un_cambio() {
        assertFalse(EditarPerfilActivity.cambiaEmail("ana@test.local", "ana@test.local"));
    }

    @Test
    public void solo_mayusculas_o_espacios_no_es_un_cambio() {
        // La restricción única de la base no distingue mayúsculas: no es otro correo.
        assertFalse(EditarPerfilActivity.cambiaEmail("ana@test.local", "  ANA@test.local "));
    }

    @Test
    public void sin_perfil_cargado_no_hay_cambio_que_pedir() {
        // Sin el correo original no se sabe si es un cambio; el campo sigue bloqueado.
        assertFalse(EditarPerfilActivity.cambiaEmail(null, "ana@test.local"));
    }
}
