package es.pmdm.gymprofit.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import es.pmdm.gymprofit.arranque.DecisionArranque;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

// ============================================================
// SinSesionTest — sin sesión guardada, «Tu sesión ha terminado» no sale nunca (GP-258).
//
// En dos runs de app-release-r8 la release recién instalada abrió Entrar con ese aviso. La
// cadena: MainActivity, abierta sin pasar por el arranque (sin la categoría LAUNCHER, o
// recreada por un cambio de configuración mientras decidía), montaba Inicio sin sesión;
// Inicio pedía sus datos sin token, la API decía 401 y ApiCallback lo trataba como una sesión
// que se acaba. El mismo 401 sale de una contraseña mal escrita en Entrar.
//
// Tres reglas, cada una con su prueba:
//   1. Un 401 solo cierra la sesión si la petición llevaba una (DEC-059 no cambia para ella).
//   2. Una petición que necesita sesión no sale sin token ni refresh.
//   3. MainActivity sin sesión pasa siempre por el arranque.
// ============================================================
public class SinSesionTest {

    private static final String BASE = "https://api.gymprofit.app/api/";

    private int avisos;

    @Before
    public void preparar() {
        UtilREST.setAlmacen(null);
        UtilREST.olvidarMemoria();
        avisos = 0;
        UtilREST.setOnUnauthorizedListener(desactivada -> avisos++);
    }

    @After
    public void limpiar() {
        UtilREST.setOnUnauthorizedListener(null);
        UtilREST.setAlmacen(null);
        UtilREST.olvidarMemoria();
    }

    private static Request peticion(String ruta, String token) {
        Request.Builder b = new Request.Builder().url(BASE + ruta);
        if (token != null) b.header("Authorization", "Bearer " + token);
        return b.build();
    }

    private static retrofit2.Response<Void> un401(Request req) {
        Response crudo = new Response.Builder().request(req).protocol(Protocol.HTTP_1_1)
                .code(401).message("Unauthorized").build();
        return retrofit2.Response.error(ResponseBody.create("{\"code\":401}", null), crudo);
    }

    private static final class Fallo extends ApiCallback<Void> {
        int codigo;
        @Override public void onOk(Void body) { fail("no era un éxito"); }
        @Override public void onFail(int code, String message) { codigo = code; }
    }

    // ── 1. El 401 ────────────────────────────────────────────────────────────

    @Test
    public void un_401_a_una_peticion_sin_token_no_avisa_de_sesion_terminada() {
        // Inicio montado sin sesión, o la contraseña mal escrita en Entrar (POST /auth/login).
        for (String ruta : new String[]{"usuarios/3", "auth/login"}) {
            Fallo cb = new Fallo();
            cb.onResponse(null, un401(peticion(ruta, null)));
            assertEquals(ruta + ": la pantalla sigue recibiendo su 401", 401, cb.codigo);
        }
        assertEquals("sin sesión no hay sesión que se termine", 0, avisos);
    }

    @Test
    public void un_401_a_una_peticion_con_token_sigue_cerrando_la_sesion() {
        // DEC-059 tal cual: la sesión real que caduca o se revoca lleva a Entrar con el aviso.
        UtilREST.setToken("access");
        Fallo cb = new Fallo();
        cb.onResponse(null, un401(peticion("usuarios/3", "access")));
        assertEquals(401, cb.codigo);
        assertEquals(1, avisos);
        assertNull("y la sesión se borra", UtilREST.getToken());
    }

    // ── 2. Sin token no sale ─────────────────────────────────────────────────

    /** Lo que llega al servidor tras el AuthInterceptor, o null si no salió nada. */
    private static Request loQueSale(Request req) throws IOException {
        final Request[] salio = {null};
        OkHttpClient cliente = new OkHttpClient.Builder()
                .addInterceptor(new ApiClient.AuthInterceptor())
                .addInterceptor(chain -> {
                    salio[0] = chain.request();
                    return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                            .code(200).message("OK").body(ResponseBody.create("{}", null)).build();
                })
                .build();
        cliente.newCall(req).execute().close();
        return salio[0];
    }

    @Test
    public void sin_sesion_una_peticion_que_la_necesita_no_sale() {
        for (String ruta : new String[]{"usuarios/3", "sesiones/historial", "programas/seguido",
                "notificaciones/token", "auth/change-password"}) {
            try {
                Request salio = loQueSale(peticion(ruta, null));
                fail(ruta + ": salió sin token (" + salio.url() + ") y la API diría 401");
            } catch (IOException e) {
                // Falla en el móvil, como un 401 que no avisa (regla 1).
                Fallo cb = new Fallo();
                cb.onFailure(null, e);
                assertEquals(ruta, 401, cb.codigo);
            }
        }
        assertEquals(0, avisos);
    }

    @Test
    public void sin_sesion_las_rutas_publicas_salen() throws IOException {
        // Las que la API deja sin token (SecurityConfig) y la app usa antes de entrar.
        for (String ruta : new String[]{"auth/login", "auth/register", "auth/forgot-password",
                "auth/reset-password", "programas/recomendado", "programas/fuerza-3/vista-previa",
                "actuator/health"}) {
            Request salio = loQueSale(peticion(ruta, null));
            assertNotNull(ruta, salio);
            assertNull(ruta + ": sin token", salio.header("Authorization"));
        }
    }

    @Test
    public void con_solo_el_refresh_sale_y_la_renueva_el_authenticator() throws IOException {
        // El access se perdió pero el refresh sigue: es una sesión, y el 401 la renueva.
        UtilREST.setRefreshToken("r1");
        assertNotNull(loQueSale(peticion("usuarios/3", null)));
    }

    @Test
    public void cambiar_la_contrasena_lleva_el_token() throws IOException {
        // /auth/change-password es la única de /auth/ que pide sesión (SecurityConfig).
        UtilREST.setToken("access");
        assertEquals("Bearer access", loQueSale(peticion("auth/change-password", null)).header("Authorization"));
        assertNull(loQueSale(peticion("auth/login", null)).header("Authorization"));
    }

    // ── 3. MainActivity sin sesión pasa por el arranque ──────────────────────

    @Test
    public void sin_sesion_main_activity_siempre_decide_por_el_arranque() {
        // Abierta por el icono, sin la categoría LAUNCHER o recreada: sin sesión, el arranque.
        assertTrue(DecisionArranque.pasaPorElArranque(true, false));
        assertTrue(DecisionArranque.pasaPorElArranque(false, false));
        // Con sesión, como antes: el icono decide; lo demás, las pestañas.
        assertTrue(DecisionArranque.pasaPorElArranque(true, true));
        assertFalse(DecisionArranque.pasaPorElArranque(false, true));
    }
}
