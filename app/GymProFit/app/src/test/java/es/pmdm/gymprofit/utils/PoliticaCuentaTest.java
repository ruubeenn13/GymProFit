package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// PoliticaCuentaTest — GP-095: la app acepta exactamente lo que acepta la API.
//
// El registro solo exigía 6 caracteres y la API (RegisterDTO) pide de 8 a 100 con
// minúscula, mayúscula, dígito y símbolo: «gymprofit1» pasaba en la app, la API la
// rechazaba con 400 y el usuario veía «Error al crear la cuenta» sin saber por qué.
// Los casos de aquí son los que la API acepta y rechaza; si uno cambia de lado, la
// app y la API vuelven a discrepar.
// ============================================================
public class PoliticaCuentaTest {

    // ---------- Contraseña ----------

    @Test
    public void la_contrasena_del_fallo_se_rechaza() {
        // Sin mayúscula ni símbolo: la que dio «Error al crear la cuenta».
        assertFalse(PoliticaCuenta.passwordValida("gymprofit1"));
        // Con mayúscula pero sin símbolo: tampoco la acepta la API.
        assertFalse(PoliticaCuenta.passwordValida("Gymprofit1"));
    }

    @Test
    public void le_falta_una_clase_de_caracter() {
        assertFalse("sin minúscula", PoliticaCuenta.passwordValida("GYMPROFIT1!"));
        assertFalse("sin mayúscula", PoliticaCuenta.passwordValida("gymprofit1!"));
        assertFalse("sin dígito", PoliticaCuenta.passwordValida("Gymprofit!!"));
        assertFalse("sin símbolo", PoliticaCuenta.passwordValida("Gymprofit12"));
    }

    @Test
    public void longitud_de_8_a_100() {
        assertFalse("7", PoliticaCuenta.passwordValida("Gym1.ab"));
        assertTrue("8", PoliticaCuenta.passwordValida("Gym1.abc"));
        assertTrue("100", PoliticaCuenta.passwordValida("Gym1." + "a".repeat(95)));
        assertFalse("101", PoliticaCuenta.passwordValida("Gym1." + "a".repeat(96)));
    }

    @Test
    public void se_aceptan_las_normales() {
        assertTrue(PoliticaCuenta.passwordValida("Gymprofit1!"));
        assertTrue(PoliticaCuenta.passwordValida("Prueba1234."));
        // El espacio y la ñ cuentan como símbolo: no son [A-Za-z0-9], igual que en la API.
        assertTrue(PoliticaCuenta.passwordValida("Gym profit1"));
        assertTrue(PoliticaCuenta.passwordValida("Contraseña1"));
    }

    @Test
    public void el_digito_es_ascii_como_en_la_api() {
        // En Android \d también casa con dígitos de otras escrituras; en la API no.
        // «١» (dígito árabe) no es [0-9]: sin otro dígito, la API la rechaza.
        assertFalse(PoliticaCuenta.passwordValida("Gymprofit\u0661!"));
    }

    @Test
    public void nula_o_vacia_se_rechaza() {
        assertFalse(PoliticaCuenta.passwordValida(null));
        assertFalse(PoliticaCuenta.passwordValida(""));
    }

    @Test
    public void un_salto_de_linea_no_pasa() {
        // El «.+» de la API no casa con saltos de línea.
        assertFalse(PoliticaCuenta.passwordValida("Gym1.abc\ndef"));
    }

    // ---------- Usuario y correo ----------

    @Test
    public void usuario_de_3_a_50() {
        assertFalse(PoliticaCuenta.usuarioValido("ab"));
        assertTrue(PoliticaCuenta.usuarioValido("abc"));
        assertTrue(PoliticaCuenta.usuarioValido("a".repeat(50)));
        assertFalse(PoliticaCuenta.usuarioValido("a".repeat(51)));
        assertFalse(PoliticaCuenta.usuarioValido(null));
        // Espacio y tilde: la API no los prohíbe.
        assertTrue(PoliticaCuenta.usuarioValido("José Pérez"));
    }

    @Test
    public void correo_hasta_100() {
        String dominio = "@gymprofit.app"; // 14
        assertTrue(PoliticaCuenta.correoLongitudValida("a".repeat(86) + dominio));
        assertFalse(PoliticaCuenta.correoLongitudValida("a".repeat(87) + dominio));
        assertFalse(PoliticaCuenta.correoLongitudValida(null));
    }

    // ---------- Código de la API ----------

    @Test
    public void campo_en_uso_por_el_codigo() {
        assertEquals(PoliticaCuenta.CampoEnUso.USUARIO, PoliticaCuenta.campoEnUso(
                "{\"code\":400,\"message\":\"El username 'x' ya está en uso\",\"cause\":\"USERNAME_EN_USO\"}"));
        assertEquals(PoliticaCuenta.CampoEnUso.CORREO, PoliticaCuenta.campoEnUso(
                "<Response><code>400</code><cause>EMAIL_EN_USO</cause></Response>"));
    }

    @Test
    public void sin_codigo_no_se_adivina() {
        // Una API sin desplegar manda el mismo 400 sin código: no se sabe cuál es.
        assertEquals(PoliticaCuenta.CampoEnUso.DESCONOCIDO, PoliticaCuenta.campoEnUso(
                "{\"code\":400,\"message\":\"El username 'x' ya está en uso\",\"cause\":null}"));
        assertEquals(PoliticaCuenta.CampoEnUso.DESCONOCIDO, PoliticaCuenta.campoEnUso(null));
    }
}
