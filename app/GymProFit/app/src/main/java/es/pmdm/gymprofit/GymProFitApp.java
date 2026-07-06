package es.pmdm.gymprofit;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import es.pmdm.gymprofit.utils.PreferencesManager;

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
        aplicarIdiomaGuardado();
        crearCanalPush();
    }

    // Aplica el idioma elegido por el usuario vía la API per-app locales de AndroidX
    // (AppCompatDelegate). Sustituye al antiguo aplicarIdioma() con updateConfiguration
    // (deprecado) que cada Activity repetía: AndroidX aplica el locale a TODAS las
    // pantallas y lo mantiene, sin tocar Configuration a mano. Idioma nuevo = solo strings.
    private void aplicarIdiomaGuardado() {
        String lang = new PreferencesManager(this).getLanguage();
        if (lang != null && !lang.isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang));
        }
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
