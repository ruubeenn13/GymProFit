package es.pmdm.gymprofit.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.PageDTO;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.rutina.EjercicioSeleccionado;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.EjercicioApi;
import es.pmdm.gymprofit.ui.adapters.EjercicioAdapter;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PaginacionScrollListener;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// AnadirEjerciciosActivity — buscador y selector de ejercicios para una rutina
// Permite buscar/filtrar ejercicios por dificultad (búsqueda en SERVIDOR,
// paginada con scroll infinito), elegir series/repeticiones y continuar
// hacia el resumen de creación (o devolver resultado en editMode).
// ============================================================
public class AnadirEjerciciosActivity extends AppCompatActivity {

    // Tamaño de página del catálogo y retardo del debounce del buscador
    private static final int TAM_PAGINA = 30;
    private static final long DEBOUNCE_MS = 400;

    private TextInputEditText etBuscar;
    private ChipGroup chipGroupDificultad;
    private RecyclerView rvBusqueda;
    private MaterialButton btnContinuar;
    private PreferencesManager prefsManager;

    // Ejercicios elegidos por el usuario junto con sus series/repeticiones
    private final List<EjercicioSeleccionado> ejerciciosSeleccionados = new ArrayList<>();
    private EjercicioAdapter ejercicioAdapter;

    // Estado de la búsqueda paginada en servidor
    private String dificultadActual = null;  // nombre del enum (PRINCIPIANTE...) o null = todas
    private String textoActual = "";
    private int paginaActual = 0;
    private boolean cargando = false;
    private boolean ultimaPagina = false;

    // Debounce del buscador: pospone la petición hasta que el usuario deja de teclear
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    // Indica si se llegó desde la edición de una rutina existente (afecta al resultado devuelto)
    private boolean editMode = false;

    // Launcher hacia ResumenCrearRutinaActivity; propaga el resultado o sincroniza la lista al volver
    private ActivityResultLauncher<Intent> resumenLauncher;

    // Interfaz Retrofit tipada del dominio ejercicios (cacheada por ApiClient).
    private final EjercicioApi api = ApiClient.service(EjercicioApi.class);

    // Aplica tema/idioma, infla el layout y configura toolbar, búsqueda y launcher de resultado
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        setContentView(R.layout.activity_anadir_ejercicios);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        editMode = getIntent().getBooleanExtra("editMode", false);

        etBuscar            = findViewById(R.id.etBuscarEjercicio);
        chipGroupDificultad = findViewById(R.id.chipGroupDificultad);
        rvBusqueda          = findViewById(R.id.rvEjerciciosBusqueda);
        btnContinuar        = findViewById(R.id.btnContinuar);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvBusqueda.setLayoutManager(layoutManager);
        // Adapter único (la lista se rellena por páginas desde el servidor)
        ejercicioAdapter = new EjercicioAdapter(new ArrayList<>(), e -> mostrarDialogSeriesReps(e));
        rvBusqueda.setAdapter(ejercicioAdapter);
        // Scroll infinito: pide la siguiente página al acercarse al final
        rvBusqueda.addOnScrollListener(new PaginacionScrollListener(layoutManager) {
            @Override protected void cargarMas() { cargarPagina(paginaActual + 1); }
            @Override protected boolean isCargando() { return cargando; }
            @Override protected boolean esUltimaPagina() { return ultimaPagina; }
        });

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

    // Configura el buscador por texto (con debounce hacia el servidor) y los
    // chips de dificultad (filtro también en servidor).
    private void configurarBusqueda() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                textoActual = s.toString().trim();
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> cargarEjercicios();
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
            }
        });

        chipGroupDificultad.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipDifPrincipiante)    dificultadActual = "PRINCIPIANTE";
            else if (id == R.id.chipDifIntermedio) dificultadActual = "INTERMEDIO";
            else if (id == R.id.chipDifAvanzado)   dificultadActual = "AVANZADO";
            else                                   dificultadActual = null; // Todas
            cargarEjercicios();
        });
    }

    // Reinicia la búsqueda desde la página 0 con los filtros actuales.
    private void cargarEjercicios() {
        paginaActual = 0;
        ultimaPagina = false;
        cargarPagina(0);
    }

    // Pide una página al servidor; la 0 reemplaza la lista (con spinner),
    // las siguientes se añaden al final en silencio (scroll infinito).
    private void cargarPagina(int pagina) {
        cargando = true;
        if (pagina == 0) LoadingDialog.show(this);
        api.buscar(textoActual.isEmpty() ? null : textoActual, null, dificultadActual, pagina, TAM_PAGINA)
                .enqueue(new ApiCallback<PageDTO<Ejercicio>>() {
                    @Override public void onOk(PageDTO<Ejercicio> resultado) {
                        cargando = false;
                        if (pagina == 0) LoadingDialog.hide(AnadirEjerciciosActivity.this);
                        if (resultado == null) return;
                        paginaActual = resultado.getPage();
                        ultimaPagina = resultado.isLast();
                        List<Ejercicio> lista = resultado.getContent() != null
                                ? resultado.getContent() : new ArrayList<>();
                        if (pagina == 0) ejercicioAdapter.setEjercicios(lista);
                        else ejercicioAdapter.addEjercicios(lista);
                    }
                    @Override public void onFail(int code, String message) {
                        cargando = false;
                        if (pagina == 0) LoadingDialog.hide(AnadirEjerciciosActivity.this);
                        UiFeedback.toastError(AnadirEjerciciosActivity.this, code, message);
                    }
                });
    }

    // Cancela el debounce pendiente al destruir la Activity.
    @Override
    protected void onDestroy() {
        if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
        super.onDestroy();
    }

    // Muestra un diálogo para introducir series/repeticiones al seleccionar un ejercicio (evita duplicados)
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

    // Devuelve el resultado directamente si está en editMode, o navega al resumen de creación de rutina
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

    // Serializa la lista de ejercicios seleccionados a JSON para pasarla entre activities
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

    // Reconstruye la lista de ejercicios seleccionados a partir del JSON recibido al volver del resumen
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

    // Actualiza el texto del botón "Continuar" mostrando el número de ejercicios seleccionados
    private void actualizarBoton() {
        btnContinuar.setText(String.format(
                getString(R.string.anadir_ejercicios_continuar_fmt),
                ejerciciosSeleccionados.size()));
    }
}
