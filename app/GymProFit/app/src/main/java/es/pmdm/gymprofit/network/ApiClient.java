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
//    (POST /auth/refresh, una llamada por intento: TurnoRenovacion) y OkHttp reintenta
//    la petición. Solo un 401 de /auth/refresh cierra la sesión (DEC-059); sin
//    respuesta o 5xx, la petición falla como sin red; con 429, como un 429; los tokens
//    se quedan, y en la espera (5 s, o Retry-After) no se vuelve a llamar.
// Expone interfaces Retrofit tipadas por dominio vía service(Class), que
// deserializan JSON→POJO con Gson (etapa 2).
// ============================================================
public class ApiClient {

    // Tipo de contenido JSON para el cuerpo de la petición de refresh.
    static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Retrofit compartido y caché de servicios (interfaces tipadas por dominio).
    private static Retrofit retrofit;
    private static final Map<Class<?>, Object> SERVICIOS = new ConcurrentHashMap<>();

    // El contexto de la aplicación, para saber en qué idioma se pinta (lo pone GymProFitApp).
    @androidx.annotation.Nullable private static android.content.Context app;

    /** Lo llama GymProFitApp al arrancar el proceso. */
    public static void usarContexto(@androidx.annotation.NonNull android.content.Context contexto) {
        app = contexto.getApplicationContext();
    }

