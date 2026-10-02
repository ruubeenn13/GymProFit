package es.pmdm.gymprofit.ui.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import es.pmdm.gymprofit.network.AlimentoApi;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.ui.adapters.BusquedaAlimentoAdapter;
import es.pmdm.gymprofit.utils.CamposMacro;
import es.pmdm.gymprofit.utils.GruposBusqueda;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PaginacionScrollListener;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;
import retrofit2.Call;

// ============================================================
// AnadirAlimentoActivity — Añadir y Buscar (tableros 4 y 5 del lienzo, lotes 1.6.1 y 1.6.2)
//
// Cabecera con cerrar, «Añadir alimento» y la etiqueta de la comida (decisión 15), que
// abre «¿A qué comida?» con lo que lleva cada una ese día (ElegirComida). Si se añade a
// otra comida que la de origen, al volver se dice a cuál (EXTRA_ANADIDO_A).
// Debajo, el buscador y el botón naranja del escáner.
//   · Sin escribir (o con menos de 2 letras): lo que da la búsqueda vacía, «Recientes»
//     (lo tuyo) y «Habituales» (los básicos). La última lista se guarda en memoria y se
//     enseña al instante al volver a entrar, refrescándola por detrás.
//   · Buscando: Tuyo, Básicos y Productos, y al final «¿No lo encuentras?», con
//     «Escanéalo» y «Créalo». Espera 250 ms tras la última tecla y cancela la petición
//     anterior; una respuesta vieja que llegue tarde no pisa la nueva.
// Cada fila abre la ficha, que es la que añade. Mantener pulsado un alimento propio da
// editar y eliminar (y, a un ADMIN, desactivar uno del catálogo), como antes.
// Al añadir desde la ficha o el escáner, la pantalla se cierra y el diario recarga.
// ============================================================
public class AnadirAlimentoActivity extends BaseActivity {

    /** Extras: la comida (DESAYUNO…CENA) y el día (yyyy-MM-dd). */
    public static final String EXTRA_TIPO = "tipoComida";
    public static final String EXTRA_FECHA = "fecha";
    /** Resultado: la comida a la que se añadió, solo si no es la de origen (decisión 15). */
    public static final String EXTRA_ANADIDO_A = "anadidoA";

    static final String[] TIPOS = {"DESAYUNO", "ALMUERZO", "COMIDA", "MERIENDA", "CENA"};
    private static final int[] ANADIR_A = {R.string.comida_anadir_desayuno, R.string.comida_anadir_almuerzo,
            R.string.comida_anadir_comida, R.string.comida_anadir_merienda, R.string.comida_anadir_cena};

    private static final int TAM_PAGINA = 30;
    private static final long ESPERA_MS = 250;

    // La última lista sin texto, para enseñarla al instante la próxima vez (B1). Solo en
    // memoria: se pierde con el proceso, y entonces se carga como la primera vez.
    @Nullable private static List<Alimento> listaGuardada;

    private String tipoComida;
    private String tipoInicial;
    private String fecha;
    // Las kcal de cada comida del día, para la hoja; null mientras carga o si falla.
    @Nullable private Map<String, Integer> kcalDia;

    private final AlimentoApi alimentoApi = ApiClient.service(AlimentoApi.class);
    private BusquedaAlimentoAdapter adapter;
    private VistaEstado estado;
    private View etiqueta;
    private TextInputEditText etBuscar;

    // Búsqueda en curso: la petición, su texto, la página y lo acumulado.
    @Nullable private Call<PageDTO<Alimento>> enCurso;
    private int turno;
    private String consulta = "";
    private int pagina;
    private boolean ultimaPagina = true;
    private boolean cargando;
    private final List<Alimento> resultados = new ArrayList<>();
    private boolean yaEntro;

    private final Handler espera = new Handler(Looper.getMainLooper());
    private final Runnable buscarAhora = this::buscarDesdeCero;

