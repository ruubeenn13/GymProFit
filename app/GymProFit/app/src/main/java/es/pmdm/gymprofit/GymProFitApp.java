package es.pmdm.gymprofit;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

// ============================================================
// GymProFitApp — clase Application de la app.
// Crea el canal de notificaciones push al arrancar el proceso: cuando llega una
// push con la app en BACKGROUND, Android la pinta él solo en el canal declarado
// en el manifest (id "5"), así que el canal debe existir de antemano (los canales
// del NotificationHelper se crean perezosos y no valdrían para ese caso).
// ============================================================
public class GymProFitApp extends Application {

    // Debe coincidir con NotificationHelper.CANAL_PUSH y el meta-data del manifest.
    private static final String CANAL_PUSH = "5";

    @Override
    public void onCreate() {
        super.onCreate();
        crearCanalPush();
    }

    // Registra el canal de push (API 26+). Crear un canal ya existente es no-op.
    private void crearCanalPush() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_PUSH,
                    getString(R.string.notif_canal_push),
                    NotificationManager.IMPORTANCE_HIGH);
            getSystemService(NotificationManager.class).createNotificationChannel(canal);
        }
    }
}
