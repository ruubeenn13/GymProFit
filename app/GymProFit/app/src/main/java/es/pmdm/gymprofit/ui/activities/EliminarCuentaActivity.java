package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.record.Records;
import es.pmdm.gymprofit.model.usuario.UsuarioEstadisticas;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.RecordApi;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.ConfirmarEliminar;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// EliminarCuentaActivity — borrado definitivo de la cuenta (GP-008; rehecha en GP-216, tablero 21).
//
// Es una pantalla y NO un diálogo: un diálogo se cierra tocando fuera, y esa
// facilidad para salir es exactamente lo contrario de la deliberación que pide
// algo irreversible. Cuatro líneas de lo que se pierde (con tus números de verdad cuando hay
// red), el detalle plegado, la contraseña y una casilla: marcarla enciende el botón, y con la
// contraseña vacía pulsar la pide en vez de borrar (ConfirmarEliminar). El borrado en sí no
// cambia: el mismo DELETE con la contraseña, y nada queda en el móvil.
//
// El texto es el mismo que el de gymprofit.app/eliminar-cuenta. Si las dos versiones prometieran
// cosas distintas, una de las dos estaría mintiendo, y la publicada es la que Google Play tiene
// enlazada. La política de privacidad NO está dentro del APK: el enlace la abre en el navegador,
// para poder corregirla sin publicar una versión nueva.
// ============================================================
public class EliminarCuentaActivity extends BaseActivity {

    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;
    private MaterialCheckBox cbConfirmacion;
    private com.google.android.material.button.MaterialButton btnEliminar;
    private boolean borrando;

    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eliminar_cuenta);

        inicializarVistas();
        configurarEventos();
        actualizarBoton();
        cargarNumeros();
    }

    // Enlaza las vistas del formulario de confirmación y la barra superior.
    private void inicializarVistas() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilPassword    = findViewById(R.id.tilEliminarPassword);
        etPassword     = findViewById(R.id.etEliminarPassword);
        cbConfirmacion = findViewById(R.id.cbEliminarConfirmacion);
        btnEliminar    = findViewById(R.id.btnEliminarCuenta);

        linea(R.id.pierdeCuenta, R.drawable.ic_ms_person, R.string.eliminar_pierde_cuenta, R.string.eliminar_pierde_cuenta_sub);
        linea(R.id.pierdeEntrenos, R.drawable.ic_ms_fitness_center, R.string.eliminar_pierde_entrenos,
                R.string.eliminar_pierde_entrenos_sub);
        linea(R.id.pierdeDiario, R.drawable.ic_ms_restaurant, R.string.eliminar_pierde_diario, R.string.eliminar_pierde_diario_sub);
        linea(R.id.pierdeProgreso, R.drawable.ic_ms_monitoring, R.string.eliminar_pierde_progreso,
                R.string.eliminar_pierde_progreso_sub);
    }

    // Una línea de «lo que pierdes»: informa, no se pulsa.
    private void linea(int id, @DrawableRes int icono, @StringRes int titulo, @StringRes int sub) {
        View f = findViewById(id);
        ((ImageView) f.findViewById(R.id.ivIcono)).setImageResource(icono);
        ((TextView) f.findViewById(R.id.tvTitulo)).setText(titulo);
        TextView tvSub = f.findViewById(R.id.tvSub);
        tvSub.setText(sub);
        tvSub.setVisibility(View.VISIBLE);
        f.findViewById(R.id.ivFlecha).setVisibility(View.GONE);
        f.setClickable(false);
        f.setBackground(null);
        f.setContentDescription(getString(R.string.ajustes_fila_sub_a11y, getString(titulo), getString(sub)));
    }

    // Tus números de verdad en la línea de los entrenos: «13 entrenos y 25 récords». Si no llegan, la
    // línea queda en su texto de siempre; sin ellos la pantalla se entiende igual.
    private void cargarNumeros() {
        int uid = prefsManager.getUsuarioId();
        if (uid == -1) return;
        final int[] numeros = {-1, -1};
        usuarioApi.getEstadisticas(uid).enqueue(new ApiCallback<UsuarioEstadisticas>() {
            @Override
            public void onOk(UsuarioEstadisticas e) {
                if (e == null) return;
                numeros[0] = e.getSesionesCompletadas();
                pintarNumeros(numeros);
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito: es un dato de apoyo, no una condición para borrar.
            }
        });
        ApiClient.service(RecordApi.class).getRecords(null).enqueue(new ApiCallback<Records>() {
            @Override
            public void onOk(Records r) {
                if (r == null) return;
                numeros[1] = r.getRecords().size();
                pintarNumeros(numeros);
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito, como los entrenos: un dato de apoyo.
            }
        });
    }

    private void pintarNumeros(int[] n) {
        if (isFinishing() || n[0] < 0 || n[1] < 0) return;
        NumberFormat nf = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(this));
        String titulo = getString(R.string.eliminar_pierde_entrenos_n,
                getResources().getQuantityString(R.plurals.eliminar_entrenos, n[0], nf.format(n[0])),
                getResources().getQuantityString(R.plurals.eliminar_records, n[1], nf.format(n[1])));
        View f = findViewById(R.id.pierdeEntrenos);
        ((TextView) f.findViewById(R.id.tvTitulo)).setText(titulo);
        f.setContentDescription(getString(R.string.ajustes_fila_sub_a11y, titulo,
                getString(R.string.eliminar_pierde_entrenos_sub)));
    }

    // Cablea el enlace a la política, el detalle plegado, la casilla y el borrado.
    private void configurarEventos() {
        findViewById(R.id.llEliminarConservacion).setOnClickListener(v ->
                UIHelper.abrirUrl(this, getString(R.string.url_privacidad_conservacion)));

        findViewById(R.id.btnVerDetalle).setOnClickListener(v -> alternarDetalle());
        findViewById(R.id.btnMejorNo).setOnClickListener(v -> finish());

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                tilPassword.setError(null);
            }
        });
        cbConfirmacion.setOnCheckedChangeListener((v, marcado) -> actualizarBoton());

        btnEliminar.setOnClickListener(v -> eliminarCuenta());
    }

    // Despliega o pliega el detalle de lo que se borra y lo que no.
    private void alternarDetalle() {
        View detalle = findViewById(R.id.detallePierdes);
        boolean abrir = detalle.getVisibility() != View.VISIBLE;
        detalle.setVisibility(abrir ? View.VISIBLE : View.GONE);
        ((ImageView) findViewById(R.id.ivVerDetalle)).setImageResource(abrir
                ? R.drawable.ic_ms_keyboard_arrow_up : R.drawable.ic_ms_keyboard_arrow_down);
        ((TextView) findViewById(R.id.tvVerDetalle)).setText(abrir
                ? R.string.eliminar_cuenta_ocultar_detalle : R.string.eliminar_cuenta_ver_detalle);
        // El estado nuevo, dicho a TalkBack en la fila que se acaba de tocar.
        findViewById(R.id.btnVerDetalle).setContentDescription(getString(abrir
                ? R.string.eliminar_cuenta_ocultar_detalle : R.string.eliminar_cuenta_ver_detalle));
    }

    /**
     * La contraseña tal como se envía: sin los espacios de los extremos, como al
     * entrar, al cambiarla y al cambiar el correo (DEC-034). El teclado del móvil
     * añade un espacio al final sin que se vea; los de dentro sí cuentan.
     *
     * @param tecleado lo que hay en el campo.
     */
    static String contrasenaParaEnviar(String tecleado) {
        return tecleado == null ? "" : tecleado.trim();
    }

    // El botón se enciende al marcar la casilla (ConfirmarEliminar).
    private void actualizarBoton() {
        btnEliminar.setEnabled(ConfirmarEliminar.botonHabilitado(cbConfirmacion.isChecked(), borrando));
    }

    /**
     * Pide a la API el borrado definitivo y, si sale, deja el móvil sin rastro.
     *
     * <p>El usuario que se borra sale del token (DEC-013): el cuerpo solo lleva la
     * contraseña actual, que la API comprueba antes de tocar la primera tabla.
     */
    private void eliminarCuenta() {
        String password = texto();
        ConfirmarEliminar.Paso paso = ConfirmarEliminar.alPulsar(cbConfirmacion.isChecked(), password);
        if (paso == ConfirmarEliminar.Paso.NADA) return;
        if (paso == ConfirmarEliminar.Paso.PEDIR_CLAVE) {
            tilPassword.setError(getString(R.string.eliminar_cuenta_error_password_vacia));
            etPassword.requestFocus();
            return;
        }
        tilPassword.setError(null);

        Map<String, Object> body = new HashMap<>();
        body.put("password", contrasenaParaEnviar(password));

        LoadingDialog.show(this, getString(R.string.eliminar_cuenta_cargando));
        // Mientras la petición viaja, el botón no acepta un segundo toque: repetir
        // un borrado en vuelo solo puede acabar en un 403 confuso contra una cuenta
        // que ya no existe.
        borrando = true;
        actualizarBoton();

        usuarioApi.eliminarCuentaPropia(body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignorado) {
                LoadingDialog.hide(EliminarCuentaActivity.this);

                // Ya no hay cuenta al otro lado: el token en memoria y TODO lo
                // guardado en el móvil sobran, incluido el perfil local que
                // cerrarSesion() conserva a propósito para volver a entrar.
                UtilREST.clearToken();
                // La sesión en curso, si había, también se va del móvil (GP-012).
                es.pmdm.gymprofit.envivo.SesionEnCursoRepositorio.get(EliminarCuentaActivity.this)
                        .borrarDeCuenta(prefsManager.getUsuarioId());
                prefsManager.borrarDatosLocales();

                UIHelper.mostrarToastExito(EliminarCuentaActivity.this,
                        getString(R.string.eliminar_cuenta_ok));

                // Al login sin dejar nada detrás: con la cuenta borrada, cualquier
                // pantalla que siguiera en la pila pintaría datos de un usuario
                // que ya no existe en cuanto se pulsara atrás.
                Intent intent = new Intent(EliminarCuentaActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(EliminarCuentaActivity.this);
                borrando = false;
                actualizarBoton();

                if (code == 403) {
                    // La contraseña no es la de la cuenta: el error va al campo que
                    // lo causa y el usuario se queda donde está, sin perder nada.
                    tilPassword.setError(getString(R.string.eliminar_cuenta_error_password));
                    etPassword.requestFocus();
                } else if (code == 401) {
                    // Sesión irrecuperable: el listener global de BaseActivity ya
                    // limpia y lleva al login. Un toast encima solo estorbaría.
                    Log.w("GymProFit", "eliminarCuentaPropia 401: sesión expirada");
                } else {
                    // Red caída, servidor dormido o 5xx: el aviso general. Nunca en
                    // silencio, que aquí el silencio se leería como "ya está hecho".
                    UiFeedback.toastError(EliminarCuentaActivity.this, code, message);
                }
            }
        });
    }

    // Contenido actual del campo de contraseña, sin nulos.
    private String texto() {
        return etPassword.getText() == null ? "" : etPassword.getText().toString();
    }
}
