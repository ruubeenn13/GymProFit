package es.pmdm.gymprofit.ui.activities;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
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
// ============================================================
public class EditarPerfilActivity extends AppCompatActivity {

    private PreferencesManager prefsManager;
    private TextInputEditText etEmail, etPeso, etAltura, etEdad;
    private Spinner spNivel, spObjetivo;
    // Interfaz Retrofit tipada del dominio usuarios (etapa 2)
    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);

    // Valores enviados a la API para nivel de experiencia.
    private static final String[] NIVELES = {
            "PRINCIPIANTE", "INTERMEDIO", "AVANZADO", "EXPERTO"
    };
    // Valores enviados a la API para el objetivo del usuario.
    private static final String[] OBJETIVOS = {
            "PERDER_PESO", "GANAR_MASA_MUSCULAR", "MANTENER_PESO",
            "MEJORAR_RESISTENCIA", "MEJORAR_FUERZA", "REDUCIR_GRASA_CORPORAL",
            "MEJORAR_FLEXIBILIDAD", "MEJORAR_VELOCIDAD", "AUMENTAR_CALORIAS", "MEJORAR_MOVILIDAD"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdioma();
        setContentView(R.layout.activity_editar_perfil);

        inicializarVistas();
        configurarSpinners();
        cargarDatosUsuario();
        configurarBotones();
    }

    // Enlaza las referencias a las vistas del layout.
    private void inicializarVistas() {
        etEmail    = findViewById(R.id.etEmail);
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
                getString(R.string.objetivo_resistencia),
                getString(R.string.objetivo_fuerza),
                getString(R.string.objetivo_reducir_grasa),
                getString(R.string.objetivo_flexibilidad),
                getString(R.string.objetivo_velocidad),
                getString(R.string.objetivo_aumentar_calorias),
                getString(R.string.objetivo_movilidad)
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
                if (u.getEmail() != null && !u.getEmail().isEmpty())
                    etEmail.setText(u.getEmail());
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
            public void onFail(int code, String message) {}
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
    }

    // Valida el email, construye el PATCH con los campos editados, y al
    // tener éxito actualiza las preferencias locales y recalcula las
    // macros nutricionales con los nuevos datos del usuario.
    private void guardarCambios() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        if (email.isEmpty()) {
            UIHelper.mostrarToastError(this, getString(R.string.editar_perfil_email_requerido));
            return;
        }

        int id = prefsManager.getUsuarioId();
        try {
            // Cuerpo de escritura como Map: los decimales viajan como BigDecimal y un
            // valor null BORRA el campo (Gson con serializeNulls, equivalente al antiguo JSONObject.NULL).
            Map<String, Object> body = new HashMap<>();
            body.put("email", email);

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
            UIHelper.mostrarToastError(this, getString(R.string.editar_perfil_error));
        }
    }

    // Aplica el idioma guardado en preferencias a la configuración de recursos.
    private void aplicarIdioma() {
        String lang = prefsManager.getLanguage();
        if (!lang.isEmpty()) {
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            Resources resources = getResources();
            Configuration config = resources.getConfiguration();
            config.setLocale(locale);
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        }
    }
}
