package es.pmdm.gymprofit.ui.activities;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AbstractSavedStateViewModelFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.ui.viewmodels.VerificarCorreoViewModel;
import es.pmdm.gymprofit.ui.widget.CampoCodigo;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VerificacionCorreo;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// VerificarCorreoActivity — «Mira tu correo» (GP-045, DEC-066).
//
// Dos entradas: tras el alta, entre «Todo listo» y «¿Te avisamos?», y desde el aviso de
// Perfil. Nada bloquea: arriba está «Ahora no» y atrás hace lo mismo.
//
// Tres estados:
//   · El código. Seis cajas que se comprueban solas al poner la sexta cifra; no hay botón.
//     Si no es, las cajas en rojo, el aviso con los intentos que quedan (región viva) y un
//     temblor; al tocar, se vacían. «Abrir el correo» solo si hay app de correo; «Pegar
//     código» solo si hay texto copiado, y al pegar se queda con sus cifras.
//   · «Ese código ya no vale»: pasaron 15 minutos o se gastaron los cinco intentos. La API
//     da el mismo 400 a todo, así que esto lo cuenta el móvil (PreferencesManager); si se
//     desvía, manda el 400. Nunca se ABRE así desde Perfil (GP-267): al entrar se pide un
//     código nuevo (VerificarCorreoViewModel, una vez por entrada) y se abre en el normal.
//   · «Correo confirmado»: el círculo dorado y, al segundo, se sigue (a los avisos tras el
//     alta, de vuelta a Perfil desde allí).
//
// El reenvío no tiene cuenta atrás propia: la que se enseña es la del límite de la API
// (Retry-After de un 429), y mientras corre no se puede pulsar.
// ============================================================
public class VerificarCorreoActivity extends BaseActivity {

    private static final String EXTRA_DEL_ALTA = "del_alta";
    private static final String EXTRA_CORREO = "correo";
    private static final String ESTADO_CORREO = "correo";

    /** Lo que se queda «Correo confirmado» a la vista antes de seguir. */
    private static final long ESPERA_CONFIRMADO_MS = 1000;

    /**
     * Abre la pantalla como paso del alta: al acabar sigue a «¿Te avisamos?».
     *
     * @param ctx    pantalla que la abre.
     * @param correo dirección a la que la API acaba de mandar el código.
     */
    public static Intent delAlta(@NonNull Context ctx, @Nullable String correo) {
        return new Intent(ctx, VerificarCorreoActivity.class)
                .putExtra(EXTRA_DEL_ALTA, true)
                .putExtra(EXTRA_CORREO, correo);
    }

