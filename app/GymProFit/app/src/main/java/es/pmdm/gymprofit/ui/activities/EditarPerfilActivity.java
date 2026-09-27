package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

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
// EditarPerfilActivity — tus datos y objetivos (GP-105: Ajustes › Tus datos y
// objetivos).
// Peso, altura y edad; sexo y actividad; objetivo y nivel. Guarda vía PATCH lo que
// guarda la API y recalcula las macros nutricionales locales con los datos nuevos.
//
// Sexo y actividad solo se elegían en el onboarding y viven en el teléfono: se
// guardan donde entonces (PreferencesManager) y recalculan como entonces. El correo ya
// no está aquí: se cambia en Ajustes › Correo, con la contraseña (GP-083).
// ============================================================
public class EditarPerfilActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private PreferencesManager prefsManager;
    private TextInputEditText etPeso, etAltura, etEdad;
    private Spinner spNivel, spObjetivo, spSexo, spActividad;
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
    // Valores guardados en el teléfono para sexo y actividad (los del onboarding).
    private static final String[] SEXOS = {"HOMBRE", "MUJER"};
    private static final String[] ACTIVIDADES = {
            CalculadoraNutricional.ACTIVIDAD_SEDENTARIO, CalculadoraNutricional.ACTIVIDAD_LIGERO,
            CalculadoraNutricional.ACTIVIDAD_MODERADO, CalculadoraNutricional.ACTIVIDAD_ACTIVO
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        setContentView(R.layout.activity_editar_perfil);

        etPeso      = findViewById(R.id.etPeso);
        etAltura    = findViewById(R.id.etAltura);
        etEdad      = findViewById(R.id.etEdad);
        spNivel     = findViewById(R.id.spNivel);
        spObjetivo  = findViewById(R.id.spObjetivo);
        spSexo      = findViewById(R.id.spSexo);
        spActividad = findViewById(R.id.spActividad);

        configurarSpinners();
        cargarDatosUsuario();
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardarPerfil());
    }

    // Spinners con textos localizados; los valores reales son las constantes de arriba.
    private void configurarSpinners() {
        rellenar(spNivel, getString(R.string.nivel_principiante), getString(R.string.nivel_intermedio),
                getString(R.string.nivel_avanzado), getString(R.string.nivel_experto));
        rellenar(spObjetivo, getString(R.string.objetivo_perder_peso), getString(R.string.objetivo_ganar_musculo),
                getString(R.string.objetivo_mantener), getString(R.string.objetivo_fuerza));
        rellenar(spSexo, getString(R.string.onboarding_hombre), getString(R.string.onboarding_mujer));
        rellenar(spActividad, getString(R.string.onboarding_sedentario), getString(R.string.onboarding_ligero),
                getString(R.string.onboarding_moderado), getString(R.string.onboarding_activo));
        seleccionarSpinner(spSexo, SEXOS, prefsManager.getSexo());
        seleccionarSpinner(spActividad, ACTIVIDADES, prefsManager.getActividad());
    }

    private void rellenar(Spinner spinner, String... textos) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, textos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    // Obtiene los datos del usuario actual desde la API y rellena el formulario.
    private void cargarDatosUsuario() {
        int id = prefsManager.getUsuarioId();
        if (id == -1) return;

        usuarioApi.getPorId(id).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                if (u == null) return;
                if (u.getPeso() != null && !u.getPeso().isEmpty()) etPeso.setText(u.getPeso());
                if (u.getAltura() > 0) etAltura.setText(String.valueOf((int) u.getAltura()));
                if (u.getEdad() > 0) etEdad.setText(String.valueOf(u.getEdad()));
                seleccionarSpinner(spNivel, NIVELES, u.getNivelExperiencia());
                seleccionarSpinner(spObjetivo, OBJETIVOS, u.getObjetivo());
            }

            @Override
            public void onFail(int code, String message) {
                // Sin el perfil cargado, guardar pisaría los datos reales con el
                // formulario en blanco: se avisa. El 401 ya lo resuelve el aviso global.
                if (code != 401) {
                    UIHelper.mostrarToastError(EditarPerfilActivity.this,
                            getString(R.string.editar_perfil_error_carga));
                }
            }
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

    // Guarda el perfil por PATCH y, al tener éxito, guarda sexo y actividad en el
    // teléfono y recalcula las macros con los datos nuevos.
    private void guardarPerfil() {
        int id = prefsManager.getUsuarioId();
        try {
            // Un valor null BORRA el campo (Gson con serializeNulls).
            Map<String, Object> body = new HashMap<>();

            String pesoStr = etPeso.getText() != null ? etPeso.getText().toString().trim() : "";
            body.put("peso", pesoStr.isEmpty() ? null : new BigDecimal(pesoStr.replace(",", ".")));

            String alturaStr = etAltura.getText() != null ? etAltura.getText().toString().trim() : "";
            body.put("altura", alturaStr.isEmpty() ? null : new BigDecimal(alturaStr));

            String edadStr = etEdad.getText() != null ? etEdad.getText().toString().trim() : "";
            body.put("edad", edadStr.isEmpty() ? null : Integer.parseInt(edadStr));

            body.put("nivelExperiencia", NIVELES[spNivel.getSelectedItemPosition()]);
            body.put("objetivo", OBJETIVOS[spObjetivo.getSelectedItemPosition()]);

            LoadingDialog.show(this);
            usuarioApi.patch(id, body).enqueue(new ApiCallback<Void>() {
                @Override
                public void onOk(Void response) {
                    LoadingDialog.hide(EditarPerfilActivity.this);
                    if (!pesoStr.isEmpty()) prefsManager.savePeso(Double.parseDouble(pesoStr.replace(",", ".")));
                    if (!alturaStr.isEmpty()) prefsManager.saveAltura(Double.parseDouble(alturaStr));
                    if (!edadStr.isEmpty()) prefsManager.saveEdad(Integer.parseInt(edadStr));
                    String objetivo = OBJETIVOS[spObjetivo.getSelectedItemPosition()];
                    prefsManager.saveObjetivo(objetivo);
                    prefsManager.saveNivel(NIVELES[spNivel.getSelectedItemPosition()]);
                    prefsManager.saveSexo(SEXOS[spSexo.getSelectedItemPosition()]);
                    prefsManager.saveActividad(ACTIVIDADES[spActividad.getSelectedItemPosition()]);

                    // Recalcular macros con los nuevos datos
                    ResultadoNutricional r = CalculadoraNutricional.calcular(prefsManager.getPeso(),
                            prefsManager.getAltura(), prefsManager.getEdad(),
                            "HOMBRE".equals(prefsManager.getSexo()), prefsManager.getActividad(), objetivo);
                    prefsManager.saveResultadoNutricional(r.calorias, r.proteinas, r.carbohidratos, r.grasas, r.agua);

                    UIHelper.mostrarToastExito(EditarPerfilActivity.this, getString(R.string.editar_perfil_guardado));
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onFail(int code, String message) {
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
