package es.pmdm.gymprofit.ui.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.material.button.MaterialButton;


import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.AuthApi;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.PushTokenManager;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// BaseActivity — clase base para todas las Activities de GymProFit
// Centraliza tema claro/oscuro, idioma, los diálogos de tema e idioma y cerrar
// sesión (que desde GP-105 abre Ajustes; el menú de tres puntos ya no existe) y el
// logout automático ante 401 no autorizado.
// ============================================================
public abstract class BaseActivity extends AppCompatActivity {

    protected PreferencesManager prefsManager;

    // Aplica la escala de fuente global (agranda todo el texto de la app de forma
    // uniforme, incluidos los textSize hardcodeados). La nav queda fuera (mide en dp).
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }
    // Interfaz Retrofit tipada de auth (etapa 2), usada para revocar el refresh en el logout
    private final AuthApi authApi = ApiClient.service(AuthApi.class);

    // Aplica tema e idioma guardados antes de crear la vista y registra el listener de 401
    // que fuerza logout y redirige a LoginActivity cuando el token expira o es inválido.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        // El idioma lo aplica AndroidX (AppCompatDelegate) globalmente; ver GymProFitApp.
        // La transición de deslizamiento la registra GymProFitApp para TODAS las Activities.
        super.onCreate(savedInstanceState);

        UtilREST.setOnUnauthorizedListener(cuentaDesactivada -> {
            prefsManager.cerrarSesion();
            Intent intent = new Intent(this, LoginActivity.class);
            // El login explica el cierre si la cuenta está desactivada (GP-083).
            intent.putExtra(LoginActivity.EXTRA_CUENTA_DESACTIVADA, cuentaDesactivada);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Muestra toast de "solo usuarios registrados" si el usuario es invitado.
     * @return {@code true} si puede continuar, {@code false} si es guest.
     */
    protected boolean verificarAccesoRegistrado() {
        if (prefsManager.isGuest()) {
            UIHelper.mostrarToastError(this, getString(R.string.error_solo_usuarios_registrados));
            return false;
        }
        return true;
    }

    // Muestra un diálogo custom para elegir tema claro/oscuro; guarda la preferencia y recrea la activity
    protected void mostrarDialogoTema() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_tema);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.5f);
        }

        android.widget.LinearLayout root = dialog.findViewById(R.id.dialogRoot);
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorBackground, tv, true);
        GradientDrawable fondo = new GradientDrawable();
        fondo.setShape(GradientDrawable.RECTANGLE);
        fondo.setCornerRadius(20 * getResources().getDisplayMetrics().density);
        fondo.setColor(tv.data);
        root.setBackground(fondo);

        int temaActual = prefsManager.getTheme();
        View ivCheckClaro = dialog.findViewById(R.id.ivCheckClaro);
        View ivCheckOscuro = dialog.findViewById(R.id.ivCheckOscuro);
        if (temaActual == AppCompatDelegate.MODE_NIGHT_YES) {
            ivCheckOscuro.setVisibility(View.VISIBLE);
        } else {
            ivCheckClaro.setVisibility(View.VISIBLE);
        }

        dialog.findViewById(R.id.optionTemaClaro).setOnClickListener(v -> {
            prefsManager.saveTheme(AppCompatDelegate.MODE_NIGHT_NO);
            dialog.dismiss();
            recreate();
        });
        dialog.findViewById(R.id.optionTemaOscuro).setOnClickListener(v -> {
            prefsManager.saveTheme(AppCompatDelegate.MODE_NIGHT_YES);
            dialog.dismiss();
            recreate();
        });
        ((com.google.android.material.button.MaterialButton) dialog.findViewById(R.id.btnCerrarTema))
                .setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88);
            dialog.getWindow().setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    // Muestra un diálogo custom para elegir idioma español/inglés; guarda la preferencia y recrea la activity
    protected void mostrarDialogoIdioma() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_idioma);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.5f);
        }

        android.widget.LinearLayout root = dialog.findViewById(R.id.dialogRoot);
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorBackground, tv, true);
        GradientDrawable fondo = new GradientDrawable();
        fondo.setShape(GradientDrawable.RECTANGLE);
        fondo.setCornerRadius(20 * getResources().getDisplayMetrics().density);
        fondo.setColor(tv.data);
        root.setBackground(fondo);

        String idiomaActual = prefsManager.getLanguage();
        ImageView ivCheckEspanol = dialog.findViewById(R.id.ivCheckEspanol);
        ImageView ivCheckIngles = dialog.findViewById(R.id.ivCheckIngles);
        if ("es".equals(idiomaActual) || idiomaActual.isEmpty()) {
            ivCheckEspanol.setVisibility(View.VISIBLE);
        } else {
            ivCheckIngles.setVisibility(View.VISIBLE);
        }

        dialog.findViewById(R.id.optionEspanol).setOnClickListener(v -> {
            dialog.dismiss();
            cambiarIdioma("es");
        });
        dialog.findViewById(R.id.optionIngles).setOnClickListener(v -> {
            dialog.dismiss();
            cambiarIdioma("en");
        });
        ((MaterialButton) dialog.findViewById(R.id.btnCerrarIdioma))
                .setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88);
            dialog.getWindow().setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    // Re-registra el token FCM tras cambiar el idioma: el backend guarda el idioma junto
    // al token para localizar las push, así que hay que re-enviarlo. Se invalida la caché
    // (si no, PushTokenManager saltaría el POST al no haber cambiado el token).
    private void resincronizarIdiomaPush() {
        prefsManager.clearFcmTokenEnviado();
        PushTokenManager.registrar(this);
    }

    // Muestra un diálogo de confirmación y, al aceptar, limpia token/sesión y redirige a LoginActivity
    protected void confirmarCerrarSesion() {
        UIHelper.mostrarDialogoConIcono(
                this,
                getString(R.string.perfil_cerrar_sesion),
                getString(R.string.dialog_cerrar_sesion_mensaje),
                R.drawable.ic_ms_logout,
                () -> {
                    // Da de baja el token FCM del dispositivo (best-effort, mientras el JWT sigue vivo).
                    PushTokenManager.eliminar(this);
                    // Revoca el refresh token en el servidor (best-effort) antes de limpiar la sesión local.
                    String refresh = prefsManager.getRefreshToken();
                    if (refresh != null && !refresh.isEmpty()) {
                        Map<String, Object> body = new HashMap<>();
                        body.put("refreshToken", refresh);
                        authApi.logout(body).enqueue(new ApiCallback<Void>() {
                            @Override public void onOk(Void ignored) { }
                            @Override public void onFail(int code, String message) {
                                // Se ignora a propósito (GP-017): la sesión se cierra igual en el
                                // dispositivo, que borra el refresh; si la revocación no llega, ese
                                // refresh queda en el servidor hasta caducar pero ya no lo tiene nadie.
                                // Avisar de un fallo en algo que el usuario ya no ve solo confundiría.
                            }
                        });
                    }
                    UtilREST.clearToken();
                    prefsManager.cerrarSesion();
                    es.pmdm.gymprofit.utils.AvatarUtils.olvidar();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    // Cambia el idioma de la app vía AndroidX per-app locales: guarda la preferencia,
    // re-sincroniza el idioma del token push y aplica el locale (AndroidX recrea las
    // Activities automáticamente, por eso no hace falta recreate()).
    protected void cambiarIdioma(String lang) {
        prefsManager.saveLanguage(lang);
        resincronizarIdiomaPush();
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(lang));
    }
}
