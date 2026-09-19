package es.pmdm.gymprofit.ui.activities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// EliminarCuentaActivityTest — la puerta del borrado de cuenta (GP-008).
//
// Solo se prueba puedeEliminar(), que es la única barrera entre un toque
// distraído y una cuenta vaciada sin vuelta atrás. Es estática y sin Android
// dentro justamente para poder probarla sin levantar la pantalla.
//
// Lo que estos tests defienden es que NADA de lo de abajo vuelva a abrir el
// botón: ni la casilla sola, ni la contraseña sola, ni unos espacios.
// ============================================================
public class EliminarCuentaActivityTest {

    @Test
    public void con_password_y_casilla_marcada_se_puede_borrar() {
        assertTrue(EliminarCuentaActivity.puedeEliminar("Contrasena1!", true));
    }

    @Test
    public void la_casilla_sin_password_no_basta() {
        assertFalse(EliminarCuentaActivity.puedeEliminar("", true));
    }

    @Test
    public void la_password_sin_casilla_no_basta() {
        // Escribir la contraseña es reflejo; marcar la casilla es la parte
        // deliberada. Sin ella no hay borrado.
        assertFalse(EliminarCuentaActivity.puedeEliminar("Contrasena1!", false));
    }

    @Test
    public void solo_espacios_no_cuenta_como_password() {
        assertFalse(EliminarCuentaActivity.puedeEliminar("    ", true));
    }

    @Test
    public void password_nula_no_revienta_ni_abre_el_boton() {
        // El campo devuelve null antes del primer tecleo si se toca a mano.
        assertFalse(EliminarCuentaActivity.puedeEliminar(null, true));
    }

    @Test
    public void ni_casilla_ni_password_es_el_estado_inicial_de_la_pantalla() {
        assertFalse(EliminarCuentaActivity.puedeEliminar("", false));
    }
}
