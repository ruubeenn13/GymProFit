package es.pmdm.gymprofit.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.model.comida.AnadirRespuesta;
import es.pmdm.gymprofit.model.plato.Ingrediente;
import es.pmdm.gymprofit.model.plato.IngredienteElegido;
import es.pmdm.gymprofit.model.plato.Plato;
import es.pmdm.gymprofit.model.plato.Valores;
import es.pmdm.gymprofit.network.AlimentoApi;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ComidaApi;
import es.pmdm.gymprofit.network.PlatoApi;
import es.pmdm.gymprofit.ui.adapters.IngredientesPlatoAdapter;
import es.pmdm.gymprofit.utils.AtrasHoja;
import es.pmdm.gymprofit.utils.Transiciones;
import es.pmdm.gymprofit.utils.Anadidos;
import es.pmdm.gymprofit.utils.AvisoDescartar;
import es.pmdm.gymprofit.utils.CantidadFicha;
import es.pmdm.gymprofit.utils.CuentaPlato;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.IngredientesPlato;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.Numeros;
import es.pmdm.gymprofit.utils.PedidoCantidad;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// CrearPlatoActivity — crear y editar un plato propio (tablero 10, lote 1.6.6, DEC-047)
//
//   · El nombre; los ingredientes, cada uno con su icono, su cantidad y sus kcal, y arriba
//     cuántos son y lo que suman. «Añadir ingrediente» abre Añadir en modo ingredientes y
//     el botón del escáner, el escáner igual: nada va a la API hasta guardar el plato.
//     Tocar un ingrediente abre su cantidad («Actualizar»); deslizarlo lo quita, con
//     «Deshacer», como en una comida.
//   · «¿Cuántas raciones salen?», de 1 a 99, con sus botones y, tocando el número,
//     escribiéndolo. El peso del plato hecho, opcional: «Para apuntarlo también en gramos:
//     105 kcal por 100 g», o lo que esté mal, que no deja guardar.
//   · «Por ración, de 4» («El plato entero», si es una), con las cuatro cifras en vivo: la
//     misma cuenta que la API (CuentaPlato), que es lo que se apuntará con «1 ración».
//   · «Guardar plato» pide el nombre y al menos un ingrediente, y lo que falte se dice en
//     su sitio. Al guardar uno nuevo sube la hoja: «Añadir 1 ración a la cena» (la comida de
//     la etiqueta de Añadir; lo añadido vuelve a Añadir y cuenta en su barra) y «Ver Mis
//     platos». Desde una comida («Guardar como plato»), la hoja solo confirma y vuelve.
//   · Editar es la misma pantalla, «Editar plato» y «Guardar cambios», sin hoja al acabar.
//     Salir con cambios sin guardar pregunta antes (AvisoDescartar).
// ============================================================
public class CrearPlatoActivity extends BaseActivity {

    /** El plato que se edita (su id). */
    public static final String EXTRA_EDITAR = "editarPlato";
    /** «Guardar como plato»: las líneas de la comida (AlimentoComida en JSON). */
    public static final String EXTRA_LINEAS = "lineasComida";

    private static final int DURACION_AVISO = 5000;

    private final PlatoApi platoApi = ApiClient.service(PlatoApi.class);
    private final AlimentoApi alimentoApi = ApiClient.service(AlimentoApi.class);
    private final ComidaApi comidaApi = ApiClient.service(ComidaApi.class);

    private int editarId = -1;
    private boolean desdeComida;
    @Nullable private String tipoComida;
    @Nullable private String fecha;

    private IngredientesPlato ingredientes = new IngredientesPlato();
    private int raciones = 1;
    private String estadoInicial = "";
    private boolean cargado;

    private IngredientesPlatoAdapter adapter;
    private VistaEstado estado;
    private EditText etNombre;
    private EditText etPeso;
    private NumberFormat nf;
    private NumberFormat nfEntero;
    @Nullable private Snackbar aviso;
    @Nullable private Plato guardado;
    private AtrasHoja cerrarHoja;

    private ActivityResultLauncher<Intent> anadirLauncher;
    private ActivityResultLauncher<Intent> ingredienteLauncher;
    private ActivityResultLauncher<Intent> cambiarLauncher;
    // El ingrediente cuya cantidad se está cambiando en la ficha.
    private int cambiando = -1;

    /** Para editar un plato. */
    @NonNull
    public static Intent paraEditar(@NonNull Context ctx, int platoId) {
        return new Intent(ctx, CrearPlatoActivity.class).putExtra(EXTRA_EDITAR, platoId);
    }

