package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.app.AlertDialog;
import android.app.Dialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.comida.Comida;
import es.pmdm.gymprofit.network.AlimentoApi;
import es.pmdm.gymprofit.network.AlimentoComidaApi;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ComidaApi;
import es.pmdm.gymprofit.ui.adapters.AlimentoAdapter;
import es.pmdm.gymprofit.utils.UIHelper;


// ============================================================
// AnadirAlimentoActivity — buscador y selector de alimentos para una comida
// Permite listar/filtrar alimentos, crear uno nuevo, editar/eliminar los
// propios (o desactivar predefinidos si es admin) y añadirlos con gramos.
// ============================================================
/**
 * Permite al usuario buscar un alimento y añadirlo a una comida del día.
 * Long-press sobre alimento propio muestra menú para editar o eliminar.
 */
public class AnadirAlimentoActivity extends BaseActivity {

    // Tipo de comida al que se añadirá el alimento (DESAYUNO, ALMUERZO, ...)
    private String tipoComida;
    // Id de la comida existente, o -1 si aún no se ha creado
    private int comidaId;
    // Fecha (YYYY-MM-DD) de la comida
    private String fecha;

    // Lista completa de alimentos cargados desde la API
    private final List<Alimento> listaAlimentosFull = new ArrayList<>();
    // Lista filtrada según el texto de búsqueda, mostrada en el RecyclerView
    private final List<Alimento> listaAlimentosFiltrada = new ArrayList<>();
    private AlimentoAdapter adapter;

    // Launcher para lanzar CrearAlimentoActivity y recargar la lista si se creó uno nuevo
    private ActivityResultLauncher<Intent> crearAlimentoLauncher;

    // Servicios Retrofit tipados de los dominios alimentos, comidas y alimentos-comida (etapa 2).
    private final AlimentoApi alimentoApi = ApiClient.service(AlimentoApi.class);
    private final ComidaApi comidaApi = ApiClient.service(ComidaApi.class);
    private final AlimentoComidaApi alimentoComidaApi = ApiClient.service(AlimentoComidaApi.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anadir_alimento);

        tipoComida = getIntent().getStringExtra("tipoComida");
        comidaId   = getIntent().getIntExtra("comidaId", -1);
        fecha      = getIntent().getStringExtra("fecha");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView rvAlimentos = findViewById(R.id.rvAlimentos);
        rvAlimentos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AlimentoAdapter(listaAlimentosFiltrada, this::mostrarDialogoGramos);
        adapter.setOnItemLongClickListener((alimento, anchor) -> {
            boolean esAdmin = "ROLE_ADMIN".equals(prefsManager.getRol());
            boolean esPropio = alimento.getUsuarioId() != null
                    && alimento.getUsuarioId() == prefsManager.getUsuarioId();
            if (esPropio || esAdmin) {
                mostrarMenuContextualAlimento(alimento, anchor);
            }
        });
        rvAlimentos.setAdapter(adapter);

