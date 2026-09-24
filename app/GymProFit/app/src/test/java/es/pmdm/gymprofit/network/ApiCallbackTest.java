package es.pmdm.gymprofit.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// ApiCallbackTest — reconocer el 401 de cuenta desactivada (GP-083).
//
// Los cuerpos son los que devuelve la API de verdad: en JSON si el cliente manda
// Accept, y en XML si no lo manda, que es lo que hace OkHttp por defecto.
// ============================================================
public class ApiCallbackTest {

    @Test
    public void reconoce_el_codigo_en_json() {
        assertTrue(ApiCallback.esCuentaDesactivada(
                "{\"code\":401,\"message\":\"La cuenta está desactivada\",\"cause\":\"CUENTA_DESACTIVADA\"}"));
    }

    @Test
    public void reconoce_el_codigo_en_xml() {
        assertTrue(ApiCallback.esCuentaDesactivada(
                "<Response><code>401</code><message>La cuenta está desactivada</message>"
                        + "<cause>CUENTA_DESACTIVADA</cause></Response>"));
    }

    @Test
    public void un_401_de_token_caducado_no_es_cuenta_desactivada() {
        assertFalse(ApiCallback.esCuentaDesactivada(
                "{\"code\":401,\"message\":\"No autenticado\",\"cause\":null}"));
    }

    @Test
    public void sin_cuerpo_no_es_cuenta_desactivada() {
        assertFalse(ApiCallback.esCuentaDesactivada(null));
    }
}
