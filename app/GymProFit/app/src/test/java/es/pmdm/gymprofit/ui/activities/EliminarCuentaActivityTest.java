package es.pmdm.gymprofit.ui.activities;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import es.pmdm.gymprofit.utils.ConfirmarEliminar;

// ============================================================
// EliminarCuentaActivityTest — la puerta del borrado de cuenta (GP-008; GP-216, tablero 21).
//
// Desde GP-216 la casilla enciende el botón y, con la contraseña vacía, pulsar la pide en vez de
// borrar (ConfirmarEliminar, que tiene sus propios tests de la casilla). La barrera de siempre
// sigue entera: no se borra sin las DOS cosas, la casilla y la contraseña, y estos tests defienden
// que nada de lo de abajo la abra: ni la casilla sola, ni la contraseña sola, ni unos espacios.
// ============================================================
public class EliminarCuentaActivityTest {

    @Test
    public void con_password_y_casilla_marcada_se_borra() {
        assertEquals(ConfirmarEliminar.Paso.BORRAR, ConfirmarEliminar.alPulsar(true, "Contrasena1!"));
    }

    @Test
    public void la_casilla_sin_password_no_borra() {
        assertEquals(ConfirmarEliminar.Paso.PEDIR_CLAVE, ConfirmarEliminar.alPulsar(true, ""));
    }

    @Test
    public void la_password_sin_casilla_no_borra() {
        // Escribir la contraseña es reflejo; marcar la casilla es la parte deliberada. Sin ella no hay borrado.
        assertEquals(ConfirmarEliminar.Paso.NADA, ConfirmarEliminar.alPulsar(false, "Contrasena1!"));
    }

    @Test
    public void solo_espacios_no_cuenta_como_password() {
        assertEquals(ConfirmarEliminar.Paso.PEDIR_CLAVE, ConfirmarEliminar.alPulsar(true, "    "));
    }

    @Test
    public void password_nula_no_revienta_ni_borra() {
        // El campo devuelve null antes del primer tecleo si se toca a mano.
        assertEquals(ConfirmarEliminar.Paso.PEDIR_CLAVE, ConfirmarEliminar.alPulsar(true, null));
    }

    @Test
    public void ni_casilla_ni_password_es_el_estado_inicial_de_la_pantalla() {
        assertEquals(ConfirmarEliminar.Paso.NADA, ConfirmarEliminar.alPulsar(false, ""));
    }

    // --- GP-119: la contraseña se recorta como en el resto de la app (DEC-034) ---

    @Test
    public void la_contrasena_se_envia_sin_los_espacios_de_los_extremos() {
        assertEquals("una frase larga", EliminarCuentaActivity.contrasenaParaEnviar("  una frase larga "));
    }

    @Test
    public void los_espacios_de_dentro_cuentan() {
        assertEquals("una  frase", EliminarCuentaActivity.contrasenaParaEnviar("una  frase"));
    }
}
