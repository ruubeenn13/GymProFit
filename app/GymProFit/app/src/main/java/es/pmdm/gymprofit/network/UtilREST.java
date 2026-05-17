package es.pmdm.gymprofit.network;

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

public class UtilREST {

    public interface OnResponseListener {
        void onSuccess(String response, int statusCode);
        void onError(String message, int statusCode);
    }

    private static String token = null;

    public static void setToken(String t) { token = t; }
    public static void clearToken() { token = null; }
    public static String getToken() { return token; }

    public static void request(String url, String method, String body, OnResponseListener listener) {
        new RequestTask(url, method, body, listener).execute();
    }

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

        @Override
        protected void onPostExecute(Object[] result) {
            String response = (String) result[0];
            int status = (int) result[1];
            String error = (String) result[2];

            if (error != null) {
                listener.onError(error, -1);
            } else if (status >= 200 && status < 300) {
                listener.onSuccess(response != null ? response : "", status);
            } else {
                listener.onError(response != null ? response : "Error " + status, status);
            }
        }
    }
}
