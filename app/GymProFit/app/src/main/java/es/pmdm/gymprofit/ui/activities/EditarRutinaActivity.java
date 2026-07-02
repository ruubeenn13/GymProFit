package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

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
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// EditarRutinaActivity — pantalla para editar una rutina propia del usuario.
// Permite modificar los datos generales de la rutina y gestionar su
// lista de ejercicios (añadir vía AnadirEjerciciosActivity y eliminar
// mediante confirmación), sincronizando los cambios con la API.
// ============================================================
public class EditarRutinaActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etDescripcion, etDuracion;
    private ChipGroup chipGroupNivel;
    private TextView tvEjerciciosTitulo;
    private EjercicioSeleccionadoAdapter adapter;
    private PreferencesManager prefsManager;

    private final List<EjercicioSeleccionado> ejercicios = new ArrayList<>();
    private int rutinaId;

    // Lanzador para recibir los ejercicios añadidos desde AnadirEjerciciosActivity.
    private ActivityResultLauncher<Intent> anadirLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdioma();
        setContentView(R.layout.activity_editar_rutina);

        rutinaId = getIntent().getIntExtra("rutinaId", -1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etNombre       = findViewById(R.id.etNombreEditar);
        etDescripcion  = findViewById(R.id.etDescripcionEditar);
        etDuracion     = findViewById(R.id.etDuracionEditar);
        chipGroupNivel = findViewById(R.id.chipGroupNivelEditar);

        etNombre.setText(getIntent().getStringExtra("nombre"));
        etDescripcion.setText(getIntent().getStringExtra("descripcion"));
        etDuracion.setText(String.valueOf(getIntent().getIntExtra("duracion", 0)));
        preseleccionarNivel(getIntent().getStringExtra("nivel"));

        anadirLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Recibe el JSON de ejercicios seleccionados en modo edición.
                        String json = result.getData().getStringExtra("ejerciciosJson");
                        if (json != null) añadirEjerciciosNuevos(json);
                    }
                });

        configurarRecycler();
        cargarEjercicios();

        findViewById(R.id.btnAnadirEjercicioEditar).setOnClickListener(v -> abrirAnadir());
        findViewById(R.id.btnGuardarEditar).setOnClickListener(v -> guardarCambios());
    }

    // Marca el chip de nivel correspondiente al valor recibido por Intent.
    private void preseleccionarNivel(String nivel) {
        if ("INTERMEDIO".equals(nivel))
            chipGroupNivel.check(R.id.chipEditarIntermedio);
        else if ("AVANZADO".equals(nivel))
            chipGroupNivel.check(R.id.chipEditarAvanzado);
        else
            chipGroupNivel.check(R.id.chipEditarPrincipiante);
    }

    // Traduce el chip de nivel seleccionado a su valor de texto para la API.
    private String obtenerNivel() {
        int id = chipGroupNivel.getCheckedChipId();
        if (id == R.id.chipEditarIntermedio) return "INTERMEDIO";
        if (id == R.id.chipEditarAvanzado)   return "AVANZADO";
        return "PRINCIPIANTE";
    }

    // Configura el RecyclerView de ejercicios en modo edición, mostrando
    // confirmación antes de eliminar cada ejercicio.
    private void configurarRecycler() {
        tvEjerciciosTitulo = findViewById(R.id.tvEjerciciosTituloEditar);
        RecyclerView rv = findViewById(R.id.rvEjerciciosEditar);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EjercicioSeleccionadoAdapter(ejercicios, item ->
                UIHelper.mostrarDialogoConIcono(
                        this,
                        getString(R.string.resumen_crear_rutina_confirmar_titulo),
                        getString(R.string.editar_rutina_eliminar_ejercicio),
                        R.drawable.ic_delete,
                        () -> eliminarEjercicio(item)));
        rv.setAdapter(adapter);
        actualizarTitulo();
    }

    // Lanza en paralelo dos llamadas (catálogo de ejercicios activos y
    // relaciones rutina-ejercicio) y combina resultados cuando ambas terminan.
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
                if (pendientes.decrementAndGet() == 0)
                    runOnUiThread(() -> combinarYMostrar(ejercicioMap, relacionesRaw));
            }
        });
    }

    // Une cada relación rutina-ejercicio con su Ejercicio del catálogo
    // (o crea uno mínimo con el id si no se encontró) y refresca el adapter.
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

    // Elimina la relación rutina-ejercicio en la API y, si tiene éxito,
    // lo quita también de la lista local y refresca el adapter.
    private void eliminarEjercicio(EjercicioSeleccionado item) {
        API.eliminarEjercicioDeRutina(rutinaId, item.getEjercicio().getId(),
                new UtilREST.OnResponseListener() {
                    @Override public void onSuccess(String r, int s) {
                        runOnUiThread(() -> {
                            ejercicios.remove(item);
                            adapter.notifyDataSetChanged();
                            actualizarTitulo();
                        });
                    }
                    @Override public void onError(String m, int s) {
                        runOnUiThread(() -> UIHelper.mostrarToastError(EditarRutinaActivity.this,
                                getString(R.string.error_conexion)));
                    }
                });
    }

    // Abre AnadirEjerciciosActivity en modo edición para seleccionar
    // ejercicios adicionales a añadir a la rutina.
    private void abrirAnadir() {
        Intent intent = new Intent(this, AnadirEjerciciosActivity.class);
        intent.putExtra("editMode", true);
        anadirLauncher.launch(intent);
    }

    // Añade a la lista local (evitando duplicados) los ejercicios recibidos
    // y los persiste en la API uno a uno mediante addEjercicioARutina.
    private void añadirEjerciciosNuevos(String json) {
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                int ejercicioId = obj.getInt("ejercicioId");
                boolean yaExiste = false;
                for (EjercicioSeleccionado sel : ejercicios) {
                    if (sel.getEjercicio().getId() == ejercicioId) { yaExiste = true; break; }
                }
                if (yaExiste) continue;

                Ejercicio e = new Ejercicio();
                e.setId(ejercicioId);
                e.setNombre(obj.optString("nombre", "Ejercicio " + ejercicioId));
                e.setCalorias(obj.optInt("caloriasEjercicio", 0));
                ejercicios.add(new EjercicioSeleccionado(e,
                        obj.optInt("series", 3), obj.optInt("repeticiones", 10)));

                try {
                    JSONObject body = new JSONObject();
                    body.put("rutinaId",     rutinaId);
                    body.put("ejercicioId",  ejercicioId);
                    body.put("series",       obj.optInt("series", 3));
                    body.put("repeticiones", obj.optInt("repeticiones", 10));
                    body.put("orden",        ejercicios.size());
                    API.addEjercicioARutina(body, new UtilREST.OnResponseListener() {
                        @Override public void onSuccess(String r, int s) {}
                        @Override public void onError(String m, int s) {
                            runOnUiThread(() -> UIHelper.mostrarToastError(
                                    EditarRutinaActivity.this, getString(R.string.error_conexion)));
                        }
                    });
                } catch (JSONException ignored) {}
            }
            adapter.notifyDataSetChanged();
            actualizarTitulo();
        } catch (JSONException ignored) {}
    }

    // Valida los campos generales de la rutina y envía el PATCH con los
    // datos actualizados (nombre, descripción, nivel y duración).
    private void guardarCambios() {
        String nom = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
        String desc = etDescripcion.getText() != null ? etDescripcion.getText().toString().trim() : "";
        String dur  = etDuracion.getText() != null ? etDuracion.getText().toString().trim() : "";

        if (nom.isEmpty())  { UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido)); etNombre.requestFocus(); return; }
        if (desc.isEmpty()) { UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido)); etDescripcion.requestFocus(); return; }
        if (dur.isEmpty())  { UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido)); etDuracion.requestFocus(); return; }

        try {
            JSONObject body = new JSONObject();
            body.put("nombre",          nom);
            body.put("descripcion",     desc);
            body.put("nivel",           obtenerNivel());
            body.put("duracionMinutos", Integer.parseInt(dur));

            API.patchRutina(rutinaId, body, new UtilREST.OnResponseListener() {
                @Override public void onSuccess(String response, int statusCode) {
                    runOnUiThread(() -> {
                        UIHelper.mostrarToastExito(EditarRutinaActivity.this,
                                getString(R.string.editar_rutina_guardado));
                        setResult(RESULT_OK);
                        finish();
                    });
                }
                @Override public void onError(String message, int statusCode) {
                    runOnUiThread(() -> UIHelper.mostrarToastError(EditarRutinaActivity.this,
                            getString(R.string.error_conexion)));
                }
            });
        } catch (JSONException e) {
            UIHelper.mostrarToastError(this, getString(R.string.error_conexion));
        }
    }

    // Actualiza el título de la sección con el número de ejercicios cargados.
    private void actualizarTitulo() {
        tvEjerciciosTitulo.setText(String.format(
                getString(R.string.editar_rutina_ejercicios_fmt), ejercicios.size()));
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
