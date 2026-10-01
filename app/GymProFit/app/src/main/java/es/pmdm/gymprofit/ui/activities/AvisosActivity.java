package es.pmdm.gymprofit.ui.activities;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.button.MaterialButton;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.ui.widget.FilaAviso;
import es.pmdm.gymprofit.utils.AvisosCuenta;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.PermisoAvisos;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// AvisosActivity — «¿Te avisamos?», después de crear la cuenta (GP-103 y GP-112,
// tablero 12 del lienzo; decisión 10).
//
// Se dice qué se manda y se elige ANTES del permiso de Android: el fin de cada descanso,
// que va con la sesión, y los tres interruptores de la cuenta con sus valores de serie
// (entrenar sí, comidas no, progreso sí). Arriba, una notificación de ejemplo que baja
// como una de verdad (momento 11).
//
// Los dos botones guardan los interruptores con el PATCH. «Activar avisos» pide además
// el permiso del sistema (Android 13+); «Ahora no», no, y Inicio tampoco lo pedirá
// después por su cuenta. Las dos acaban en Inicio: nada aquí bloquea el alta.
//
// GP-151 (lote 1.5.2): sustituye también a la petición suelta que hacía Inicio. Sale una
// vez por cuenta y móvil a quien no se le ha preguntado —quien actualiza desde la 1.4.0,
// quien entra en otro móvil, la cuenta antigua tras «Empezar»—, y entonces los
// interruptores son los de la cuenta, leídos antes de abrirla (abrirSiToca); si no se
// pueden leer, esa vez no sale. El botón principal depende del permiso (PermisoAvisos):
// concedido, «Guardar» y solo guarda; bloqueado, lleva a los ajustes de la app.
// ============================================================
public class AvisosActivity extends BaseActivity {

