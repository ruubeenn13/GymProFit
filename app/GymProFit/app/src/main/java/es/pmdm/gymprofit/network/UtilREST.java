package es.pmdm.gymprofit.network;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// ============================================================
// UtilREST — cliente HTTP de bajo nivel para hablar con la API GymProFit
// Gestiona el token JWT en memoria, ejecuta peticiones REST (incluido
// PATCH vía reflexión) y subidas multipart en AsyncTask, y notifica al
// llamador mediante OnResponseListener. También detecta el 401 (token
// expirado) y avisa a través de OnUnauthorizedListener.
// ============================================================
public class UtilREST {

    // Callback de resultado de una petición: éxito con cuerpo de respuesta, o error con mensaje
    public interface OnResponseListener {
        void onSuccess(String response, int statusCode);
        void onError(String message, int statusCode);
    }

    // Callback invocado cuando la API responde 401 (token JWT expirado o inválido)
    public interface OnUnauthorizedListener {
        void onTokenExpired();
    }

    // Token JWT actual, compartido en memoria por toda la app
    private static String token = null;
    private static OnUnauthorizedListener unauthorizedListener = null;

    public static void setToken(String t) { token = t; }
    public static void clearToken() { token = null; }
    public static String getToken() { return token; }

    // Registra el listener global que se dispara al recibir un 401
    public static void setOnUnauthorizedListener(OnUnauthorizedListener l) { unauthorizedListener = l; }

    // Lanza una petición HTTP asíncrona (GET/POST/PUT/PATCH/DELETE) con cuerpo JSON opcional
    public static void request(String url, String method, String body, OnResponseListener listener) {
        new RequestTask(url, method, body, listener).execute();
    }

    // Lanza una subida de archivo asíncrona como multipart/form-data (p.ej. foto de perfil)
    public static void uploadMultipart(Context context, String url, Uri fileUri, String fieldName, OnResponseListener listener) {
        new MultipartTask(context, url, fileUri, fieldName, listener).execute();
    }

    // Tarea en background que ejecuta una petición HTTP genérica con HttpURLConnection
    @SuppressWarnings("deprecation")
    private static class RequestTask extends AsyncTask<Void, Void, Object[]> {

        private final String url, method, body;
        private final OnResponseListener listener;

        RequestTask(String url, String method, String body, OnResponseListener listener) {
            this.url = url;
            this.method = method;
            this.body = body;
            this.listener = listener;
        }

        // Abre la conexión, envía el body si lo hay y lee la respuesta (o el error) como texto
        @Override
        protected Object[] doInBackground(Void... voids) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();

                // PATCH no está soportado nativamente en algunas versiones, se fuerza via reflexión
                try {
                    conn.setRequestMethod(method);
                } catch (Exception e) {
                    Field f = HttpURLConnection.class.getDeclaredField("method");
                    f.setAccessible(true);
                    f.set(conn, method);
                }

                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                if (body != null && !body.isEmpty()) {
                    conn.setDoOutput(true);
                    OutputStream os = conn.getOutputStream();
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    os.close();
                }

                int status = conn.getResponseCode();
                Log.d("GymProFit", method + " " + url + " → " + status);
                InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
                StringBuilder sb = new StringBuilder();
                if (is != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                }
                return new Object[]{sb.toString(), status, null};

            } catch (Exception e) {
                Log.e("GymProFit", method + " " + url + " exception: " + e.getMessage());
                return new Object[]{null, -1, e.getMessage()};
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        // Interpreta el resultado en el hilo principal: excepción, 401, éxito o error HTTP
        @Override
        protected void onPostExecute(Object[] result) {
            String response = (String) result[0];
            int status = (int) result[1];
            String error = (String) result[2];

            if (error != null) {
                listener.onError(error, -1);
            } else if (status == 401) {
                clearToken();
                if (unauthorizedListener != null) unauthorizedListener.onTokenExpired();
                else listener.onError("Sesión expirada", 401);
            } else if (status >= 200 && status < 300) {
                listener.onSuccess(response != null ? response : "", status);
            } else {
                listener.onError(response != null ? response : "Error " + status, status);
            }
        }
    }

    // Tarea en background que sube un archivo (imagen) como multipart/form-data
    @SuppressWarnings("deprecation")
    private static class MultipartTask extends AsyncTask<Void, Void, Object[]> {

        private final Context context;
        private final String url, fieldName;
        private final Uri fileUri;
        private final OnResponseListener listener;

        MultipartTask(Context ctx, String url, Uri fileUri, String fieldName, OnResponseListener listener) {
            this.context = ctx.getApplicationContext();
            this.url = url;
            this.fileUri = fileUri;
            this.fieldName = fieldName;
            this.listener = listener;
        }

        // Construye manualmente el cuerpo multipart (boundary + cabeceras + bytes del archivo) y lo envía
        @Override
        protected Object[] doInBackground(Void... voids) {
            HttpURLConnection conn = null;
            String boundary = "GymProFitBoundary" + System.currentTimeMillis();
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setRequestProperty("Accept", "application/json");
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();

                String partHeader = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"foto.jpg\"\r\n"
                        + "Content-Type: image/jpeg\r\n\r\n";
                os.write(partHeader.getBytes(StandardCharsets.UTF_8));

                InputStream is = context.getContentResolver().openInputStream(fileUri);
                if (is != null) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = is.read(buf)) != -1) os.write(buf, 0, n);
                    is.close();
                }

                os.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int status = conn.getResponseCode();
                Log.d("GymProFit", "MULTIPART POST " + url + " → " + status);
                InputStream resp = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
                StringBuilder sb = new StringBuilder();
                if (resp != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(resp, StandardCharsets.UTF_8));
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                }
                return new Object[]{sb.toString(), status, null};
            } catch (Exception e) {
                Log.e("GymProFit", "MULTIPART POST " + url + " exception: " + e.getMessage());
                return new Object[]{null, -1, e.getMessage()};
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        // Interpreta el resultado en el hilo principal: excepción, 401, éxito o error HTTP
        @Override
        protected void onPostExecute(Object[] result) {
            String response = (String) result[0];
            int status = (int) result[1];
            String error = (String) result[2];

            if (error != null) {
                listener.onError(error, -1);
            } else if (status == 401) {
                clearToken();
                if (unauthorizedListener != null) unauthorizedListener.onTokenExpired();
                else listener.onError("Sesión expirada", 401);
            } else if (status >= 200 && status < 300) {
                listener.onSuccess(response != null ? response : "", status);
            } else {
                listener.onError(response != null ? response : "Error " + status, status);
            }
        }
    }
}