        EditText etBuscador = findViewById(R.id.etBuscador);
        etBuscador.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase(Locale.getDefault());
                listaAlimentosFiltrada.clear();
                if (query.isEmpty()) {
                    listaAlimentosFiltrada.addAll(listaAlimentosFull);
                } else {
                    for (Alimento a : listaAlimentosFull) {
                        if (a.getNombre().toLowerCase(Locale.getDefault()).contains(query)) {
                            listaAlimentosFiltrada.add(a);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });

        crearAlimentoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        cargarAlimentos();
                    }
                });

        MaterialButton btnCrearAlimento = findViewById(R.id.btnCrearAlimento);
        btnCrearAlimento.setOnClickListener(v -> {
            Intent intent = new Intent(this, CrearAlimentoActivity.class);
            crearAlimentoLauncher.launch(intent);
        });

        cargarAlimentos();
    }

    // Construye el menú contextual (editar/desactivar/eliminar) según el rol y si es propio o predefinido
    private void mostrarMenuContextualAlimento(Alimento alimento, View anchorView) {
        boolean esAdmin = "ROLE_ADMIN".equals(prefsManager.getRol());
        boolean esPredefinido = alimento.getUsuarioId() == null;

        List<UIHelper.MenuAction> actions = new ArrayList<>();
        actions.add(new UIHelper.MenuAction(R.drawable.ic_edit, getString(R.string.alimento_editar),
                () -> mostrarDialogoEditarAlimento(alimento)));
        if (esAdmin && esPredefinido) {
            actions.add(new UIHelper.MenuAction(R.drawable.ic_visibility_off, getString(R.string.comida_desactivar_alimento),
                    () -> UIHelper.mostrarDialogoConIcono(this,
                            getString(R.string.comida_desactivar_alimento),
                            getString(R.string.alimento_desactivar_confirmar),
                            R.drawable.ic_visibility_off,
                            () -> desactivarAlimento(alimento))));
        }
        actions.add(new UIHelper.MenuAction(R.drawable.ic_delete, getString(R.string.alimento_eliminar), true,
                () -> UIHelper.mostrarDialogoConIcono(this,
                        getString(R.string.alimento_eliminar),
                        getString(R.string.alimento_eliminar_confirmar),
                        R.drawable.ic_delete,
                        () -> eliminarAlimento(alimento))));
        UIHelper.mostrarMenuAnclado(this, anchorView, alimento.getNombre(), actions);
    }

    // Muestra un diálogo con formulario para editar nombre/macros del alimento vía PATCH
    private void mostrarDialogoEditarAlimento(Alimento alimento) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_editar_alimento, null);
        ((TextView) dialogView.findViewById(R.id.tvDialogTitulo)).setText(getString(R.string.alimento_editar));

        TextInputEditText etNombre    = dialogView.findViewById(R.id.etNombreEditar);
        TextInputEditText etCalorias  = dialogView.findViewById(R.id.etCaloriasEditar);
        TextInputEditText etProteinas = dialogView.findViewById(R.id.etProteinasEditar);
        TextInputEditText etCarbos    = dialogView.findViewById(R.id.etCarbohidratosEditar);
        TextInputEditText etGrasas    = dialogView.findViewById(R.id.etGrasasEditar);

        etNombre.setText(alimento.getNombre());
        etCalorias.setText(String.valueOf(alimento.getCalorias()));
        etProteinas.setText(String.format(Locale.getDefault(), "%.1f", alimento.getProteinas()));
        etCarbos.setText(String.format(Locale.getDefault(), "%.1f", alimento.getCarbohidratos()));
        etGrasas.setText(String.format(Locale.getDefault(), "%.1f", alimento.getGrasas()));

        Dialog dialog = UIHelper.prepararDialogoFormulario(this, dialogView);

        dialogView.findViewById(R.id.btnDialogConfirmar).setOnClickListener(v -> {
            try {
                // Cuerpo parcial: BigDecimal en los macros decimales; nombre/calorías solo si vienen.
                Map<String, Object> body = new HashMap<>();
                String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
                if (!nombre.isEmpty()) body.put("nombre", nombre);
                String calStr = etCalorias.getText() != null ? etCalorias.getText().toString().trim() : "";
                if (!calStr.isEmpty()) body.put("calorias", Integer.parseInt(calStr));
                body.put("proteinas",     BigDecimal.valueOf(parseDoubleOrZero(etProteinas)));
                body.put("carbohidratos", BigDecimal.valueOf(parseDoubleOrZero(etCarbos)));
                body.put("grasas",        BigDecimal.valueOf(parseDoubleOrZero(etGrasas)));
                dialog.dismiss();
                alimentoApi.patch(alimento.getId(), body).enqueue(new ApiCallback<Void>() {
                    @Override
                    public void onOk(Void ignored) {
                        cargarAlimentos();
                    }
                    @Override
                    public void onFail(int code, String message) {
                        UIHelper.mostrarToastError(
                                AnadirAlimentoActivity.this, getString(R.string.error_conexion));
                    }
                });
            } catch (NumberFormatException e) {
                UIHelper.mostrarToastError(this, getString(R.string.error_conexion));
            }
        });
        dialogView.findViewById(R.id.btnDialogCancelar).setOnClickListener(v -> dialog.dismiss());

        UIHelper.mostrarDialogoFormulario(this, dialog);
    }

    // Desactiva un alimento predefinido (solo admin), sin borrarlo de la BD
    private void desactivarAlimento(Alimento alimento) {
        alimentoApi.eliminar(alimento.getId()).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                cargarAlimentos();
            }
            @Override
            public void onFail(int code, String message) {
                UIHelper.mostrarToastError(
                        AnadirAlimentoActivity.this, getString(R.string.error_conexion));
            }
        });
    }

    // Elimina (lógicamente, desactivando) un alimento propio del usuario
    private void eliminarAlimento(Alimento alimento) {
        alimentoApi.eliminar(alimento.getId()).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                cargarAlimentos();
            }
            @Override
            public void onFail(int code, String message) {
                UIHelper.mostrarToastError(
                        AnadirAlimentoActivity.this, getString(R.string.error_conexion));
            }
        });
    }

    // Carga todos los alimentos activos desde la API y actualiza ambas listas (full y filtrada)
    private void cargarAlimentos() {
        alimentoApi.getActivos().enqueue(new ApiCallback<List<Alimento>>() {
            @Override
            public void onOk(List<Alimento> alimentos) {
                listaAlimentosFull.clear();
                if (alimentos != null) listaAlimentosFull.addAll(alimentos);
                listaAlimentosFiltrada.clear();
                listaAlimentosFiltrada.addAll(listaAlimentosFull);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFail(int code, String message) {
                Toast.makeText(AnadirAlimentoActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Muestra un diálogo para introducir los gramos a añadir, con preview de macros en tiempo real
    private void mostrarDialogoGramos(Alimento alimento) {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_gramos, null);
        EditText etGramos = dialogView.findViewById(R.id.etGramos);
        TextView tvPreviewMacros = dialogView.findViewById(R.id.tvPreviewMacros);

        etGramos.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String raw = s.toString().trim();
                if (raw.isEmpty()) {
                    tvPreviewMacros.setText(getString(R.string.anadir_alimento_preview, 0, 0.0, 0.0, 0.0));
                    return;
                }
                try {
                    double gramos = Double.parseDouble(raw);
                    int kcal  = (int) (alimento.getCalorias()      * gramos / 100);
                    double prot  = alimento.getProteinas()     * gramos / 100;
                    double carbs = alimento.getCarbohidratos() * gramos / 100;
                    double gras  = alimento.getGrasas()        * gramos / 100;
                    tvPreviewMacros.setText(getString(R.string.anadir_alimento_preview, kcal, prot, carbs, gras));
                } catch (NumberFormatException ignored) {
                    tvPreviewMacros.setText(getString(R.string.anadir_alimento_preview, 0, 0.0, 0.0, 0.0));
                }
            }
        });

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(getString(R.string.btn_anadir), (dialog, which) -> {
                    String raw = etGramos.getText().toString().trim();
                    if (raw.isEmpty()) {
                        Toast.makeText(this, getString(R.string.anadir_alimento_gramos_hint), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double gramos;
                    try {
                        gramos = Double.parseDouble(raw);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, getString(R.string.anadir_alimento_gramos_hint), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (gramos <= 0) {
                        Toast.makeText(this, getString(R.string.anadir_alimento_gramos_hint), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    anadirAlimento(alimento, gramos);
                })
                .setNegativeButton(getString(R.string.dialog_cancelar), null)
                .show();
    }

    // Añade el alimento a la comida; si la comida aún no existe (comidaId == -1) la crea primero
    private void anadirAlimento(Alimento alimento, double gramos) {
        if (comidaId == -1) {
            // Cuerpo de creación: la fecha del path es yyyy-MM-dd; aquí se envía como ISO con hora 00:00:00.
            Map<String, Object> body = new HashMap<>();
            body.put("usuarioId", prefsManager.getUsuarioId());
            body.put("tipoComida", tipoComida);
            body.put("fecha", fecha + "T00:00:00");
            comidaApi.crear(body).enqueue(new ApiCallback<Comida>() {
                @Override
                public void onOk(Comida creada) {
                    if (creada == null) {
                        Toast.makeText(AnadirAlimentoActivity.this,
                                getString(R.string.error_conexion), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    comidaId = creada.getId();
                    postAlimentoComida(alimento, gramos);
                }
                @Override
                public void onFail(int code, String message) {
                    Toast.makeText(AnadirAlimentoActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            postAlimentoComida(alimento, gramos);
        }
    }

    // Envía la petición que asocia el alimento (con sus gramos) a la comida y cierra la pantalla al éxito
    private void postAlimentoComida(Alimento alimento, double gramos) {
        // Cuerpo parcial: cantidadGramos como BigDecimal (decimal).
        Map<String, Object> body = new HashMap<>();
        body.put("comidaId", comidaId);
        body.put("alimentoId", alimento.getId());
        body.put("cantidadGramos", BigDecimal.valueOf(gramos));
        alimentoComidaApi.anadir(body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                Intent result = new Intent();
                result.putExtra("comidaId", comidaId);
                setResult(RESULT_OK, result);
                finish();
            }
            @Override
            public void onFail(int code, String message) {
                Toast.makeText(AnadirAlimentoActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Parsea el contenido de un campo a double, devolviendo 0.0 si está vacío o no es válido
    private double parseDoubleOrZero(TextInputEditText field) {
        if (field.getText() == null) return 0.0;
        String raw = field.getText().toString().trim();
        if (raw.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