    /** Los interruptores de una cuenta que ya existe; sin ellos, los de serie del alta. */
    private static final String EXTRA_ENTRENAR = "avisos_entrenar";
    private static final String EXTRA_COMIDAS = "avisos_comidas";
    private static final String EXTRA_PROGRESO = "avisos_progreso";
    /** Al terminar, cerrarse y volver a donde estaba (Inicio) en vez de abrir Inicio de cero. */
    private static final String EXTRA_VOLVER = "avisos_volver";

    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);
    private FilaAviso entrenar, comidas, progreso;
    @Nullable private ValueAnimator bucle;
    private ActivityResultLauncher<String> pedirPermiso;
    private PermisoAvisos.Estado permiso = PermisoAvisos.Estado.PEDIBLE;
    private long pedidoEn;

    /**
     * Abre «¿Te avisamos?» para una cuenta que ya existe, si toca: no es el invitado y en
     * este móvil no se le ha preguntado. Antes lee sus avisos, porque la pantalla enseña
     * los de la cuenta y nunca los de serie; si no se pueden leer, esa vez no se abre ni
     * se pide nada, y se intenta en la siguiente apertura.
     *
     * @param desde   la pantalla que la abre.
     * @param volver  true desde Inicio (al acabar se cierra y vuelve); false desde el
     *                cuestionario de una cuenta antigua (al acabar abre Inicio).
     * @param siNo    lo que se hace cuando no se abre; null si nada.
     */
    public static void abrirSiToca(@NonNull Activity desde, boolean volver, @Nullable Runnable siNo) {
        PreferencesManager prefs = new PreferencesManager(desde);
        int id = prefs.getUsuarioId();
        if (id == -1 || !PermisoAvisos.tocaPreguntar(prefs.isGuest(), prefs.isAvisosPreguntados(prefs.getUsername()))) {
            if (siNo != null) siNo.run();
            return;
        }
        ApiClient.service(UsuarioApi.class).getPorId(id).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                if (desde.isFinishing() || desde.isDestroyed()) return;
                if (u == null) {
                    if (siNo != null) siNo.run();
                    return;
                }
                AvisosCuenta.Estado e = AvisosCuenta.Estado.de(u.getAvisosEntrenar(), u.getAvisosComidas(),
                        u.getAvisosProgreso());
                Intent i = new Intent(desde, AvisosActivity.class)
                        .putExtra(EXTRA_ENTRENAR, e.entrenar)
                        .putExtra(EXTRA_COMIDAS, e.comidas)
                        .putExtra(EXTRA_PROGRESO, e.progreso)
                        .putExtra(EXTRA_VOLVER, volver);
                if (!volver) i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                LoadingDialog.hide(desde); // la de «Empezar», antes de que se cierre su pantalla
                desde.startActivity(i);
            }

            @Override
            public void onFail(int code, String message) {
                // Sin los de la cuenta no se enseña nada (GP-151): ni pantalla, ni permiso,
                // ni error, porque quien no la ha pedido no echa nada en falta. Se queda
                // sin marcar y sale en la siguiente apertura con red.
                if (desde.isFinishing() || desde.isDestroyed()) return;
                if (siNo != null) siNo.run();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avisos);

        new FilaAviso(findViewById(R.id.filaAvisoDescanso), R.drawable.ic_ms_timer,
                R.string.avisos_descanso, R.string.avisos_descanso_sub, false);
        entrenar = new FilaAviso(findViewById(R.id.filaAvisoEntrenar), R.drawable.ic_ms_notifications_active,
                R.string.avisos_entrenar, R.string.avisos_entrenar_sub, true);
        comidas = new FilaAviso(findViewById(R.id.filaAvisoComidas), R.drawable.ic_ms_restaurant,
                R.string.avisos_comidas, R.string.avisos_comidas_sub, true);
        progreso = new FilaAviso(findViewById(R.id.filaAvisoProgreso), R.drawable.ic_ms_trophy,
                R.string.avisos_progreso, R.string.avisos_progreso_sub, true);
        // Los de la cuenta si vienen (cuenta que ya existe); si no, los de serie (alta).
        AvisosCuenta.Estado deSerie = AvisosCuenta.Estado.deSerie();
        Intent in = getIntent();
        AvisosCuenta.Estado inicial = new AvisosCuenta.Estado(
                in.getBooleanExtra(EXTRA_ENTRENAR, deSerie.entrenar),
                in.getBooleanExtra(EXTRA_COMIDAS, deSerie.comidas),
                in.getBooleanExtra(EXTRA_PROGRESO, deSerie.progreso));
        boolean girada = savedInstanceState != null;
        entrenar.setActiva(girada ? savedInstanceState.getBoolean("entrenar") : inicial.entrenar);
        comidas.setActiva(girada ? savedInstanceState.getBoolean("comidas") : inicial.comidas);
        progreso.setActiva(girada ? savedInstanceState.getBoolean("progreso") : inicial.progreso);
        // Vista una vez, cuenta como preguntada, también si se sale con atrás.
        if (!girada) prefsManager.setAvisosPreguntados(prefsManager.getUsername());
        FilaAviso.AlCambiar nada = activa -> { /* se guardan al pulsar un botón */ };
        entrenar.alCambiar(nada);
        comidas.alCambiar(nada);
        progreso.alCambiar(nada);

        pedirPermiso = registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
            // La 1.4.0 lo pedía sin apuntarlo: si Android ya no lo deja pedir, contesta al
            // instante sin enseñar nada, y entonces se lleva a los ajustes de la app.
            boolean explicar = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS);
            boolean ajustes = PermisoAvisos.sinDialogo(concedido, explicar, SystemClock.elapsedRealtime() - pedidoEn);
            seguir();
            if (ajustes) abrirAjustes();
        });

        View activar = findViewById(R.id.btnActivarAvisos);
        activar.setOnClickListener(v -> activar());
        findViewById(R.id.btnAhoraNo).setOnClickListener(v -> {
            guardar();
            seguir();
        });

        if (!girada) {
            Movimiento.entrar(100, findViewById(R.id.tvTituloAvisos), findViewById(R.id.tvTextoAvisos),
                    findViewById(R.id.cardTiposAviso), findViewById(R.id.filaAvisoDescanso),
                    findViewById(R.id.filaAvisoEntrenar), findViewById(R.id.filaAvisoComidas),
                    findViewById(R.id.filaAvisoProgreso), activar, findViewById(R.id.btnAhoraNo));
            Movimiento.respirar(activar, 1400);
        }
        bajarEjemplo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pintarPermiso();
    }

    // El botón principal y el texto, según lo que se pueda hacer con el permiso.
    private void pintarPermiso() {
        boolean concedido = NotificationManagerCompat.from(this).areNotificationsEnabled();
        boolean explicar = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS);
        permiso = PermisoAvisos.estado(Build.VERSION.SDK_INT, concedido, explicar, prefsManager.isPermisoAvisosPedido());
        MaterialButton activar = findViewById(R.id.btnActivarAvisos);
        TextView texto = findViewById(R.id.tvTextoAvisos);
        switch (permiso) {
            case CONCEDIDO:
                activar.setText(R.string.avisos_guardar);
                texto.setText(R.string.avisos_texto_concedido);
                break;
            case BLOQUEADO:
                activar.setText(R.string.avisos_activar);
                texto.setText(R.string.avisos_texto_bloqueado);
                break;
            default:
                activar.setText(R.string.avisos_activar);
                texto.setText(R.string.avisos_texto);
        }
    }

    // «Activar avisos» (o «Guardar»): guarda siempre; luego, según el permiso, nada, el
    // diálogo de Android o los ajustes de la app.
    private void activar() {
        guardar();
        switch (permiso) {
            case PEDIBLE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    prefsManager.setPermisoAvisosPedido();
                    pedidoEn = SystemClock.elapsedRealtime();
                    pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS);
                    return;
                }
                seguir();
                break;
            case BLOQUEADO:
                seguir();
                abrirAjustes();
                break;
            default:
                seguir();
        }
    }

    // Los ajustes de notificaciones de la app, en su propia tarea para que Inicio, que se
    // abre a la vez, no los tape.
    private void abrirAjustes() {
        startActivity(NotificacionesActivity.ajustesDelSistema(this).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean("entrenar", entrenar.isActiva());
        out.putBoolean("comidas", comidas.isActiva());
        out.putBoolean("progreso", progreso.isActiva());
    }

    @Override
    protected void onDestroy() {
        if (bucle != null) bucle.cancel();
        super.onDestroy();
    }

    /**
     * Momento 11: la notificación de ejemplo baja con rebote, se queda, sube y vuelve a
     * bajar, en un bucle de 5 s; su icono zumba al llegar. Con «Quitar animaciones» se
     * queda quieta, a la vista.
     */
    private void bajarEjemplo() {
        View carta = findViewById(R.id.cardAvisoEjemplo);
        View icono = findViewById(R.id.iconoAvisoEjemplo);
        if (Movimiento.quieto(this)) return;
        bucle = ValueAnimator.ofFloat(0f, 1f);
        bucle.setDuration(Movimiento.AVISO);
        bucle.setStartDelay(400);
        bucle.setRepeatCount(ValueAnimator.INFINITE);
        bucle.setInterpolator(Movimiento.LINEAL);
        bucle.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            float alto = carta.getHeight() + carta.getTop();
            float y, alfa;
            if (t < 0.10f) {
                float f = Movimiento.REBOTE.getInterpolation(t / 0.10f);
                y = -1.5f * alto * (1f - f);
                alfa = Math.min(1f, f);
            } else if (t < 0.78f) {
                y = 0f;
                alfa = 1f;
            } else if (t < 0.88f) {
                float f = (t - 0.78f) / 0.10f;
                y = -1.5f * alto * f;
                alfa = 1f - f;
            } else {
                y = -1.5f * alto;
                alfa = 0f;
            }
            carta.setTranslationY(y);
            carta.setAlpha(alfa);
            // Zumba entre el 10 y el 20 %: -14°, 12°, -8°, 5° y quieto.
            float r = 0f;
            if (t >= 0.10f && t < 0.20f) {
                float[] g = {0f, -14f, 12f, -8f, 5f, 0f};
                float p = (t - 0.10f) / 0.02f;
                int i = Math.min(4, (int) p);
                r = g[i] + (g[i + 1] - g[i]) * (p - i);
            }
            icono.setRotation(r);
        });
        bucle.start();
    }

    // Los tres interruptores a la cuenta. Si falla, se dice y se sigue: se cambian
    // luego en Ajustes › Notificaciones, y la cuenta conserva los de serie.
    private void guardar() {
        int id = prefsManager.getUsuarioId();
        if (id == -1) return;
        AvisosCuenta.Estado e = new AvisosCuenta.Estado(entrenar.isActiva(), comidas.isActiva(), progreso.isActiva());
        usuarioApi.patch(id, AvisosCuenta.cuerpo(e)).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void r) {
                // Guardados: no hay nada que enseñar, la pantalla ya se ha ido.
            }

            @Override
            public void onFail(int code, String message) {
                UIHelper.mostrarToastError(getApplicationContext(), getString(R.string.avisos_error_guardar));
            }
        });
    }

    // Desde Inicio, se vuelve a él; desde el alta, se abre de cero.
    private void seguir() {
        if (getIntent().getBooleanExtra(EXTRA_VOLVER, false)) finish();
        else irAInicio();
    }

    private void irAInicio() {
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
