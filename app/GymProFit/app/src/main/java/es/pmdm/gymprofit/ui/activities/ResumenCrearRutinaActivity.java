package es.pmdm.gymprofit.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.rutina.EjercicioSeleccionado;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.ui.adapters.EjercicioSeleccionadoAdapter;
import es.pmdm.gymprofit.utils.NotificationHelper;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// ResumenCrearRutinaActivity — Paso final de creación de rutina: resumen y guardado.
// Muestra los datos generales de la rutina (nombre, descripción, nivel, duración)
// y la lista de ejercicios seleccionados con sus calorías estimadas, y al confirmar
// crea la rutina y sus ejercicios asociados en la API.
// ============================================================
public class ResumenCrearRutinaActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private final List<EjercicioSeleccionado> ejercicios = new ArrayList<>();
    private EjercicioSeleccionadoAdapter adapter;
    private TextView tvEjerciciosTitulo;
    private PreferencesManager prefsManager;

    // Interfaz Retrofit tipada del dominio rutinas (etapa 2)
    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);

    private String nombre, descripcion, nivel;
    private int duracion;

    // Inicializa la pantalla: recupera los datos de la rutina desde el
    // intent, monta el RecyclerView de ejercicios y configura los botones.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        setContentView(R.layout.activity_resumen_crear_rutina);

        nombre      = getIntent().getStringExtra("nombre");
        descripcion = getIntent().getStringExtra("descripcion");
        nivel       = getIntent().getStringExtra("nivel");
        duracion    = getIntent().getIntExtra("duracion", 0);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> handleBack());

        configurarRecycler();
        cargarEjerciciosDesdeExtras();
        poblarInfoRutina();

        findViewById(R.id.btnCancelar).setOnClickListener(v -> cancelar());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardarRutina());
    }

    // Al pulsar atrás, devuelve la lista actual de ejercicios seleccionados
    // a la pantalla anterior en lugar de descartarlos.
    // Se mantiene el override de onBackPressed() (deprecado) por simplicidad,
    // en lugar de migrar a OnBackPressedCallback/OnBackPressedDispatcher.
    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        handleBack();
    }

    // Devuelve como resultado (RESULT_CANCELED) la lista de ejercicios
    // serializada, para no perder la selección al volver atrás.
    private void handleBack() {
        Intent result = new Intent();
        result.putExtra("ejerciciosJson", serializarEjercicios());
        setResult(RESULT_CANCELED, result);
        finish();
    }

    // Cancela por completo la creación de la rutina (resultado distinto al
    // de "volver atrás" para descartar todo el proceso).
    private void cancelar() {
        setResult(Activity.RESULT_FIRST_USER);
        finish();
    }

    // Muestra el nombre y descripción de la rutina y actualiza el texto de
    // detalles (nivel, duración, kcal totales).
    private void poblarInfoRutina() {
        ((TextView) findViewById(R.id.tvNombreRutina)).setText(nombre);
        ((TextView) findViewById(R.id.tvDescripcionRutina)).setText(descripcion);
        actualizarDetalles();
    }

    // Calcula las kcal totales de la rutina (suma de series x repeticiones x
    // calorías de cada ejercicio) y actualiza el texto de detalles.
    private void actualizarDetalles() {
        int kcalTotal = 0;
        for (EjercicioSeleccionado sel : ejercicios)
            kcalTotal += sel.getSeries() * sel.getRepeticiones() * sel.getEjercicio().getCalorias();
        String detalles = nivel + "  ·  " + duracion + " min  ·  " + kcalTotal + " kcal";
        ((TextView) findViewById(R.id.tvDetallesRutina)).setText(detalles);
    }

    // Configura el RecyclerView de ejercicios seleccionados con un adapter
    // que permite eliminar un ejercicio previa confirmación por diálogo.
    private void configurarRecycler() {
        tvEjerciciosTitulo = findViewById(R.id.tvEjerciciosTitulo);
        RecyclerView rv = findViewById(R.id.rvEjercicios);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EjercicioSeleccionadoAdapter(ejercicios, item ->
                UIHelper.mostrarDialogoConIcono(
                        this,
                        getString(R.string.resumen_crear_rutina_confirmar_titulo),
                        getString(R.string.resumen_crear_rutina_confirmar_msg),
                        R.drawable.ic_delete,
                        () -> {
                            ejercicios.remove(item);
                            adapter.notifyDataSetChanged();
                            actualizarTituloEjercicios();
                            actualizarDetalles();
                        }));
        rv.setAdapter(adapter);
    }

    // Deserializa la lista de ejercicios recibida por intent (JSON) y la
    // carga en el RecyclerView.
    private void cargarEjerciciosDesdeExtras() {
        String json = getIntent().getStringExtra("ejerciciosJson");
        if (json == null) { actualizarTituloEjercicios(); return; }
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Ejercicio e = new Ejercicio();
                e.setId(obj.getInt("ejercicioId"));
                e.setNombre(obj.getString("nombre"));
                e.setCalorias(obj.optInt("caloriasEjercicio", 0));
                ejercicios.add(new EjercicioSeleccionado(e, obj.getInt("series"), obj.getInt("repeticiones")));
            }
            adapter.notifyDataSetChanged();
        } catch (JSONException ignored) {}
        actualizarTituloEjercicios();
    }

    // Actualiza el título con el número de ejercicios seleccionados.
    private void actualizarTituloEjercicios() {
        tvEjerciciosTitulo.setText(String.format(
                getString(R.string.resumen_crear_rutina_ejercicios_fmt), ejercicios.size()));
    }

    // Crea la rutina en la API con los datos generales; si tiene ejercicios,
    // encadena la creación de cada relación rutina-ejercicio.
    private void guardarRutina() {
        Map<String, Object> body = new HashMap<>();
        body.put("nombre", nombre);
        body.put("descripcion", descripcion);
        body.put("nivel", nivel);
        body.put("duracionMinutos", duracion);
        body.put("usuarioId", prefsManager.getUsuarioId());
        body.put("esPredefinida", false);

        rutinaApi.crear(body).enqueue(new ApiCallback<Rutina>() {
            @Override public void onOk(Rutina rutina) {
                int rutinaId = rutina != null ? rutina.getId() : -1;
                if (rutinaId != -1 && !ejercicios.isEmpty()) {
                    addEjercicios(rutinaId);
                } else {
                    finalizar();
                }
            }
            @Override public void onFail(int code, String message) {
                UIHelper.mostrarToastError(ResumenCrearRutinaActivity.this,
                        getString(R.string.error_conexion));
            }
        });
    }

    // Añade cada ejercicio seleccionado a la rutina creada, respetando su
    // orden, y finaliza cuando todas las llamadas a la API han terminado.
    private void addEjercicios(int rutinaId) {
        List<EjercicioSeleccionado> copia = new ArrayList<>(ejercicios);
        AtomicInteger pendientes = new AtomicInteger(copia.size());
        for (int i = 0; i < copia.size(); i++) {
            EjercicioSeleccionado sel = copia.get(i);
            int orden = i + 1;
            Map<String, Object> body = new HashMap<>();
            body.put("rutinaId",     rutinaId);
            body.put("ejercicioId",  sel.getEjercicio().getId());
            body.put("series",       sel.getSeries());
            body.put("repeticiones", sel.getRepeticiones());
            body.put("orden",        orden);
            rutinaApi.addEjercicio(body).enqueue(new ApiCallback<Void>() {
                @Override public void onOk(Void b) {
                    if (pendientes.decrementAndGet() == 0) finalizar();
                }
                @Override public void onFail(int code, String message) {
                    if (pendientes.decrementAndGet() == 0) finalizar();
                }
            });
        }
    }

    // Notifica la creación de la rutina, muestra confirmación al usuario y
    // cierra la pantalla devolviendo RESULT_OK.
    private void finalizar() {
        NotificationHelper.notificarRutinaCreada(this, nombre);
        UIHelper.mostrarToastExito(this, getString(R.string.rutinas_guardada_exito));
        setResult(RESULT_OK);
        finish();
    }

    // Serializa la lista de ejercicios seleccionados a JSON para poder
    // devolverla como resultado al volver atrás.
    private String serializarEjercicios() {
        JSONArray arr = new JSONArray();
        try {
            for (EjercicioSeleccionado sel : ejercicios) {
                JSONObject obj = new JSONObject();
                obj.put("ejercicioId",       sel.getEjercicio().getId());
                obj.put("nombre",            sel.getEjercicio().getNombre());
                obj.put("caloriasEjercicio", sel.getEjercicio().getCalorias());
                obj.put("series",            sel.getSeries());
                obj.put("repeticiones",      sel.getRepeticiones());
                arr.put(obj);
            }
        } catch (JSONException ignored) {}
        return arr.toString();
    }
}
