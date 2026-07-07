package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import es.pmdm.gymprofit.model.PageDTO;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.comida.Comida;
import es.pmdm.gymprofit.network.AlimentoApi;
import es.pmdm.gymprofit.network.AlimentoComidaApi;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ComidaApi;
import es.pmdm.gymprofit.ui.adapters.AlimentoAdapter;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PaginacionScrollListener;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;


// ============================================================
// AnadirAlimentoActivity — buscador y selector de alimentos para una comida
// Permite buscar alimentos (búsqueda en SERVIDOR, paginada con scroll
// infinito), crear uno nuevo, editar/eliminar los propios (o desactivar
// predefinidos si es admin) y añadirlos con gramos.
// ============================================================
/**
 * Permite al usuario buscar un alimento y añadirlo a una comida del día.
 * Long-press sobre alimento propio muestra menú para editar o eliminar.
 */
public class AnadirAlimentoActivity extends BaseActivity {

    // Tamaño de página del catálogo y retardo del debounce del buscador
    private static final int TAM_PAGINA = 30;
    private static final long DEBOUNCE_MS = 400;

    // Tipo de comida al que se añadirá el alimento (DESAYUNO, ALMUERZO, ...)
    private String tipoComida;
    // Id de la comida existente, o -1 si aún no se ha creado
    private int comidaId;
    // Fecha (YYYY-MM-DD) de la comida
    private String fecha;

    // Lista mostrada en el RecyclerView (se rellena por páginas del servidor)
    private final List<Alimento> listaAlimentos = new ArrayList<>();
    private AlimentoAdapter adapter;

    // Estado de la búsqueda paginada en servidor
    private String queryActual = "";
    private int paginaActual = 0;
    private boolean cargando = false;
    private boolean ultimaPagina = false;

    // Debounce del buscador: pospone la petición hasta que el usuario deja de teclear
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

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
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvAlimentos.setLayoutManager(layoutManager);
        adapter = new AlimentoAdapter(listaAlimentos, this::mostrarDialogoGramos);
        adapter.setOnItemLongClickListener((alimento, anchor) -> {
            boolean esAdmin = "ROLE_ADMIN".equals(prefsManager.getRol());
            boolean esPropio = alimento.getUsuarioId() != null
                    && alimento.getUsuarioId() == prefsManager.getUsuarioId();
            if (esPropio || esAdmin) {
                mostrarMenuContextualAlimento(alimento, anchor);
            }
        });
        rvAlimentos.setAdapter(adapter);
        // Scroll infinito: pide la siguiente página al acercarse al final
        rvAlimentos.addOnScrollListener(new PaginacionScrollListener(layoutManager) {
            @Override protected void cargarMas() { cargarPagina(paginaActual + 1); }
            @Override protected boolean isCargando() { return cargando; }
            @Override protected boolean esUltimaPagina() { return ultimaPagina; }
        });

