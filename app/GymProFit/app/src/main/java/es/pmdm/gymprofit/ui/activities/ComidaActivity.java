package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import es.pmdm.gymprofit.utils.UIHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.network.AlimentoApi;
import es.pmdm.gymprofit.network.AlimentoComidaApi;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.ui.adapters.AlimentoComidaAdapter;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import com.google.android.material.appbar.MaterialToolbar;

// ============================================================
// ComidaActivity — log de alimentos de una comida del día
// Muestra los alimentos añadidos, sus totales de macros/calorías y permite
// editar cantidad, desactivar (admin) o eliminar cada alimento del listado.
// ============================================================
/**
 * Muestra el log de alimentos de una comida del día (desayuno, almuerzo, comida, merienda, cena).
 * Recibe extras: tipoComida (String), comidaId (int, -1 si no existe), fecha (String YYYY-MM-DD).
 */
public class ComidaActivity extends BaseActivity {


    private String tipoComida;
    private int comidaId;
    private String fecha;

    private TextView tvTotalCalorias, tvTotalProteinas, tvTotalCarbos, tvTotalGrasas;
    private RecyclerView rvAlimentosComida;

    private List<AlimentoComida> listaAlimentos = new ArrayList<>();
    private AlimentoComidaAdapter adapter;

    // Servicios Retrofit tipados de los dominios alimentos-comida y alimentos (etapa 2).
    private final AlimentoComidaApi alimentoComidaApi = ApiClient.service(AlimentoComidaApi.class);
    private final AlimentoApi alimentoApi = ApiClient.service(AlimentoApi.class);

    // Launcher hacia AnadirAlimentoActivity; recoge el comidaId (si se creó la comida) y recarga
    private ActivityResultLauncher<Intent> anadirLauncher;

    /** Mapa de tipo de comida (enum string) a string resource id. */
    private static final Map<String, Integer> TIPO_LABELS = new HashMap<>();
    static {
        TIPO_LABELS.put("DESAYUNO",  R.string.nutricion_desayuno);
        TIPO_LABELS.put("ALMUERZO",  R.string.nutricion_almuerzo);
        TIPO_LABELS.put("COMIDA",    R.string.nutricion_comida);
        TIPO_LABELS.put("MERIENDA",  R.string.nutricion_merienda);
        TIPO_LABELS.put("CENA",      R.string.nutricion_cena);
    }

