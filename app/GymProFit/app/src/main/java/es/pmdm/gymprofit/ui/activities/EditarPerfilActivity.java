package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.utils.CalculadoraNutricional;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.ResultadoNutricional;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// EditarPerfilActivity — pantalla para editar el perfil del usuario.
// Permite modificar email, peso, altura, edad, nivel de experiencia y
// objetivo, guarda los cambios vía PATCH y recalcula las macros
// nutricionales locales con los nuevos datos.
//
// El correo NO viaja en el PATCH (GP-083): si se ha cambiado, primero va por
// PUT /usuarios/me/email con la contraseña actual, y solo si sale bien se
// guarda el resto del perfil.
// ============================================================
public class EditarPerfilActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private PreferencesManager prefsManager;
    private TextInputEditText etEmail, etPeso, etAltura, etEdad, etPasswordEmail;
    private TextInputLayout tilEmail, tilPasswordEmail;
    // Correo que tiene la cuenta, tal como lo devolvió la API. null mientras no ha
    // cargado: sin él no se sabe si lo escrito es un cambio, y el campo sigue bloqueado.
    private String emailOriginal;
    private Spinner spNivel, spObjetivo;
    // Interfaz Retrofit tipada del dominio usuarios (etapa 2)
    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);

    // Valores enviados a la API para nivel de experiencia.
    private static final String[] NIVELES = {
            "PRINCIPIANTE", "INTERMEDIO", "AVANZADO", "EXPERTO"
    };
    // Valores enviados a la API para el objetivo del usuario.
    private static final String[] OBJETIVOS = {
            "PERDER_PESO", "GANAR_MASA_MUSCULAR", "MANTENER_PESO", "MEJORAR_FUERZA"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        setContentView(R.layout.activity_editar_perfil);

        inicializarVistas();
        configurarSpinners();
        cargarDatosUsuario();
        configurarBotones();
    }

    // Enlaza las referencias a las vistas del layout.
    private void inicializarVistas() {
        etEmail    = findViewById(R.id.etEmail);
        tilEmail   = findViewById(R.id.tilEmail);
        etPasswordEmail  = findViewById(R.id.etPasswordEmail);
        tilPasswordEmail = findViewById(R.id.tilPasswordEmail);
        etEmail.setEnabled(false);
        etPeso     = findViewById(R.id.etPeso);
        etAltura   = findViewById(R.id.etAltura);
        etEdad     = findViewById(R.id.etEdad);
        spNivel    = findViewById(R.id.spNivel);
        spObjetivo = findViewById(R.id.spObjetivo);
    }

    // Configura los spinners de nivel y objetivo con textos localizados
    // (los valores reales enviados a la API son NIVELES/OBJETIVOS).
    private void configurarSpinners() {
        String[] nivelesDisplay = {
                getString(R.string.nivel_principiante),
                getString(R.string.nivel_intermedio),
                getString(R.string.nivel_avanzado),
                getString(R.string.nivel_experto)
        };
        ArrayAdapter<String> adapterNivel = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, nivelesDisplay);
        adapterNivel.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNivel.setAdapter(adapterNivel);

        String[] objetivosDisplay = {
                getString(R.string.objetivo_perder_peso),
                getString(R.string.objetivo_ganar_musculo),
                getString(R.string.objetivo_mantener),
                getString(R.string.objetivo_fuerza)
        };
        ArrayAdapter<String> adapterObjetivo = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, objetivosDisplay);
        adapterObjetivo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spObjetivo.setAdapter(adapterObjetivo);
    }

    // Obtiene los datos del usuario actual desde la API y rellena el formulario.
    private void cargarDatosUsuario() {
        int id = prefsManager.getUsuarioId();
        if (id == -1) return;

        // Perfil ya deserializado a Usuario por Gson; ApiCallback entrega en hilo UI.
        usuarioApi.getPorId(id).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                if (u == null) return;
                if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                    emailOriginal = u.getEmail();
                    etEmail.setText(u.getEmail());
                    etEmail.setEnabled(true);
                }
                if (u.getPeso() != null && !u.getPeso().isEmpty())
                    etPeso.setText(u.getPeso());
                if (u.getAltura() > 0)
                    etAltura.setText(String.valueOf((int) u.getAltura()));
                if (u.getEdad() > 0)
                    etEdad.setText(String.valueOf(u.getEdad()));
                seleccionarSpinner(spNivel, NIVELES, u.getNivelExperiencia());
                seleccionarSpinner(spObjetivo, OBJETIVOS, u.getObjetivo());
            }

            @Override
            public void onFail(int code, String message) {
                // Sin el perfil cargado, guardar pisaría los datos reales con el
                // formulario en blanco: se avisa y el correo sigue bloqueado. El 401
                // ya lo resuelve el aviso global.
                if (code != 401) {
                    UIHelper.mostrarToastError(EditarPerfilActivity.this,
                            getString(R.string.editar_perfil_error_carga));
                }
            }
        });
    }

    /**
     * Dice si lo escrito en el campo de correo es un cambio respecto al de la cuenta.
     *
     * <p>Sin distinguir mayúsculas, como la restricción única de la base: cambiar solo
     * mayúsculas no es otro correo y no merece pedir la contraseña.
     *
     * @param original correo de la cuenta, o null si no ha cargado.
     * @param escrito lo que hay en el campo.
     * @return {@code true} si hay que ir por el cambio de correo con contraseña.
     */
    static boolean cambiaEmail(String original, String escrito) {
        if (original == null || escrito == null) return false;
        return !escrito.trim().equalsIgnoreCase(original.trim());
    }

    // Muestra el campo de contraseña solo mientras el correo escrito es distinto del actual.
    private void vigilarCambioDeEmail() {
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void afterTextChanged(Editable e) {
                tilEmail.setError(null);
                boolean cambia = cambiaEmail(emailOriginal, e.toString());
                tilPasswordEmail.setVisibility(cambia ? View.VISIBLE : View.GONE);
                if (!cambia) {
                    etPasswordEmail.setText("");
                    tilPasswordEmail.setError(null);
                }
            }
        });
        etPasswordEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void afterTextChanged(Editable e) { tilPasswordEmail.setError(null); }
        });
    }

    // Selecciona en el spinner la posición cuyo valor coincide con el actual del usuario.
    private void seleccionarSpinner(Spinner spinner, String[] valores, String valorActual) {
        if (valorActual == null || valorActual.isEmpty()) return;
        for (int i = 0; i < valores.length; i++) {
            if (valores[i].equalsIgnoreCase(valorActual)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void configurarBotones() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardarCambios());
        vigilarCambioDeEmail();
    }

    // Valida el email, construye el PATCH con los campos editados, y al
    // tener éxito actualiza las preferencias locales y recalcula las
    // macros nutricionales con los nuevos datos del usuario.
    private void guardarCambios() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.editar_perfil_email_requerido));
            etEmail.requestFocus();
            return;
        }

        if (!cambiaEmail(emailOriginal, email)) {
            guardarPerfil();
            return;
        }

        // El formato se mira aquí para no gastar un viaje, pero la API lo vuelve a mirar.
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.registro_email_invalido));
            etEmail.requestFocus();
            return;
        }
        String password = etPasswordEmail.getText() != null ? etPasswordEmail.getText().toString() : "";
        if (password.isEmpty()) {
            tilPasswordEmail.setError(getString(R.string.editar_perfil_password_requerida));
            etPasswordEmail.requestFocus();
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        LoadingDialog.show(this);
        usuarioApi.cambiarEmail(body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignorado) {
                // El correo ya es el nuevo: si el resto del perfil fallara ahora, al
                // reintentar no se volvería a pedir la contraseña por un cambio hecho.
                emailOriginal = email;
                tilPasswordEmail.setVisibility(View.GONE);
                etPasswordEmail.setText("");
                guardarPerfil();
            }

            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(EditarPerfilActivity.this);
                // Cada rechazo va al campo que lo causa, y el usuario no pierde nada.
                if (code == 403) {
                    tilPasswordEmail.setError(getString(R.string.editar_perfil_password_incorrecta));
                    etPasswordEmail.requestFocus();
                } else if (code == 409) {
                    tilEmail.setError(getString(R.string.editar_perfil_email_en_uso));
                    etEmail.requestFocus();
                } else if (code == 400) {
                    tilEmail.setError(getString(R.string.registro_email_invalido));
                    etEmail.requestFocus();
                } else {
                    // 401 lo resuelve el aviso global; el resto, el mensaje por código.
                    UiFeedback.toastError(EditarPerfilActivity.this, code, message);
                }
            }
        });
    }

    // Guarda el resto del perfil por PATCH. El correo no va: se cambia por su ruta.
    private void guardarPerfil() {
        int id = prefsManager.getUsuarioId();
        try {
            // Cuerpo de escritura como Map: los decimales viajan como BigDecimal y un
            // valor null BORRA el campo (Gson con serializeNulls, equivalente al antiguo JSONObject.NULL).
            Map<String, Object> body = new HashMap<>();

            String pesoStr = etPeso.getText() != null ? etPeso.getText().toString().trim() : "";
            body.put("peso", pesoStr.isEmpty() ? null
                    : new BigDecimal(pesoStr.replace(",", ".")));

            String alturaStr = etAltura.getText() != null ? etAltura.getText().toString().trim() : "";
            body.put("altura", alturaStr.isEmpty() ? null
                    : new BigDecimal(alturaStr));

            String edadStr = etEdad.getText() != null ? etEdad.getText().toString().trim() : "";
            body.put("edad", edadStr.isEmpty() ? null
                    : Integer.parseInt(edadStr));

            body.put("nivelExperiencia", NIVELES[spNivel.getSelectedItemPosition()]);
            body.put("objetivo", OBJETIVOS[spObjetivo.getSelectedItemPosition()]);

            // Muestra el overlay de carga mientras se guarda el perfil
            LoadingDialog.show(this);
            usuarioApi.patch(id, body).enqueue(new ApiCallback<Void>() {
                @Override
                public void onOk(Void response) {
                    // Oculta el overlay al guardar con exito
                    LoadingDialog.hide(EditarPerfilActivity.this);
                    // Guardar datos de perfil en prefs para recálculo de macros
                    if (!pesoStr.isEmpty()) prefsManager.savePeso(Double.parseDouble(pesoStr.replace(",", ".")));
                    if (!alturaStr.isEmpty()) prefsManager.saveAltura(Double.parseDouble(alturaStr));
                    if (!edadStr.isEmpty()) prefsManager.saveEdad(Integer.parseInt(edadStr));
                    String objetivoSeleccionado = OBJETIVOS[spObjetivo.getSelectedItemPosition()];
                    prefsManager.saveObjetivo(objetivoSeleccionado);
                    prefsManager.saveNivel(NIVELES[spNivel.getSelectedItemPosition()]);

                    // Recalcular macros con los nuevos datos
                    double peso = prefsManager.getPeso();
                    double altura = prefsManager.getAltura();
                    int edad = prefsManager.getEdad();
                    boolean esHombre = "HOMBRE".equals(prefsManager.getSexo());
                    String actividad = prefsManager.getActividad();
                    ResultadoNutricional r = CalculadoraNutricional.calcular(peso, altura, edad, esHombre, actividad, objetivoSeleccionado);
                    prefsManager.saveResultadoNutricional(r.calorias, r.proteinas, r.carbohidratos, r.grasas, r.agua);

                    UIHelper.mostrarToastExito(EditarPerfilActivity.this,
                            getString(R.string.editar_perfil_guardado));
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onFail(int code, String message) {
                    // Oculta el overlay y mapea el error de red segun el codigo
                    LoadingDialog.hide(EditarPerfilActivity.this);
                    UiFeedback.toastError(EditarPerfilActivity.this, code, message);
                }
            });
        } catch (NumberFormatException e) {
            LoadingDialog.hide(this);
            UIHelper.mostrarToastError(this, getString(R.string.editar_perfil_error));
        }
    }
}
