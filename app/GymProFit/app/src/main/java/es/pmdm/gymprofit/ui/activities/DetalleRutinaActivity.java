package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.rutina.EjercicioSeleccionado;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.EjercicioSeleccionadoAdapter;
import es.pmdm.gymprofit.utils.EjercicioNavHelper;
import es.pmdm.gymprofit.utils.PreferencesManager;

public class DetalleRutinaActivity extends AppCompatActivity {

    private final List<EjercicioSeleccionado> ejercicios = new ArrayList<>();
    private EjercicioSeleccionadoAdapter adapter;
    private TextView tvEjerciciosTitulo;
    private PreferencesManager prefsManager;

    private int rutinaId;
    private String nombre, descripcion, nivel;
    private int duracion, calorias;
    private boolean predefinida;
    private int rutinaUsuarioId;

    private ActivityResultLauncher<Intent> editarLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdioma();
        setContentView(R.layout.activity_detalle_rutina);

        rutinaId       = getIntent().getIntExtra("rutinaId", -1);
        nombre         = getIntent().getStringExtra("nombre");
        descripcion    = getIntent().getStringExtra("descripcion");
        nivel          = getIntent().getStringExtra("nivel");
        duracion       = getIntent().getIntExtra("duracion", 0);
        calorias       = getIntent().getIntExtra("calorias", 0);
        predefinida    = getIntent().getBooleanExtra("predefinida", false);
        rutinaUsuarioId = getIntent().getIntExtra("usuarioId", -1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        editarLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        setResult(RESULT_OK);
                        finish();
                    }
                });

        poblarInfoRutina();
        configurarRecycler();
        configurarBotonEditar();
        cargarEjercicios();
    }

    private void poblarInfoRutina() {
        ((TextView) findViewById(R.id.tvNombreDetalle)).setText(nombre);
        ((TextView) findViewById(R.id.tvDescripcionDetalle)).setText(descripcion);
        ((Chip) findViewById(R.id.chipNivelDetalle)).setText(nivel);
        ((TextView) findViewById(R.id.tvDuracionDetalle)).setText(duracion + " min");
        ((TextView) findViewById(R.id.tvCaloriasDetalle)).setText("~" + calorias + " kcal");
    }

    private void configurarRecycler() {
        tvEjerciciosTitulo = findViewById(R.id.tvEjerciciosTituloDetalle);
        RecyclerView rv = findViewById(R.id.rvEjerciciosDetalle);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EjercicioSeleccionadoAdapter(ejercicios, null,
                sel -> EjercicioNavHelper.abrir(this, sel));
        rv.setAdapter(adapter);
        actualizarTitulo();
    }

    private void configurarBotonEditar() {
        MaterialButton btnEditar = findViewById(R.id.btnEditarRutina);
        boolean esPropia = !predefinida && rutinaUsuarioId == prefsManager.getUsuarioId();
        if (esPropia) {
            btnEditar.setVisibility(View.VISIBLE);
            btnEditar.setOnClickListener(v -> abrirEditar());
        }
    }

    private void abrirEditar() {
        Intent intent = new Intent(this, EditarRutinaActivity.class);
        intent.putExtra("rutinaId",    rutinaId);
        intent.putExtra("nombre",      nombre);
        intent.putExtra("descripcion", descripcion);
        intent.putExtra("nivel",       nivel);
        intent.putExtra("duracion",    duracion);
        editarLauncher.launch(intent);
    }

    private void cargarEjercicios() {
        final Map<Integer, Ejercicio> ejercicioMap = new HashMap<>();
        final List<JSONObject> relacionesRaw = new ArrayList<>();
        AtomicInteger pendientes = new AtomicInteger(2);

        API.getEjerciciosActivos(new UtilREST.OnResponseListener() {
            @Override public void onSuccess(String response, int statusCode) {
                try {
                    for (Ejercicio e : UtilJSONParser.parseEjercicioList(response))
                        ejercicioMap.put(e.getId(), e);
                } catch (JSONException ignored) {}
                if (pendientes.decrementAndGet() == 0)
                    runOnUiThread(() -> combinarYMostrar(ejercicioMap, relacionesRaw));
            }
            @Override public void onError(String message, int statusCode) {
                if (pendientes.decrementAndGet() == 0)
                    runOnUiThread(() -> combinarYMostrar(ejercicioMap, relacionesRaw));
            }
        });

        API.getRutinaEjerciciosPorRutina(rutinaId, new UtilREST.OnResponseListener() {
            @Override public void onSuccess(String response, int statusCode) {
                try {
                    JSONArray arr = new JSONArray(response);
                    for (int i = 0; i < arr.length(); i++)
                        relacionesRaw.add(arr.getJSONObject(i));
                } catch (JSONException ignored) {}
                if (pendientes.decrementAndGet() == 0)
                    runOnUiThread(() -> combinarYMostrar(ejercicioMap, relacionesRaw));
            }
            @Override public void onError(String message, int statusCode) {
                // 404 = sin ejercicios
                if (pendientes.decrementAndGet() == 0)
                    runOnUiThread(() -> combinarYMostrar(ejercicioMap, relacionesRaw));
            }
        });
    }

    private void combinarYMostrar(Map<Integer, Ejercicio> map, List<JSONObject> relaciones) {
        ejercicios.clear();
        for (JSONObject obj : relaciones) {
            try {
                int ejercicioId = obj.getInt("ejercicioId");
                int series      = obj.optInt("series", 3);
                int reps        = obj.optInt("repeticiones", 10);
                Ejercicio e = map.get(ejercicioId);
                if (e == null) {
                    e = new Ejercicio();
                    e.setId(ejercicioId);
                    e.setNombre("Ejercicio " + ejercicioId);
                }
                ejercicios.add(new EjercicioSeleccionado(e, series, reps));
            } catch (JSONException ignored) {}
        }
        adapter.notifyDataSetChanged();
        actualizarTitulo();
    }

    private void actualizarTitulo() {
        tvEjerciciosTitulo.setText(String.format(
                getString(R.string.detalle_rutina_ejercicios_fmt), ejercicios.size()));
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