    // Lee los extras de la comida, infla vistas y configura toolbar, lista, FAB y launcher
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comida);

        tipoComida = getIntent().getStringExtra("tipoComida");
        comidaId   = getIntent().getIntExtra("comidaId", -1);
        fecha      = getIntent().getStringExtra("fecha");

        inicializarVistas();
        configurarToolbar();
        configurarRecyclerView();
        configurarFab();
        configurarLauncher();
    }

    // Recarga los alimentos al volver a la pantalla si la comida ya existe
    @Override
    protected void onResume() {
        super.onResume();
        if (comidaId != -1) {
            cargarAlimentos();
        }
    }

    // Enlaza las referencias de vista de los totales y del RecyclerView
    private void inicializarVistas() {
        tvTotalCalorias  = findViewById(R.id.tvTotalCalorias);
        tvTotalProteinas = findViewById(R.id.tvTotalProteinas);
        tvTotalCarbos    = findViewById(R.id.tvTotalCarbos);
        tvTotalGrasas    = findViewById(R.id.tvTotalGrasas);
        rvAlimentosComida = findViewById(R.id.rvAlimentosComida);
    }

    // Configura el botón de volver y el título de la toolbar según el tipo de comida
    private void configurarToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        if (tipoComida != null) {
            Integer resId = TIPO_LABELS.get(tipoComida.toUpperCase(Locale.ROOT));
            if (resId != null) {
                toolbar.setTitle(resId);
            } else {
                toolbar.setTitle(tipoComida);
            }
        }
    }

    // Inicializa el adapter y el layout manager del RecyclerView de alimentos
    private void configurarRecyclerView() {
        adapter = new AlimentoComidaAdapter(listaAlimentos, this::mostrarMenuContextual);
        // Tocar un alimento abre su ficha con su cantidad y «Actualizar» (lote 1.6.1): el
        // diálogo de gramos de antes ya no existe.
        adapter.setOnItemClickListener(this::abrirFicha);
        rvAlimentosComida.setLayoutManager(new LinearLayoutManager(this));
        rvAlimentosComida.setAdapter(adapter);
    }

    // Configura el FAB para lanzar AnadirAlimentoActivity con los datos de la comida actual
    private void configurarFab() {
        FloatingActionButton fab = findViewById(R.id.fabAnadir);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnadirAlimentoActivity.class);
            intent.putExtra("tipoComida", tipoComida);
            intent.putExtra("comidaId", comidaId);
            intent.putExtra("fecha", fecha);
            anadirLauncher.launch(intent);
        });
    }

    // Registra el ActivityResultLauncher que recoge el comidaId devuelto y recarga la lista
    private void configurarLauncher() {
        anadirLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        int nuevoId = result.getData().getIntExtra("comidaId", comidaId);
                        if (nuevoId != -1) comidaId = nuevoId;
                    }
                    if (comidaId != -1) cargarAlimentos();
                });
    }

    // Carga los alimentos de la comida actual desde la API. Una comida sin alimentos
    // llega como 200 con [] desde GP-069; un 404 significa que la comida no existe.
    private void cargarAlimentos() {
        // Spinner de carga durante la petición (la lista estaría en blanco mientras carga).
        LoadingDialog.show(this);
        alimentoComidaApi.getDeComida(comidaId).enqueue(new ApiCallback<List<AlimentoComida>>() {
            @Override
            public void onOk(List<AlimentoComida> lista) {
                // Punto terminal de la carga: oculta el spinner.
                LoadingDialog.hide(ComidaActivity.this);
                listaAlimentos.clear();
                if (lista != null) listaAlimentos.addAll(lista);
                adapter.notifyDataSetChanged();
                actualizarTotales();
            }

            @Override
            public void onFail(int code, String message) {
                // Punto terminal de la carga: oculta el spinner.
                LoadingDialog.hide(ComidaActivity.this);
                // Ya no hay caso benigno que distinguir: la comida vacía es 200 con [].
                // Se vacía la lista para no dejar en pantalla datos de una carga previa,
                // y se avisa del fallo (404 = la comida ya no existe).
                listaAlimentos.clear();
                adapter.notifyDataSetChanged();
                actualizarTotales();
                UiFeedback.toastError(ComidaActivity.this, code, message);
            }
        });
    }

    // Recalcula y muestra los totales de calorías y macros sumando todos los alimentos de la lista
    private void actualizarTotales() {
        int totalCal = 0;
        double totalProt = 0, totalCarb = 0, totalGras = 0;
        for (AlimentoComida a : listaAlimentos) {
            totalCal  += a.getCaloriasTotales();
            totalProt += a.getProteinasTotales();
            totalCarb += a.getCarbohidratosTotales();
            totalGras += a.getGrasasTotales();
        }
        tvTotalCalorias.setText(getString(R.string.unidad_kcal, totalCal));
        tvTotalProteinas.setText(getString(R.string.comida_total_prot, totalProt));
        tvTotalCarbos.setText(getString(R.string.comida_total_carbos, totalCarb));
        tvTotalGrasas.setText(getString(R.string.comida_total_grasas, totalGras));

        View tvVacio = findViewById(R.id.tvSinAlimentos);
        if (tvVacio != null) {
            tvVacio.setVisibility(listaAlimentos.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    // Construye el menú contextual (editar cantidad/desactivar/eliminar) según rol y origen del alimento
    private void mostrarMenuContextual(AlimentoComida item, View anchorView) {
        boolean esAdmin = "ROLE_ADMIN".equals(prefsManager.getRol());
        boolean esPredefinido = item.getUsuarioIdAlimento() == null;

        List<UIHelper.MenuAction> actions = new ArrayList<>();
        actions.add(new UIHelper.MenuAction(R.drawable.ic_ms_edit, getString(R.string.comida_editar_cantidad),
                () -> abrirFicha(item)));
        if (esAdmin && esPredefinido) {
            actions.add(new UIHelper.MenuAction(R.drawable.ic_ms_visibility_off, getString(R.string.comida_desactivar_alimento),
                    () -> UIHelper.mostrarDialogoConIcono(ComidaActivity.this,
                            getString(R.string.comida_desactivar_alimento),
                            getString(R.string.alimento_desactivar_confirmar),
                            R.drawable.ic_ms_visibility_off,
                            () -> desactivarAlimento(item))));
        }
        actions.add(new UIHelper.MenuAction(R.drawable.ic_ms_delete, getString(R.string.comida_eliminar_de_comida), true,
                () -> UIHelper.mostrarDialogoConIcono(ComidaActivity.this,
                        getString(R.string.comida_eliminar_titulo),
                        getString(R.string.comida_eliminar_confirmar),
                        R.drawable.ic_ms_delete,
                        () -> eliminarAlimento(item))));
        UIHelper.mostrarMenuAnclado(this, anchorView, item.getNombreAlimento(), actions);
    }

    // La ficha del alimento con la cantidad de esta línea, para cambiarla (lote 1.6.1).
    private void abrirFicha(AlimentoComida item) {
        anadirLauncher.launch(FichaAlimentoActivity.paraEditar(this, item, tipoComida, fecha));
    }

    // Desactiva el alimento predefinido asociado a este registro (solo admin)
    private void desactivarAlimento(AlimentoComida item) {
        // Desactiva (borrado lógico) el alimento predefinido subyacente → DELETE alimentos/{id}.
        // Spinner durante la desactivación.
        LoadingDialog.show(this);
        alimentoApi.eliminar(item.getAlimentoId()).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                // Oculta el spinner de la desactivación (cargarAlimentos gestiona el suyo).
                LoadingDialog.hide(ComidaActivity.this);
                cargarAlimentos();
            }
            @Override
            public void onFail(int code, String message) {
                // Oculta el spinner y mapea el error a feedback.
                LoadingDialog.hide(ComidaActivity.this);
                UiFeedback.toastError(ComidaActivity.this, code, message);
            }
        });
    }

    // Elimina el registro de alimento-comida (quita el alimento de esta comida)
    private void eliminarAlimento(AlimentoComida item) {
        // Spinner durante la eliminación.
        LoadingDialog.show(this);
        alimentoComidaApi.eliminar(item.getId()).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                // Oculta el spinner de la eliminación (cargarAlimentos gestiona el suyo).
                LoadingDialog.hide(ComidaActivity.this);
                cargarAlimentos();
            }
            @Override
            public void onFail(int code, String message) {
                // Oculta el spinner y mapea el error a feedback.
                LoadingDialog.hide(ComidaActivity.this);
                UiFeedback.toastError(ComidaActivity.this, code, message);
            }
        });
    }
}
