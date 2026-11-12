package es.pmdm.gymprofit.network;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import es.pmdm.gymprofit.utils.EmbudoAlta;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

// ============================================================
// EmbudoAnonimoTest — GP-019: el aviso de un paso del alta sale sin nada de quién.
//
// Con una sesión abierta (token y refresh puestos), la petición que sale por
// ApiClient.anonimo no lleva Authorization, ni cookies, ni Accept-Language, y su cuerpo
// es solo el paso. Un interceptor al final la captura y responde 204 sin salir a la red.
// Y en la app ya no hay firebase-analytics ni el permiso del ID de publicidad.
// ============================================================
public class EmbudoAnonimoTest {

    private Request capturada;
    private String cuerpo;

    @After
    public void limpiar() {
        UtilREST.clearToken();
        UtilREST.setRefreshToken(null);
    }

    @Test
    public void el_aviso_no_lleva_token_ni_nada_mas_que_el_paso() throws IOException {
        UtilREST.setToken("token-de-una-sesion");
        UtilREST.setRefreshToken("refresh-de-una-sesion");

        OkHttpClient cliente = ApiClient.clienteAnonimo().newBuilder()
                .addInterceptor(chain -> {
                    capturada = chain.request();
                    Buffer b = new Buffer();
                    if (capturada.body() != null) capturada.body().writeTo(b);
                    cuerpo = b.readUtf8();
                    return new Response.Builder().request(capturada).protocol(Protocol.HTTP_1_1)
                            .code(204).message("No Content")
                            .body(ResponseBody.create("", MediaType.get("application/json"))).build();
                })
                .build();
        EmbudoApi api = ApiClient.retrofitAnonimo(cliente).create(EmbudoApi.class);

        assertEquals(204, api.avisar(EmbudoAlta.cuerpo(EmbudoAlta.Paso.CUENTA)).execute().code());

        assertEquals("POST", capturada.method());
        assertTrue(capturada.url().encodedPath().endsWith("/alta/pasos"));
        assertNull(capturada.header("Authorization"));
        assertNull(capturada.header("Cookie"));
        assertNull(capturada.header("Accept-Language"));
        assertEquals("{\"paso\":\"CUENTA\"}", cuerpo);
    }

    @Test
    public void el_cliente_anonimo_no_reintenta_ni_renueva_la_sesion() {
        OkHttpClient c = ApiClient.clienteAnonimo();
        assertFalse(c.retryOnConnectionFailure());
        assertEquals(okhttp3.Authenticator.NONE, c.authenticator());
        assertTrue(c.interceptors().isEmpty());
    }

    @Test
    public void sin_firebase_analytics_ni_id_de_publicidad() throws IOException {
        String gradle = new String(Files.readAllBytes(Paths.get("build.gradle")), StandardCharsets.UTF_8)
                .replaceAll("//[^\\n]*", "");
        assertFalse(gradle.contains("firebase-analytics"));
        String manifiesto = new String(Files.readAllBytes(Paths.get("src", "main", "AndroidManifest.xml")),
                StandardCharsets.UTF_8).replaceAll("(?s)<!--.*?-->", "");
        assertTrue(manifiesto.matches("(?s).*<uses-permission\\s+android:name=\"com\\.google\\.android\\.gms"
                + "\\.permission\\.AD_ID\"\\s+tools:node=\"remove\"\\s*/>.*"));
    }
}