        EditText etBuscador = findViewById(R.id.etBuscador);
        // Buscador con debounce: la búsqueda se resuelve en el servidor
        etBuscador.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                queryActual = s.toString().trim();
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> cargarAlimentos();
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
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
                // Muestra el spinner modal mientras se guarda la edición del alimento
                LoadingDialog.show(this);
                alimentoApi.patch(alimento.getId(), body).enqueue(new ApiCallback<Void>() {
                    @Override
                    public void onOk(Void ignored) {
                        // Oculta el spinner al confirmarse la edición
                        LoadingDialog.hide(AnadirAlimentoActivity.this);
                        cargarAlimentos();
                    }
                    @Override
                    public void onFail(int code, String message) {
                        // Oculta el spinner y mapea el código de error a un mensaje de usuario
                        LoadingDialog.hide(AnadirAlimentoActivity.this);
                        UiFeedback.toastError(AnadirAlimentoActivity.this, code, message);
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
        // Muestra el spinner modal mientras se desactiva el alimento predefinido
        LoadingDialog.show(this);
        alimentoApi.eliminar(alimento.getId()).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                // Oculta el spinner al confirmarse la desactivación
                LoadingDialog.hide(AnadirAlimentoActivity.this);
                cargarAlimentos();
            }
            @Override
            public void onFail(int code, String message) {
                // Oculta el spinner y mapea el código de error a un mensaje de usuario
                LoadingDialog.hide(AnadirAlimentoActivity.this);
                UiFeedback.toastError(AnadirAlimentoActivity.this, code, message);
            }
        });
    }

    // Elimina (lógicamente, desactivando) un alimento propio del usuario
    private void eliminarAlimento(Alimento alimento) {
        // Muestra el spinner modal mientras se elimina el alimento propio
        LoadingDialog.show(this);
        alimentoApi.eliminar(alimento.getId()).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                // Oculta el spinner al confirmarse la eliminación
                LoadingDialog.hide(AnadirAlimentoActivity.this);
                cargarAlimentos();
            }
            @Override
            public void onFail(int code, String message) {
                // Oculta el spinner y mapea el código de error a un mensaje de usuario
                LoadingDialog.hide(AnadirAlimentoActivity.this);
                UiFeedback.toastError(AnadirAlimentoActivity.this, code, message);
            }
        });
    }

    // Reinicia la búsqueda desde la página 0 con el texto actual (también se
    // usa para recargar tras crear/editar/eliminar un alimento).
    private void cargarAlimentos() {
        paginaActual = 0;
        ultimaPagina = false;
        cargarPagina(0);
    }

    // Pide una página al servidor; la 0 reemplaza la lista (con spinner),
    // las siguientes se añaden al final en silencio (scroll infinito).
    private void cargarPagina(int pagina) {
        cargando = true;
        if (pagina == 0) LoadingDialog.show(this);
        alimentoApi.buscar(queryActual.isEmpty() ? null : queryActual, null, pagina, TAM_PAGINA)
                .enqueue(new ApiCallback<PageDTO<Alimento>>() {
                    @Override
                    public void onOk(PageDTO<Alimento> resultado) {
                        cargando = false;
                        if (pagina == 0) LoadingDialog.hide(AnadirAlimentoActivity.this);
                        if (resultado == null) return;
                        paginaActual = resultado.getPage();
                        ultimaPagina = resultado.isLast();
                        List<Alimento> alimentos = resultado.getContent() != null
                                ? resultado.getContent() : new ArrayList<>();
                        if (pagina == 0) {
                            listaAlimentos.clear();
                            listaAlimentos.addAll(alimentos);
                            adapter.notifyDataSetChanged();
                        } else if (!alimentos.isEmpty()) {
                            int desde = listaAlimentos.size();
                            listaAlimentos.addAll(alimentos);
                            adapter.notifyItemRangeInserted(desde, alimentos.size());
                        }
                    }

                    @Override
                    public void onFail(int code, String message) {
                        cargando = false;
                        if (pagina == 0) LoadingDialog.hide(AnadirAlimentoActivity.this);
                        UiFeedback.toastError(AnadirAlimentoActivity.this, code, message);
                    }
                });
    }

    // Cancela el debounce pendiente al destruir la Activity.
    @Override
    protected void onDestroy() {
        if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
        super.onDestroy();
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
            // Muestra el spinner modal mientras se crea la comida contenedora
            LoadingDialog.show(this);
            comidaApi.crear(body).enqueue(new ApiCallback<Comida>() {
                @Override
                public void onOk(Comida creada) {
                    if (creada == null) {
                        // Oculta el spinner si la respuesta viene vacía
                        LoadingDialog.hide(AnadirAlimentoActivity.this);
                        Toast.makeText(AnadirAlimentoActivity.this,
                                getString(R.string.error_conexion), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // El spinner permanece: postAlimentoComida encadena la segunda llamada
                    comidaId = creada.getId();
                    postAlimentoComida(alimento, gramos);
                }
                @Override
                public void onFail(int code, String message) {
                    // Oculta el spinner y mapea el código de error a un mensaje de usuario
                    LoadingDialog.hide(AnadirAlimentoActivity.this);
                    UiFeedback.toastError(AnadirAlimentoActivity.this, code, message);
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
        // Muestra el spinner modal mientras se asocia el alimento a la comida
        LoadingDialog.show(this);
        alimentoComidaApi.anadir(body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                // Oculta el spinner al confirmarse el añadido
                LoadingDialog.hide(AnadirAlimentoActivity.this);
                Intent result = new Intent();
                result.putExtra("comidaId", comidaId);
                setResult(RESULT_OK, result);
                finish();
            }
            @Override
            public void onFail(int code, String message) {
                // Oculta el spinner y mapea el código de error a un mensaje de usuario
                LoadingDialog.hide(AnadirAlimentoActivity.this);
                UiFeedback.toastError(AnadirAlimentoActivity.this, code, message);
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
