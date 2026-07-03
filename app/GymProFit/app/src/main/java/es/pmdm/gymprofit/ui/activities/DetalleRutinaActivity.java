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

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.rutina.EjercicioSeleccionado;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.EjercicioSeleccionadoAdapter;
import es.pmdm.gymprofit.utils.EjercicioNavHelper;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// DetalleRutinaActivity — pantalla de detalle de una rutina.
// Muestra la información general de la rutina (nombre, descripción,
// nivel, duración, calorías) y la lista de ejercicios que la componen,
// combinando el catálogo de ejercicios con las relaciones rutina-ejercicio
// obtenidas de la API. Permite editar la rutina si es propia del usuario.
// ============================================================
public class DetalleRutinaActivity extends AppCompatActivity {

    private final List<EjercicioSeleccionado> ejercicios = new ArrayList<>();
    private EjercicioSeleccionadoAdapter adapter;
    private TextView tvEjerciciosTitulo;
    private PreferencesManager prefsManager;

    // Interfaz Retrofit tipada del dominio rutinas (etapa 2)
    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);

    private int rutinaId;
    private String nombre, descripcion, nivel;
    private int duracion, calorias;
    private boolean predefinida;
    private int rutinaUsuarioId;

    // Lanzador para recibir el resultado de EditarRutinaActivity.
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
                        // Si se editó la rutina, propaga el OK y cierra el detalle.
                        setResult(RESULT_OK);
                        finish();
                    }
                });

        poblarInfoRutina();
        configurarRecycler();
        configurarBotonEditar();
        cargarEjercicios();
    }

    // Rellena las vistas de cabecera con la info general de la rutina.
    private void poblarInfoRutina() {
        ((TextView) findViewById(R.id.tvNombreDetalle)).setText(nombre);
        ((TextView) findViewById(R.id.tvDescripcionDetalle)).setText(descripcion);
        ((Chip) findViewById(R.id.chipNivelDetalle)).setText(nivel);
        ((TextView) findViewById(R.id.tvDuracionDetalle)).setText(duracion + " min");
        ((TextView) findViewById(R.id.tvCaloriasDetalle)).setText("~" + calorias + " kcal");
    }

    // Configura el RecyclerView de ejercicios de la rutina (solo lectura,
    // navegando al detalle del ejercicio al pulsar sobre uno).
    private void configurarRecycler() {
        tvEjerciciosTitulo = findViewById(R.id.tvEjerciciosTituloDetalle);
        RecyclerView rv = findViewById(R.id.rvEjerciciosDetalle);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EjercicioSeleccionadoAdapter(ejercicios, null,
                sel -> EjercicioNavHelper.abrir(this, sel));
        rv.setAdapter(adapter);
        actualizarTitulo();
    }

    // Muestra el botón de editar solo si la rutina no es predefinida
    // y pertenece al usuario actualmente autenticado.
    private void configurarBotonEditar() {
        MaterialButton btnEditar = findViewById(R.id.btnEditarRutina);
        boolean esPropia = !predefinida && rutinaUsuarioId == prefsManager.getUsuarioId();
        if (esPropia) {
            btnEditar.setVisibility(View.VISIBLE);
            btnEditar.setOnClickListener(v -> abrirEditar());
        }
    }

    // Abre EditarRutinaActivity pasando los datos actuales de la rutina.
    private void abrirEditar() {
        Intent intent = new Intent(this, EditarRutinaActivity.class);
        intent.putExtra("rutinaId",    rutinaId);
        intent.putExtra("nombre",      nombre);
        intent.putExtra("descripcion", descripcion);
        intent.putExtra("nivel",       nivel);
        intent.putExtra("duracion",    duracion);
        editarLauncher.launch(intent);
    }

    // Lanza en paralelo dos llamadas (catálogo de ejercicios activos y
    // relaciones rutina-ejercicio) y combina resultados cuando ambas terminan.
    private void cargarEjercicios() {
        final Map<Integer, Ejercicio> ejercicioMap = new HashMap<>();
        final List<RutinaEjercicio> relaciones = new ArrayList<>();
        AtomicInteger pendientes = new AtomicInteger(2);

        // Catálogo de ejercicios (dominio ejercicios: se mantiene la capa vieja).
        API.getEjerciciosActivos(new UtilREST.OnResponseListener() {
            @Override public void onSuccess(String response, int statusCode) {
                try {
                    for (Ejercicio e : UtilJSONParser.parseEjercicioList(response))
                        ejercicioMap.put(e.getId(), e);
                } catch (JSONException ignored) {}
                if (pendientes.decrementAndGet() == 0)
                    runOnUiThread(() -> combinarYMostrar(ejercicioMap, relaciones));
            }
            @Override public void onError(String message, int statusCode) {
                if (pendientes.decrementAndGet() == 0)
                    runOnUiThread(() -> combinarYMostrar(ejercicioMap, relaciones));
            }
        });

        // Relaciones rutina-ejercicio (ya deserializadas por Gson).
        rutinaApi.getEjerciciosDeRutina(rutinaId).enqueue(new ApiCallback<List<RutinaEjercicio>>() {
            @Override public void onOk(List<RutinaEjercicio> lista) {
                if (lista != null) relaciones.addAll(lista);
                if (pendientes.decrementAndGet() == 0)
                    combinarYMostrar(ejercicioMap, relaciones);
            }
            @Override public void onFail(int code, String message) {
                // 404 = sin ejercicios
                if (pendientes.decrementAndGet() == 0)
                    combinarYMostrar(ejercicioMap, relaciones);
            }
        });
    }

    // Une cada relación rutina-ejercicio con su Ejercicio del catálogo
    // (o crea uno mínimo con el id si no se encontró) y refresca el adapter.
    private void combinarYMostrar(Map<Integer, Ejercicio> map, List<RutinaEjercicio> relaciones) {
        ejercicios.clear();
        for (RutinaEjercicio rel : relaciones) {
            int ejercicioId = rel.getEjercicioId();
            int series      = rel.getSeries() > 0 ? rel.getSeries() : 3;
            int reps        = rel.getRepeticiones() > 0 ? rel.getRepeticiones() : 10;
            Ejercicio e = map.get(ejercicioId);
            if (e == null) {
                e = new Ejercicio();
                e.setId(ejercicioId);
                e.setNombre("Ejercicio " + ejercicioId);
            }
            ejercicios.add(new EjercicioSeleccionado(e, series, reps));
        }
        adapter.notifyDataSetChanged();
        actualizarTitulo();
    }

    // Actualiza el título de la sección con el número de ejercicios cargados.
    private void actualizarTitulo() {
        tvEjerciciosTitulo.setText(String.format(
                getString(R.string.detalle_rutina_ejercicios_fmt), ejercicios.size()));
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
