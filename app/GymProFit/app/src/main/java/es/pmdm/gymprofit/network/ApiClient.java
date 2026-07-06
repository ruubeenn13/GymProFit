package es.pmdm.gymprofit.network;

import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import es.pmdm.gymprofit.BuildConfig;
import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// ============================================================
// ApiClient — motor HTTP de la app (Retrofit + OkHttp), singleton.
// Configura el OkHttpClient con:
//  - AuthInterceptor: añade el header "Authorization: Bearer <token>" a cada
//    petición (salvo las de /auth/).
//  - TokenAuthenticator: ante un 401, renueva el access token con el refresh
//    (POST /auth/refresh) y OkHttp reintenta la petición automáticamente. Si
//    la renovación falla, el 401 propaga y la app cierra sesión.
// Expone interfaces Retrofit tipadas por dominio vía service(Class), que
// deserializan JSON→POJO con Gson (etapa 2).
// ============================================================
public class ApiClient {

    // Tipo de contenido JSON para el cuerpo de la petición de refresh.
    static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Retrofit compartido y caché de servicios (interfaces tipadas por dominio).
    private static Retrofit retrofit;
    private static final Map<Class<?>, Object> SERVICIOS = new ConcurrentHashMap<>();

    // Devuelve (y cachea) una interfaz de servicio Retrofit tipada por dominio (p. ej. MedicionApi).
    @SuppressWarnings("unchecked")
    public static <T> T service(Class<T> clazz) {
        Object s = SERVICIOS.get(clazz);
        if (s == null) {
            s = retrofit().create(clazz);
            SERVICIOS.put(clazz, s);
        }
        return (T) s;
    }

    // Retrofit perezoso y compartido (mismo OkHttpClient + Gson para todos los servicios).
    private static synchronized Retrofit retrofit() {
        if (retrofit == null) {
            retrofit = crear();
        }
        return retrofit;
    }

    // Construye el OkHttpClient (interceptor + authenticator + timeouts) y el Retrofit con Gson.
    private static Retrofit crear() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // Solo loguea en debug; en release no expone URLs/cabeceras.
        logging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BASIC : HttpLoggingInterceptor.Level.NONE);

        OkHttpClient client = new OkHttpClient.Builder()
                // connect corto: falla rápido si el server está caído de verdad.
                .connectTimeout(15, TimeUnit.SECONDS)
                // read/call largos: Render (free) hace cold-start ~40s al despertar;
                // la 1ª petición tras dormir debe esperar la respuesta, no dar SocketTimeout.
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor())
                // Accept-Language: el backend localiza con él los textos del catálogo
                // (ejercicios, logros...). Locale.getDefault() ya refleja el idioma elegido
                // en la app (aplicarIdioma hace Locale.setDefault al arrancar cada pantalla).
                .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                        .header("Accept-Language", java.util.Locale.getDefault().getLanguage())
                        .build()))
                .addInterceptor(logging)
                .authenticator(new TokenAuthenticator())
                .build();

        // Gson: las interfaces tipadas por dominio deserializan JSON→POJO con este converter.
        // serializeNulls: permite enviar {"campo": null} en cuerpos Map para BORRAR un
        // campo en un PATCH (equivalente al antiguo JSONObject.NULL). Omitir la clave = sin cambio.
        Gson gson = new GsonBuilder().serializeNulls().create();

        return new Retrofit.Builder()
                // baseUrl para los paths relativos de las interfaces tipadas por dominio.
                .baseUrl(BuildConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    // Añade el token Bearer a las peticiones autenticadas (todas menos las de /auth/).
    static class AuthInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            String tok = UtilREST.getToken();
            if (tok != null && !tok.isEmpty() && !esEndpointAuth(original)) {
                original = original.newBuilder()
                        .header("Authorization", "Bearer " + tok)
                        .build();
            }
            return chain.proceed(original);
        }
    }

    // Ante un 401, renueva el access token con el refresh y deja que OkHttp reintente.
    static class TokenAuthenticator implements Authenticator {
        @Override
        public Request authenticate(Route route, Response response) {
            // No renovar sobre endpoints de auth (login/refresh 401 = credenciales, no expiración).
            if (esEndpointAuth(response.request())) return null;
            // Evita bucles: si ya se reintentó una vez, rendirse.
            if (contarRespuestas(response) >= 2) return null;

            String refresh = UtilREST.getRefreshToken();
            if (refresh == null || refresh.isEmpty()) return null;

            synchronized (TokenAuthenticator.class) {
                // Si otro hilo ya renovó (el token cambió respecto al que llevaba la petición), reintentar con ese.
                String actual = UtilREST.getToken();
                String usado = tokenDePeticion(response.request());
                if (actual != null && !actual.equals(usado)) {
                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + actual)
                            .build();
                }

                String nuevoToken = renovar(refresh);
                if (nuevoToken == null) return null;

                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + nuevoToken)
                        .build();
            }
        }
    }

    // Llama a POST /auth/refresh con un cliente aparte (sin authenticator, para no recursar).
    // Si va bien, actualiza y persiste los tokens en UtilREST y devuelve el nuevo access token.
    private static String renovar(String refreshToken) {
        OkHttpClient plano = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                // Mismo margen para el cold-start de Render en el refresh.
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .build();
        try {
            JSONObject cuerpo = new JSONObject();
            cuerpo.put("refreshToken", refreshToken);
            Request req = new Request.Builder()
                    .url(BuildConfig.BASE_URL + "auth/refresh")
                    .post(RequestBody.create(JSON, cuerpo.toString()))
                    .build();

            try (Response resp = plano.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    Log.d("GymProFit", "Refresh fallido → " + resp.code());
                    return null;
                }
                JSONObject json = new JSONObject(resp.body().string());
                String nuevoToken = json.optString("token", "");
                String nuevoRefresh = json.optString("refreshToken", "");
                if (nuevoToken.isEmpty() || nuevoRefresh.isEmpty()) return null;

                UtilREST.onTokensRefreshed(nuevoToken, nuevoRefresh);
                Log.d("GymProFit", "Access token renovado vía refresh");
                return nuevoToken;
            }
        } catch (Exception e) {
            Log.e("GymProFit", "Refresh exception: " + e.getMessage());
            return null;
        }
    }

    // ¿La URL es de autenticación (/auth/...)? Sobre ellas no se inyecta token ni se refresca.
    private static boolean esEndpointAuth(Request request) {
        return request.url().encodedPath().contains("/auth/");
    }

    // Extrae el token que llevaba una petición (para detectar si ya fue renovado por otro hilo).
    private static String tokenDePeticion(Request request) {
        String h = request.header("Authorization");
        return h != null && h.startsWith("Bearer ") ? h.substring(7) : null;
    }

    // Cuenta cuántas respuestas encadenadas lleva (para no reintentar en bucle).
    private static int contarRespuestas(Response response) {
        int n = 1;
        while ((response = response.priorResponse()) != null) n++;
        return n;
    }
}
