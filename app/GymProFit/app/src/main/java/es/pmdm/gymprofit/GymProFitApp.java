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
        verificarEntornoAislado();
        aplicarIdiomaGuardado();
        crearCanalPush();
        registrarTransicionesGlobales();
    }

    // ------------------------------------------------------------------
    // Aislamiento del entorno de pruebas
    // ------------------------------------------------------------------

    // Hosts que se consideran "mi máquina": el alias del emulador para el PC
    // anfitrión, el propio localhost y las redes privadas (móvil físico en la
    // misma WiFi). Cualquier otra cosa en un build de desarrollo es un error.
    private static final String[] HOSTS_LOCALES = {
            "10.0.2.2", "localhost", "127.0.0.1", "192.168.", "172.16.", "172.17.",
            "172.18.", "172.19.", "172.2", "172.30.", "172.31.", "10."};

    /**
     * Comprueba, al arrancar el proceso, que un build de desarrollo apunta a una
     * API local y no a producción.
     *
     * <p>El build ya falla en Gradle si {@code BASE_URL} no es local, así que esto
     * es la segunda red: cubre un APK de desarrollo generado antes de esa guarda o
     * compilado en otra máquina. Se prefiere reventar en el arranque, con un mensaje
     * explícito, a escribir datos de prueba en la base de datos real sin que nadie
     * lo note.
     *
     * <p>En release no hace nada: ahí apuntar a producción es justamente lo correcto.
     */
    private void verificarEntornoAislado() {
        if (!BuildConfig.DEBUG) return;

        String url = BuildConfig.BASE_URL;
        for (String host : HOSTS_LOCALES) {
            if (url.contains("://" + host)) return;
        }

        throw new IllegalStateException(
                "Build de desarrollo apuntando fuera de la red local: " + url
                        + ". Este APK escribiría en producción. Revisa BASE_URL en local.properties.");
    }

    /**
     * Marca cada pantalla con un distintivo del entorno cuando la app es de pruebas.
     *
     * <p>Sirve para no confundir nunca una captura o una prueba con la app real: si
     * el distintivo no está, se está mirando producción. Se dibuja sobre la ventana
     * de la Activity, así que aparece en las 42 pantallas sin tocar ningún layout,
     * y no intercepta toques.
     */
    private void marcarPantallaComoPruebas(@NonNull Activity actividad) {
        android.view.View raiz = actividad.getWindow().getDecorView().getRootView();
        if (!(raiz instanceof android.view.ViewGroup)) return;

        float d = getResources().getDisplayMetrics().density;

        android.widget.TextView distintivo = new android.widget.TextView(actividad);
        distintivo.setText(R.string.entorno_pruebas_distintivo);
        distintivo.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 9f);
        distintivo.setTextColor(0xFFFFFFFF);
        distintivo.setBackgroundColor(0xCCB4231F);
        distintivo.setPadding((int) (6 * d), (int) (2 * d), (int) (6 * d), (int) (2 * d));
        distintivo.setLetterSpacing(0.12f);
        // Decorativo: el lector de pantalla no debe leerlo en cada pantalla.
        distintivo.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        distintivo.setClickable(false);
        distintivo.setFocusable(false);

        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
        // Centrado arriba: las esquinas superiores ya las ocupan los controles de
        // tema e idioma de varias pantallas.
        lp.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
        // Margen provisional hasta que lleguen los insets reales, justo abajo.
        lp.topMargin = (int) (24 * d);

        ((android.view.ViewGroup) raiz).addView(distintivo, lp);

        // La app todavía no gestiona insets (está en el plan de trabajo), así que el
        // distintivo se coloca él mismo bajo la barra de estado en lugar de quedar
        // tapado por ella.
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(distintivo, (v, insets) -> {
            int arriba = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
            android.widget.FrameLayout.LayoutParams p =
                    (android.widget.FrameLayout.LayoutParams) v.getLayoutParams();
            p.topMargin = arriba + (int) (2 * d);
            v.setLayoutParams(p);
            return insets;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(distintivo);
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
                // Distintivo de entorno de pruebas: solo en builds de desarrollo, y
                // una vez por instancia de Activity. Se aplaza a que la ventana tenga
                // ya su contenido montado.
                if (BuildConfig.DEBUG) {
                    a.getWindow().getDecorView().post(() -> marcarPantallaComoPruebas(a));
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
