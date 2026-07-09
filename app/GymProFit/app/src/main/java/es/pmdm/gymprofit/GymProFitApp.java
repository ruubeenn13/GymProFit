package es.pmdm.gymprofit;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import es.pmdm.gymprofit.utils.AnimUtils;
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

    // Pantallas que se presentan como MODAL (entran desde abajo): tareas de crear/
    // registrar/añadir. El resto usa el push lateral por defecto.
    private static final Set<String> PANTALLAS_MODAL = new HashSet<>(Arrays.asList(
            "CrearRutinaActivity",
            "RegistrarSesionActivity",
            "RegistrarMedicionActivity",
            "AnadirEjerciciosActivity"));

    @Override
    public void onCreate() {
        super.onCreate();
        aplicarIdiomaGuardado();
        crearCanalPush();
        registrarTransicionesGlobales();
    }

    // Aplica la transición de deslizamiento lateral a TODAS las Activities de la app.
    // Se hace aquí (no en BaseActivity) porque muchas pantallas extienden
    // AppCompatActivity directamente y no heredarían la transición. Se registra en
    // onActivityCreated, antes de que la ventana se muestre/anime.
    private void registrarTransicionesGlobales() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(@NonNull Activity a, @Nullable Bundle s) {
                if (PANTALLAS_MODAL.contains(a.getClass().getSimpleName())) {
                    AnimUtils.aplicarModal(a);
                } else {
                    AnimUtils.aplicarDeslizamiento(a);
                }
            }
            @Override public void onActivityStarted(@NonNull Activity a) {}
            @Override public void onActivityResumed(@NonNull Activity a) {}
            @Override public void onActivityPaused(@NonNull Activity a) {}
            @Override public void onActivityStopped(@NonNull Activity a) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle s) {}
            @Override public void onActivityDestroyed(@NonNull Activity a) {}
        });
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
