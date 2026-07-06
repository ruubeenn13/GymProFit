package es.pmdm.gymprofit.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.DeviceTokenApi;

// ============================================================
// PushTokenManager — registro/baja del token FCM del dispositivo en el backend.
// Centraliza el flujo push del cliente:
//   - registrar(): obtiene el token de FirebaseMessaging y lo envía al backend,
//     saltándose el envío si ya se registró ese mismo token (caché en prefs).
//     Se llama tras el login y desde onNewToken (rotación de token de Firebase).
//   - eliminar(): da de baja el token en el backend (logout, best-effort) y limpia la caché.
// Todo es best-effort y silencioso: un fallo de push nunca molesta al usuario.
// ============================================================
public final class PushTokenManager {

    private PushTokenManager() {}

    private static final DeviceTokenApi api = ApiClient.service(DeviceTokenApi.class);

    // Obtiene el token FCM y lo registra en el backend si es nuevo o cambió.
    // getToken() figura como deprecado desde firebase-messaging 25.1.0 (migración a
    // Firebase Installations ID), pero sigue siendo la vía funcional y verificada; la
    // migración no es trivial y se difiere. Se suprime el aviso conscientemente.
    @SuppressWarnings("deprecation")
    public static void registrar(Context context) {
        PreferencesManager prefs = new PreferencesManager(context.getApplicationContext());

        // Sin sesión (o invitado sin id) no hay a quién asociar el token.
        if (prefs.getUsuarioId() == -1) return;

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Log.w("GymProFit", "FCM: no se pudo obtener el token", task.getException());
                return;
            }
            String token = task.getResult();

            // Ya registrado este mismo token: nada que hacer.
            if (token.equals(prefs.getFcmTokenEnviado())) return;

            Map<String, Object> body = new HashMap<>();
            body.put("token", token);
            body.put("plataforma", "ANDROID");
            // Idioma de la app: el backend localiza las push con él ("es" si no hay preferencia).
            String idioma = prefs.getLanguage();
            body.put("idioma", idioma == null || idioma.isEmpty()
                    ? java.util.Locale.getDefault().getLanguage() : idioma);

            api.registrar(body).enqueue(new ApiCallback<Void>() {
                @Override
                public void onOk(Void ignored) {
                    // Cachea el token enviado para no repetir el POST en cada arranque.
                    prefs.saveFcmTokenEnviado(token);
                    Log.d("GymProFit", "FCM: token registrado en el backend");
                }
                @Override
                public void onFail(int code, String message) {
                    // Silencioso: se reintentará en el siguiente login/arranque.
                    Log.w("GymProFit", "FCM: fallo registrando token (" + code + ")");
                }
            });
        });
    }

    // Da de baja el token en el backend (logout) y limpia la caché local. Best-effort.
    public static void eliminar(Context context) {
        PreferencesManager prefs = new PreferencesManager(context.getApplicationContext());
        String token = prefs.getFcmTokenEnviado();
        prefs.clearFcmTokenEnviado();
        if (token.isEmpty()) return;

        api.eliminar(token).enqueue(new ApiCallback<Void>() {
            @Override public void onOk(Void ignored) { }
            @Override public void onFail(int code, String message) { }
        });
    }
}
