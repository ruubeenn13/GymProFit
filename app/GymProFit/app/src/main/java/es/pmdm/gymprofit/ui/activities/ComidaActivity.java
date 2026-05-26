package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import es.pmdm.gymprofit.utils.UIHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.AlimentoComidaAdapter;
import es.pmdm.gymprofit.utils.UIHelper;

/**
 * Muestra el log de alimentos de una comida del día (desayuno, almuerzo, comida, merienda, cena).
 * Recibe extras: tipoComida (String), comidaId (int, -1 si no existe), fecha (String YYYY-MM-DD).
 */
public class ComidaActivity extends BaseActivity {

    private static final String TAG = "ComidaActivity";

    private String tipoComida;
    private int comidaId;
    private String fecha;

    private TextView tvTotalCalorias, tvTotalProteinas, tvTotalCarbos, tvTotalGrasas;
    private RecyclerView rvAlimentosComida;

    private List<AlimentoComida> listaAlimentos = new ArrayList<>();
    private AlimentoComidaAdapter adapter;

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

    @Override
    protected void onResume() {
        super.onResume();
        if (comidaId != -1) {
            cargarAlimentos();
        }
    }

    private void inicializarVistas() {
        tvTotalCalorias  = findViewById(R.id.tvTotalCalorias);
        tvTotalProteinas = findViewById(R.id.tvTotalProteinas);
        tvTotalCarbos    = findViewById(R.id.tvTotalCarbos);
        tvTotalGrasas    = findViewById(R.id.tvTotalGrasas);
        rvAlimentosComida = findViewById(R.id.rvAlimentosComida);
    }

