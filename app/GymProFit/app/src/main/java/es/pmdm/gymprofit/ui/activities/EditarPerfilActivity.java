package es.pmdm.gymprofit.ui.activities;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.CalculadoraNutricional;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.ResultadoNutricional;
import es.pmdm.gymprofit.utils.UIHelper;

public class EditarPerfilActivity extends AppCompatActivity {

    private PreferencesManager prefsManager;
    private TextInputEditText etEmail, etPeso, etAltura, etEdad;
    private Spinner spNivel, spObjetivo;

    private static final String[] NIVELES = {
            "PRINCIPIANTE", "INTERMEDIO", "AVANZADO"
    };
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

    private void inicializarVistas() {
        etEmail    = findViewById(R.id.etEmail);
        etPeso     = findViewById(R.id.etPeso);
        etAltura   = findViewById(R.id.etAltura);
        etEdad     = findViewById(R.id.etEdad);
        spNivel    = findViewById(R.id.spNivel);
        spObjetivo = findViewById(R.id.spObjetivo);
    }

    private void configurarSpinners() {
        String[] nivelesDisplay = {
                getString(R.string.nivel_principiante),
                getString(R.string.nivel_intermedio),
                getString(R.string.nivel_avanzado)
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

    private void cargarDatosUsuario() {
        int id = prefsManager.getUsuarioId();
        if (id == -1) return;

        API.getUsuarioPorId(id, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    Usuario u = UtilJSONParser.parseUsuario(response);
                    if (u == null) return;
                    runOnUiThread(() -> {
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
                    });
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(String message, int statusCode) {}
        });
    }

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

    private void guardarCambios() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        if (email.isEmpty()) {
            UIHelper.mostrarToastError(this, getString(R.string.editar_perfil_email_requerido));
            return;
        }

        int id = prefsManager.getUsuarioId();
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);

            String pesoStr = etPeso.getText() != null ? etPeso.getText().toString().trim() : "";
            body.put("peso", pesoStr.isEmpty() ? JSONObject.NULL
                    : Double.parseDouble(pesoStr.replace(",", ".")));

            String alturaStr = etAltura.getText() != null ? etAltura.getText().toString().trim() : "";
            body.put("altura", alturaStr.isEmpty() ? JSONObject.NULL
                    : Double.parseDouble(alturaStr));

            String edadStr = etEdad.getText() != null ? etEdad.getText().toString().trim() : "";
            body.put("edad", edadStr.isEmpty() ? JSONObject.NULL
                    : Integer.parseInt(edadStr));

            body.put("nivelExperiencia", NIVELES[spNivel.getSelectedItemPosition()]);
            body.put("objetivo", OBJETIVOS[spObjetivo.getSelectedItemPosition()]);

            API.patchUsuario(id, body, new UtilREST.OnResponseListener() {
                @Override
                public void onSuccess(String response, int statusCode) {
                    runOnUiThread(() -> {
                        // Guardar datos de perfil en prefs para recálculo de macros
                        if (!pesoStr.isEmpty()) prefsManager.savePeso(Double.parseDouble(pesoStr.replace(",", ".")));
                        if (!alturaStr.isEmpty()) prefsManager.saveAltura(Double.parseDouble(alturaStr));
                        if (!edadStr.isEmpty()) prefsManager.saveEdad(Integer.parseInt(edadStr));
                        String objetivoSeleccionado = OBJETIVOS[spObjetivo.getSelectedItemPosition()];
                        prefsManager.saveObjetivo(objetivoSeleccionado);

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
                    });
                }

                @Override
                public void onError(String message, int statusCode) {
                    runOnUiThread(() ->
                            UIHelper.mostrarToastError(EditarPerfilActivity.this,
                                    getString(R.string.editar_perfil_error)));
                }
            });
        } catch (JSONException e) {
            UIHelper.mostrarToastError(this, getString(R.string.editar_perfil_error));
        }
    }

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
