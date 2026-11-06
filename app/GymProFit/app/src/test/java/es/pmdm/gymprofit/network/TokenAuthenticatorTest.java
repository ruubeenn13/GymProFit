package es.pmdm.gymprofit.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;

import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

// ============================================================
// TokenAuthenticatorTest — cómo acaba en la pantalla una renovación a mitad de uso
// (GP-237, DEC-059).
//
// Antes, una renovación sin respuesta devolvía null y el 401 original llegaba a
// ApiCallback, que borraba los tokens y llevaba a Entrar. Ahora solo un 401 de
// /auth/refresh llega como sesión cerrada; un 429, como el 429 de GP-096 con su espera
// (onFail(429)); lo demás, como sin red (onFail(-1)).
// ============================================================
public class TokenAuthenticatorTest {

    private static final class TokensFalsos implements TurnoRenovacion.Tokens {
        String access = "viejo", refresh = "r1";
        @Override public String access() { return access; }
        @Override public String refresh() { return refresh; }
        @Override public void guardar(String a, String r) { access = a; refresh = r; }
    }

    private static Response un401(String cuerpo) {
        Request req = new Request.Builder().url("https://api.gymprofit.app/api/alimentos/mios")
                .header("Authorization", "Bearer viejo").build();
        return new Response.Builder().request(req).protocol(Protocol.HTTP_1_1)
                .code(401).message("Unauthorized")
                .body(ResponseBody.create(cuerpo, null)).build();
    }

    private static ApiClient.TokenAuthenticator con(TurnoRenovacion.Respuesta r, TokensFalsos t) {
        return new ApiClient.TokenAuthenticator(new TurnoRenovacion(refresh -> r, t, System::currentTimeMillis));
    }

    @After
    public void limpiar() {
        UtilREST.setOnUnauthorizedListener(null);
        UtilREST.olvidarMemoria();
    }

    @Test
    public void con_el_refresh_vivo_repite_la_peticion_con_el_access_nuevo() throws IOException {
        TokensFalsos t = new TokensFalsos();
        Request otra = con(new TurnoRenovacion.Respuesta(200, "{}", "nuevo", "r2"), t)
                .authenticate(null, un401("{}"));
        assertEquals("Bearer nuevo", otra.header("Authorization"));
    }

    @Test
    public void sin_respuesta_429_o_5xx_falla_como_sin_red_y_no_como_401() {
        int[] codigos = {-1, 429, 500, 503};
        for (int c : codigos) {
            TokensFalsos t = new TokensFalsos();
            try {
                con(new TurnoRenovacion.Respuesta(c, "{}", null, null), t).authenticate(null, un401("{}"));
                fail("código " + c + ": devolvió en vez de lanzar, y el 401 cerraba la sesión");
            } catch (SesionCerradaException e) {
                fail("código " + c + ": cerró la sesión");
            } catch (IOException e) {
                assertEquals("r1", t.refresh);
            }
        }
    }

    @Test
    public void un_401_de_refresh_cierra_la_sesion() throws IOException {
        try {
            con(new TurnoRenovacion.Respuesta(401, "{\"cause\":null}", null, null), new TokensFalsos())
                    .authenticate(null, un401("{}"));
            fail("no cerró la sesión");
        } catch (SesionCerradaException e) {
            assertFalse(e.cuentaDesactivada);
        }
    }

    @Test
    public void la_cuenta_desactivada_se_avisa_la_diga_el_refresh_o_la_peticion() throws IOException {
        try {
            con(new TurnoRenovacion.Respuesta(401, "{\"cause\":\"CUENTA_DESACTIVADA\"}", null, null), new TokensFalsos())
                    .authenticate(null, un401("{}"));
            fail("no cerró la sesión");
        } catch (SesionCerradaException e) {
            assertTrue(e.cuentaDesactivada);
        }
        try {
            con(new TurnoRenovacion.Respuesta(401, "{\"cause\":null}", null, null), new TokensFalsos())
                    .authenticate(null, un401("{\"cause\":\"CUENTA_DESACTIVADA\"}"));
            fail("no cerró la sesión");
        } catch (SesionCerradaException e) {
            assertTrue(e.cuentaDesactivada);
        }
    }

    @Test
    public void sin_refresh_guardado_deja_pasar_el_401() throws IOException {
        TokensFalsos t = new TokensFalsos();
        t.refresh = null;
        assertNull(con(new TurnoRenovacion.Respuesta(200, "{}", "nuevo", "r2"), t).authenticate(null, un401("{}")));
    }

    @Test
    public void con_429_falla_con_la_espera_de_retry_after_y_no_como_sin_red() throws IOException {
        TokensFalsos t = new TokensFalsos();
        try {
            con(new TurnoRenovacion.Respuesta(429, "{}", null, null, 30), t).authenticate(null, un401("{}"));
            fail("no lanzó");
        } catch (RenovacionLimitadaException e) {
            assertEquals(30, e.segundosDeEspera);
            assertEquals("r1", t.refresh);
        }
    }

    // ApiCallback: un 429 de /auth/refresh llega a la pantalla como el 429 de GP-096, con
    // su espera, y no como sin red.
    @Test
    public void api_callback_da_el_429_con_su_espera() {
        final int[] codigo = {0};
        final String[] mensaje = {null};
        ApiCallback<Void> cb = new ApiCallback<Void>() {
            @Override public void onOk(Void body) { }
            @Override public void onFail(int code, String message) { codigo[0] = code; mensaje[0] = message; }
        };
        cb.onFailure(null, new RenovacionLimitadaException(20));
        assertEquals(429, codigo[0]);
        assertEquals(Integer.valueOf(20), es.pmdm.gymprofit.utils.UiFeedback.segundosDeEspera(mensaje[0]));
    }

    // ApiCallback: la sesión cerrada por /auth/refresh lleva a Entrar como un 401; el
    // sin respuesta, no.
    @Test
    public void api_callback_cierra_con_sesion_cerrada_y_no_con_sin_respuesta() {
        final int[] codigo = {0};
        final Boolean[] aviso = {null};
        UtilREST.setOnUnauthorizedListener(desactivada -> aviso[0] = desactivada);
        ApiCallback<Void> cb = new ApiCallback<Void>() {
            @Override public void onOk(Void body) { }
            @Override public void onFail(int code, String message) { codigo[0] = code; }
        };

        cb.onFailure(null, new IOException("Sin respuesta al renovar la sesión"));
        assertEquals(-1, codigo[0]);
        assertNull(aviso[0]);

        cb.onFailure(null, new SesionCerradaException(true));
        assertEquals(401, codigo[0]);
        assertEquals(Boolean.TRUE, aviso[0]);
    }
}