    private void configurarToolbar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        TextView tvTitulo = findViewById(R.id.tvTituloComida);
        if (tipoComida != null) {
            Integer resId = TIPO_LABELS.get(tipoComida.toUpperCase(Locale.ROOT));
            if (resId != null) {
                tvTitulo.setText(getString(resId));
            } else {
                tvTitulo.setText(tipoComida);
            }
        }
    }

    private void configurarRecyclerView() {
        adapter = new AlimentoComidaAdapter(listaAlimentos, this::mostrarMenuContextual);
        rvAlimentosComida.setLayoutManager(new LinearLayoutManager(this));
        rvAlimentosComida.setAdapter(adapter);
    }

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

    private void cargarAlimentos() {
        API.getAlimentosDeComida(comidaId, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    List<AlimentoComida> lista = UtilJSONParser.parseListaAlimentosComida(response);
                    runOnUiThread(() -> {
                        listaAlimentos.clear();
                        if (lista != null) listaAlimentos.addAll(lista);
                        adapter.notifyDataSetChanged();
                        actualizarTotales();
                    });
                } catch (JSONException e) {
                    Log.e(TAG, "Error parseando alimentos: " + e.getMessage());
                }
            }

            @Override
            public void onError(String message, int statusCode) {
                if (statusCode == 404) {
                    runOnUiThread(() -> {
                        listaAlimentos.clear();
                        adapter.notifyDataSetChanged();
                        actualizarTotales();
                    });
                } else {
                    Log.e(TAG, "Error cargando alimentos: " + message);
                }
            }
        });
    }

    private void actualizarTotales() {
        int totalCal = 0;
        double totalProt = 0, totalCarb = 0, totalGras = 0;
        for (AlimentoComida a : listaAlimentos) {
            totalCal  += a.getCaloriasTotales();
            totalProt += a.getProteinasTotales();
            totalCarb += a.getCarbohidratosTotales();
            totalGras += a.getGrasasTotales();
        }
        tvTotalCalorias.setText(String.format(Locale.getDefault(), "%d kcal", totalCal));
        tvTotalProteinas.setText(String.format(Locale.getDefault(), "%.1fg prot", totalProt));
        tvTotalCarbos.setText(String.format(Locale.getDefault(), "%.1fg carbos", totalCarb));
        tvTotalGrasas.setText(String.format(Locale.getDefault(), "%.1fg grasas", totalGras));

        View tvVacio = findViewById(R.id.tvSinAlimentos);
        if (tvVacio != null) {
            tvVacio.setVisibility(listaAlimentos.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void mostrarMenuContextual(AlimentoComida item, View anchorView) {
        boolean esAdmin = "ROLE_ADMIN".equals(prefsManager.getRol());
        boolean esPredefinido = item.getUsuarioIdAlimento() == null;

        List<UIHelper.MenuAction> actions = new ArrayList<>();
        actions.add(new UIHelper.MenuAction(R.drawable.ic_edit, getString(R.string.comida_editar_cantidad),
                () -> mostrarDialogoEditarCantidad(item)));
        if (esAdmin && esPredefinido) {
            actions.add(new UIHelper.MenuAction(R.drawable.ic_visibility_off, getString(R.string.comida_desactivar_alimento),
                    () -> UIHelper.mostrarDialogoConIcono(ComidaActivity.this,
                            getString(R.string.comida_desactivar_alimento),
                            getString(R.string.alimento_desactivar_confirmar),
                            R.drawable.ic_visibility_off,
                            () -> desactivarAlimento(item))));
        }
        actions.add(new UIHelper.MenuAction(R.drawable.ic_delete, getString(R.string.comida_eliminar_de_comida), true,
                () -> UIHelper.mostrarDialogoConIcono(ComidaActivity.this,
                        getString(R.string.comida_eliminar_titulo),
                        getString(R.string.comida_eliminar_confirmar),
                        R.drawable.ic_delete,
                        () -> eliminarAlimento(item))));
        UIHelper.mostrarBottomMenu(this, item.getNombreAlimento(), actions);
    }

    private void mostrarDialogoEditarCantidad(AlimentoComida item) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_gramos, null);
        EditText etGramos = dialogView.findViewById(R.id.etGramos);
        TextView tvPreview = dialogView.findViewById(R.id.tvPreviewMacros);

        etGramos.setText(String.format(Locale.getDefault(), "%.0f", item.getCantidadGramos()));

        etGramos.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    double g = Double.parseDouble(s.toString());
                    // Las calorías almacenadas son por 100 g
                    int kcal = item.getCaloriasTotales() > 0
                            ? (int) Math.round((item.getCaloriasTotales() / item.getCantidadGramos()) * g)
                            : 0;
                    tvPreview.setText(String.format(Locale.getDefault(),
                            "%d kcal | %.1fg prot | %.1fg carbos | %.1fg grasas",
                            kcal,
                            (item.getProteinasTotales() / item.getCantidadGramos()) * g,
                            (item.getCarbohidratosTotales() / item.getCantidadGramos()) * g,
                            (item.getGrasasTotales() / item.getCantidadGramos()) * g));
                } catch (NumberFormatException ignored) {
                    tvPreview.setText("");
                }
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.comida_editar_cantidad))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.dialog_confirmar), (d, w) -> {
                    String texto = etGramos.getText().toString().trim();
                    if (texto.isEmpty()) return;
                    try {
                        double nuevosGramos = Double.parseDouble(texto);
                        if (nuevosGramos <= 0) return;
                        JSONObject body = new JSONObject();
                        body.put("cantidadGramos", nuevosGramos);
                        API.patchAlimentoComida(item.getId(), body, new UtilREST.OnResponseListener() {
                            @Override
                            public void onSuccess(String response, int statusCode) {
                                runOnUiThread(() -> cargarAlimentos());
                            }
                            @Override
                            public void onError(String message, int statusCode) {
                                runOnUiThread(() -> UIHelper.mostrarToastError(
                                        ComidaActivity.this, getString(R.string.error_conexion)));
                            }
                        });
                    } catch (NumberFormatException | JSONException e) {
                        Log.e(TAG, "Error editando cantidad: " + e.getMessage());
                    }
                })
                .setNegativeButton(getString(R.string.dialog_cancelar), null)
                .show();
    }

    private void desactivarAlimento(AlimentoComida item) {
        API.adminToggleActivoAlimento(item.getAlimentoId(), false, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                runOnUiThread(() -> cargarAlimentos());
            }
            @Override
            public void onError(String message, int statusCode) {
                runOnUiThread(() -> UIHelper.mostrarToastError(
                        ComidaActivity.this, getString(R.string.error_conexion)));
            }
        });
    }

    private void eliminarAlimento(AlimentoComida item) {
        API.eliminarAlimentoDeComida(item.getId(), new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                runOnUiThread(() -> cargarAlimentos());
            }
            @Override
            public void onError(String message, int statusCode) {
                runOnUiThread(() -> UIHelper.mostrarToastError(
                        ComidaActivity.this, getString(R.string.error_conexion)));
            }
        });
    }
}
