package es.pmdm.gymprofit.services;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import es.pmdm.gymprofit.utils.NotificationHelper;
import es.pmdm.gymprofit.utils.PushTokenManager;

// ============================================================
// GymFirebaseMessagingService — receptor de notificaciones push (FCM).
// - onNewToken: Firebase rota el token del dispositivo → se re-registra en el
//   backend (PushTokenManager decide si hay sesión y si el token cambió).
// - onMessageReceived: se ejecuta con la app en PRIMER PLANO (en background,
//   Android pinta solo la notificación del payload "notification" en el canal
//   por defecto del manifest). Aquí la mostramos manualmente con NotificationHelper.
// ============================================================
public class GymFirebaseMessagingService extends FirebaseMessagingService {

    // Firebase generó/rotó el token del dispositivo: re-registrarlo en el backend.
    @Override
    public void onNewToken(@NonNull String token) {
        PushTokenManager.registrar(this);
    }

    // Push recibida con la app en primer plano: construir y mostrar la notificación.
    @Override
    public void onMessageReceived(@NonNull RemoteMessage mensaje) {
        String titulo = null;
        String cuerpo = null;

        // Payload "notification" (el que envía el backend con Notification.builder).
        if (mensaje.getNotification() != null) {
            titulo = mensaje.getNotification().getTitle();
            cuerpo = mensaje.getNotification().getBody();
        }
        // Fallback por si llegara solo payload "data".
        if (cuerpo == null && !mensaje.getData().isEmpty()) {
            titulo = mensaje.getData().get("titulo");
            cuerpo = mensaje.getData().get("mensaje");
        }
        if (cuerpo == null && titulo == null) return;

        NotificationHelper.notificarPush(this, titulo, cuerpo);
    }
}