    private ActivityResultLauncher<Intent> fichaLauncher;
    private ActivityResultLauncher<Intent> escanerLauncher;
    private ActivityResultLauncher<Intent> crearLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anadir_alimento);

        tipoComida = getIntent().getStringExtra(EXTRA_TIPO);
        if (tipoComida == null || indiceTipo(tipoComida) < 0) tipoComida = "MERIENDA";
        fecha = getIntent().getStringExtra(EXTRA_FECHA);
        tipoInicial = tipoComida;
        if (savedInstanceState != null) {
            tipoComida = savedInstanceState.getString(EXTRA_TIPO, tipoComida);
        }

        // Lo que se añade desde la ficha, el escáner o lo creado vuelve aquí con OK:
        // se cierra la pantalla y el diario recarga.
        fichaLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                r -> { if (r.getResultCode() == RESULT_OK) volverAlDiario(); else refrescar(); });
        escanerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                r -> { if (r.getResultCode() == RESULT_OK) volverAlDiario(); });
        crearLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                r -> { if (r.getResultCode() == RESULT_OK) volverAlDiario(); else refrescar(); });

        findViewById(R.id.btnCerrarAnadir).setOnClickListener(v -> finish());
        etiqueta = findViewById(R.id.etiquetaComida);
        etiqueta.setOnClickListener(v -> elegirComida());
        es.pmdm.gymprofit.ui.nutricion.ElegirComida.colocarEtiqueta(etiqueta, findViewById(R.id.filaTituloAnadir),
                findViewById(R.id.tvTituloAnadir), findViewById(R.id.ranuraEtiquetaDerecha),
                findViewById(R.id.ranuraEtiquetaDebajo));
        pintarComida();
        cargarDia();
        findViewById(R.id.btnEscanear).setOnClickListener(v -> abrirEscaner());

        estado = new VistaEstado(findViewById(R.id.estadoAnadir));
        RecyclerView rv = findViewById(R.id.rvAnadir);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        rv.setLayoutManager(lm);
        adapter = new BusquedaAlimentoAdapter(new BusquedaAlimentoAdapter.Acciones() {
            @Override public void abrir(@NonNull Alimento alimento) { abrirFicha(alimento); }
            @Override public boolean tieneOpciones(@NonNull Alimento a) { return puedeOpciones(a); }
            @Override public void opciones(@NonNull Alimento a, @NonNull View fila) { mostrarMenuContextualAlimento(a, fila); }
            @Override public void escanear() { abrirEscaner(); }
            @Override public void crear() { abrirCrear(); }
        });
        rv.setAdapter(adapter);
        rv.addOnScrollListener(new PaginacionScrollListener(lm) {
            @Override protected void cargarMas() { siguientePagina(); }
            @Override protected boolean isCargando() { return cargando; }
            @Override protected boolean esUltimaPagina() { return ultimaPagina; }
        });

        etBuscar = findViewById(R.id.etBuscarAnadir);
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(Editable s) {
                espera.removeCallbacks(buscarAhora);
                espera.postDelayed(buscarAhora, ESPERA_MS);
            }
        });
        etBuscar.setOnEditorActionListener((v, accion, ev) -> {
            if (accion != EditorInfo.IME_ACTION_SEARCH) return false;
            espera.removeCallbacks(buscarAhora);
            buscarDesdeCero();
            return true;
        });

        // Al instante lo de la última vez; por detrás, lo de ahora.
        if (listaGuardada != null) {
            pintar(listaGuardada, false);
            yaEntro = true;
        }
        buscarDesdeCero();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putString(EXTRA_TIPO, tipoComida);
    }

    @Override
    protected void onDestroy() {
        espera.removeCallbacks(buscarAhora);
        if (enCurso != null) enCurso.cancel();
        super.onDestroy();
    }

    // ── La comida ───────────────────────────────────────────────────────────

    static int indiceTipo(String tipo) {
        for (int i = 0; i < TIPOS.length; i++) if (TIPOS[i].equals(tipo)) return i;
        return -1;
    }

    /** «Añadir a la merienda», «Add to snack»… para la comida que toca. */
    static int textoAnadirA(String tipo) {
        int i = indiceTipo(tipo);
        return ANADIR_A[i < 0 ? 3 : i];
    }

    private void pintarComida() {
        es.pmdm.gymprofit.ui.nutricion.ElegirComida.pintarEtiqueta(etiqueta, tipoComida);
    }

    // La etiqueta abre «¿A qué comida?»; elegir cambia la etiqueta y nada más.
    private void elegirComida() {
        es.pmdm.gymprofit.ui.nutricion.ElegirComida.abrirHoja(this, fecha, tipoComida, kcalDia, t -> {
            tipoComida = t;
            pintarComida();
            // La etiqueta puede cambiar de ancho («Almuerzo» frente a «Cena»).
            etiqueta.requestLayout();
        });
    }

    // Lo que lleva cada comida del día, para la hoja. Sin esto la hoja funciona igual,
    // solo que sin las kcal: por eso un fallo no se enseña (y se reintenta al volver).
    private void cargarDia() {
        int usuarioId = prefsManager.getUsuarioId();
        if (usuarioId == -1) return;
        String dia = fecha != null ? fecha
                : new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
        ApiClient.service(es.pmdm.gymprofit.network.ComidaApi.class).getDeUsuarioFecha(usuarioId, dia)
                .enqueue(new ApiCallback<List<es.pmdm.gymprofit.model.comida.Comida>>() {
                    @Override
                    public void onOk(List<es.pmdm.gymprofit.model.comida.Comida> lista) {
                        Map<String, Integer> m = new HashMap<>();
                        if (lista != null) {
                            for (es.pmdm.gymprofit.model.comida.Comida c : lista) {
                                m.put(c.getTipoComida(), c.getTotalCalorias());
                            }
                        }
                        kcalDia = m;
                    }

                    @Override
                    public void onFail(int code, String message) {
                        // Se ignora a propósito: la hoja sigue sirviendo para elegir, solo
                        // que sin decir lo que lleva cada comida; no hay nada que reintentar
                        // a la vista del usuario.
                    }
                });
    }

    // ── Abrir ───────────────────────────────────────────────────────────────

    private void abrirFicha(Alimento alimento) {
        fichaLauncher.launch(FichaAlimentoActivity.paraAnadir(this, alimento, tipoComida, fecha));
    }

    private void abrirEscaner() {
        escanerLauncher.launch(new Intent(this, EscanerActivity.class)
                .putExtra(EXTRA_TIPO, tipoComida).putExtra(EXTRA_FECHA, fecha));
    }

    private void abrirCrear() {
        crearLauncher.launch(new Intent(this, CrearAlimentoActivity.class)
                .putExtra(EXTRA_TIPO, tipoComida).putExtra(EXTRA_FECHA, fecha));
    }

    private void volverAlDiario() {
        Intent datos = new Intent();
        if (!tipoComida.equals(tipoInicial)) datos.putExtra(EXTRA_ANADIDO_A, tipoComida);
        setResult(RESULT_OK, datos);
        finish();
    }

    // ── Buscar ──────────────────────────────────────────────────────────────

    // Tras editar o borrar un alimento propio, o al volver de la ficha sin añadir.
    private void refrescar() {
        buscarDesdeCero();
    }

    private void buscarDesdeCero() {
        CharSequence texto = etBuscar.getText();
        consulta = GruposBusqueda.esBusqueda(texto) ? texto.toString().trim() : "";
        pagina = 0;
        ultimaPagina = true;
        pedir(0);
    }

    private void siguientePagina() {
        if (!ultimaPagina && !cargando) pedir(pagina + 1);
    }

    private void pedir(int numero) {
        if (enCurso != null) enCurso.cancel();
        int miTurno = ++turno;
        cargando = true;
        boolean buscando = !consulta.isEmpty();
        // Solo se enseña «cargando» si no hay nada que enseñar mientras tanto.
        if (numero == 0 && adapter.getItemCount() == 0) estado.cargando();
        Call<PageDTO<Alimento>> llamada = alimentoApi.buscar(buscando ? consulta : null, null, numero, TAM_PAGINA);
        enCurso = llamada;
        llamada.enqueue(new ApiCallback<PageDTO<Alimento>>() {
            @Override
            public void onOk(PageDTO<Alimento> pag) {
                if (miTurno != turno || isDestroyed()) return;
                cargando = false;
                List<Alimento> contenido = pag != null && pag.getContent() != null ? pag.getContent() : new ArrayList<>();
                if (numero == 0) resultados.clear();
                resultados.addAll(contenido);
                pagina = pag != null ? pag.getPage() : 0;
                ultimaPagina = pag == null || pag.isLast();
                if (!buscando && numero == 0) listaGuardada = new ArrayList<>(contenido);
                pintar(resultados, numero == 0 && !yaEntro);
                yaEntro = true;
            }

            @Override
            public void onFail(int code, String message) {
                // Una petición cancelada (otra búsqueda la ha sustituido) también llega
                // aquí: no es un error, y la nueva pintará lo suyo.
                if (miTurno != turno || isDestroyed()) return;
                cargando = false;
                if (numero > 0) {
                    UiFeedback.toastError(AnadirAlimentoActivity.this, code, message);
                    return;
                }
                adapter.poner(new ArrayList<>(), false);
                estado.error(VistaEstado.mensaje(AnadirAlimentoActivity.this, R.string.anadir_error_lista, code, message),
                        AnadirAlimentoActivity.this::buscarDesdeCero);
            }
        });
    }

    private void pintar(List<Alimento> alimentos, boolean cascada) {
        boolean buscando = !consulta.isEmpty();
        List<GruposBusqueda.Elemento> lista = GruposBusqueda.de(alimentos, buscando);
        adapter.poner(lista, cascada);
        if (lista.isEmpty()) estado.vacio(R.string.anadir_vacio);
        else estado.oculto();
    }

    // ── Mantener pulsado: lo de siempre ─────────────────────────────────────

    private boolean puedeOpciones(Alimento alimento) {
        if (alimento.esExterno()) return false;
        boolean esAdmin = "ROLE_ADMIN".equals(prefsManager.getRol());
        boolean esPropio = alimento.getUsuarioId() != null && alimento.getUsuarioId() == prefsManager.getUsuarioId();
        return esPropio || esAdmin;
    }

    // Construye el menú contextual (editar/desactivar/eliminar) según el rol y si es propio o predefinido
    private void mostrarMenuContextualAlimento(Alimento alimento, View anchorView) {
        boolean esAdmin = "ROLE_ADMIN".equals(prefsManager.getRol());
        boolean esPredefinido = alimento.getUsuarioId() == null;

        List<UIHelper.MenuAction> actions = new ArrayList<>();
        actions.add(new UIHelper.MenuAction(R.drawable.ic_ms_edit, getString(R.string.alimento_editar),
                () -> mostrarDialogoEditarAlimento(alimento)));
        if (esAdmin && esPredefinido) {
            actions.add(new UIHelper.MenuAction(R.drawable.ic_ms_visibility_off, getString(R.string.comida_desactivar_alimento),
                    () -> UIHelper.mostrarDialogoConIcono(this,
                            getString(R.string.comida_desactivar_alimento),
                            getString(R.string.alimento_desactivar_confirmar),
                            R.drawable.ic_ms_visibility_off,
                            () -> desactivarAlimento(alimento))));
        }
        actions.add(new UIHelper.MenuAction(R.drawable.ic_ms_delete, getString(R.string.alimento_eliminar), true,
                () -> UIHelper.mostrarDialogoConIcono(this,
                        getString(R.string.alimento_eliminar),
                        getString(R.string.alimento_eliminar_confirmar),
                        R.drawable.ic_ms_delete,
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
                // Un macro que no se entiende se marca y el diálogo sigue abierto (GP-142).
                double[] macros = CamposMacro.leer(etProteinas, etCarbos, etGrasas);
                if (macros == null) return;
                body.put("proteinas",     BigDecimal.valueOf(macros[0]));
                body.put("carbohidratos", BigDecimal.valueOf(macros[1]));
                body.put("grasas",        BigDecimal.valueOf(macros[2]));
                dialog.dismiss();
                // Muestra el spinner modal mientras se guarda la edición del alimento
                LoadingDialog.show(this);
                alimentoApi.patch(alimento.getId(), body).enqueue(new ApiCallback<Void>() {
                    @Override
                    public void onOk(Void ignored) {
                        // Oculta el spinner al confirmarse la edición
                        LoadingDialog.hide(AnadirAlimentoActivity.this);
                        refrescar();
                    }
                    @Override
                    public void onFail(int code, String message) {
                        // Oculta el spinner y mapea el código de error a un mensaje de usuario
                        LoadingDialog.hide(AnadirAlimentoActivity.this);
                        UiFeedback.toastError(AnadirAlimentoActivity.this, code, message);
                    }
                });
            } catch (NumberFormatException e) {
                // Solo pueden ser las calorías (los macros ya se han comprobado): demasiado
                // grandes para un número. Se marca el campo y el diálogo sigue abierto.
                etCalorias.setError(getString(R.string.error_numero_invalido));
                etCalorias.requestFocus();
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
                refrescar();
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
                refrescar();
            }
            @Override
            public void onFail(int code, String message) {
                // Oculta el spinner y mapea el código de error a un mensaje de usuario
                LoadingDialog.hide(AnadirAlimentoActivity.this);
                UiFeedback.toastError(AnadirAlimentoActivity.this, code, message);
            }
        });
    }
}
