package es.pmdm.gymprofit.network;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// ============================================================
// ApiCallback<T> — callback tipado para las interfaces Retrofit de la etapa 2.
// Envuelve el Callback<T> de Retrofit y entrega directamente el POJO ya
// deserializado (onOk), sin parseo manual. Retrofit invoca estos métodos en el
// hilo principal, así que no hace falta runOnUiThread. Centraliza el manejo del
// 401 no recuperable (limpia sesión + avisa para volver a login).
//
// Uso típico en una Activity:
//   api.getOrdenadas(id).enqueue(new ApiCallback<List<MedicionCorporal>>() {
//       public void onOk(List<MedicionCorporal> lista) { ...pintar UI... }
//       public void onFail(int code, String msg) { ...toast de error... }   // obligatorio
//   });
// ============================================================
public abstract class ApiCallback<T> implements Callback<T> {

    /**
     * Prefijo con el que viaja en el mensaje de un 429 la espera que pide la API en
     * Retry-After (GP-096). onFail solo lleva código y texto, y cambiar su firma tocaría
     * todas las pantallas; UiFeedback lo lee de ahí para decir cuánto esperar.
     */
    public static final String PREFIJO_RETRY_AFTER = "Retry-After=";

    // Código que manda la API en "cause" cuando la cuenta está desactivada (GP-083).
    static final String CODIGO_CUENTA_DESACTIVADA = "CUENTA_DESACTIVADA";

    // Éxito (2xx): recibe el cuerpo ya deserializado (puede ser null en 204 No Content).
    public abstract void onOk(T body);

    // Error de red o respuesta no-2xx (401 ya gestionado aparte). Abstracto a propósito
    // (GP-017): antes, por defecto, solo escribía en el log, y un fallo sin onFail propio
    // era un fallo mudo. Cada llamada decide: mostrarlo, reintentar o ignorarlo con un
    // comentario que diga por qué es seguro (OnFailVacioTest vigila lo último).
    public abstract void onFail(int code, String message);

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        int code = response.code();
        if (code == 401) {
            // Un 401 que el TokenAuthenticator deja pasar: sin refresh guardado, o el
            // reintento con el access recién renovado también dio 401.
            // Si la API dice que la cuenta está desactivada (GP-083), el aviso lo explica.
            // Solo cierra la sesión si la petición llevaba una (GP-258): el 401 de una
            // contraseña mal escrita en Entrar, o el de una petición sin token, no es una
            // sesión que se acaba, y abría Entrar con «Tu sesión ha terminado».
            if (llevabaSesion(response)) {
                UtilREST.notifyUnauthorized(esCuentaDesactivada(leerError(response)));
            }
            onFail(401, "Sesión expirada");
        } else if (response.isSuccessful()) {
            onOk(response.body());
        } else if (code == 429 || code == 503) {
            // 503 con Retry-After: sin cupo para leer de Open Food Facts (lote 1.6.1).
            String espera = response.headers().get("Retry-After");
            onFail(code, (espera != null ? PREFIJO_RETRY_AFTER + espera.trim() + ";" : "") + leerError(response));
        } else {
            onFail(code, leerError(response));
        }
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        if (t instanceof SesionCerradaException) {
            // /auth/refresh dijo 401 (DEC-059): el mismo cierre que el 401 de arriba.
            UtilREST.notifyUnauthorized(((SesionCerradaException) t).cuentaDesactivada);
            onFail(401, "Sesión expirada");
            return;
        }
        if (t instanceof SinSesionException) {
            // No salió: pedía sesión y no la hay (GP-258). Como un 401, sin sesión que cerrar.
            onFail(401, t.getMessage());
            return;
        }
        if (t instanceof RenovacionLimitadaException) {
            // /auth/refresh dijo 429, o sigue su espera: el aviso de GP-096 con lo que
            // falta, no el de sin red. La sesión sigue.
            onFail(429, PREFIJO_RETRY_AFTER + ((RenovacionLimitadaException) t).segundosDeEspera + ";"
                    + t.getMessage());
            return;
        }
        // Sin red, tiempo agotado o una renovación sin respuesta (5xx): la sesión sigue y,
        // pasados 5 s, la siguiente acción vuelve a intentar (GP-237).
        onFail(-1, t != null ? t.getMessage() : "Error de red");
    }

    /**
     * Dice si un cuerpo de error 401 es el de una cuenta desactivada (GP-083).
     *
     * <p>Busca el código estable de la API y no el texto, que puede cambiar. Tampoco
     * parsea: sin cabecera Accept, la API puede responder el error en XML en vez de JSON,
     * y el código aparece igual en los dos.
     *
     * @param cuerpo cuerpo de error tal cual, o null.
     */
    static boolean esCuentaDesactivada(String cuerpo) {
        return cuerpo != null && cuerpo.contains(CODIGO_CUENTA_DESACTIVADA);
    }

    // Si la petición que dio el 401 salió con un access token: la última, la del reintento
    // si el TokenAuthenticator la repitió con uno renovado.
    private static boolean llevabaSesion(Response<?> response) {
        String h = response.raw().request().header("Authorization");
        return h != null && h.startsWith("Bearer ");
    }

    // Lee el cuerpo de error como texto (o un mensaje genérico si no se puede).
    private String leerError(Response<T> response) {
        try {
            if (response.errorBody() != null) return response.errorBody().string();
        } catch (Exception ignored) {
        }
        return "Error " + response.code();
    }
}