    /**
     * El idioma con el que salen ahora las peticiones (la cabecera Accept-Language): el de la app
     * (IdiomaApp.deLaApp), no el Locale por defecto del proceso. Ese lo vuelve a poner Android en
     * el del sistema cuando algo del proceso aplica la configuración del sistema (un servicio de
     * accesibilidad del propio proceso, por ejemplo), y entonces la API contestaba en español con
     * la app en inglés: «Press de banca» en «Frente a la última vez» junto a «Bench press» en el
     * récord (GP-251). Lo que se guarda en memoria con textos de la API se guarda con él
     * (CachePorIdioma). Sin contexto (antes de arrancar), el Locale por defecto.
     */
    public static String idiomaDePeticiones() {
        android.content.Context c = app;
        return c != null ? es.pmdm.gymprofit.utils.IdiomaApp.deLaApp(c) : java.util.Locale.getDefault().getLanguage();
    }

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
                // (ejercicios, logros, programas...). Es el idioma en que se pinta la app
                // (idiomaDePeticiones, GP-251): el elegido, el de la app en Android o, sin
                // ninguno, el de la lista del sistema entre español e inglés (GP-240).
                .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                        .header("Accept-Language", idiomaDePeticiones())
                        .build()))
                .addInterceptor(logging)
                .authenticator(new TokenAuthenticator(TURNO))
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

    /**
     * El turno de renovación de toda la app: el TokenAuthenticator lo usa a mitad de uso y
     * la renovación del arranque va por su mismo cerrojo.
     */
    static final TurnoRenovacion TURNO = new TurnoRenovacion(ApiClient::pedirRefresh, new TurnoRenovacion.Tokens() {
        @Override public String access() { return UtilREST.getToken(); }
        @Override public String refresh() { return UtilREST.getRefreshToken(); }
        @Override public void guardar(String access, String refresh) {
            UtilREST.onTokensRefreshed(access, refresh);
            Log.d("GymProFit", "Access token renovado vía refresh");
        }
    }, android.os.SystemClock::elapsedRealtime);

    // Ante un 401, renueva el access token con el refresh y deja que OkHttp reintente.
    static class TokenAuthenticator implements Authenticator {
        private final TurnoRenovacion turno;

        TokenAuthenticator(TurnoRenovacion turno) {
            this.turno = turno;
        }

        /**
         * @return la petición con el access nuevo, o null para que el 401 llegue tal cual.
         * @throws SesionCerradaException si /auth/refresh dijo 401: ApiCallback cierra la sesión.
         * @throws IOException            si no hubo respuesta útil: la petición falla como sin red.
         */
        @Override
        public Request authenticate(Route route, Response response) throws IOException {
            // No renovar sobre endpoints de auth (login/refresh 401 = credenciales, no expiración).
            if (esEndpointAuth(response.request())) return null;
            // Evita bucles: si ya se reintentó una vez, rendirse.
            if (contarRespuestas(response) >= 2) return null;

            // Una llamada por intento y, si falla, una espera sin llamar (GP-235).
            TurnoRenovacion.Resultado r = turno.renovar(tokenDePeticion(response.request()));
            switch (r.tipo) {
                case RENOVADA:
                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + r.token)
                            .build();
                case SIN_REFRESH:
                    // Sin refresh no hay sesión que salvar: el 401 sigue a ApiCallback.
                    return null;
                case CERRADA: {
                    // El aviso de cuenta desactivada puede venir de /auth/refresh o de la
                    // petición original (el filtro de la API la corta con un access vigente).
                    boolean desactivada = r.cuentaDesactivada || ApiCallback.esCuentaDesactivada(leerCuerpo(response));
                    // Lanzando, OkHttp no cierra la respuesta: se cierra aquí.
                    response.close();
                    throw new SesionCerradaException(desactivada);
                }
                case LIMITADA:
                    // 429 de /auth/refresh: llega a la pantalla como el 429 de GP-096.
                    response.close();
                    throw new RenovacionLimitadaException(r.segundosDeEspera);
                default:
                    response.close();
                    throw new IOException("Sin respuesta al renovar la sesión");
            }
        }
    }

    // Los segundos de un Retry-After numérico, o null (no venía, o es una fecha HTTP).
    static Integer segundosRetryAfter(String cabecera) {
        if (cabecera == null) return null;
        try {
            int s = Integer.parseInt(cabecera.trim());
            return s > 0 ? s : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // El cuerpo de la respuesta sin consumirlo, o null si no se puede leer.
    private static String leerCuerpo(Response response) {
        try {
            return response.peekBody(8192).string();
        } catch (IOException | IllegalStateException e) {
            return null;
        }
    }

    // POST /auth/refresh con un cliente aparte (sin authenticator, para no recursar). No
    // guarda nada: lo decide TurnoRenovacion con lo que devuelve.
    private static TurnoRenovacion.Respuesta pedirRefresh(String refreshToken) {
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
                    .header("Accept", "application/json")
                    // OkHttp 4.x: orden (contenido, tipo); la forma (tipo, contenido) está deprecada.
                    .post(RequestBody.create(cuerpo.toString(), JSON))
                    .build();

            try (Response resp = plano.newCall(req).execute()) {
                String texto = resp.body() != null ? resp.body().string() : null;
                String token = null, nuevoRefresh = null;
                if (resp.isSuccessful() && texto != null) {
                    try {
                        JSONObject json = new JSONObject(texto);
                        token = json.optString("token", "");
                        nuevoRefresh = json.optString("refreshToken", "");
                    } catch (org.json.JSONException e) {
                        // Cuerpo ilegible: TurnoRenovacion lo trata como sin respuesta.
                        Log.d("GymProFit", "Refresh con cuerpo ilegible");
                    }
                } else {
                    Log.d("GymProFit", "Refresh fallido → " + resp.code());
                }
                return new TurnoRenovacion.Respuesta(resp.code(), texto, token, nuevoRefresh,
                        segundosRetryAfter(resp.header("Retry-After")));
            }
        } catch (Exception e) {
            // Red que no llega o tiempo agotado: sin respuesta, no sesión cerrada.
            Log.d("GymProFit", "Refresh sin respuesta: " + e.getClass().getSimpleName());
            return TurnoRenovacion.Respuesta.sinRespuesta();
        }
    }

    // ── La renovación del arranque (GP-223) ─────────────────────────────────

    /** Lo que responde la renovación del arranque, ya en el hilo principal. */
    public interface AlRenovar {
        /**
         * @param codigo el código HTTP de /auth/refresh, o -1 si no hubo respuesta.
         * @param cuerpo el cuerpo tal cual (para ver si la cuenta está desactivada), o null.
         */
        void listo(int codigo, @androidx.annotation.Nullable String cuerpo);
    }

    /**
     * Una renovación en marcha. Cancelarla es dejar de esperarla: su respuesta ya no llega a
     * la pantalla, y si aún no se había enviado, ya no se envía.
     *
     * <p>Una petición ya enviada no se corta a propósito. El servidor puede haber rotado el
     * refresh aunque la respuesta tarde: si se cortara, el refresh nuevo se perdería, el
     * guardado seguiría siendo el viejo (ya revocado) y «Reintentar» daría un 401 de una
     * sesión que no había caducado. Dejándola acabar, los tokens nuevos se guardan igual.
     */
    public static final class Renovacion {
        private volatile boolean cancelada;

        /** Deja de esperar (a los 15 s del arranque, o al cerrar la pantalla). */
        public void cancelar() {
            cancelada = true;
        }
    }

    /**
     * Renueva la sesión al arrancar la app (GP-223), en un hilo aparte.
     *
     * <p>Va por el mismo cerrojo que el TokenAuthenticator (el de {@link #TURNO}): el refresh es de un solo uso y
     * dos renovaciones a la vez con el mismo gastarían una la de la otra, y la segunda daría
     * 401 sin que la sesión hubiera caducado. Si mientras se esperaba el cerrojo otro hilo ya
     * renovó, se da por buena sin volver a pedir.
     *
     * @param retardoDepuracionMs solo en depuración: espera antes de pedir, para ver
     *                            «Conectando…» y el error sin tocar el servidor. 0 si no.
     * @param alAcabar            se llama una vez en el hilo principal, salvo si se canceló.
     */
    @androidx.annotation.NonNull
    public static Renovacion renovarAlArrancar(long retardoDepuracionMs, @androidx.annotation.NonNull AlRenovar alAcabar) {
        Renovacion r = new Renovacion();
        android.os.Handler principal = new android.os.Handler(android.os.Looper.getMainLooper());
        String refreshAntes = UtilREST.getRefreshToken();
        Thread hilo = new Thread(() -> {
            int codigo = -1;
            String cuerpo = null;
            try {
                if (BuildConfig.DEBUG && retardoDepuracionMs > 0) Thread.sleep(retardoDepuracionMs);
                synchronized (TURNO) {
                    String refresh = UtilREST.getRefreshToken();
                    if (refresh != null && !refresh.equals(refreshAntes)) {
                        codigo = 200;
                    } else if (refresh != null && !refresh.isEmpty() && !r.cancelada) {
                        JSONObject peticion = new JSONObject();
                        peticion.put("refreshToken", refresh);
                        OkHttpClient plano = new OkHttpClient.Builder()
                                .connectTimeout(15, TimeUnit.SECONDS)
                                .readTimeout(15, TimeUnit.SECONDS)
                                .callTimeout(20, TimeUnit.SECONDS)
                                .build();
                        okhttp3.Call llamada = plano.newCall(new Request.Builder()
                                .url(BuildConfig.BASE_URL + "auth/refresh")
                                .header("Accept", "application/json")
                                .post(RequestBody.create(peticion.toString(), JSON))
                                .build());
                        try (Response resp = llamada.execute()) {
                            codigo = resp.code();
                            cuerpo = resp.body() != null ? resp.body().string() : null;
                            if (resp.isSuccessful() && cuerpo != null) {
                                JSONObject json = new JSONObject(cuerpo);
                                String token = json.optString("token", "");
                                String nuevoRefresh = json.optString("refreshToken", "");
                                if (token.isEmpty() || nuevoRefresh.isEmpty()) {
                                    codigo = -1;
                                } else {
                                    UtilREST.onTokensRefreshed(token, nuevoRefresh);
                                    Log.d("GymProFit", "Sesión renovada al arrancar");
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Red que no llega, tiempo agotado o respuesta ilegible: sin respuesta
                // útil. Lo decide DecisionArranque (código -1), no aquí.
                Log.d("GymProFit", "Renovación al arrancar sin respuesta: " + e.getClass().getSimpleName());
                codigo = -1;
            }
            int c = codigo;
            String b = cuerpo;
            principal.post(() -> {
                if (!r.cancelada) alAcabar.listo(c, b);
            });
        }, "renovar-arranque");
        hilo.start();
        return r;
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
