package es.pmdm.gymprofit.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.rutina.EjercicioSeleccionado;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.EjercicioSeleccionadoAdapter;
import es.pmdm.gymprofit.utils.NotificationHelper;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

public class ResumenCrearRutinaActivity extends AppCompatActivity {

    private final List<EjercicioSeleccionado> ejercicios = new ArrayList<>();
    private EjercicioSeleccionadoAdapter adapter;
    private TextView tvEjerciciosTitulo;
    private PreferencesManager prefsManager;

    private String nombre, descripcion, nivel;
    private int duracion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdioma();
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

    @Override
    public void onBackPressed() {
        handleBack();
    }

    private void handleBack() {
        Intent result = new Intent();
        result.putExtra("ejerciciosJson", serializarEjercicios());
        setResult(RESULT_CANCELED, result);
        finish();
    }

    private void cancelar() {
        setResult(Activity.RESULT_FIRST_USER);
        finish();
    }

    private void poblarInfoRutina() {
        ((TextView) findViewById(R.id.tvNombreRutina)).setText(nombre);
        ((TextView) findViewById(R.id.tvDescripcionRutina)).setText(descripcion);
        actualizarDetalles();
    }

    private void actualizarDetalles() {
        int kcalTotal = 0;
        for (EjercicioSeleccionado sel : ejercicios)
            kcalTotal += sel.getSeries() * sel.getRepeticiones() * sel.getEjercicio().getCalorias();
        String detalles = nivel + "  ·  " + duracion + " min  ·  " + kcalTotal + " kcal";
        ((TextView) findViewById(R.id.tvDetallesRutina)).setText(detalles);
    }

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

    private void actualizarTituloEjercicios() {
        tvEjerciciosTitulo.setText(String.format(
                getString(R.string.resumen_crear_rutina_ejercicios_fmt), ejercicios.size()));
    }

    private void guardarRutina() {
        try {
            JSONObject body = new JSONObject();
            body.put("nombre", nombre);
            body.put("descripcion", descripcion);
            body.put("nivel", nivel);
            body.put("duracionMinutos", duracion);
            body.put("usuarioId", prefsManager.getUsuarioId());
            body.put("esPredefinida", false);

            API.crearRutina(body, new UtilREST.OnResponseListener() {
                @Override public void onSuccess(String response, int statusCode) {
                    try {
                        int rutinaId = new JSONObject(response).optInt("id", -1);
                        if (rutinaId != -1 && !ejercicios.isEmpty()) {
                            addEjercicios(rutinaId);
                        } else {
                            runOnUiThread(() -> finalizar());
                        }
                    } catch (JSONException e) {
                        runOnUiThread(() -> finalizar());
                    }
                }
                @Override public void onError(String message, int statusCode) {
                    runOnUiThread(() -> UIHelper.mostrarToastError(ResumenCrearRutinaActivity.this,
                            getString(R.string.error_conexion)));
                }
            });
        } catch (JSONException e) {
            UIHelper.mostrarToastError(this, getString(R.string.error_conexion));
        }
    }

    private void addEjercicios(int rutinaId) {
        List<EjercicioSeleccionado> copia = new ArrayList<>(ejercicios);
        AtomicInteger pendientes = new AtomicInteger(copia.size());
        for (int i = 0; i < copia.size(); i++) {
            EjercicioSeleccionado sel = copia.get(i);
            int orden = i + 1;
            try {
                JSONObject body = new JSONObject();
                body.put("rutinaId",     rutinaId);
                body.put("ejercicioId",  sel.getEjercicio().getId());
                body.put("series",       sel.getSeries());
                body.put("repeticiones", sel.getRepeticiones());
                body.put("orden",        orden);
                API.addEjercicioARutina(body, new UtilREST.OnResponseListener() {
                    @Override public void onSuccess(String r, int s) {
                        if (pendientes.decrementAndGet() == 0) runOnUiThread(() -> finalizar());
                    }
                    @Override public void onError(String m, int s) {
                        if (pendientes.decrementAndGet() == 0) runOnUiThread(() -> finalizar());
                    }
                });
            } catch (JSONException e) {
                if (pendientes.decrementAndGet() == 0) runOnUiThread(() -> finalizar());
            }
        }
    }

    private void finalizar() {
        NotificationHelper.notificarRutinaCreada(this, nombre);
        UIHelper.mostrarToastExito(this, getString(R.string.rutinas_guardada_exito));
        setResult(RESULT_OK);
        finish();
    }

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
