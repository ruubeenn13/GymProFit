package es.pmdm.gymprofit.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.appbar.MaterialToolbar;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.ui.widget.FilaAviso;
import es.pmdm.gymprofit.utils.AvisosCuenta;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// NotificacionesActivity — Ajustes › Notificaciones (GP-112, lote 1.5.1).
//
// Los mismos avisos que «¿Te avisamos?» del alta: el fin del descanso, que va con la
// sesión, y los tres interruptores de la cuenta, leídos de la API. Cada cambio se guarda
// en el momento con el PATCH; si falla, el interruptor vuelve a como estaba y se dice.
// Antes la fila de Ajustes abría los ajustes del sistema, y la 1.4.0 no tenía dónde
// encender los avisos de comidas, que la 1.5.0 dejó apagados para todos (DEC-037).
//
// Sin el permiso de Android no llega ninguno: arriba se dice, con «Activar».
// ============================================================
public class NotificacionesActivity extends BaseActivity {

    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);
    private FilaAviso entrenar, comidas, progreso;
    private ActivityResultLauncher<String> pedirPermiso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());

        new FilaAviso(findViewById(R.id.filaAvisoDescanso), R.drawable.ic_ms_timer,
                R.string.avisos_descanso, R.string.avisos_descanso_sub, false);
        entrenar = new FilaAviso(findViewById(R.id.filaAvisoEntrenar), R.drawable.ic_ms_notifications_active,
                R.string.avisos_entrenar, R.string.avisos_entrenar_sub, true);
        comidas = new FilaAviso(findViewById(R.id.filaAvisoComidas), R.drawable.ic_ms_restaurant,
                R.string.avisos_comidas, R.string.avisos_comidas_sub, true);
        progreso = new FilaAviso(findViewById(R.id.filaAvisoProgreso), R.drawable.ic_ms_trophy,
                R.string.avisos_progreso, R.string.avisos_progreso_sub, true);
        // Hasta saber lo de la cuenta, apagadas: no se cambia lo que no se ha leído.
        habilitar(false);
        entrenar.alCambiar(a -> guardar(entrenar));
        comidas.alCambiar(a -> guardar(comidas));
        progreso.alCambiar(a -> guardar(progreso));

        View sistema = findViewById(R.id.filaAjustesSistema);
        ((ImageView) sistema.findViewById(R.id.ivIcono)).setImageResource(R.drawable.ic_ms_settings);
        ((TextView) sistema.findViewById(R.id.tvTitulo)).setText(R.string.notificaciones_sistema);
        ((ImageView) sistema.findViewById(R.id.ivFlecha)).setImageResource(R.drawable.ic_ms_open_in_new);
        sistema.setContentDescription(getString(R.string.ajustes_abre_fuera_a11y, getString(R.string.notificaciones_sistema)));
        sistema.setOnClickListener(v -> abrirAjustesDelSistema());

        pedirPermiso = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                concedido -> pintarPermiso());
        findViewById(R.id.btnPermisoAvisos).setOnClickListener(v -> activarPermiso());

        cargar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pintarPermiso();
    }

    private void cargar() {
        int id = prefsManager.getUsuarioId();
        if (id == -1 || prefsManager.isGuest()) return;
        usuarioApi.getPorId(id).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                if (isFinishing() || u == null) return;
                AvisosCuenta.Estado e = AvisosCuenta.Estado.de(u.getAvisosEntrenar(), u.getAvisosComidas(),
                        u.getAvisosProgreso());
                entrenar.setActiva(e.entrenar);
                comidas.setActiva(e.comidas);
                progreso.setActiva(e.progreso);
                habilitar(true);
            }

            @Override
            public void onFail(int code, String message) {
                if (isFinishing()) return;
                // Sin leerlos no se pueden cambiar sin pisar lo de la cuenta: se dice y se
                // quedan apagados hasta volver a entrar.
                UiFeedback.toastError(NotificacionesActivity.this, code, message);
            }
        });
    }

    // El cambio de una fila a la cuenta; si falla, vuelve a como estaba.
    private void guardar(FilaAviso cambiada) {
        int id = prefsManager.getUsuarioId();
        AvisosCuenta.Estado e = new AvisosCuenta.Estado(entrenar.isActiva(), comidas.isActiva(), progreso.isActiva());
        usuarioApi.patch(id, AvisosCuenta.cuerpo(e)).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void r) {
                // Guardado: el interruptor ya enseña el estado nuevo.
            }

            @Override
            public void onFail(int code, String message) {
                if (isFinishing()) return;
                cambiada.setActiva(!cambiada.isActiva());
                UiFeedback.toastError(NotificacionesActivity.this, code, message);
            }
        });
    }

    private void habilitar(boolean si) {
        entrenar.setHabilitada(si);
        comidas.setHabilitada(si);
        progreso.setHabilitada(si);
    }

    private void pintarPermiso() {
        boolean permitidas = NotificationManagerCompat.from(this).areNotificationsEnabled();
        findViewById(R.id.cardPermisoAvisos).setVisibility(permitidas ? View.GONE : View.VISIBLE);
    }

    // En Android 13+ se pide el permiso; si ya se denegó del todo, o en versiones
    // anteriores, solo queda abrir los ajustes de la app.
    private void activarPermiso() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !prefsManager.isAvisosPreguntados(prefsManager.getUsername())) {
            prefsManager.setAvisosPreguntados(prefsManager.getUsername());
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            abrirAjustesDelSistema();
        }
    }

    private void abrirAjustesDelSistema() {
        Intent i;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        }
        startActivity(i);
    }
}
