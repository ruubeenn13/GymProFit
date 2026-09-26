package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// PoliticaCuentaTest — la app acepta exactamente lo que acepta la API.
//
// GP-095 la creó porque la app daba por buenas contraseñas que la API rechazaba.
// GP-101 cambia la regla (DEC-034): mínimo 8 caracteres contados como caracteres,
// máximo 72 bytes en UTF-8 y sin reglas de composición. Los casos son los mismos que
// PoliticaContrasenaTest en la API; si uno cambia de lado, vuelven a discrepar. La
// lista de bloqueo y el nombre solo los sabe la API: aquí se prueba que la app lee
// sus códigos.
// ============================================================
public class PoliticaCuentaTest {

    // ---------- Contraseña ----------

    @Test
    public void siete_caracteres_es_corta() {
        assertEquals(PoliticaCuenta.ProblemaPassword.CORTA, PoliticaCuenta.problemaPassword("zqxmplo"));
    }

    @Test
    public void ocho_minusculas_sin_nada_mas_vale() {
        assertNull(PoliticaCuenta.problemaPassword("zqxmplok"));
        // La que tumbó el registro en GP-095 ya vale en forma (la API la juzga por la lista).
        assertTrue(PoliticaCuenta.passwordValida("gymprofit1"));
    }

    @Test
    public void una_frase_con_espacios_vale() {
        assertNull(PoliticaCuenta.problemaPassword("tren de lavar"));
    }

    @Test
    public void se_cuentan_caracteres_no_unidades_utf16() {
        // 7 emojis son 14 unidades UTF-16: con length() pasaría.
        assertEquals(PoliticaCuenta.ProblemaPassword.CORTA,
                PoliticaCuenta.problemaPassword("🏋".repeat(7)));
        assertNull(PoliticaCuenta.problemaPassword("ñañañaña"));
    }

    @Test
    public void hasta_72_bytes_en_utf8() {
        assertNull(PoliticaCuenta.problemaPassword("€".repeat(24)));   // 72 bytes
        assertEquals(PoliticaCuenta.ProblemaPassword.LARGA,
                PoliticaCuenta.problemaPassword("€".repeat(25)));      // 75 bytes, 25 caracteres
        assertNull(PoliticaCuenta.problemaPassword("a".repeat(72)));
        assertEquals(PoliticaCuenta.ProblemaPassword.LARGA,
                PoliticaCuenta.problemaPassword("a".repeat(73)));
    }

    @Test
    public void nula_o_vacia_es_corta() {
        assertFalse(PoliticaCuenta.passwordValida(null));
        assertFalse(PoliticaCuenta.passwordValida(""));
    }

    @Test
    public void rechazo_por_el_codigo_de_la_api() {
        assertEquals(PoliticaCuenta.RechazoPassword.COMUN, PoliticaCuenta.rechazoPassword(
                "{\"code\":400,\"message\":\"Esa contraseña es demasiado común\",\"cause\":\"PASSWORD_COMUN\"}"));
        assertEquals(PoliticaCuenta.RechazoPassword.CONTIENE_NOMBRE, PoliticaCuenta.rechazoPassword(
                "{\"code\":400,\"cause\":\"PASSWORD_CONTIENE_NOMBRE\"}"));
        assertEquals(PoliticaCuenta.RechazoPassword.NINGUNO, PoliticaCuenta.rechazoPassword(
                "{\"code\":400,\"cause\":\"USERNAME_EN_USO\"}"));
        assertEquals(PoliticaCuenta.RechazoPassword.NINGUNO, PoliticaCuenta.rechazoPassword(null));
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
