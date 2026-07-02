package es.pmdm.gymprofit.network;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// ============================================================
// UtilREST — fachada de red de la app GymProFit.
// Mantiene el token/refresh en memoria y expone request()/uploadMultipart()
// con callback OnResponseListener, igual que antes, pero POR DENTRO delega en
// ApiClient (Retrofit + OkHttp). La renovación del token en un 401 la hace el
// TokenAuthenticator de OkHttp de forma transparente; si falla, esta clase
// avisa vía OnUnauthorizedListener para volver a login.
// (Ya no usa AsyncTask/HttpURLConnection.)
// ============================================================
public class UtilREST {

    // Callback de resultado de una petición: éxito con cuerpo de respuesta, o error con mensaje.
    public interface OnResponseListener {
        void onSuccess(String response, int statusCode);
        void onError(String message, int statusCode);
    }

    // Callback invocado cuando la API responde 401 y NO se ha podido renovar la sesión
    // (refresh ausente, expirado o revocado): hay que volver a login.
    public interface OnUnauthorizedListener {
        void onTokenExpired();
    }

    // Callback para persistir los tokens renovados (p. ej. en PreferencesManager),
    // de modo que la sesión renovada sobreviva a reinicios de la app.
    public interface TokenPersister {
        void persist(String token, String refreshToken);
    }

    // Access token JWT actual (de vida corta), compartido en memoria por toda la app.
    private static volatile String token = null;
    // Refresh token opaco (de vida larga) para renovar el access token sin re-login.
    private static volatile String refreshToken = null;
    private static OnUnauthorizedListener unauthorizedListener = null;
    private static TokenPersister tokenPersister = null;

    // Handler al hilo principal para entregar errores construidos fuera de la red.
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void setToken(String t) { token = t; }
    // Limpia AMBOS tokens (logout / sesión no recuperable).
    public static void clearToken() { token = null; refreshToken = null; }
    public static String getToken() { return token; }

    public static void setRefreshToken(String t) { refreshToken = t; }
    public static String getRefreshToken() { return refreshToken; }

    // Registra el listener global que se dispara al recibir un 401 no recuperable.
    public static void setOnUnauthorizedListener(OnUnauthorizedListener l) { unauthorizedListener = l; }

    // Registra el persistidor de tokens renovados (se llama tras un refresh exitoso).
    public static void setTokenPersister(TokenPersister p) { tokenPersister = p; }

    // Llamado por ApiClient.TokenAuthenticator tras renovar el token: actualiza el estado
    // en memoria y lo persiste para que sobreviva a reinicios.
    static void onTokensRefreshed(String nuevoToken, String nuevoRefresh) {
        token = nuevoToken;
        refreshToken = nuevoRefresh;
        if (tokenPersister != null) tokenPersister.persist(nuevoToken, nuevoRefresh);
    }

    // Lanza una petición HTTP asíncrona (GET/POST/PUT/PATCH/DELETE) con cuerpo JSON opcional.
    public static void request(String url, String method, String body, OnResponseListener listener) {
        RequestBody rb = RequestBody.create(ApiClient.JSON, body != null ? body : "");
        ApiClient.RawApi api = ApiClient.api();

        Call<ResponseBody> call;
        switch (method.toUpperCase()) {
            case "GET":    call = api.get(url); break;
            case "POST":   call = api.post(url, rb); break;
            case "PUT":    call = api.put(url, rb); break;
            case "PATCH":  call = api.patch(url, rb); break;
            case "DELETE": call = api.delete(url); break;
            default:
                listener.onError("Método HTTP no soportado: " + method, -1);
                return;
        }
        call.enqueue(callbackHacia(listener));
    }

    // Lanza una subida de archivo asíncrona como multipart/form-data (p. ej. foto de perfil).
    public static void uploadMultipart(Context context, String url, Uri fileUri, String fieldName, OnResponseListener listener) {
        final Context appCtx = context.getApplicationContext();
        // Lee el archivo en un hilo aparte para no bloquear la UI; luego encola la subida.
        new Thread(() -> {
            try (InputStream is = appCtx.getContentResolver().openInputStream(fileUri)) {
                if (is == null) {
                    MAIN.post(() -> listener.onError("No se pudo abrir el archivo", -1));
                    return;
                }
                byte[] bytes = leerTodo(is);
                RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpeg"), bytes);
                MultipartBody.Part part = MultipartBody.Part.createFormData(fieldName, "foto.jpg", fileBody);
                ApiClient.api().upload(url, part).enqueue(callbackHacia(listener));
            } catch (Exception e) {
                MAIN.post(() -> listener.onError(e.getMessage(), -1));
            }
        }).start();
    }

    // Callback Retrofit → OnResponseListener. Retrofit invoca estos métodos en el hilo principal.
    private static Callback<ResponseBody> callbackHacia(OnResponseListener listener) {
        return new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                int status = response.code();
                String cuerpo = leerCuerpo(response.isSuccessful() ? response.body() : response.errorBody());

                if (status == 401) {
                    // El TokenAuthenticator ya intentó renovar y no pudo → sesión no recuperable.
                    clearToken();
                    if (unauthorizedListener != null) unauthorizedListener.onTokenExpired();
                    else listener.onError("Sesión expirada", 401);
                } else if (response.isSuccessful()) {
                    listener.onSuccess(cuerpo != null ? cuerpo : "", status);
                } else {
                    listener.onError(cuerpo != null ? cuerpo : "Error " + status, status);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                listener.onError(t.getMessage(), -1);
            }
        };
    }

    // Lee el cuerpo de la respuesta como String (o "" si falla/está vacío).
    private static String leerCuerpo(ResponseBody body) {
        if (body == null) return "";
        try {
            return body.string();
        } catch (Exception e) {
            return "";
        }
    }

    // Lee un InputStream completo a byte[].
    private static byte[] leerTodo(InputStream is) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = is.read(chunk)) != -1) buffer.write(chunk, 0, n);
        return buffer.toByteArray();
    }
}
