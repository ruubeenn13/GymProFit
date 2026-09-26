package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import es.pmdm.gymprofit.network.ApiCallback;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Response;

// ============================================================
// UiFeedbackTest — GP-096: un 429 dice que hay que esperar, y cuánto.
//
// Antes salía el aviso genérico «Algo salió mal, inténtalo de nuevo», que invita a
// reintentar en el acto. La API manda Retry-After con la espera: ApiCallback la pone
// al principio del mensaje de onFail y UiFeedback la lee de ahí. Aquí se prueban esas
// dos mitades; el texto final sale de recursos y se comprueba en el emulador.
// ============================================================
public class UiFeedbackTest {

    private static final String CUERPO_429 =
            "{\"code\":429,\"message\":\"Demasiadas peticiones. Inténtalo de nuevo en unos segundos.\"}";

    // Lo que recibe onFail para una respuesta de la API, sin red de por medio.
    private static String[] onFailDe(int code, String retryAfter) {
        okhttp3.Response.Builder raw = new okhttp3.Response.Builder()
                .code(code).message("x").protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://localhost/api/auth/login").build());
        if (retryAfter != null) raw.header("Retry-After", retryAfter);
        Response<Void> respuesta = Response.error(
                ResponseBody.create(CUERPO_429, MediaType.get("application/json")), raw.build());

        AtomicInteger codigo = new AtomicInteger();
        AtomicReference<String> mensaje = new AtomicReference<>();
        new ApiCallback<Void>() {
            @Override public void onOk(Void body) { }
            @Override public void onFail(int c, String m) { codigo.set(c); mensaje.set(m); }
        }.onResponse(null, respuesta);
        return new String[]{String.valueOf(codigo.get()), mensaje.get()};
    }

    @Test
    public void el_429_lleva_la_espera_de_la_api_hasta_onFail() {
        String[] r = onFailDe(429, "60");
        assertEquals("429", r[0]);
        assertEquals(Integer.valueOf(60), UiFeedback.segundosDeEspera(r[1]));
    }

    @Test
    public void sin_retry_after_no_se_inventa_una_espera() {
        assertNull(UiFeedback.segundosDeEspera(onFailDe(429, null)[1]));
        assertNull(UiFeedback.segundosDeEspera(CUERPO_429));
        assertNull(UiFeedback.segundosDeEspera(null));
    }

    @Test
    public void una_fecha_en_retry_after_no_rompe_nada() {
        assertNull(UiFeedback.segundosDeEspera(onFailDe(429, "Wed, 21 Oct 2026 07:28:00 GMT")[1]));
    }

    @Test
    public void otros_errores_no_llevan_espera() {
        assertNull(UiFeedback.segundosDeEspera(onFailDe(400, "60")[1]));
    }

    @Test
    public void hasta_90_segundos_se_dice_en_segundos_y_luego_en_minutos_hacia_arriba() {
        assertNull(UiFeedback.minutosDeEspera(60));
        assertNull(UiFeedback.minutosDeEspera(90));
        assertEquals(Integer.valueOf(2), UiFeedback.minutosDeEspera(91));
        assertEquals(Integer.valueOf(15), UiFeedback.minutosDeEspera(900));
        assertEquals(Integer.valueOf(16), UiFeedback.minutosDeEspera(901));
    }
}