    /** «Guardar como plato» desde una comida: sus alimentos de ingredientes. */
    @NonNull
    public static Intent desdeComida(@NonNull Context ctx, @NonNull List<AlimentoComida> lineas) {
        return new Intent(ctx, CrearPlatoActivity.class).putExtra(EXTRA_LINEAS, new Gson().toJson(lineas));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_plato);
        nf = NumberFormat.getNumberInstance(FechaUtils.localeDeLaApp(this));
        nf.setMaximumFractionDigits(1);
        nfEntero = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(this));

        editarId = getIntent().getIntExtra(EXTRA_EDITAR, -1);
        desdeComida = getIntent().hasExtra(EXTRA_LINEAS);
        tipoComida = getIntent().getStringExtra(AnadirAlimentoActivity.EXTRA_TIPO);
        fecha = getIntent().getStringExtra(AnadirAlimentoActivity.EXTRA_FECHA);

        estado = new VistaEstado(findViewById(R.id.estadoPlato));
        etNombre = findViewById(R.id.etNombrePlato);
        etPeso = findViewById(R.id.etPesoPlato);
        ((TextView) findViewById(R.id.tvRotuloPeso)).setText(conOpcional());

        // Las filas no tapan las esquinas redondeadas de su tarjeta.
        findViewById(R.id.cardIngredientes).setClipToOutline(true);
        RecyclerView rv = findViewById(R.id.rvIngredientes);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IngredientesPlatoAdapter(ingredientes, new IngredientesPlatoAdapter.Acciones() {
            @Override public void abrir(int posicion) { cambiarCantidad(posicion); }
            @Override public void quitar(int posicion) { quitarIngrediente(posicion); }
        });
        rv.setAdapter(adapter);
        DefaultItemAnimator animador = new DefaultItemAnimator();
        animador.setRemoveDuration(Movimiento.RECOLOCA);
        animador.setMoveDuration(Movimiento.RECOLOCA);
        animador.setAddDuration(Movimiento.RECOLOCA);
        rv.setItemAnimator(animador);
        new ItemTouchHelper(new Deslizar()).attachToRecyclerView(rv);

        anadirLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::alVolverDeAnadir);
        ingredienteLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                this::alVolverDeIngrediente);
        cambiarLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                this::alVolverDeCambiar);

        findViewById(R.id.btnAnadirIngrediente).setOnClickListener(v -> abrirAnadir());
        findViewById(R.id.btnEscanearIngrediente).setOnClickListener(v -> abrirEscaner());
        findViewById(R.id.btnMenosRaciones).setOnClickListener(v -> ponerRaciones(raciones - 1));
        findViewById(R.id.btnMasRaciones).setOnClickListener(v -> ponerRaciones(raciones + 1));
        findViewById(R.id.tvRaciones).setOnClickListener(v -> escribirRaciones());
        findViewById(R.id.btnGuardarPlato).setOnClickListener(v -> guardar());
        etNombre.addTextChangedListener(new Cambio(() -> findViewById(R.id.tvErrorNombrePlato).setVisibility(View.GONE)));
        etPeso.addTextChangedListener(new Cambio(this::pintarCifras));

        // Salir con algo sin guardar pregunta antes de tirarlo. Con la hoja a la vista, el
        // plato ya está guardado: atrás es «Ver Mis platos».
        AvisoDescartar.instalar(this, null, this::hayCambios);
        findViewById(R.id.btnCerrarPlato).setOnClickListener(v -> AvisoDescartar.salir(this, this::hayCambios));
        cerrarHoja = new AtrasHoja(findViewById(R.id.hojaPlato)) {
            @Override
            protected void cerrar() {
                terminarGuardado();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, cerrarHoja);

        boolean editar = editarId > 0;
        ((TextView) findViewById(R.id.tvTituloPlato)).setText(editar ? R.string.plato_titulo_editar : R.string.plato_titulo_nuevo);
        ((MaterialButton) findViewById(R.id.btnGuardarPlato)).setText(editar ? R.string.plato_guardar_cambios : R.string.plato_guardar);

        if (savedInstanceState != null) {
            restaurar(savedInstanceState);
        } else if (editar) {
            cargarPlato();
        } else if (desdeComida) {
            cargarDesdeComida();
        } else {
            listo(false);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("ingredientes", new Gson().toJson(ingredientes.lista()));
        out.putInt("raciones", raciones);
        out.putString("inicial", estadoInicial);
        out.putBoolean("cargado", cargado);
        out.putString("platosFuera", ((TextView) findViewById(R.id.tvPlatosFuera)).getText().toString());
    }

    private void restaurar(@NonNull Bundle b) {
        IngredienteElegido[] lista = new Gson().fromJson(b.getString("ingredientes"), IngredienteElegido[].class);
        ingredientes = new IngredientesPlato(lista != null ? Arrays.asList(lista) : null);
        adapter = nuevoAdapter();
        raciones = b.getInt("raciones", 1);
        estadoInicial = b.getString("inicial", "");
        String fuera = b.getString("platosFuera", "");
        if (!fuera.isEmpty()) mostrarPlatosFuera(fuera);
        if (b.getBoolean("cargado")) {
            cargado = true;
            pintarTodo();
        } else if (editarId > 0) {
            cargarPlato();
        } else if (desdeComida) {
            cargarDesdeComida();
        }
    }

    private IngredientesPlatoAdapter nuevoAdapter() {
        IngredientesPlatoAdapter a = new IngredientesPlatoAdapter(ingredientes, new IngredientesPlatoAdapter.Acciones() {
            @Override public void abrir(int posicion) { cambiarCantidad(posicion); }
            @Override public void quitar(int posicion) { quitarIngrediente(posicion); }
        });
        ((RecyclerView) findViewById(R.id.rvIngredientes)).setAdapter(a);
        return a;
    }

    // ── Cargar ──────────────────────────────────────────────────────────────

    private void cargarPlato() {
        estado.cargando();
        findViewById(R.id.scrollPlato).setVisibility(View.INVISIBLE);
        platoApi.porId(editarId).enqueue(new ApiCallback<Plato>() {
            @Override
            public void onOk(Plato p) {
                if (isDestroyed()) return;
                if (p == null) {
                    estado.error(getString(R.string.plato_error_cargar), CrearPlatoActivity.this::cargarPlato);
                    return;
                }
                etNombre.setText(p.getNombre());
                raciones = Math.max(CuentaPlato.RACIONES_MIN, Math.min(CuentaPlato.RACIONES_MAX, p.getRaciones()));
                if (p.getPesoGramos() != null) etPeso.setText(numeroEscrito(p.getPesoGramos()));
                List<IngredienteElegido> lista = new ArrayList<>();
                for (Ingrediente i : p.getIngredientes()) lista.add(IngredienteElegido.de(i));
                ingredientes = new IngredientesPlato(lista);
                adapter = nuevoAdapter();
                listo(true);
            }

            @Override
            public void onFail(int code, String message) {
                if (isDestroyed()) return;
                estado.error(VistaEstado.mensaje(CrearPlatoActivity.this, R.string.plato_error_cargar, code, message),
                        CrearPlatoActivity.this::cargarPlato);
            }
        });
    }

    // «Guardar como plato»: los alimentos de la comida, cada uno con su cantidad. Hacen
    // falta enteros (sus valores y sus raciones), así que se piden; los platos no entran.
    private void cargarDesdeComida() {
        AlimentoComida[] lineas = new Gson().fromJson(getIntent().getStringExtra(EXTRA_LINEAS), AlimentoComida[].class);
        List<AlimentoComida> entran = new ArrayList<>();
        List<String> fuera = new ArrayList<>();
        if (lineas != null) {
            for (AlimentoComida l : lineas) {
                if (l.isAlimentoEsPlato()) fuera.add(l.getNombreAlimento());
                else entran.add(l);
            }
        }
        if (!fuera.isEmpty()) {
            mostrarPlatosFuera(getResources().getQuantityString(R.plurals.plato_platos_fuera, fuera.size(),
                    String.join(", ", fuera)));
        }
        if (entran.isEmpty()) {
            listo(false);
            return;
        }
        estado.cargando();
        findViewById(R.id.scrollPlato).setVisibility(View.INVISIBLE);
        Alimento[] alimentos = new Alimento[entran.size()];
        int[] quedan = {entran.size()};
        boolean[] fallo = {false};
        for (int k = 0; k < entran.size(); k++) {
            int indice = k;
            alimentoApi.porId(entran.get(k).getAlimentoId()).enqueue(new ApiCallback<Alimento>() {
                @Override
                public void onOk(Alimento a) {
                    alimentos[indice] = a;
                    if (--quedan[0] == 0) desdeComidaListo(entran, alimentos, fallo[0]);
                }

                @Override
                public void onFail(int code, String message) {
                    fallo[0] = true;
                    if (--quedan[0] == 0) desdeComidaListo(entran, alimentos, true);
                }
            });
        }
    }

    private void desdeComidaListo(List<AlimentoComida> lineas, Alimento[] alimentos, boolean fallo) {
        if (isDestroyed()) return;
        if (fallo) {
            estado.error(getString(R.string.plato_error_cargar), this::cargarDesdeComida);
            return;
        }
        List<IngredienteElegido> lista = new ArrayList<>();
        for (int k = 0; k < lineas.size(); k++) {
            AlimentoComida l = lineas.get(k);
            Alimento a = alimentos[k];
            if (a == null || a.esPlato()) continue;
            CantidadFicha c = CantidadFicha.deLinea(a, l.getRacionId(), l.getRaciones(), l.getCantidadGramos());
            lista.add(IngredienteElegido.de(a, c));
        }
        ingredientes = new IngredientesPlato(lista);
        adapter = nuevoAdapter();
        listo(true);
    }

    private void mostrarPlatosFuera(String texto) {
        TextView tv = findViewById(R.id.tvPlatosFuera);
        tv.setText(texto);
        tv.setVisibility(View.VISIBLE);
    }

    // Ya se puede tocar: se pinta todo y lo de ahora es lo que no hay que avisar al salir.
    private void listo(boolean cascada) {
        cargado = true;
        estado.oculto();
        findViewById(R.id.scrollPlato).setVisibility(View.VISIBLE);
        if (cascada) adapter.cascada();
        pintarTodo();
        estadoInicial = foto();
    }

    // ── Pintar ──────────────────────────────────────────────────────────────

    private void pintarTodo() {
        adapter.notifyDataSetChanged();
        pintarIngredientes();
        pintarRaciones(false);
    }

    private void pintarIngredientes() {
        int n = ingredientes.cuantos();
        ((TextView) findViewById(R.id.tvTotalIngredientes)).setText(n == 0 ? ""
                : getResources().getQuantityString(R.plurals.plato_total_ingredientes, n, n,
                nfEntero.format(ingredientes.total().kcal)));
        if (n > 0) findViewById(R.id.tvErrorIngredientes).setVisibility(View.GONE);
        pintarCifras();
    }

    private void pintarRaciones(boolean caer) {
        TextView tv = findViewById(R.id.tvRaciones);
        tv.setText(nfEntero.format(raciones));
        tv.setContentDescription(getResources().getQuantityString(R.plurals.plato_raciones_a11y, raciones, raciones));
        findViewById(R.id.btnMenosRaciones).setEnabled(raciones > CuentaPlato.RACIONES_MIN);
        findViewById(R.id.btnMasRaciones).setEnabled(raciones < CuentaPlato.RACIONES_MAX);
        findViewById(R.id.btnMenosRaciones).setAlpha(raciones > CuentaPlato.RACIONES_MIN ? 1f : 0.4f);
        findViewById(R.id.btnMasRaciones).setAlpha(raciones < CuentaPlato.RACIONES_MAX ? 1f : 0.4f);
        pintarCifras();
        if (caer) {
            // Las cifras de «Por ración» caen a su valor nuevo (momento 23).
            for (int id : new int[]{R.id.cifraPlatoKcal, R.id.cifraPlatoProteina, R.id.cifraPlatoCarb, R.id.cifraPlatoGrasa}) {
                Movimiento.caerCifra(findViewById(id).findViewById(R.id.tvValorCifra));
            }
        }
    }

    // El peso escrito, si vale; null si está vacío. NaN si no se entiende.
    @Nullable
    private Double pesoEscrito() {
        String t = etPeso.getText().toString().trim();
        if (t.isEmpty()) return null;
        try {
            return Numeros.leerDecimal(t);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /** Lo que impide guardar el peso, o null si vale (o no hay). */
    @Nullable
    private String errorPeso() {
        Double peso = pesoEscrito();
        if (peso == null) return null;
        if (peso.isNaN() || CuentaPlato.pesoMalo(peso, raciones)) return getString(R.string.plato_peso_malo);
        if (CuentaPlato.demasiadasKcal(ingredientes.total(), raciones, peso)) {
            return getString(R.string.plato_peso_demasiadas_kcal, nfEntero.format(CuentaPlato.KCAL_MAXIMAS_POR_100));
        }
        return null;
    }

    // «Por ración» y la línea del peso, con la cuenta de la API.
    private void pintarCifras() {
        Double peso = pesoEscrito();
        String error = errorPeso();
        Double valido = error == null ? peso : null;
        CuentaPlato.Cifras total = ingredientes.total();
        TextView ayuda = findViewById(R.id.tvAyudaPeso);
        if (peso == null) {
            ayuda.setText(R.string.plato_peso_ayuda_vacio);
            ayuda.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        } else if (error != null) {
            ayuda.setText(error);
            ayuda.setTextColor(ContextCompat.getColor(this, R.color.gp_error));
        } else {
            ayuda.setText(getString(R.string.plato_peso_ayuda, nfEntero.format(CuentaPlato.guardado(total, raciones, peso).kcal)));
            ayuda.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        }

        CuentaPlato.Cifras r = CuentaPlato.porRacion(total, raciones, valido);
        ((TextView) findViewById(R.id.tvTituloPorRacion)).setText(raciones == 1 ? getString(R.string.plato_el_plato_entero)
                : getString(R.string.plato_por_racion_de, raciones));
        String kcal = nfEntero.format(r.kcal);
        String p = nfEntero.format(Math.round(r.proteinas.doubleValue()));
        String c = nfEntero.format(Math.round(r.carbohidratos.doubleValue()));
        String g = nfEntero.format(Math.round(r.grasas.doubleValue()));
        cifra(R.id.cifraPlatoKcal, kcal, R.string.ficha_kcal, 0);
        cifra(R.id.cifraPlatoProteina, getString(R.string.ficha_gramos_valor, p), etiquetaProteina(),
                ContextCompat.getColor(this, R.color.gp_macro_proteinas));
        cifra(R.id.cifraPlatoCarb, getString(R.string.ficha_gramos_valor, c), R.string.ficha_carb, 0);
        cifra(R.id.cifraPlatoGrasa, getString(R.string.ficha_gramos_valor, g), R.string.ficha_grasa, 0);
        findViewById(R.id.cifrasPlato).setContentDescription(getString(raciones == 1 ? R.string.plato_cifras_entero_a11y
                : R.string.plato_cifras_racion_a11y, kcal, p, c, g));
    }

    private void cifra(int id, String valor, int etiqueta, int colorValor) {
        View v = findViewById(id);
        TextView tv = v.findViewById(R.id.tvValorCifra);
        tv.setText(valor);
        if (colorValor != 0) tv.setTextColor(colorValor);
        ((TextView) v.findViewById(R.id.tvEtiquetaCifra)).setText(etiqueta);
    }

    // Con letra grande «proteína» no cabe en la cuarta parte del ancho: «prot.», como en la ficha.
    private int etiquetaProteina() {
        return getResources().getConfiguration().fontScale >= 1.3f ? R.string.ficha_proteina_corta : R.string.ficha_proteina;
    }

    private CharSequence conOpcional() {
        android.text.SpannableStringBuilder s = new android.text.SpannableStringBuilder(getString(R.string.plato_peso)).append(' ');
        int desde = s.length();
        s.append(getString(R.string.crear_opcional));
        s.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.NORMAL), desde, s.length(),
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new android.text.style.ForegroundColorSpan(color(com.google.android.material.R.attr.colorOnSurfaceVariant)),
                desde, s.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    // ── Raciones ────────────────────────────────────────────────────────────

    private void ponerRaciones(int nuevas) {
        int r = Math.max(CuentaPlato.RACIONES_MIN, Math.min(CuentaPlato.RACIONES_MAX, nuevas));
        if (r == raciones) return;
        raciones = r;
        Movimiento.vibrar(findViewById(R.id.tvRaciones), Movimiento.Vibracion.LIGERA);
        pintarRaciones(true);
    }

    private void escribirRaciones() {
        View vista = getLayoutInflater().inflate(R.layout.dialog_cantidad, null);
        EditText et = vista.findViewById(R.id.etCantidad);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf(raciones));
        et.selectAll();
        ((TextView) vista.findViewById(R.id.tvUnidadCantidad)).setText(R.string.plato_raciones_unidad);
        androidx.appcompat.app.AlertDialog d = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.plato_raciones_pregunta)
                .setView(vista)
                .setPositiveButton(R.string.dialog_confirmar, null)
                .setNegativeButton(R.string.dialog_cancelar, null)
                .create();
        String inicial = et.getText().toString();
        AvisoDescartar.instalarEnDialogo(this, d, () -> AvisoDescartar.distinto(inicial, et.getText()), et);
        d.setOnShowListener(x -> d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            int valor;
            try {
                valor = Integer.parseInt(et.getText().toString().trim());
            } catch (NumberFormatException e) {
                valor = -1;
            }
            if (valor < CuentaPlato.RACIONES_MIN || valor > CuentaPlato.RACIONES_MAX) {
                UIHelper.marcarError(et, getString(R.string.plato_raciones_invalidas, CuentaPlato.RACIONES_MAX));
                return;
            }
            ponerRaciones(valor);
            d.dismiss();
        }));
        d.show();
    }

    // ── Ingredientes ────────────────────────────────────────────────────────

    private void abrirAnadir() {
        anadirLauncher.launch(new Intent(this, AnadirAlimentoActivity.class)
                .putExtra(AnadirAlimentoActivity.EXTRA_MODO_INGREDIENTES, true)
                .putExtra(AnadirAlimentoActivity.EXTRA_INGREDIENTES, new Gson().toJson(ingredientes.lista())));
    }

    private void abrirEscaner() {
        if (ingredientes.lleno()) {
            UIHelper.mostrarToastError(this, getString(R.string.plato_ingredientes_maximo, CuentaPlato.INGREDIENTES_MAX));
            return;
        }
        ingredienteLauncher.launch(new Intent(this, EscanerActivity.class)
                .putExtra(AnadirAlimentoActivity.EXTRA_MODO_INGREDIENTES, true));
    }

    // De Añadir en modo ingredientes: la lista entera, como quedó.
    private void alVolverDeAnadir(ActivityResult r) {
        Intent datos = r.getData();
        String json = datos != null ? datos.getStringExtra(AnadirAlimentoActivity.EXTRA_INGREDIENTES) : null;
        if (r.getResultCode() != RESULT_OK || json == null) return;
        int antes = ingredientes.cuantos();
        IngredienteElegido[] lista = new Gson().fromJson(json, IngredienteElegido[].class);
        ingredientes = new IngredientesPlato(lista != null ? Arrays.asList(lista) : null);
        adapter = nuevoAdapter();
        if (ingredientes.cuantos() > antes) adapter.cascada();
        pintarTodo();
    }

    // Del escáner: uno más (o la cantidad nueva de uno que ya estaba).
    private void alVolverDeIngrediente(ActivityResult r) {
        Intent datos = r.getData();
        String json = datos != null ? datos.getStringExtra(FichaAlimentoActivity.EXTRA_INGREDIENTE) : null;
        if (r.getResultCode() != RESULT_OK || json == null) return;
        IngredienteElegido i = new Gson().fromJson(json, IngredienteElegido.class);
        boolean nuevo = !ingredientes.tiene(i.getAlimento());
        ingredientes.poner(i);
        if (nuevo) adapter.notifyItemInserted(ingredientes.cuantos() - 1);
        else adapter.notifyDataSetChanged();
        pintarIngredientes();
    }

    private void cambiarCantidad(int posicion) {
        if (posicion < 0 || posicion >= ingredientes.cuantos()) return;
        cambiando = posicion;
        IngredienteElegido i = ingredientes.lista().get(posicion);
        RecyclerView.ViewHolder fila = ((RecyclerView) findViewById(R.id.rvIngredientes))
                .findViewHolderForAdapterPosition(posicion);
        Intent ficha = FichaAlimentoActivity.paraIngrediente(this, i.getAlimento(), i);
        if (fila != null) Transiciones.abrirTarjeta(this, fila.itemView, ficha, cambiarLauncher);
        else cambiarLauncher.launch(ficha);
    }

    private void alVolverDeCambiar(ActivityResult r) {
        Intent datos = r.getData();
        String json = datos != null ? datos.getStringExtra(FichaAlimentoActivity.EXTRA_INGREDIENTE) : null;
        if (r.getResultCode() != RESULT_OK || json == null) return;
        ingredientes.poner(new Gson().fromJson(json, IngredienteElegido.class));
        if (cambiando >= 0 && cambiando < ingredientes.cuantos()) adapter.notifyItemChanged(cambiando);
        else adapter.notifyDataSetChanged();
        cambiando = -1;
        pintarIngredientes();
    }

    // Deslizar lo quita al momento; «Deshacer» lo devuelve a su sitio. Nada va a la API.
    private void quitarIngrediente(int posicion) {
        if (posicion < 0 || posicion >= ingredientes.cuantos()) return;
        if (aviso != null) aviso.dismiss();
        Movimiento.vibrar(findViewById(R.id.rvIngredientes), Movimiento.Vibracion.LIGERA);
        IngredienteElegido quitado = ingredientes.quitar(posicion);
        adapter.notifyItemRemoved(posicion);
        pintarIngredientes();
        aviso = Snackbar.make(findViewById(R.id.raizCrearPlato),
                getString(R.string.comida_quitado, quitado.getAlimento().getNombre()), DURACION_AVISO);
        aviso.setAnchorView(findViewById(R.id.pieCrearPlato));
        aviso.setAction(R.string.envivo_deshacer, v -> {
            ingredientes.devolver(posicion, quitado);
            adapter.notifyItemInserted(Math.min(posicion, ingredientes.cuantos() - 1));
            pintarIngredientes();
        });
        // Invertido, como en una comida.
        aviso.setBackgroundTint(color(com.google.android.material.R.attr.colorOnSurface));
        aviso.setTextColor(color(com.google.android.material.R.attr.colorSurface));
        aviso.setActionTextColor(ContextCompat.getColor(this, R.color.gp_accion_inversa));
        aviso.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
            @Override
            public void onDismissed(Snackbar s, int evento) {
                if (aviso == s) aviso = null;
            }
        });
        aviso.show();
    }

    /** Deslizar un ingrediente hasta el final lo quita; detrás, el rojo con «Quitar». */
    private final class Deslizar extends ItemTouchHelper.SimpleCallback {
        private final Paint fondo = new Paint();
        private final Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
        @Nullable private final Drawable icono;
        private final float d = getResources().getDisplayMetrics().density;

        Deslizar() {
            super(0, ItemTouchHelper.LEFT);
            int rojo = ContextCompat.getColor(CrearPlatoActivity.this, R.color.gp_error);
            fondo.setColor(ContextCompat.getColor(CrearPlatoActivity.this, R.color.gp_error_container));
            texto.setColor(rojo);
            texto.setTextSize(getResources().getDimension(R.dimen.text_eyebrow));
            texto.setFakeBoldText(true);
            texto.setTextAlign(Paint.Align.CENTER);
            icono = ContextCompat.getDrawable(CrearPlatoActivity.this, R.drawable.ic_ms_delete);
            if (icono != null) icono.setTintList(ColorStateList.valueOf(rojo));
        }

        @Override
        public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder a, @NonNull RecyclerView.ViewHolder b) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder h, int direccion) {
            quitarIngrediente(h.getBindingAdapterPosition());
        }

        @Override
        public long getAnimationDuration(@NonNull RecyclerView rv, int tipo, float dx, float dy) {
            return tipo == ItemTouchHelper.ANIMATION_TYPE_SWIPE_SUCCESS ? Movimiento.SALE : Movimiento.RECOLOCA;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder h,
                                float dx, float dy, int accion, boolean activo) {
            View f = h.itemView;
            if (dx < 0) {
                c.drawRect(f.getRight() + dx, f.getTop(), f.getRight(), f.getBottom(), fondo);
                float cx = f.getRight() - 48 * d;
                float cy = f.getTop() + f.getHeight() / 2f;
                if (icono != null) {
                    int m = Math.round(12 * d);
                    icono.setBounds(Math.round(cx - m), Math.round(cy - 2 * m), Math.round(cx + m), Math.round(cy));
                    icono.draw(c);
                }
                c.drawText(getString(R.string.plato_quitar_fondo), cx, cy + 16 * d, texto);
            }
            super.onChildDraw(c, rv, h, dx, dy, accion, activo);
        }
    }

    // ── Guardar ─────────────────────────────────────────────────────────────

    private void guardar() {
        if (!cargado) return;
        boolean bien = true;
        String nombre = etNombre.getText().toString().trim();
        if (nombre.isEmpty()) {
            TextView e = findViewById(R.id.tvErrorNombrePlato);
            e.setText(R.string.plato_error_nombre);
            e.setVisibility(View.VISIBLE);
            Movimiento.temblar(etNombre);
            bien = false;
        }
        if (ingredientes.cuantos() == 0) {
            TextView e = findViewById(R.id.tvErrorIngredientes);
            e.setText(R.string.plato_error_ingredientes);
            e.setVisibility(View.VISIBLE);
            Movimiento.temblar(findViewById(R.id.cardIngredientes));
            bien = false;
        }
        if (errorPeso() != null) {
            Movimiento.temblar(findViewById(R.id.filaPeso));
            bien = false;
        }
        if (!bien) {
            Movimiento.vibrar(findViewById(R.id.btnGuardarPlato), Movimiento.Vibracion.ERROR);
            return;
        }
        Double peso = pesoEscrito();
        Map<String, Object> cuerpo = PedidoCantidad.plato(nombre, raciones, peso, ingredientes.lista());
        View boton = findViewById(R.id.btnGuardarPlato);
        boton.setEnabled(false);
        LoadingDialog.show(this);
        retrofit2.Call<Plato> llamada = editarId > 0 ? platoApi.cambiar(editarId, cuerpo) : platoApi.crear(cuerpo);
        llamada.enqueue(new ApiCallback<Plato>() {
            @Override
            public void onOk(Plato p) {
                LoadingDialog.hide(CrearPlatoActivity.this);
                if (isDestroyed()) return;
                boton.setEnabled(true);
                estadoInicial = foto();
                if (editarId > 0 || p == null) {
                    UIHelper.mostrarToastExito(CrearPlatoActivity.this, getString(R.string.plato_cambiado));
                    setResult(RESULT_OK);
                    finish();
                    return;
                }
                mostrarHoja(p);
            }

            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(CrearPlatoActivity.this);
                if (isDestroyed()) return;
                boton.setEnabled(true);
                UiFeedback.toastError(CrearPlatoActivity.this, code, message);
            }
        });
    }

    // La hoja del tablero: el ✓, «Lentejas de casa, en Mis platos», lo que vale una ración
    // y «Añadir 1 ración a la cena» con «Ver Mis platos». Desde una comida, solo confirma.
    private void mostrarHoja(@NonNull Plato p) {
        guardado = p;
        cerrarHoja.setEnabled(true);
        Valores r = p.getPorRacion();
        int kcal = r != null ? r.getCalorias() : 0;
        long prot = r != null ? Math.round(r.getProteinas()) : 0;
        ((TextView) findViewById(R.id.tvTituloHojaPlato)).setText(getString(R.string.plato_hoja_titulo, p.getNombre()));
        ((TextView) findViewById(R.id.tvDetalleHojaPlato)).setText(p.getRaciones() == 1
                ? getString(R.string.plato_hoja_detalle_entero, nfEntero.format(kcal), nfEntero.format(prot))
                : getResources().getQuantityString(R.plurals.plato_hoja_detalle, p.getRaciones(), p.getRaciones(),
                nfEntero.format(kcal), nfEntero.format(prot)));
        MaterialButton principal = findViewById(R.id.btnHojaAnadirRacion);
        View verPlatos = findViewById(R.id.btnHojaVerPlatos);
        if (desdeComida) {
            principal.setText(R.string.plato_hoja_volver);
            principal.setOnClickListener(v -> terminarGuardado());
            verPlatos.setVisibility(View.GONE);
        } else if (tipoComida != null && p.getAlimento() != null && !p.getAlimento().getRaciones().isEmpty()) {
            principal.setText(textoAnadirRacion(tipoComida));
            principal.setOnClickListener(v -> anadirRacion(p, principal));
            verPlatos.setOnClickListener(v -> terminarGuardado());
        } else {
            principal.setText(R.string.plato_hoja_ver_platos);
            principal.setOnClickListener(v -> terminarGuardado());
            verPlatos.setVisibility(View.GONE);
        }
        Movimiento.subirHojaPlato(findViewById(R.id.veloPlato), findViewById(R.id.hojaPlato), findViewById(R.id.okPlato));
        findViewById(R.id.tvTituloHojaPlato).sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED);
    }

    private static final int[] ANADIR_RACION_A = {R.string.plato_hoja_anadir_desayuno, R.string.plato_hoja_anadir_almuerzo,
            R.string.plato_hoja_anadir_comida, R.string.plato_hoja_anadir_merienda, R.string.plato_hoja_anadir_cena};

    private String textoAnadirRacion(@NonNull String tipo) {
        int i = AnadirAlimentoActivity.indiceTipo(tipo);
        return getString(ANADIR_RACION_A[i < 0 ? 3 : i]);
    }

    // «Añadir 1 ración a la cena»: lo añadido vuelve a Añadir, que lo cuenta en su barra y
    // deja la fila del plato en ✓.
    private void anadirRacion(@NonNull Plato p, @NonNull View boton) {
        Alimento a = p.getAlimento();
        if (a == null || tipoComida == null) return;
        boton.setEnabled(false);
        LoadingDialog.show(this);
        String dia = fecha != null ? fecha
                : new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(new java.util.Date());
        CantidadFicha una = CantidadFicha.nueva(a);
        comidaApi.anadir(PedidoCantidad.anadir(a, una, dia, tipoComida)).enqueue(new ApiCallback<AnadirRespuesta>() {
            @Override
            public void onOk(AnadirRespuesta r) {
                LoadingDialog.hide(CrearPlatoActivity.this);
                if (isDestroyed()) return;
                Movimiento.vibrar(boton, Movimiento.Vibracion.LIGERA);
                Intent datos = new Intent();
                if (r != null && r.getLinea() != null) {
                    String texto = es.pmdm.gymprofit.utils.AnadirRapido.texto(
                            es.pmdm.gymprofit.utils.Cantidades.Formatos.de(CrearPlatoActivity.this),
                            FechaUtils.localeDeLaApp(CrearPlatoActivity.this), una);
                    datos.putExtra(AnadirAlimentoActivity.EXTRA_ANADIDO, new Gson().toJson(
                            Anadidos.Anadido.de(r, null, tipoComida, texto, es.pmdm.gymprofit.utils.AnadirRapido.kcal(a, una))));
                }
                setResult(RESULT_OK, datos);
                finish();
            }

            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(CrearPlatoActivity.this);
                if (isDestroyed()) return;
                boton.setEnabled(true);
                UiFeedback.toastError(CrearPlatoActivity.this, code, message);
            }
        });
    }

    // «Ver Mis platos», atrás con la hoja o, desde una comida, volver: el plato ya está.
    private void terminarGuardado() {
        setResult(RESULT_OK);
        finish();
    }

    // ── Cambios sin guardar ─────────────────────────────────────────────────

    private boolean hayCambios() {
        return cargado && guardado == null && !foto().equals(estadoInicial);
    }

    private String foto() {
        StringBuilder sb = new StringBuilder(etNombre.getText().toString().trim()).append('|').append(raciones)
                .append('|').append(etPeso.getText().toString().trim());
        for (IngredienteElegido i : ingredientes.lista()) {
            sb.append('|').append(i.clave()).append(':').append(i.getRacionId()).append(':').append(i.getRaciones())
                    .append(':').append(i.getGramos());
        }
        return sb.toString();
    }

    // ── Ayudas ──────────────────────────────────────────────────────────────

    private String numeroEscrito(double valor) {
        return BigDecimal.valueOf(valor).stripTrailingZeros().toPlainString();
    }

    private int color(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    /** Un TextWatcher que solo avisa al cambiar. */
    private static final class Cambio implements TextWatcher {
        private final Runnable r;

        Cambio(Runnable r) {
            this.r = r;
        }

        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }

        @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

        @Override public void afterTextChanged(Editable s) { r.run(); }
    }
}
