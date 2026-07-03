package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.medicion.MedicionCorporal;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.MedicionApi;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// MedicionesActivity — pantalla de mediciones corporales del usuario.
// Muestra la última medición registrada (peso, altura, grasa, músculo,
// perímetros...) y permite editar cada campo individualmente o crear una
// medición inicial a partir de los datos del perfil dentro de GymProFit.
// ============================================================
public class MedicionesActivity extends AppCompatActivity {

    private View tvVacio;
    private View scrollMediciones;
    private TextView tvFechaUltima;
    private TextView tvPesoVal, tvAlturaVal, tvGrasaVal, tvMusculoVal;
    private TextView tvCinturaVal, tvPechoVal, tvBrazosVal, tvPiernasVal, tvNotasVal;
    private PreferencesManager prefsManager;
    private MedicionCorporal ultimaMedicion;
    // Interfaz Retrofit tipada del dominio mediciones (etapa 2)
    private final MedicionApi medicionApi = ApiClient.service(MedicionApi.class);

    private ActivityResultLauncher<Intent> nuevaLauncher;

    // Aplica tema/idioma, referencia las vistas, registra el launcher para
    // el registro de nuevas mediciones y carga la última medición existente.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdioma();
        setContentView(R.layout.activity_mediciones);

        tvVacio         = findViewById(R.id.tvVacio);
        scrollMediciones = findViewById(R.id.scrollMediciones);
        tvFechaUltima   = findViewById(R.id.tvFechaUltima);
        tvPesoVal       = findViewById(R.id.tvPesoVal);
        tvAlturaVal     = findViewById(R.id.tvAlturaVal);
        tvGrasaVal      = findViewById(R.id.tvGrasaVal);
        tvMusculoVal    = findViewById(R.id.tvMusculoVal);
        tvCinturaVal    = findViewById(R.id.tvCinturaVal);
        tvPechoVal      = findViewById(R.id.tvPechoVal);
        tvBrazosVal     = findViewById(R.id.tvBrazosVal);
        tvPiernasVal    = findViewById(R.id.tvPiernasVal);
        tvNotasVal      = findViewById(R.id.tvNotasVal);

        nuevaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        setResult(RESULT_OK);
                        cargarMedicion();
                    }
                });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((FloatingActionButton) findViewById(R.id.fabRegistrar)).setOnClickListener(v ->
                nuevaLauncher.launch(new Intent(this, RegistrarMedicionActivity.class)));

        configurarFilas();
        cargarMedicion();
    }

    // Configura los listeners de cada fila (peso, altura, grasa, etc.) para
    // abrir el diálogo de edición del campo correspondiente al pulsarla.
    private void configurarFilas() {
        findViewById(R.id.rowPeso).setOnClickListener(v ->
                editarCampoNumerico("peso", getString(R.string.perfil_peso),
                        ultimaMedicion != null ? ultimaMedicion.getPeso() : 0));

        findViewById(R.id.rowAltura).setOnClickListener(v ->
                editarCampoNumerico("altura", getString(R.string.perfil_altura),
                        ultimaMedicion != null ? ultimaMedicion.getAltura() : 0));

        findViewById(R.id.rowGrasa).setOnClickListener(v ->
                editarCampoNumerico("grasaCorporal", getString(R.string.medicion_label_grasa),
                        ultimaMedicion != null ? ultimaMedicion.getGrasaCorporal() : 0));

        findViewById(R.id.rowMusculo).setOnClickListener(v ->
                editarCampoNumerico("masaMuscular", getString(R.string.medicion_label_musculo),
                        ultimaMedicion != null ? ultimaMedicion.getMasaMuscular() : 0));

        findViewById(R.id.rowCintura).setOnClickListener(v ->
                editarCampoNumerico("cintura", getString(R.string.medicion_label_cintura),
                        ultimaMedicion != null ? ultimaMedicion.getCintura() : 0));

        findViewById(R.id.rowPecho).setOnClickListener(v ->
                editarCampoNumerico("pecho", getString(R.string.medicion_label_pecho),
                        ultimaMedicion != null ? ultimaMedicion.getPecho() : 0));

        findViewById(R.id.rowBrazos).setOnClickListener(v ->
                editarCampoNumerico("brazos", getString(R.string.medicion_label_brazos),
                        ultimaMedicion != null ? ultimaMedicion.getBrazos() : 0));

        findViewById(R.id.rowPiernas).setOnClickListener(v ->
                editarCampoNumerico("piernas", getString(R.string.medicion_label_piernas),
                        ultimaMedicion != null ? ultimaMedicion.getPiernas() : 0));

        findViewById(R.id.rowNotas).setOnClickListener(v ->
                editarCampoTexto("notas", getString(R.string.medicion_label_notas),
                        ultimaMedicion != null && ultimaMedicion.getNotas() != null
                                ? ultimaMedicion.getNotas() : ""));
    }

    // Obtiene la lista de mediciones del usuario y muestra la más reciente;
    // si no hay ninguna, intenta crear una a partir de los datos del perfil.
    private void cargarMedicion() {
        int usuarioId = prefsManager.getUsuarioId();
        if (usuarioId == -1) return;

        // Respuesta ya deserializada a POJOs por Gson (sin UtilJSONParser).
        medicionApi.getOrdenadas(usuarioId).enqueue(new ApiCallback<List<MedicionCorporal>>() {
            @Override
            public void onOk(List<MedicionCorporal> lista) {
                if (lista == null || lista.isEmpty()) {
                    intentarCrearDesdePerfil(usuarioId);
                } else {
                    ultimaMedicion = lista.get(0);
                    mostrarMedicion();
                }
            }

            @Override
            public void onFail(int code, String message) {
                // Sin mediciones o error de lectura: intenta sembrar una desde el perfil.
                intentarCrearDesdePerfil(usuarioId);
            }
        });
    }

    // Si el usuario no tiene mediciones, crea una inicial usando el peso/altura
    // guardados en su perfil (si existen); si no, muestra el estado vacío.
    private void intentarCrearDesdePerfil(int usuarioId) {
        API.getUsuarioPorId(usuarioId, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    Usuario u = UtilJSONParser.parseUsuario(response);
                    String pesoStr = (u != null && u.getPeso() != null) ? u.getPeso().trim() : "";
                    if (!pesoStr.isEmpty() && !pesoStr.equals("null")) {
                        Map<String, Object> body = new HashMap<>();
                        body.put("usuarioId", usuarioId);
                        body.put("peso", new BigDecimal(pesoStr));
                        if (u.getAltura() > 0) body.put("altura", u.getAltura());

                        medicionApi.crear(body).enqueue(new ApiCallback<MedicionCorporal>() {
                            @Override
                            public void onOk(MedicionCorporal m) {
                                cargarMedicion();
                            }

                            @Override
                            public void onFail(int code, String msg) {
                                Log.e("MedicionesActivity", "Error al crear medición desde perfil: " + msg);
                                mostrarVacio();
                            }
                        });
                    } else {
                        mostrarVacio();
                    }
                } catch (Exception e) {
                    mostrarVacio();
                }
            }

            @Override
            public void onError(String message, int statusCode) {
                runOnUiThread(() -> mostrarVacio());
            }
        });
    }

    // Oculta el scroll de datos y muestra el mensaje de "sin mediciones".
    private void mostrarVacio() {
        scrollMediciones.setVisibility(View.GONE);
        tvVacio.setVisibility(View.VISIBLE);
    }

    // Rellena todos los TextView con los valores de la última medición,
    // mostrando el texto "añadir" en los campos aún no registrados.
    private void mostrarMedicion() {
        tvVacio.setVisibility(View.GONE);
        scrollMediciones.setVisibility(View.VISIBLE);

        String anadir = getString(R.string.medicion_anadir);

        if (ultimaMedicion.getFecha() != null && !ultimaMedicion.getFecha().isEmpty()) {
            tvFechaUltima.setText(getString(R.string.medicion_fecha_ultima, ultimaMedicion.getFecha()));
        }

        tvPesoVal.setText(ultimaMedicion.getPeso() > 0
                ? String.format(Locale.getDefault(), "%.1f kg", ultimaMedicion.getPeso()) : anadir);
        tvAlturaVal.setText(ultimaMedicion.getAltura() > 0
                ? String.format(Locale.getDefault(), "%.0f cm", ultimaMedicion.getAltura()) : anadir);
        tvGrasaVal.setText(ultimaMedicion.getGrasaCorporal() > 0
                ? String.format(Locale.getDefault(), "%.1f%%", ultimaMedicion.getGrasaCorporal()) : anadir);
        tvMusculoVal.setText(ultimaMedicion.getMasaMuscular() > 0
                ? String.format(Locale.getDefault(), "%.1f kg", ultimaMedicion.getMasaMuscular()) : anadir);
        tvCinturaVal.setText(ultimaMedicion.getCintura() > 0
                ? String.format(Locale.getDefault(), "%.1f cm", ultimaMedicion.getCintura()) : anadir);
        tvPechoVal.setText(ultimaMedicion.getPecho() > 0
                ? String.format(Locale.getDefault(), "%.1f cm", ultimaMedicion.getPecho()) : anadir);
        tvBrazosVal.setText(ultimaMedicion.getBrazos() > 0
                ? String.format(Locale.getDefault(), "%.1f cm", ultimaMedicion.getBrazos()) : anadir);
        tvPiernasVal.setText(ultimaMedicion.getPiernas() > 0
                ? String.format(Locale.getDefault(), "%.1f cm", ultimaMedicion.getPiernas()) : anadir);
        tvNotasVal.setText(ultimaMedicion.getNotas() != null && !ultimaMedicion.getNotas().isEmpty()
                ? ultimaMedicion.getNotas() : anadir);
    }

    // Muestra un diálogo con un campo numérico decimal para editar el valor
    // de un campo de medición y enviarlo por PATCH al guardar.
    private void editarCampoNumerico(String campo, String label, double valorActual) {
        if (ultimaMedicion == null) return;

        TextInputLayout til = new TextInputLayout(this);
        TextInputEditText et = new TextInputEditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (valorActual > 0) {
            et.setText(String.format(Locale.getDefault(), "%.2f", valorActual));
            et.selectAll();
        }
        til.setHint(label);
        til.addView(et);

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        til.setPadding(padding, padding / 2, padding, 0);

        new AlertDialog.Builder(this)
                .setTitle(label)
                .setView(til)
                .setPositiveButton(R.string.medicion_guardar, (d, w) -> {
                    String valor = et.getText() != null ? et.getText().toString().trim() : "";
                    if (valor.isEmpty()) {
                        if ("peso".equals(campo)) {
                            UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido));
                        }
                        return;
                    }
                    patchCampo(campo, valor, false);
                })
                .setNegativeButton(R.string.dialog_cancelar, null)
                .show();
    }

    // Muestra un diálogo con un campo de texto multilínea para editar el
    // valor de un campo de medición (p.ej. notas) y enviarlo por PATCH.
    private void editarCampoTexto(String campo, String label, String valorActual) {
        if (ultimaMedicion == null) return;

        TextInputLayout til = new TextInputLayout(this);
        TextInputEditText et = new TextInputEditText(this);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        if (!valorActual.isEmpty()) et.setText(valorActual);

        til.setHint(label);
        til.addView(et);

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        til.setPadding(padding, padding / 2, padding, 0);

        new AlertDialog.Builder(this)
                .setTitle(label)
                .setView(til)
                .setPositiveButton(R.string.medicion_guardar, (d, w) -> {
                    String valor = et.getText() != null ? et.getText().toString().trim() : "";
                    patchCampo(campo, valor, true);
                })
                .setNegativeButton(R.string.dialog_cancelar, null)
                .show();
    }

    // Construye el JSON con el campo modificado y lo envía como PATCH parcial
    // a la medición actual; recarga los datos al finalizar con éxito.
    private void patchCampo(String campo, String valorStr, boolean esTexto) {
        try {
            // Cuerpo con un solo campo. En texto vacío se envía null explícito (Gson lo
            // serializa por serializeNulls) para BORRAR el campo, como el antiguo JSONObject.NULL.
            Map<String, Object> body = new HashMap<>();
            if (esTexto) {
                body.put(campo, valorStr.isEmpty() ? null : valorStr);
            } else {
                body.put(campo, new BigDecimal(valorStr));
            }

            medicionApi.patch(ultimaMedicion.getId(), body).enqueue(new ApiCallback<MedicionCorporal>() {
                @Override
                public void onOk(MedicionCorporal m) {
                    setResult(RESULT_OK);
                    cargarMedicion();
                }

                @Override
                public void onFail(int code, String message) {
                    UIHelper.mostrarToastError(
                            MedicionesActivity.this, getString(R.string.error_conexion));
                }
            });
        } catch (NumberFormatException e) {
            UIHelper.mostrarToastError(this, getString(R.string.error_conexion));
        }
    }

    // Aplica el idioma guardado en preferencias a la configuración de recursos.
    private void aplicarIdioma() {
        String lang = prefsManager.getLanguage();
        if (!lang.isEmpty()) {
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            Resources res = getResources();
            Configuration cfg = res.getConfiguration();
            cfg.setLocale(locale);
            res.updateConfiguration(cfg, res.getDisplayMetrics());
        }
    }
}
