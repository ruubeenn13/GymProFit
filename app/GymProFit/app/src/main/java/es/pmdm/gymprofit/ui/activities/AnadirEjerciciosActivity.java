package es.pmdm.gymprofit.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.rutina.EjercicioSeleccionado;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.EjercicioAdapter;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

public class AnadirEjerciciosActivity extends AppCompatActivity {

    private TextInputEditText etBuscar;
    private ChipGroup chipGroupDificultad;
    private RecyclerView rvBusqueda;
    private MaterialButton btnContinuar;
    private PreferencesManager prefsManager;

    private final List<EjercicioSeleccionado> ejerciciosSeleccionados = new ArrayList<>();
    private EjercicioAdapter ejercicioAdapter;

    private String dificultadActual = "Todos";
    private String textoActual = "";
    private boolean editMode = false;

    private ActivityResultLauncher<Intent> resumenLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdioma();
        setContentView(R.layout.activity_anadir_ejercicios);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        editMode = getIntent().getBooleanExtra("editMode", false);

        etBuscar            = findViewById(R.id.etBuscarEjercicio);
        chipGroupDificultad = findViewById(R.id.chipGroupDificultad);
        rvBusqueda          = findViewById(R.id.rvEjerciciosBusqueda);
        btnContinuar        = findViewById(R.id.btnContinuar);

        rvBusqueda.setLayoutManager(new LinearLayoutManager(this));

        resumenLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        setResult(RESULT_OK);
                        finish();
                    } else if (result.getResultCode() == Activity.RESULT_FIRST_USER) {
                        setResult(Activity.RESULT_FIRST_USER);
                        finish();
                    } else if (result.getData() != null) {
                        // back desde resumen — sincronizar lista
                        String json = result.getData().getStringExtra("ejerciciosJson");
                        if (json != null) sincronizarDesdeJson(json);
                    }
                });

        configurarBusqueda();
        cargarEjercicios();
        actualizarBoton();

        btnContinuar.setOnClickListener(v -> abrirResumen());
    }

    private void configurarBusqueda() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                textoActual = s.toString();
                if (ejercicioAdapter != null) ejercicioAdapter.filtrarCombinado(textoActual, dificultadActual);
            }
        });

        chipGroupDificultad.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipDifPrincipiante)    dificultadActual = "PRINCIPIANTE";
            else if (id == R.id.chipDifIntermedio) dificultadActual = "INTERMEDIO";
            else if (id == R.id.chipDifAvanzado)   dificultadActual = "AVANZADO";
            else                                   dificultadActual = "Todos";
            if (ejercicioAdapter != null) ejercicioAdapter.filtrarCombinado(textoActual, dificultadActual);
        });
    }

    private void cargarEjercicios() {
        API.getEjerciciosActivos(new UtilREST.OnResponseListener() {
            @Override public void onSuccess(String response, int statusCode) {
                try {
                    List<Ejercicio> lista = UtilJSONParser.parseEjercicioList(response);
                    runOnUiThread(() -> {
                        ejercicioAdapter = new EjercicioAdapter(lista,
                                e -> mostrarDialogSeriesReps(e));
                        rvBusqueda.setAdapter(ejercicioAdapter);
                    });
                } catch (JSONException ignored) {}
            }
            @Override public void onError(String message, int statusCode) {
                runOnUiThread(() -> UIHelper.mostrarToastError(AnadirEjerciciosActivity.this,
                        getString(R.string.error_conexion)));
            }
        });
    }

    private void mostrarDialogSeriesReps(Ejercicio ejercicio) {
        for (EjercicioSeleccionado sel : ejerciciosSeleccionados) {
            if (sel.getEjercicio().getId() == ejercicio.getId()) {
                UIHelper.mostrarToastError(this, getString(R.string.crear_rutina_ejercicio_duplicado));
                return;
            }
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_series_reps, null);
        TextInputEditText etSeries       = dialogView.findViewById(R.id.etSeries);
        TextInputEditText etRepeticiones = dialogView.findViewById(R.id.etRepeticiones);

        new MaterialAlertDialogBuilder(this)
                .setTitle(ejercicio.getNombre())
                .setView(dialogView)
                .setPositiveButton(getString(R.string.dialog_confirmar), (dialog, which) -> {
                    String serStr = etSeries.getText() != null ? etSeries.getText().toString().trim() : "3";
                    String repStr = etRepeticiones.getText() != null ? etRepeticiones.getText().toString().trim() : "10";
                    int series = serStr.isEmpty() ? 3 : Integer.parseInt(serStr);
                    int reps   = repStr.isEmpty()  ? 10 : Integer.parseInt(repStr);
                    ejerciciosSeleccionados.add(new EjercicioSeleccionado(ejercicio, series, reps));
                    actualizarBoton();
                })
                .setNegativeButton(getString(R.string.dialog_cancelar), null)
                .show();
    }

    private void abrirResumen() {
        if (editMode) {
            Intent result = new Intent();
            result.putExtra("ejerciciosJson", serializarEjercicios());
            setResult(RESULT_OK, result);
            finish();
            return;
        }
        Intent intent = new Intent(this, ResumenCrearRutinaActivity.class);
        intent.putExtra("nombre",      getIntent().getStringExtra("nombre"));
        intent.putExtra("descripcion", getIntent().getStringExtra("descripcion"));
        intent.putExtra("nivel",       getIntent().getStringExtra("nivel"));
        intent.putExtra("duracion",    getIntent().getIntExtra("duracion", 0));
        intent.putExtra("ejerciciosJson", serializarEjercicios());
        resumenLauncher.launch(intent);
    }

    private String serializarEjercicios() {
        JSONArray arr = new JSONArray();
        try {
            for (EjercicioSeleccionado sel : ejerciciosSeleccionados) {
                JSONObject obj = new JSONObject();
                obj.put("ejercicioId",      sel.getEjercicio().getId());
                obj.put("nombre",           sel.getEjercicio().getNombre());
                obj.put("caloriasEjercicio", sel.getEjercicio().getCalorias());
                obj.put("series",           sel.getSeries());
                obj.put("repeticiones",     sel.getRepeticiones());
                arr.put(obj);
            }
        } catch (JSONException ignored) {}
        return arr.toString();
    }

    private void sincronizarDesdeJson(String json) {
        try {
            ejerciciosSeleccionados.clear();
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Ejercicio e = new Ejercicio();
                e.setId(obj.getInt("ejercicioId"));
                e.setNombre(obj.getString("nombre"));
                ejerciciosSeleccionados.add(
                        new EjercicioSeleccionado(e, obj.getInt("series"), obj.getInt("repeticiones")));
            }
            actualizarBoton();
        } catch (JSONException ignored) {}
    }

    private void actualizarBoton() {
        btnContinuar.setText(String.format(
                getString(R.string.anadir_ejercicios_continuar_fmt),
                ejerciciosSeleccionados.size()));
    }

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
