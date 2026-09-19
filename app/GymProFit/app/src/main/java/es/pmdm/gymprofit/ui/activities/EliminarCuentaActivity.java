package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// EliminarCuentaActivity — borrado definitivo de la cuenta (GP-008).
//
// Es una pantalla y NO un diálogo: un diálogo se cierra tocando fuera, y esa
// facilidad para salir es exactamente lo contrario de la deliberación que pide
// algo irreversible. Aquí hay que desplazarse por lo que se pierde, teclear la
// contraseña y marcar la casilla antes de que el botón se encienda.
//
// El texto explicativo es el mismo que el de gymprofit.app/eliminar-cuenta. Si
// las dos versiones prometieran cosas distintas, una de las dos estaría
// mintiendo, y la publicada es la que Google Play tiene enlazada.
//
// La política de privacidad NO está dentro del APK: el enlace la abre en el
// navegador, para poder corregirla sin publicar una versión nueva.
// ============================================================
public class EliminarCuentaActivity extends BaseActivity {

    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;
    private MaterialCheckBox cbConfirmacion;
    private com.google.android.material.button.MaterialButton btnEliminar;

    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eliminar_cuenta);

        inicializarVistas();
        configurarEventos();
        actualizarBoton();
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
    }

    // Cablea el enlace a la política, el interruptor del botón y el borrado.
    private void configurarEventos() {
        findViewById(R.id.llEliminarConservacion).setOnClickListener(v ->
                UIHelper.abrirUrl(this, getString(R.string.url_privacidad_conservacion)));

        // El botón se enciende solo cuando hay contraseña Y casilla marcada: no
        // llega con haber bajado hasta aquí, hay que hacer las dos cosas.
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                tilPassword.setError(null);
                actualizarBoton();
            }
        });
        cbConfirmacion.setOnCheckedChangeListener((v, marcado) -> actualizarBoton());

        btnEliminar.setOnClickListener(v -> eliminarCuenta());
    }

    /**
     * Decide si el botón de borrar puede estar activo.
     *
     * <p>Es estático y sin dependencias de Android a propósito: es la única
     * barrera entre un toque distraído y una cuenta vaciada, y así se puede
     * probar sin levantar la pantalla entera.
     *
     * @param password lo tecleado en el campo de contraseña, tal cual.
     * @param confirmado si la casilla de "entiendo que no se puede deshacer" está marcada.
     * @return {@code true} solo si hay contraseña no vacía y casilla marcada.
     */
    static boolean puedeEliminar(String password, boolean confirmado) {
        return confirmado && password != null && !password.trim().isEmpty();
    }

    // Refleja en el botón el estado actual del formulario.
    private void actualizarBoton() {
        btnEliminar.setEnabled(puedeEliminar(texto(), cbConfirmacion.isChecked()));
    }

    /**
     * Pide a la API el borrado definitivo y, si sale, deja el móvil sin rastro.
     *
     * <p>El usuario que se borra sale del token (DEC-013): el cuerpo solo lleva la
     * contraseña actual, que la API comprueba antes de tocar la primera tabla.
     */
    private void eliminarCuenta() {
        String password = texto();
        if (!puedeEliminar(password, cbConfirmacion.isChecked())) {
            tilPassword.setError(getString(R.string.eliminar_cuenta_error_password_vacia));
            return;
        }
        tilPassword.setError(null);

        Map<String, Object> body = new HashMap<>();
        body.put("password", password);

        LoadingDialog.show(this, getString(R.string.eliminar_cuenta_cargando));
        // Mientras la petición viaja, el botón no acepta un segundo toque: repetir
        // un borrado en vuelo solo puede acabar en un 403 confuso contra una cuenta
        // que ya no existe.
        btnEliminar.setEnabled(false);

        usuarioApi.eliminarCuentaPropia(body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignorado) {
                LoadingDialog.hide(EliminarCuentaActivity.this);

                // Ya no hay cuenta al otro lado: el token en memoria y TODO lo
                // guardado en el móvil sobran, incluido el perfil local que
                // cerrarSesion() conserva a propósito para volver a entrar.
                UtilREST.clearToken();
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
                } else if (code == 404) {
                    // UiFeedback trata el 404 como "lista vacía" y no dice nada; aquí
                    // no hay nada benigno que callar, así que se avisa a mano.
                    UIHelper.mostrarToastError(EliminarCuentaActivity.this,
                            getString(R.string.feedback_error_generico));
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
