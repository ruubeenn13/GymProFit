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

    // Lote 1.6.1: el escáner dice cuánto esperar cuando la API se queda sin cupo para
    // leer de Open Food Facts (503 con Retry-After), como ya hacía con el 429.
    @Test
    public void un_503_lleva_su_retry_after_al_mensaje() {
        final int[] codigo = {0};
        final String[] mensaje = {null};
        ApiCallback<Void> cb = new ApiCallback<Void>() {
            @Override public void onOk(Void body) { }
            @Override public void onFail(int code, String message) { codigo[0] = code; mensaje[0] = message; }
        };
        okhttp3.Response crudo = new okhttp3.Response.Builder()
                .request(new okhttp3.Request.Builder().url("https://api.gymprofit.app/api/alimentos/codigo/1").build())
                .protocol(okhttp3.Protocol.HTTP_1_1).code(503).message("Service Unavailable")
                .header("Retry-After", "42").build();
        cb.onResponse(null, retrofit2.Response.error(okhttp3.ResponseBody.create("{}", null), crudo));
        org.junit.Assert.assertEquals(503, codigo[0]);
        org.junit.Assert.assertEquals(Integer.valueOf(42), es.pmdm.gymprofit.utils.UiFeedback.segundosDeEspera(mensaje[0]));
    }
}