    /**
     * Abre la pantalla desde el aviso de Perfil: al acabar vuelve.
     *
     * @param ctx    pantalla que la abre.
     * @param correo correo de la cuenta.
     */
    public static Intent desdePerfil(@NonNull Context ctx, @Nullable String correo) {
        return new Intent(ctx, VerificarCorreoActivity.class).putExtra(EXTRA_CORREO, correo);
    }

    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);
    private final Handler reloj = new Handler(Looper.getMainLooper());
    private final Runnable alCaducar = this::mostrarCaducado;

    private boolean delAlta;
    private VerificarCorreoViewModel vm;
    @Nullable private String correo;
    private CampoCodigo campo;
    private TextView aviso;
    private MaterialButton btnReenviar, btnPegar, btnAbrir;
    @Nullable private CountDownTimer cuentaAtras;
    // Evita mandar dos veces el mismo código.
    private boolean verificando;

    private final ActivityResultLauncher<Intent> cambiarCorreo = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK) {
                    // CambiarCorreoActivity ya apuntó el código nuevo que mandó la API.
                    releerCorreo();
                    mostrarCodigo();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;
        setContentView(R.layout.activity_verificar_correo);

        delAlta = getIntent().getBooleanExtra(EXTRA_DEL_ALTA, false);
        correo = savedInstanceState != null
                ? savedInstanceState.getString(ESTADO_CORREO)
                : getIntent().getStringExtra(EXTRA_CORREO);

        campo = findViewById(R.id.campoCodigo);
        aviso = findViewById(R.id.tvAvisoCodigo);
        btnReenviar = findViewById(R.id.btnReenviar);
        btnPegar = findViewById(R.id.btnPegar);
        btnAbrir = findViewById(R.id.btnAbrirCorreo);

        findViewById(R.id.btnAhoraNo).setOnClickListener(v -> seguir());
        findViewById(R.id.btnAhoraNoCaducado).setOnClickListener(v -> seguir());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                seguir();
            }
        });
        findViewById(R.id.btnCambiarCorreo).setOnClickListener(v ->
                cambiarCorreo.launch(new Intent(this, CambiarCorreoActivity.class)));
        btnReenviar.setOnClickListener(v -> reenviar());
        findViewById(R.id.btnEnviarOtro).setOnClickListener(v -> reenviar());
        btnPegar.setOnClickListener(v -> pegar());
        btnAbrir.setOnClickListener(v -> abrirCorreo());
        campo.setAlCompletar(this::verificar);

        pintarCorreo();
        if (correo == null) releerCorreo();
        prepararAbrirCorreo();

        vm = new ViewModelProvider(this, new AbstractSavedStateViewModelFactory(this, null) {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            protected <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> modelClass,
                                                     @NonNull SavedStateHandle handle) {
                return (T) new VerificarCorreoViewModel(handle, new PuertoApi(usuarioApi, prefsManager),
                        System::currentTimeMillis);
            }
        }).get(VerificarCorreoViewModel.class);
        vm.entrar(!delAlta);

        if (vm.abrirCaducado(prefsManager.getCodigoCorreoEn(), prefsManager.getCodigoCorreoFallos(),
                System.currentTimeMillis())) {
            mostrarCaducado();
        } else {
            mostrarCodigo();
        }
        vm.estado().observe(this, this::pintarEntrada);
        // Momento 3 (DEC-039): las piezas suben y aparecen; con «Quitar animaciones», nada.
        if (!Movimiento.quieto(this)) {
            Movimiento.entrar(0, findViewById(R.id.tvTitulo), findViewById(R.id.tvExplicacion),
                    findViewById(R.id.cardCorreo), campo);
        }
    }

    // Desde Android 10 el portapapeles solo se puede mirar con la ventana enfocada: en
    // onResume todavía no lo está y daría vacío siempre.
    @Override
    public void onWindowFocusChanged(boolean foco) {
        super.onWindowFocusChanged(foco);
        if (foco) prepararPegar();
    }

    @Override
    protected void onDestroy() {
        reloj.removeCallbacksAndMessages(null);
        if (cuentaAtras != null) cuentaAtras.cancel();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putString(ESTADO_CORREO, correo);
    }

    // ── Estados ──────────────────────────────────────────────────────────────

    private void mostrarCodigo() {
        findViewById(R.id.grupoCodigo).setVisibility(View.VISIBLE);
        findViewById(R.id.grupoCaducado).setVisibility(View.GONE);
        findViewById(R.id.grupoConfirmado).setVisibility(View.GONE);
        aviso.setVisibility(View.GONE);
        campo.limpiar();
        campo.enfocar();
        programarCaducidad();
    }

    // Al acabarse los 15 minutos con la pantalla abierta, pasa sola a «ya no vale». Solo si
    // lo apuntado es un código vivo: con uno viejo, o ninguno (desde Perfil mientras llega
    // el nuevo), no hay nada que contar y pasaría a «ya no vale» al instante.
    private void programarCaducidad() {
        reloj.removeCallbacks(alCaducar);
        long emitido = prefsManager.getCodigoCorreoEn();
        long ahora = System.currentTimeMillis();
        if (VerificacionCorreo.caducado(emitido, prefsManager.getCodigoCorreoFallos(), ahora)) return;
        reloj.postDelayed(alCaducar, VerificacionCorreo.msHastaCaducar(emitido, ahora));
    }

    // ── La entrada desde Perfil (GP-267) ─────────────────────────────────────

    // La red y las preferencias del ViewModel. Estática para no retener la Activity: el
    // ViewModel sobrevive al giro y la llamada puede seguir en vuelo. PreferencesManager no
    // guarda el Context. El ApiCallback no mira isFinishing: la respuesta va al ViewModel,
    // y de él a la pantalla que haya entonces.
    private static final class PuertoApi implements VerificarCorreoViewModel.Puerto {
        private final UsuarioApi api;
        private final PreferencesManager prefs;

        PuertoApi(@NonNull UsuarioApi api, @NonNull PreferencesManager prefs) {
            this.api = api;
            this.prefs = prefs;
        }

        @Override
        public void pedirCodigo(@NonNull Respuesta respuesta) {
            api.reenviarCodigoCorreo().enqueue(new ApiCallback<Void>() {
                @Override
                public void onOk(Void ignorado) {
                    respuesta.ok();
                }

                @Override
                public void onFail(int code, String message) {
                    // El ViewModel lo guarda y la pantalla lo dice (pintarEntrada).
                    respuesta.fallo(code, message);
                }
            });
        }

        @Override
        public void apuntarCodigo(long ahora) {
            prefs.apuntarCodigoCorreo(ahora);
        }
    }

    // Siempre en el estado del código: lo que cambia es el reenvío y lo que se dice. Lo que
    // se dice (TalkBack, toast) va una vez por respuesta, no en cada giro.
    private void pintarEntrada(@Nullable VerificarCorreoViewModel.Estado estado) {
        if (estado == null) return;
        boolean avisar = vm.tocaAvisar();
        switch (estado) {
            case ENVIANDO:
                btnReenviar.setEnabled(false);
                return;
            case ENVIADO:
                btnReenviar.setEnabled(true);
                programarCaducidad();
                if (avisar) campo.announceForAccessibility(getString(R.string.verificar_correo_reenviado));
                break;
            case ESPERA:
                int s = vm.segundosDeEspera(System.currentTimeMillis());
                if (s > 0) {
                    empezarCuentaAtras(s);
                    // El texto del botón lo pone el primer tic, que aún no ha llegado.
                    if (avisar) btnReenviar.announceForAccessibility(getString(
                            R.string.verificar_correo_reenviar_en, VerificacionCorreo.cuentaAtras(s)));
                } else {
                    btnReenviar.setEnabled(true);
                }
                break;
            case YA_VERIFICADO:
                if (avisar) mostrarConfirmado();
                break;
            case FALLO:
                btnReenviar.setEnabled(true);
                if (avisar) UiFeedback.toastError(this, vm.codigoFallo(), vm.mensajeFallo());
                break;
            case INTERRUMPIDO:
            default:
                btnReenviar.setEnabled(true);
        }
        if (avisar) vm.avisado();
    }

    private void mostrarCaducado() {
        reloj.removeCallbacks(alCaducar);
        findViewById(R.id.grupoCodigo).setVisibility(View.GONE);
        findViewById(R.id.grupoCaducado).setVisibility(View.VISIBLE);
        findViewById(R.id.grupoConfirmado).setVisibility(View.GONE);
        ocultarTeclado();
        findViewById(R.id.tvTituloCaducado).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
    }

    private void mostrarConfirmado() {
        reloj.removeCallbacks(alCaducar);
        findViewById(R.id.grupoCodigo).setVisibility(View.GONE);
        findViewById(R.id.grupoCaducado).setVisibility(View.GONE);
        findViewById(R.id.grupoConfirmado).setVisibility(View.VISIBLE);
        ocultarTeclado();
        View circulo = findViewById(R.id.circuloOro);
        Movimiento.vibrar(circulo, Movimiento.Vibracion.EXITO);
        if (!Movimiento.quieto(this)) Movimiento.saltar(circulo, Movimiento.TROFEO);
        setResult(RESULT_OK);
        reloj.postDelayed(this::seguir, ESPERA_CONFIRMADO_MS);
    }

    private void ocultarTeclado() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(campo.getWindowToken(), 0);
    }

    // ── Correo ───────────────────────────────────────────────────────────────

    private void pintarCorreo() {
        ((TextView) findViewById(R.id.tvCorreo)).setText(correo == null ? "…" : correo);
    }

    // Tras cambiarlo, o si no llegó en el Intent: el correo de la cuenta, de la API.
    private void releerCorreo() {
        int id = prefsManager.getUsuarioId();
        if (id == -1) return;
        VistaEstado estado = new VistaEstado(findViewById(R.id.estadoCorreo));
        estado.cargando();
        usuarioApi.getPorId(id).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                if (isFinishing()) return;
                estado.oculto();
                if (u == null) return;
                correo = u.getEmail();
                pintarCorreo();
                if (Boolean.TRUE.equals(u.getEmailVerificado())) mostrarConfirmado();
            }

            @Override
            public void onFail(int code, String message) {
                // El código se puede escribir igual; el fallo va bajo la tarjeta, con «Reintentar».
                if (isFinishing()) return;
                estado.error(VistaEstado.mensaje(VerificarCorreoActivity.this,
                        R.string.cambiar_correo_error_leer, code, message), VerificarCorreoActivity.this::releerCorreo);
            }
        });
    }

    // ── Verificar ────────────────────────────────────────────────────────────

    private void verificar(@NonNull String codigo) {
        if (verificando) return;
        verificando = true;
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("codigo", codigo);
        usuarioApi.verificarCorreo(cuerpo).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignorado) {
                verificando = false;
                if (!isFinishing()) mostrarConfirmado();
            }

            @Override
            public void onFail(int code, String message) {
                verificando = false;
                if (isFinishing()) return;
                if (VerificacionCorreo.canje(code) == VerificacionCorreo.Canje.CODIGO_MALO) {
                    codigoMalo();
                } else {
                    // Red o límite: el toast lo dice (con la espera si la hay); las cajas en
                    // rojo se vacían al tocar para volver a probar.
                    campo.marcarError();
                    if (code == 429) empezarCuentaAtras(UiFeedback.segundosDeEspera(message));
                    UiFeedback.toastError(VerificarCorreoActivity.this, code, message);
                }
            }
        });
    }

    private void codigoMalo() {
        int fallos = prefsManager.sumarFalloCodigoCorreo();
        if (VerificacionCorreo.caducado(prefsManager.getCodigoCorreoEn(), fallos, System.currentTimeMillis())) {
            Movimiento.vibrar(campo, Movimiento.Vibracion.ERROR);
            mostrarCaducado();
            return;
        }
        int quedan = VerificacionCorreo.intentosRestantes(fallos);
        campo.marcarError();
        aviso.setText(getResources().getQuantityString(R.plurals.verificar_correo_error, quedan, quedan));
        aviso.setVisibility(View.VISIBLE);
        // Rol de alerta: TalkBack lo dice en cuanto sale, sin esperar a que se le enfoque.
        aviso.announceForAccessibility(aviso.getText());
        // Momento 10 (DEC-039): tiembla una vez y vibra; con «Quitar animaciones», solo vibra.
        Movimiento.temblar(campo);
    }

    // ── Reenviar ─────────────────────────────────────────────────────────────

    private void reenviar() {
        btnReenviar.setEnabled(false);
        usuarioApi.reenviarCodigoCorreo().enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignorado) {
                if (isFinishing()) return;
                btnReenviar.setEnabled(true);
                prefsManager.apuntarCodigoCorreo(System.currentTimeMillis());
                UIHelper.mostrarToastExito(VerificarCorreoActivity.this, getString(R.string.verificar_correo_reenviado));
                mostrarCodigo();
            }

            @Override
            public void onFail(int code, String message) {
                if (isFinishing()) return;
                btnReenviar.setEnabled(true);
                if (VerificacionCorreo.reenvio(code) == VerificacionCorreo.Reenvio.YA_VERIFICADO) {
                    mostrarConfirmado();
                    return;
                }
                if (code == 429) empezarCuentaAtras(UiFeedback.segundosDeEspera(message));
                // Red o límite de peticiones: el toast lo dice, con la espera si la hay.
                UiFeedback.toastError(VerificarCorreoActivity.this, code, message);
            }
        });
    }

    // La espera del límite de la API: el enlace apagado con «en 0:42» hasta llegar a cero.
    private void empezarCuentaAtras(@Nullable Integer segundos) {
        if (segundos == null || segundos <= 0) return;
        if (cuentaAtras != null) cuentaAtras.cancel();
        btnReenviar.setEnabled(false);
        cuentaAtras = new CountDownTimer(segundos * 1000L, 1000L) {
            @Override
            public void onTick(long ms) {
                btnReenviar.setText(getString(R.string.verificar_correo_reenviar_en,
                        VerificacionCorreo.cuentaAtras((int) Math.ceil(ms / 1000.0))));
            }

            @Override
            public void onFinish() {
                btnReenviar.setText(R.string.verificar_correo_reenviar);
                btnReenviar.setEnabled(true);
            }
        }.start();
    }

    // ── Abrir el correo y pegar ──────────────────────────────────────────────

    // Solo si hay una app de correo que lo abra (la consulta va declarada en <queries>).
    private void prepararAbrirCorreo() {
        Intent consulta = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_EMAIL);
        boolean hay = !getPackageManager().queryIntentActivities(consulta, 0).isEmpty();
        btnAbrir.setVisibility(hay ? View.VISIBLE : View.GONE);
        ajustarHueco();
    }

    private void abrirCorreo() {
        Intent i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (android.content.ActivityNotFoundException e) {
            // Se desinstaló entre que se miró y se tocó: el botón se va y no hay nada más que decir.
            btnAbrir.setVisibility(View.GONE);
            ajustarHueco();
        }
    }

    // «Pegar código» sale si lo copiado es texto. El contenido no se lee para decidirlo:
    // leerlo sin que se pida enseña el aviso del sistema de «ha pegado del portapapeles».
    // Se lee al tocar; si no son seis cifras, se dice y el botón se va.
    private void prepararPegar() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipDescription d = cm == null ? null : cm.getPrimaryClipDescription();
        boolean texto = d != null && (d.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                || d.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML));
        btnPegar.setVisibility(texto ? View.VISIBLE : View.GONE);
        ajustarHueco();
    }

    private void pegar() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = cm == null ? null : cm.getPrimaryClip();
        CharSequence texto = clip == null || clip.getItemCount() == 0 ? null : clip.getItemAt(0).coerceToText(this);
        if (!VerificacionCorreo.seisCifras(texto)) {
            UIHelper.mostrarToastInfo(this, getString(R.string.verificar_correo_pegar_nada));
            btnPegar.setVisibility(View.GONE);
            ajustarHueco();
            return;
        }
        campo.poner(texto);
    }

    private void ajustarHueco() {
        boolean dos = btnAbrir.getVisibility() == View.VISIBLE && btnPegar.getVisibility() == View.VISIBLE;
        findViewById(R.id.huecoBotones).setVisibility(dos ? View.VISIBLE : View.GONE);
    }

    // Tras el alta, a «¿Te avisamos?», como antes de GP-045; desde Perfil, de vuelta.
    private void seguir() {
        if (isFinishing()) return;
        reloj.removeCallbacksAndMessages(null);
        if (delAlta) {
            startActivity(AvisosActivity.delAlta(this)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
        finish();
    }
}
