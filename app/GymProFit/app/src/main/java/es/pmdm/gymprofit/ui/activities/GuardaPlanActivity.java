package es.pmdm.gymprofit.ui.activities;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.auth.TokenResponse;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.AuthApi;
import es.pmdm.gymprofit.network.ProgramaApi;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.alta.CheckTrazado;
import es.pmdm.gymprofit.utils.ErrorAlta;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.NombreVisible;
import es.pmdm.gymprofit.utils.PerfilAlta;
import es.pmdm.gymprofit.utils.PoliticaCuenta;
import es.pmdm.gymprofit.utils.PushTokenManager;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// GuardaPlanActivity — «Guarda tu plan» (GP-103, tableros 9 y 10 del lienzo).
//
// La cuenta, al final y con tres campos: nombre, correo y contraseña con ojo, sin
// repetirla ni elegir usuario, que lo propone la API con el nombre (GP-147).
//
// Crear la cuenta son tres llamadas seguidas, y el botón se hace círculo y las enseña
// (momento 9 de DEC-039):
//   1. POST /auth/register con el perfil entero: la cuenta nace con lo contestado.
//   2. POST /auth/login con el correo (DEC-036), y el usuario para saber su id.
//   3. POST /programas/{codigo}/seguir con los minutos elegidos.
// Y acaba en «Todo listo», con vibración de éxito, y de ahí a los avisos.
//
// Los errores van bajo su campo (tablero 10): correo en uso con «Entrar con él», las
// reglas de la contraseña, y sin red un reintento que no pierde el plan; si la cuenta ya
// se creó, el reintento no la vuelve a crear, entra. Si seguir el programa falla, la
// cuenta vale: se apunta como pendiente e Inicio lo ofrece.
// ============================================================
public class GuardaPlanActivity extends BaseActivity {

