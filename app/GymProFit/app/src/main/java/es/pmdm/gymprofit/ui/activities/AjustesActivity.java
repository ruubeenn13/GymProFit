package es.pmdm.gymprofit.ui.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;

import es.pmdm.gymprofit.BuildConfig;
import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.ui.widget.FotoPerfil;
import es.pmdm.gymprofit.utils.Correo;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// AjustesActivity — Ajustes (GP-105), según 05-ajustes.png.
//
// Se abre con el engranaje de Progreso y reúne lo que eran la pestaña Perfil y el
// menú de tres puntos (07-mapa: ninguna opción se pierde):
//   · Cuenta: foto de perfil, correo (enmascarado; se cambia con la contraseña,
//     GP-083) y contraseña (pantalla nueva sobre /auth/change-password).
//   · Tus datos y objetivos: todo abre editar perfil, que recalcula como siempre.
//   · Preferencias: tema e idioma con los diálogos de siempre; notificaciones abre
//     los ajustes de la app en el sistema.
//   · Ayuda: soporte, informar de un error (con versión, Android y modelo; nada
//     personal) y sugerir una mejora, los tres a soporte@gymprofit.app.
//   · Legal y acerca de: privacidad, licencias y Acerca de. Términos de uso sale
//     oculto hasta que exista la página (GP-087).
//   · Cerrar sesión y Eliminar cuenta, al final: eliminar queda a tres toques de
//     cualquier pestaña (Progreso → engranaje → Eliminar cuenta, GP-008).
//   · Administración, solo para ADMIN y temporal hasta que exista la web (GP-085).
// ============================================================
public class AjustesActivity extends BaseActivity {

    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);
    private FotoPerfil fotoPerfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        fotoPerfil = new FotoPerfil(this, () -> this, prefsManager.getUsuarioId(), () -> { });

        // Cuenta
        fila(R.id.filaFoto, R.drawable.ic_ms_photo_camera, R.string.ajustes_foto, () -> {
            if (verificarAccesoRegistrado()) fotoPerfil.elegir();
        });
        fila(R.id.filaCorreo, R.drawable.ic_ms_mail, R.string.ajustes_correo, () -> {
            if (verificarAccesoRegistrado()) startActivity(new Intent(this, CambiarCorreoActivity.class));
        });
        fila(R.id.filaContrasena, R.drawable.ic_ms_lock, R.string.ajustes_contrasena, () -> {
            if (verificarAccesoRegistrado()) startActivity(new Intent(this, CambiarPasswordActivity.class));
        });

        // Tus datos y objetivos: los cuatro abren editar perfil, donde están juntos.
        Runnable editarPerfil = () -> {
            if (verificarAccesoRegistrado()) startActivity(new Intent(this, EditarPerfilActivity.class));
        };
        fila(R.id.filaPesoAlturaEdad, R.drawable.ic_ms_straighten, R.string.ajustes_peso_altura_edad, editarPerfil);
        fila(R.id.filaSexoActividad, R.drawable.ic_ms_directions_run, R.string.ajustes_sexo_actividad, editarPerfil);
        fila(R.id.filaObjetivo, R.drawable.ic_ms_flag, R.string.ajustes_objetivo, editarPerfil);
        fila(R.id.filaNivel, R.drawable.ic_ms_stairs, R.string.ajustes_nivel, editarPerfil);

        // Preferencias
        fila(R.id.filaTema, R.drawable.ic_ms_palette, R.string.ajustes_tema, this::mostrarDialogoTema);
        fila(R.id.filaIdioma, R.drawable.ic_ms_language, R.string.ajustes_idioma, this::mostrarDialogoIdioma);
        fila(R.id.filaNotificaciones, R.drawable.ic_ms_notifications, R.string.ajustes_notificaciones,
                this::abrirAjustesNotificaciones);

        // Ayuda
        fila(R.id.filaSoporte, R.drawable.ic_ms_support_agent, R.string.ajustes_soporte,
                () -> escribir(getString(R.string.email_soporte_asunto), null));
        fila(R.id.filaError, R.drawable.ic_ms_bug_report, R.string.ajustes_error,
                () -> escribir(getString(R.string.email_error_asunto), cuerpoError()));
        fila(R.id.filaMejora, R.drawable.ic_ms_lightbulb, R.string.ajustes_mejora,
                () -> escribir(getString(R.string.email_mejora_asunto), null));

        // Legal y acerca de
        fila(R.id.filaPrivacidad, R.drawable.ic_ms_policy, R.string.politica_privacidad,
                () -> UIHelper.abrirUrl(this, getString(R.string.url_privacidad)));
        abreFuera(R.id.filaPrivacidad, R.string.politica_privacidad);
        // Términos de uso: oculto hasta que exista la página (GP-087).
        findViewById(R.id.filaTerminos).setVisibility(View.GONE);
        fila(R.id.filaLicencias, R.drawable.ic_ms_license, R.string.licencias_titulo,
                () -> startActivity(new Intent(this, LicenciasActivity.class)));
        fila(R.id.filaAcerca, R.drawable.ic_ms_info, R.string.ajustes_acerca,
                () -> startActivity(new Intent(this, AcercaDeActivity.class)));
        valor(R.id.filaAcerca, R.string.ajustes_acerca, BuildConfig.VERSION_NAME);

        // Administración: solo ADMIN, temporal (GP-085).
        boolean admin = prefsManager.isAdmin();
        findViewById(R.id.tvSeccionAdmin).setVisibility(admin ? View.VISIBLE : View.GONE);
        findViewById(R.id.grupoAdmin).setVisibility(admin ? View.VISIBLE : View.GONE);
        fila(R.id.filaAdmin, R.drawable.ic_ms_workspace_premium, R.string.ajustes_admin,
                () -> startActivity(new Intent(this, AdminActivity.class)));

        findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> confirmarCerrarSesion());
        findViewById(R.id.btnEliminarCuenta).setOnClickListener(v -> {
            if (verificarAccesoRegistrado()) startActivity(new Intent(this, EliminarCuentaActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        pintarValores();
    }

    // Valores de la derecha: lo guardado en el teléfono ya, y el correo al llegar.
    private void pintarValores() {
        valor(R.id.filaSexoActividad, R.string.ajustes_sexo_actividad,
                UIHelper.traducirActividad(this, prefsManager.getActividad()));
        valor(R.id.filaObjetivo, R.string.ajustes_objetivo, UIHelper.traducirObjetivo(this, prefsManager.getObjetivo()));
        valor(R.id.filaNivel, R.string.ajustes_nivel,
                prefsManager.getNivel().isEmpty() ? null : UIHelper.traducirNivel(this, prefsManager.getNivel()));
        valor(R.id.filaTema, R.string.ajustes_tema, getString(
                prefsManager.getTheme() == AppCompatDelegate.MODE_NIGHT_NO ? R.string.ajustes_tema_claro : R.string.ajustes_tema_oscuro));
        // El idioma en uso, no la preferencia: sin elegir, la app sigue al del sistema.
        valor(R.id.filaIdioma, R.string.ajustes_idioma, getString(
                "en".equals(es.pmdm.gymprofit.utils.FechaUtils.localeDeLaApp(this).getLanguage())
                        ? R.string.ajustes_idioma_en : R.string.ajustes_idioma_es));

        int uid = prefsManager.getUsuarioId();
        if (uid == -1 || prefsManager.isGuest()) return;
        usuarioApi.getPorId(uid).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                if (u == null || isFinishing()) return;
                valor(R.id.filaCorreo, R.string.ajustes_correo, Correo.enmascarar(u.getEmail()));
                // Objetivo y nivel viven en la API; lo guardado en el teléfono puede no estar.
                if (u.getObjetivo() != null) {
                    valor(R.id.filaObjetivo, R.string.ajustes_objetivo, UIHelper.traducirObjetivo(AjustesActivity.this, u.getObjetivo()));
                }
                if (u.getNivelExperiencia() != null) {
                    valor(R.id.filaNivel, R.string.ajustes_nivel, UIHelper.traducirNivel(AjustesActivity.this, u.getNivelExperiencia()));
                }
            }
            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito (GP-017): la fila del correo se usa igual sin el
                // valor a la derecha, y la pantalla del correo lo vuelve a pedir y avisa.
            }
        });
    }

    // ── Filas ───────────────────────────────────────────────────────────────

    private void fila(int id, @DrawableRes int icono, @StringRes int titulo, Runnable accion) {
        View f = findViewById(id);
        ((ImageView) f.findViewById(R.id.ivIcono)).setImageResource(icono);
        ((TextView) f.findViewById(R.id.tvTitulo)).setText(titulo);
        f.setContentDescription(getString(titulo));
        f.setOnClickListener(v -> accion.run());
    }

    private void valor(int id, @StringRes int titulo, @Nullable String valor) {
        View f = findViewById(id);
        TextView tv = f.findViewById(R.id.tvValor);
        boolean hay = valor != null && !valor.isEmpty();
        tv.setText(valor);
        tv.setVisibility(hay ? View.VISIBLE : View.GONE);
        f.setContentDescription(hay ? getString(R.string.ajustes_fila_valor_a11y, getString(titulo), valor)
                : getString(titulo));
    }

    // Las que salen de la app llevan la flecha de «abre fuera» y lo dicen a TalkBack.
    private void abreFuera(int id, @StringRes int titulo) {
        View f = findViewById(id);
        ((ImageView) f.findViewById(R.id.ivFlecha)).setImageResource(R.drawable.ic_ms_open_in_new);
        f.setContentDescription(getString(R.string.ajustes_abre_fuera_a11y, getString(titulo)));
    }

    // ── Acciones ────────────────────────────────────────────────────────────

    // Los ajustes de notificaciones de la app en el sistema.
    private void abrirAjustesNotificaciones() {
        Intent i;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        }
        startActivity(i);
    }

    // Correo a soporte@gymprofit.app con el asunto (y el cuerpo) de cada caso.
    private void escribir(String asunto, @Nullable String cuerpo) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.email_soporte)});
        intent.putExtra(Intent.EXTRA_SUBJECT, asunto);
        if (cuerpo != null) intent.putExtra(Intent.EXTRA_TEXT, cuerpo);
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.email_elegir_app)));
        } catch (ActivityNotFoundException e) {
            UIHelper.mostrarToastError(this, getString(R.string.email_sin_app));
        }
    }

    // Versión de la app, de Android y el modelo del teléfono. Nada personal.
    private String cuerpoError() {
        return getString(R.string.email_error_cuerpo, BuildConfig.VERSION_NAME,
                Build.VERSION.RELEASE, Build.MANUFACTURER + " " + Build.MODEL);
    }
}