    private final AuthApi authApi = ApiClient.service(AuthApi.class);
    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);
    private final ProgramaApi programaApi = ApiClient.service(ProgramaApi.class);

    private TextInputLayout tilNombre, tilCorreo, tilClave;
    private TextInputEditText etNombre, etCorreo, etClave;
    private MaterialButton crear;
    private View giro, pasos, errorCorreoEnUso;
    private TextView aviso;

    // Si el alta ya respondió bien: un reintento entra, no vuelve a crearla.
    private boolean cuentaCreada;
    private boolean creando;
    private int anchoBoton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guarda_plan);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(
                v -> getOnBackPressedDispatcher().onBackPressed());

        tilNombre = findViewById(R.id.tilNombreGuarda);
        tilCorreo = findViewById(R.id.tilCorreoGuarda);
        tilClave = findViewById(R.id.tilClaveGuarda);
        etNombre = findViewById(R.id.etNombreGuarda);
        etCorreo = findViewById(R.id.etCorreoGuarda);
        etClave = findViewById(R.id.etClaveGuarda);
        crear = findViewById(R.id.btnCrearCuenta);
        giro = findViewById(R.id.giroCrear);
        pasos = findViewById(R.id.pasosCrear);
        errorCorreoEnUso = findViewById(R.id.errorCorreoEnUso);
        aviso = findViewById(R.id.tvAvisoGuarda);
        cuentaCreada = savedInstanceState != null && savedInstanceState.getBoolean("cuentaCreada");

        crear.setStateListAnimator(android.animation.AnimatorInflater.loadStateListAnimator(this, R.animator.toque));
        crear.setOnClickListener(v -> crear());
        findViewById(R.id.btnEntrarGuarda).setOnClickListener(v -> irAEntrar(null));
        findViewById(R.id.btnEntrarConCorreo).setOnClickListener(v -> irAEntrar(texto(etCorreo)));
        montarLegal();
        limpiarAlEscribir(etNombre, tilNombre, null);
        limpiarAlEscribir(etCorreo, tilCorreo, errorCorreoEnUso);
        limpiarAlEscribir(etClave, tilClave, null);

        // Mientras se crea la cuenta, atrás no deja el alta a medias.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (creando) return;
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        if (savedInstanceState == null) {
            Movimiento.entrar(0, findViewById(R.id.filaPlanListo), findViewById(R.id.tvTituloGuarda),
                    findViewById(R.id.tvTextoGuarda), findViewById(R.id.bloqueNombre),
                    findViewById(R.id.bloqueCorreo), findViewById(R.id.bloqueClave), crear);
            Movimiento.saltar(findViewById(R.id.ivPlanListo), 400);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean("cuentaCreada", cuentaCreada);
    }

    // «Al crear tu cuenta confirmas que tienes 14 años o más, aceptas los Términos de uso
    // y has leído la Política de privacidad» (GP-087), con los dos nombres pulsables
    // dentro de la frase. La edad es opcional en el alta: si no se da, no se comprueba;
    // si se da, la API la exige de 14 en adelante (DEC-038).
    private void montarLegal() {
        TextView tv = findViewById(R.id.tvLegalGuarda);
        String terminos = getString(R.string.alta_terminos_enlace);
        String privacidad = getString(R.string.registro_aviso_privacidad_enlace);
        String frase = getString(R.string.guarda_legal, terminos, privacidad);
        SpannableString texto = new SpannableString(frase);
        enlace(texto, frase, terminos, getString(R.string.url_terminos));
        enlace(texto, frase, privacidad, getString(R.string.url_privacidad));
        tv.setText(texto);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void enlace(SpannableString texto, String frase, String trozo, String url) {
        int i = frase.indexOf(trozo);
        if (i < 0) return;
        texto.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                UIHelper.abrirUrl(GuardaPlanActivity.this, url);
            }
        }, i, i + trozo.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // ── Crear la cuenta ─────────────────────────────────────────────────────

    private void crear() {
        if (creando || !validar()) return;
        ocultarTeclado();
        aviso.setVisibility(View.GONE);
        empezarCreando();
        if (cuentaCreada) entrar();
        else registrar();
    }

    /**
     * Lo que la API no aceptaría no sale del móvil, y cada error va a su campo: nombre
     * obligatorio (es de donde sale el usuario y como te saluda la app), correo con
     * forma de correo y contraseña de la política (DEC-034).
     */
    private boolean validar() {
        String nombre = NombreVisible.paraEnviar(etNombre.getText());
        String correo = texto(etCorreo);
        String clave = texto(etClave);

        String errNombre = nombre.isEmpty() ? getString(R.string.guarda_error_nombre) : null;
        String errCorreo = null;
        if (correo.isEmpty()) errCorreo = getString(R.string.error_campo_requerido);
        else if (!PoliticaCuenta.correoLongitudValida(correo))
            errCorreo = getString(R.string.registro_error_email_largo, PoliticaCuenta.CORREO_MAX);
        else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches())
            errCorreo = getString(R.string.registro_email_invalido);
        String errClave = clave.isEmpty() ? getString(R.string.error_campo_requerido)
                : UIHelper.mensajePassword(this, PoliticaCuenta.problemaPassword(clave));

        errorCorreoEnUso.setVisibility(View.GONE);
        marcar(tilNombre, errNombre);
        marcar(tilCorreo, errCorreo);
        marcar(tilClave, errClave);
        if (errNombre != null) etNombre.requestFocus();
        else if (errCorreo != null) etCorreo.requestFocus();
        else if (errClave != null) etClave.requestFocus();
        return errNombre == null && errCorreo == null && errClave == null;
    }

    // Momento 10: el campo con error tiembla y su mensaje aparece debajo.
    private void marcar(TextInputLayout til, @Nullable String error) {
        til.setError(error);
        if (error != null) Movimiento.temblar(til);
    }

    private void registrar() {
        // El perfil entero en el alta: la cuenta nace con lo contestado (PerfilAlta).
        Map<String, Object> body = PerfilAlta.cuerpo(prefsManager.getBorradorRespuestas());
        body.put("email", texto(etCorreo));
        body.put("password", texto(etClave));
        body.put("nombre", NombreVisible.paraEnviar(etNombre.getText()));
        authApi.register(body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                if (isDestroyed()) return;
                cuentaCreada = true;
                entrar();
            }

            @Override
            public void onFail(int code, String message) {
                if (isDestroyed()) return;
                fallo(code, message);
            }
        });
    }

    // Entra con el correo y la contraseña recién puestos (DEC-036).
    private void entrar() {
        Map<String, Object> body = new HashMap<>();
        body.put("username", texto(etCorreo));
        body.put("password", texto(etClave));
        authApi.login(body).enqueue(new ApiCallback<TokenResponse>() {
            @Override
            public void onOk(TokenResponse t) {
                if (isDestroyed()) return;
                if (t == null) {
                    fallo(500, null);
                    return;
                }
                UtilREST.setToken(t.getToken());
                UtilREST.setRefreshToken(t.getRefreshToken());
                prefsManager.saveSesion(t.getToken(), t.getRefreshToken());
                prefsManager.saveUsername(t.getUsername());
                prefsManager.saveRol(t.rolPrincipal());
                pedirUsuario(t.getUsername());
            }

            @Override
            public void onFail(int code, String message) {
                if (isDestroyed()) return;
                fallo(code, message);
            }
        });
    }

    private void pedirUsuario(String username) {
        usuarioApi.getPorUsername(username).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                if (isDestroyed()) return;
                if (u == null) {
                    fallo(500, null);
                    return;
                }
                prefsManager.saveUsuarioId(u.getId());
                guardarPerfilLocal(username);
                PushTokenManager.registrar(GuardaPlanActivity.this);
                completarPaso(R.id.pasoCuenta);
                seguirPrograma();
            }

            @Override
            public void onFail(int code, String message) {
                if (isDestroyed()) return;
                fallo(code, message);
            }
        });
    }

    // Lo contestado pasa a ser el perfil de esta cuenta en el móvil (PerfilAlta), y el
    // nombre, el suyo para mostrar.
    private void guardarPerfilLocal(String username) {
        prefsManager.saveNombre(username, NombreVisible.paraEnviar(etNombre.getText()));
        PerfilAlta.guardarLocal(prefsManager, prefsManager.getBorradorRespuestas(), username);
    }

    // El programa de «Tu plan» con los minutos elegidos. Si falla, la cuenta vale: queda
    // pendiente e Inicio lo ofrece.
    private void seguirPrograma() {
        String codigo = prefsManager.getBorradorProgramaCodigo();
        int minutos = prefsManager.getBorradorMinutos();
        if (codigo.isEmpty()) {
            terminar();
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("minutos", minutos);
        String nombre = prefsManager.getBorradorProgramaNombre();
        programaApi.seguir(codigo, body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                if (isDestroyed()) return;
                completarPaso(R.id.pasoPrograma);
                terminar();
            }

            @Override
            public void onFail(int code, String message) {
                if (isDestroyed()) return;
                // La cuenta ya está creada y con su perfil: no se para el alta por esto.
                prefsManager.guardarProgramaPendiente(codigo, nombre, minutos);
                terminar();
            }
        });
    }

    private void terminar() {
        String usuario = prefsManager.getUsername();
        String nombre = NombreVisible.paraEnviar(etNombre.getText());
        String programa = prefsManager.getBorradorProgramaNombre();
        boolean conPrograma = !programa.isEmpty() && prefsManager.getProgramaPendienteCodigo().isEmpty();
        PerfilAlta.terminar(prefsManager, usuario);
        creando = false;

        Movimiento.vibrar(crear, Movimiento.Vibracion.EXITO);
        View capa = findViewById(R.id.capaListo);
        capa.setVisibility(View.VISIBLE);
        capa.setAlpha(0f);
        capa.animate().alpha(1f).setDuration(250).start();
        ((TextView) findViewById(R.id.tvTituloListo)).setText(getString(R.string.guarda_todo_listo, nombre));
        ((TextView) findViewById(R.id.tvTextoListo)).setText(conPrograma
                ? getString(R.string.guarda_todo_listo_texto, programa)
                : getString(R.string.guarda_todo_listo_texto_sin_programa));
        ((CheckTrazado) findViewById(R.id.checkListo)).mostrar();
        View aro = findViewById(R.id.aroListo);
        latir(aro);
        Movimiento.entrar(450, findViewById(R.id.tvTituloListo), findViewById(R.id.tvTextoListo));
        View seguir = findViewById(R.id.btnSeguirListo);
        Movimiento.entrarUna(seguir, 700, Movimiento.ENTRA, 16);
        seguir.setOnClickListener(v -> {
            startActivity(new Intent(this, AvisosActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
        findViewById(R.id.tvTituloListo).sendAccessibilityEvent(
                android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED);
    }

    // El aro de «Todo listo» late dos veces, como el del capítulo.
    private void latir(View aro) {
        if (Movimiento.quieto(this)) return;
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(Movimiento.LATIDO);
        a.setStartDelay(350);
        a.setRepeatCount(1);
        a.addUpdateListener(an -> {
            float t = (float) an.getAnimatedValue();
            aro.setScaleX(1f + 0.9f * t);
            aro.setScaleY(1f + 0.9f * t);
            aro.setAlpha(0.7f * (1f - t));
        });
        a.start();
    }

    // ── Estados del botón ───────────────────────────────────────────────────

    // Momento 9: el botón se hace círculo de 52 dp y gira; debajo, los pasos.
    private void empezarCreando() {
        creando = true;
        anchoBoton = crear.getWidth();
        int circulo = Math.round(52 * getResources().getDisplayMetrics().density);
        crear.setText(null);
        crear.setContentDescription(getString(R.string.guarda_creando_a11y));
        crear.setClickable(false);
        ValueAnimator a = ValueAnimator.ofInt(anchoBoton, circulo);
        a.setDuration(Movimiento.CIRCULO);
        a.setInterpolator(Movimiento.ESTANDAR);
        a.addUpdateListener(an -> {
            ViewGroup.LayoutParams lp = crear.getLayoutParams();
            lp.width = (int) an.getAnimatedValue();
            crear.setLayoutParams(lp);
        });
        a.start();
        giro.setVisibility(View.VISIBLE);

        pasos.setVisibility(View.VISIBLE);
        prepararPaso(R.id.pasoCuenta, getString(R.string.guarda_paso_cuenta), cuentaCreada);
        String programa = prefsManager.getBorradorProgramaNombre();
        View pasoPrograma = findViewById(R.id.pasoPrograma);
        pasoPrograma.setVisibility(programa.isEmpty() ? View.GONE : View.VISIBLE);
        prepararPaso(R.id.pasoPrograma, getString(R.string.guarda_paso_programa, programa), false);
        Movimiento.entrar(0, pasos);
    }

    private void prepararPaso(int id, String texto, boolean hecho) {
        View paso = findViewById(id);
        ((TextView) paso.findViewById(R.id.tvPaso)).setText(texto);
        paso.findViewById(R.id.giroPaso).setVisibility(hecho ? View.INVISIBLE : View.VISIBLE);
        paso.findViewById(R.id.checkPaso).setVisibility(hecho ? View.VISIBLE : View.INVISIBLE);
        paso.setContentDescription(texto);
    }

    private void completarPaso(int id) {
        View paso = findViewById(id);
        paso.findViewById(R.id.giroPaso).setVisibility(View.INVISIBLE);
        ImageView check = paso.findViewById(R.id.checkPaso);
        check.setVisibility(View.VISIBLE);
        Movimiento.saltar(check, 300);
        paso.setContentDescription(getString(R.string.guarda_paso_hecho_a11y,
                ((TextView) paso.findViewById(R.id.tvPaso)).getText()));
    }

    // Vuelve al formulario con lo escrito intacto y dice qué ha pasado.
    private void fallo(int code, @Nullable String cuerpo) {
        creando = false;
        giro.setVisibility(View.GONE);
        pasos.setVisibility(View.GONE);
        crear.setClickable(true);
        crear.setContentDescription(null);
        ViewGroup.LayoutParams lp = crear.getLayoutParams();
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        crear.setLayoutParams(lp);

        ErrorAlta.Tipo tipo = ErrorAlta.de(code, cuerpo);
        crear.setText(tipo == ErrorAlta.Tipo.SIN_RED ? R.string.btn_reintentar : R.string.guarda_crear_cuenta);
        switch (tipo) {
            case CORREO_EN_USO:
                // El error en el campo y, debajo, «Entrar con él» con ese correo.
                marcar(tilCorreo, getString(R.string.registro_error_email_en_uso));
                errorCorreoEnUso.setVisibility(View.VISIBLE);
                Movimiento.aparecerMensaje(errorCorreoEnUso);
                etCorreo.requestFocus();
                break;
            case PASSWORD_COMUN:
                marcar(tilClave, getString(R.string.password_error_comun));
                etClave.requestFocus();
                break;
            case PASSWORD_CONTIENE_NOMBRE:
                marcar(tilClave, getString(R.string.guarda_error_clave_nombre));
                etClave.requestFocus();
                break;
            case SIN_RED:
                Movimiento.vibrar(crear, Movimiento.Vibracion.ERROR);
                aviso.setText(R.string.guarda_sin_red);
                aviso.setVisibility(View.VISIBLE);
                Movimiento.aparecerMensaje(aviso);
                break;
            default:
                Movimiento.vibrar(crear, Movimiento.Vibracion.ERROR);
                UiFeedback.toastError(this, code, cuerpo);
        }
    }

    // ── Utilidades ──────────────────────────────────────────────────────────

    // El error de un campo se va en cuanto se vuelve a escribir en él: ya no dice la verdad.
    private void limpiarAlEscribir(TextInputEditText et, TextInputLayout til, @Nullable View extra) {
        et.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(android.text.Editable e) {
                if (til.getError() != null) til.setError(null);
                if (extra != null) extra.setVisibility(View.GONE);
            }
        });
    }

    // A «Ya tengo cuenta», con el correo puesto si llega uno.
    private void irAEntrar(@Nullable String correo) {
        Intent i = new Intent(this, LoginActivity.class);
        if (correo != null && !correo.isEmpty()) i.putExtra(LoginActivity.EXTRA_USUARIO, correo);
        startActivity(i);
    }

    private void ocultarTeclado() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View foco = getCurrentFocus();
        if (imm != null && foco != null) imm.hideSoftInputFromWindow(foco.getWindowToken(), 0);
    }

    // Recortado, como en todas las pantallas de contraseña (DEC-034).
    private static String texto(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
