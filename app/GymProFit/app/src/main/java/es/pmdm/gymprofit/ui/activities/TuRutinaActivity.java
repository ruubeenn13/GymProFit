package es.pmdm.gymprofit.ui.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.rutina.BorradorRutina;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.CatalogoEjercicios;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.ui.rutina.TextosCrearRutina;
import es.pmdm.gymprofit.ui.widget.HojaElegir;
import es.pmdm.gymprofit.ui.widget.SelectorPildora;
import es.pmdm.gymprofit.utils.AvisoDescartar;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.NotificationHelper;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// TuRutinaActivity — crear una rutina, paso 3, y editarla (GP-244, tablero Mezcla-3).
// Sustituye a ResumenCrearRutinaActivity y EditarRutinaActivity: es la misma pantalla.
//
// El nombre en grande, propuesto por las zonas mientras no se escriba otro; la tarjeta con
// ejercicios, series y minutos (calculados con la fórmula de la API, DuracionRutina) y, al
// crear, la barra del tiempo con su frase; las zonas y el nivel del perfil con «Cambiar». La
// lista se ordena arrastrando el asa o con ⋮ (subir, bajar, quitar y cambiar la pauta), que
// TalkBack ofrece como acciones de la fila. Lo que dice la pantalla sale de BorradorRutina.
//
// Guardar manda la duración que dice la pantalla (duracionMinutos; el contrato no cambia) y
// cada fila con su rango y su medida. Va paso a paso y cada paso deja su id en el borrador: si
// se corta a medias, «Guardar» sigue donde se quedó sin duplicar la rutina ni sus filas.
// Al crear, atrás vuelve al paso 2 con la lista; el aspa avisa y cierra el asistente. Al
// editar, salir con cambios avisa (GP-098).
// ============================================================
public class TuRutinaActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private static final String EXTRA_RUTINA_ID = "rutinaId";
    private static final String CLAVE_ORIGINALES = "originales";
    private static final String CLAVE_INICIAL = "inicial";

    /** El paso 3 del asistente, con lo elegido en el paso 2. */
    @NonNull
    public static Intent paraCrear(@NonNull Context ctx, @NonNull BorradorRutina b) {
        return new Intent(ctx, TuRutinaActivity.class).putExtra(AnadirEjerciciosActivity.EXTRA_BORRADOR, b.aJson());
    }

    /** Editar una rutina propia: se carga aquí. */
    @NonNull
    public static Intent paraEditar(@NonNull Context ctx, int rutinaId) {
        return new Intent(ctx, TuRutinaActivity.class).putExtra(EXTRA_RUTINA_ID, rutinaId);
    }

    private final RutinaApi api = ApiClient.service(RutinaApi.class);
    private BorradorRutina borrador;
    private boolean editar;
    // Al editar, las filas que tenía la rutina (fila → ejercicio): lo que falte se borra.
    private final LinkedHashMap<Integer, Integer> originales = new LinkedHashMap<>();
    // Lo que había al abrir, para saber si hay algo sin guardar.
    private String inicial;
    private boolean cargada;
    private boolean pintandoNombre;
    private String nivelPerfil;

    private EditText etNombre;
    private TextView tvPie;
    private MaterialButton btnGuardar;
    private VistaEstado estado;
    private Adaptador adaptador;
    private ItemTouchHelper arrastre;

    private ActivityResultLauncher<Intent> anadir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tu_rutina);
        int rutinaId = getIntent().getIntExtra(EXTRA_RUTINA_ID, -1);
        editar = rutinaId != -1;
        nivelPerfil = nivelDelPerfil(new PreferencesManager(this).getNivel());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(editar ? R.string.editar_titulo : R.string.crear_titulo);
        toolbar.setNavigationOnClickListener(v -> cerrar());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (editar) cerrar();
                else volverAlPaso2();
            }
        });

        etNombre = findViewById(R.id.etNombreRutina);
        tvPie = findViewById(R.id.tvPieTuRutina);
        btnGuardar = findViewById(R.id.btnGuardarRutina);
        btnGuardar.setText(editar ? R.string.editar_guardar : R.string.crear_guardar);
        estado = new VistaEstado(findViewById(R.id.estadoTuRutina));
        montarLista();

        anadir = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                BorradorRutina vuelto = BorradorRutina.deJson(r.getData().getStringExtra(AnadirEjerciciosActivity.EXTRA_BORRADOR));
                borrador.ejercicios = vuelto.ejercicios;
                adaptador.notifyDataSetChanged();
                pintar();
            }
        });

        etNombre.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                if (pintandoNombre || borrador == null) return;
                borrador.nombre = s.toString();
                borrador.nombreTocado = true;
                pintarPie();
            }
        });
        findViewById(R.id.btnCambiarNivel).setOnClickListener(v -> elegirNivel());
        findViewById(R.id.btnAnadirEjercicios).setOnClickListener(v -> {
            if (editar) anadir.launch(AnadirEjerciciosActivity.paraDevolver(this, borrador));
            else volverAlPaso2();
        });
        findViewById(R.id.btnAnadirDescripcion).setOnClickListener(v -> abrirDescripcion(true));
        ((EditText) findViewById(R.id.etDescripcionRutina)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                if (borrador != null && cargada) borrador.descripcion = s.toString();
            }
        });
        btnGuardar.setOnClickListener(v -> guardar());

        if (savedInstanceState != null) {
            borrador = BorradorRutina.deJson(savedInstanceState.getString(AnadirEjerciciosActivity.EXTRA_BORRADOR));
            inicial = savedInstanceState.getString(CLAVE_INICIAL);
            int[] o = savedInstanceState.getIntArray(CLAVE_ORIGINALES);
            if (o != null) for (int i = 0; i + 1 < o.length; i += 2) originales.put(o[i], o[i + 1]);
            listo();
        } else if (editar) {
            borrador = new BorradorRutina();
            borrador.rutinaId = rutinaId;
            cargar();
        } else {
            borrador = BorradorRutina.deJson(getIntent().getStringExtra(AnadirEjerciciosActivity.EXTRA_BORRADOR));
            if (borrador.nivel == null) borrador.nivel = nivelPerfil != null ? nivelPerfil : "PRINCIPIANTE";
            inicial = borrador.aJson();
            listo();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if (borrador != null) out.putString(AnadirEjerciciosActivity.EXTRA_BORRADOR, borrador.aJson());
        out.putString(CLAVE_INICIAL, inicial);
        int[] o = new int[originales.size() * 2];
        int i = 0;
        for (Map.Entry<Integer, Integer> e : originales.entrySet()) {
            o[i++] = e.getKey();
            o[i++] = e.getValue();
        }
        out.putIntArray(CLAVE_ORIGINALES, o);
    }

    // El nivel del perfil en los de una rutina; EXPERTO es AVANZADO. Null si no lo tiene.
    @Nullable
    static String nivelDelPerfil(@Nullable String perfil) {
        if (perfil == null || perfil.isEmpty()) return null;
        if ("EXPERTO".equals(perfil)) return "AVANZADO";
        if (Arrays.asList("PRINCIPIANTE", "INTERMEDIO", "AVANZADO").contains(perfil)) return perfil;
        return null;
    }

    // ── Cargar al editar ────────────────────────────────────────────────

    private void cargar() {
        estado.cargando();
        btnGuardar.setEnabled(false);
        AtomicInteger faltan = new AtomicInteger(3);
        Rutina[] rutina = new Rutina[1];
        List<RutinaEjercicio> filas = new ArrayList<>();
        java.util.concurrent.atomic.AtomicReference<Map<Integer, Ejercicio>> catalogo =
                new java.util.concurrent.atomic.AtomicReference<>();
        boolean[] fallo = {false};
        Runnable alAcabar = () -> {
            if (faltan.decrementAndGet() > 0 || isFinishing()) return;
            if (fallo[0] || rutina[0] == null) {
                estado.error(getString(R.string.crear_error_cargar), this::cargar);
                return;
            }
            montarDesde(rutina[0], filas, catalogo.get());
        };
        api.getPorId(borrador.rutinaId).enqueue(new ApiCallback<Rutina>() {
            @Override public void onOk(Rutina r) { rutina[0] = r; alAcabar.run(); }
            @Override public void onFail(int code, String message) { fallo[0] = true; alAcabar.run(); }
        });
        api.getEjerciciosDeRutina(borrador.rutinaId).enqueue(new ApiCallback<List<RutinaEjercicio>>() {
            @Override public void onOk(List<RutinaEjercicio> l) { if (l != null) filas.addAll(l); alAcabar.run(); }
            @Override public void onFail(int code, String message) { fallo[0] = true; alAcabar.run(); }
        });
        // El catálogo solo da la zona y el equipo de cada ejercicio (la frase de las zonas): sin
        // él, la rutina se edita igual y esa frase no sale.
        CatalogoEjercicios.cargar(porId -> {
            catalogo.set(porId);
            alAcabar.run();
        });
    }

    private void montarDesde(Rutina r, List<RutinaEjercicio> filas, @Nullable Map<Integer, Ejercicio> catalogo) {
        borrador.nombre = r.getNombre();
        borrador.nombreTocado = true;
        borrador.descripcion = r.getDescripcion();
        borrador.nivel = r.getNivel() != null ? r.getNivel() : "PRINCIPIANTE";
        filas.sort((a, b) -> Integer.compare(a.getOrden(), b.getOrden()));
        for (RutinaEjercicio f : filas) {
            BorradorRutina.Ejercicio e = new BorradorRutina.Ejercicio();
            e.ejercicioId = f.getEjercicioId();
            e.filaId = f.getId();
            e.nombre = f.getNombreEjercicio() != null ? f.getNombreEjercicio()
                    : getString(R.string.ejercicio_sin_nombre, f.getEjercicioId());
            e.series = f.getSeries() > 0 ? f.getSeries() : 3;
            int reps = f.getRepeticiones() > 0 ? f.getRepeticiones() : 10;
            e.min = f.getRepeticionesMin() != null ? f.getRepeticionesMin() : reps;
            e.max = f.getRepeticionesMax() != null ? f.getRepeticionesMax() : reps;
            e.segundos = "SEGUNDOS".equals(f.getMedida());
            e.porLado = f.getPorLado();
            e.descanso = f.getTiempoDescanso() != null ? f.getTiempoDescanso() : 90;
            Ejercicio c = catalogo != null ? catalogo.get(f.getEjercicioId()) : null;
            if (c != null) {
                e.grupo = c.getGrupoMuscular();
                e.equipo = c.getEquipoNecesario();
            }
            borrador.ejercicios.add(e);
            originales.put(f.getId(), f.getEjercicioId());
        }
        inicial = borrador.aJson();
        estado.oculto();
        adaptador.notifyDataSetChanged();
        listo();
    }

    // Con el borrador ya montado: rellena lo que se escribe y pinta todo.
    private void listo() {
        cargada = true;
        pintandoNombre = true;
        if (!borrador.nombreTocado) borrador.nombre = borrador.nombrePropuesto(BorradorRutina.Textos.de(this));
        etNombre.setText(borrador.nombre);
        pintandoNombre = false;
        ((TextView) findViewById(R.id.tvNombreNota)).setText(borrador.zonas.isEmpty()
                ? R.string.crear_nombre_nota_todos : R.string.crear_nombre_nota);
        findViewById(R.id.tvNombreNota).setVisibility(editar ? View.GONE : View.VISIBLE);
        if (borrador.descripcion != null && !borrador.descripcion.isEmpty()) {
            ((EditText) findViewById(R.id.etDescripcionRutina)).setText(borrador.descripcion);
            abrirDescripcion(false);
        }
        adaptador.notifyDataSetChanged();
        pintar();
    }

    private void abrirDescripcion(boolean foco) {
        findViewById(R.id.btnAnadirDescripcion).setVisibility(View.GONE);
        View campo = findViewById(R.id.tilDescripcion);
        campo.setVisibility(View.VISIBLE);
        if (foco) {
            findViewById(R.id.etDescripcionRutina).requestFocus();
            Movimiento.entrarUna(campo, 0, Movimiento.PLIEGA, 8);
        }
    }

    // ── Pintar ──────────────────────────────────────────────────────────

    private void pintar() {
        // El nombre propuesto sigue a las zonas de los ejercicios mientras nadie escriba otro.
        if (!borrador.nombreTocado && cargada) {
            String propuesto = borrador.nombrePropuesto(BorradorRutina.Textos.de(this));
            if (!propuesto.equals(etNombre.getText().toString())) {
                pintandoNombre = true;
                etNombre.setText(propuesto);
                pintandoNombre = false;
                borrador.nombre = propuesto;
            }
        }
        BorradorRutina.Textos t = BorradorRutina.Textos.de(this);
        int n = borrador.ejercicios.size();
        int series = borrador.seriesTotales();
        int minutos = n > 0 ? borrador.minutosEstimados() : 0;
        // Sin ejercicios no hay nada que contar: ni «0 ejercicios» ni «unos 0 minutos» (lo dice el pie).
        findViewById(R.id.filaCifras).setVisibility(n == 0 ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.tvCifraEjercicios)).setText(String.valueOf(n));
        ((TextView) findViewById(R.id.tvRotuloEjercicios)).setText(
                getResources().getQuantityString(R.plurals.crear_cifra_ejercicios, n));
        ((TextView) findViewById(R.id.tvCifraSeries)).setText(String.valueOf(series));
        ((TextView) findViewById(R.id.tvRotuloSeries)).setText(
                getResources().getQuantityString(R.plurals.crear_cifra_series, series));
        ((TextView) findViewById(R.id.tvCifraMinutos)).setText(String.valueOf(minutos));
        findViewById(R.id.filaCifras).setContentDescription(getString(R.string.crear_resumen_a11y,
                n, getResources().getQuantityString(R.plurals.crear_cifra_ejercicios, n),
                series, getResources().getQuantityString(R.plurals.crear_cifra_series, series), minutos));

        // Al crear, la barra del tiempo con su frase; al editar no hay tiempo elegido.
        LinearProgressIndicator barra = findViewById(R.id.barraTiempo);
        TextView frase = findViewById(R.id.tvFraseTiempo);
        String fraseTiempo = borrador.fraseTiempo(t);
        boolean conTiempo = borrador.minutos != null;
        barra.setVisibility(conTiempo ? View.VISIBLE : View.GONE);
        barra.setProgressCompat(n > 0 ? Math.round(borrador.partesDelTiempo() * 100) : 0, !Movimiento.quieto(this));
        frase.setVisibility(fraseTiempo != null ? View.VISIBLE : View.GONE);
        frase.setText(fraseTiempo);

        String zonas = borrador.zonasEnFrase(t);
        TextView tvZonas = findViewById(R.id.tvZonasRutina);
        tvZonas.setVisibility(zonas.isEmpty() ? View.GONE : View.VISIBLE);
        tvZonas.setText(zonas.isEmpty() ? "" : zonas + ".");

        String nivel = TextosCrearRutina.nivel(this, borrador.nivel);
        ((TextView) findViewById(R.id.tvNivelRutina)).setText(borrador.nivel != null
                && borrador.nivel.equals(nivelPerfil)
                ? getString(R.string.crear_nivel, nivel) : getString(R.string.crear_nivel_otro, nivel));
        findViewById(R.id.btnCambiarNivel).setContentDescription(getString(R.string.crear_cambiar_nivel_a11y));
        pintarPie();
    }

    // «4 ejercicios y unos 35 minutos.» o «Añade al menos un ejercicio.»
    private void pintarPie() {
        int n = borrador.ejercicios.size();
        tvPie.setText(n == 0 ? getString(R.string.crear_pie_vacio)
                : getResources().getQuantityString(R.plurals.crear_pie, n, n, borrador.minutosEstimados()));
        btnGuardar.setEnabled(cargada && n > 0);
    }

    private void elegirNivel() {
        String[] niveles = {"PRINCIPIANTE", "INTERMEDIO", "AVANZADO"};
        List<HojaElegir.Opcion> opciones = new ArrayList<>();
        int elegida = 0;
        for (int i = 0; i < niveles.length; i++) {
            opciones.add(HojaElegir.Opcion.de(UIHelper.traducirNivel(this, niveles[i])));
            if (niveles[i].equals(borrador.nivel)) elegida = i;
        }
        HojaElegir.mostrar(this, R.string.crear_nivel_titulo, opciones, elegida, 0, i -> {
            borrador.nivel = niveles[i];
            pintar();
        });
    }

    // ── La lista ────────────────────────────────────────────────────────

    private void montarLista() {
        RecyclerView rv = findViewById(R.id.rvTuRutina);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new Adaptador();
        rv.setAdapter(adaptador);
        // Recolocar y quitar con los tiempos de DEC-039 (momento 20); quietos sin animaciones.
        if (Movimiento.quieto(this)) {
            rv.setItemAnimator(null);
        } else {
            DefaultItemAnimator a = new DefaultItemAnimator();
            a.setMoveDuration(Movimiento.RECOLOCA);
            a.setRemoveDuration(Movimiento.SALE);
            a.setAddDuration(Movimiento.ENTRA);
            a.setChangeDuration(Movimiento.RECOLOCA);
            rv.setItemAnimator(a);
        }
        arrastre = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder de,
                                            @NonNull RecyclerView.ViewHolder a) {
                int desde = de.getBindingAdapterPosition(), hasta = a.getBindingAdapterPosition();
                borrador.mover(desde, hasta);
                adaptador.notifyItemMoved(desde, hasta);
                return true;
            }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder h, int d) { }
            @Override public boolean isLongPressDragEnabled() { return false; }
            @Override public void clearView(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder h) {
                super.clearView(r, h);
                // Al soltar, los nombres accesibles («2 de 4: …») vuelven a contar desde arriba.
                adaptador.notifyItemRangeChanged(0, adaptador.getItemCount());
            }
        });
        arrastre.attachToRecyclerView(rv);
    }

    private void subir(int i) {
        if (borrador.subir(i)) moverHecho(i, i - 1);
    }

    private void bajar(int i) {
        if (borrador.bajar(i)) moverHecho(i, i + 1);
    }

    private void moverHecho(int desde, int hasta) {
        adaptador.notifyItemMoved(desde, hasta);
        adaptador.notifyItemRangeChanged(Math.min(desde, hasta), 2);
        pintar();
    }

    private void quitar(int i) {
        borrador.quitar(i);
        adaptador.notifyItemRemoved(i);
        adaptador.notifyItemRangeChanged(i, adaptador.getItemCount() - i);
        pintar();
    }

    private void opciones(View ancla, int i) {
        BorradorRutina.Ejercicio e = borrador.ejercicios.get(i);
        List<UIHelper.MenuAction> acciones = new ArrayList<>();
        acciones.add(new UIHelper.MenuAction(R.drawable.ic_ms_edit, getString(R.string.crear_cambiar_pauta),
                () -> cambiarPauta(i)));
        if (i > 0) acciones.add(new UIHelper.MenuAction(R.drawable.ic_ms_keyboard_arrow_up,
                getString(R.string.crear_subir), () -> subir(i)));
        if (i < borrador.ejercicios.size() - 1) acciones.add(new UIHelper.MenuAction(R.drawable.ic_ms_keyboard_arrow_down,
                getString(R.string.crear_bajar), () -> bajar(i)));
        acciones.add(new UIHelper.MenuAction(R.drawable.ic_ms_delete, getString(R.string.crear_quitar), true,
                () -> quitar(i)));
        UIHelper.mostrarMenuAnclado(this, ancla, e.nombre, acciones);
    }

    // La pauta en una hoja: series, repeticiones o segundos (desde y hasta) y descanso, con − y +.
    private void cambiarPauta(int i) {
        BorradorRutina.Ejercicio e = borrador.ejercicios.get(i);
        BottomSheetDialog hoja = new BottomSheetDialog(this);
        View raiz = LayoutInflater.from(this).inflate(R.layout.dialog_pauta, null, false);
        hoja.setContentView(raiz);
        ((TextView) raiz.findViewById(R.id.tvPautaTitulo)).setText(getString(R.string.crear_pauta_titulo, e.nombre));
        int[] v = {e.series, e.min, e.max, e.descanso};
        boolean[] segundos = {e.segundos};
        Paso series = new Paso(raiz.findViewById(R.id.pasoSeries), R.string.crear_pauta_series, 1, 10, 1);
        Paso desde = new Paso(raiz.findViewById(R.id.pasoDesde), R.string.crear_pauta_desde, 1, 50, 1);
        Paso hasta = new Paso(raiz.findViewById(R.id.pasoHasta), R.string.crear_pauta_hasta, 1, 50, 1);
        Paso descanso = new Paso(raiz.findViewById(R.id.pasoDescanso), R.string.crear_pauta_descanso, 0, 300, 15);
        Runnable[] pintarHoja = new Runnable[1];
        pintarHoja[0] = () -> {
            // En segundos, de 5 en 5 y hasta 10 minutos; en repeticiones, de 1 en 1 y hasta 50.
            int paso = segundos[0] ? 5 : 1;
            int tope = segundos[0] ? 600 : 50;
            desde.limites(paso, tope, paso);
            hasta.limites(paso, tope, paso);
            series.poner(v[0]);
            desde.poner(v[1]);
            hasta.poner(v[2]);
            descanso.poner(v[3]);
        };
        series.alCambiar = x -> v[0] = x;
        desde.alCambiar = x -> { v[1] = x; if (v[2] < x) { v[2] = x; hasta.poner(x); } };
        hasta.alCambiar = x -> { v[2] = x; if (v[1] > x) { v[1] = x; desde.poner(x); } };
        descanso.alCambiar = x -> v[3] = x;
        SelectorPildora medida = raiz.findViewById(R.id.selectorMedida);
        medida.setOpciones(new CharSequence[]{getString(R.string.crear_pauta_repeticiones),
                getString(R.string.crear_pauta_segundos)});
        medida.setElegida(segundos[0] ? 1 : 0, false);
        medida.setAlElegir(x -> {
            boolean ahora = x == 1;
            if (ahora == segundos[0]) return;
            segundos[0] = ahora;
            // Al pasar de una medida a otra, un rango que tenga sentido en la nueva.
            v[1] = ahora ? 30 : 8;
            v[2] = ahora ? 45 : 12;
            pintarHoja[0].run();
        });
        pintarHoja[0].run();
        raiz.findViewById(R.id.btnAplicarPauta).setOnClickListener(b -> {
            e.series = v[0];
            e.min = v[1];
            e.max = v[2];
            e.descanso = v[3];
            e.segundos = segundos[0];
            adaptador.notifyItemChanged(i);
            pintar();
            hoja.dismiss();
        });
        hoja.show();
    }

    /** Una cifra con − y + en la hoja de la pauta (view_paso_valor). */
    private final class Paso {
        final TextView valor;
        final ImageButton menos, mas;
        final String rotulo;
        int min, max, paso, actual;
        java.util.function.IntConsumer alCambiar = x -> { };

        Paso(View fila, int rotuloRes, int min, int max, int paso) {
            rotulo = getString(rotuloRes);
            ((TextView) fila.findViewById(R.id.tvPasoRotulo)).setText(rotulo);
            valor = fila.findViewById(R.id.tvPasoValor);
            menos = fila.findViewById(R.id.btnPasoMenos);
            mas = fila.findViewById(R.id.btnPasoMas);
            menos.setContentDescription(getString(R.string.crear_pauta_menos_a11y, rotulo));
            mas.setContentDescription(getString(R.string.crear_pauta_mas_a11y, rotulo));
            limites(min, max, paso);
            menos.setOnClickListener(v -> cambiar(-this.paso));
            mas.setOnClickListener(v -> cambiar(this.paso));
        }

        void limites(int min, int max, int paso) {
            this.min = min;
            this.max = max;
            this.paso = paso;
        }

        void cambiar(int d) {
            int nuevo = Math.max(min, Math.min(max, actual + d));
            if (nuevo == actual) return;
            poner(nuevo);
            alCambiar.accept(nuevo);
            Movimiento.vibrar(valor, Movimiento.Vibracion.LIGERA);
        }

        void poner(int x) {
            actual = Math.max(min, Math.min(max, x));
            valor.setText(String.valueOf(actual));
            valor.setContentDescription(rotulo + ", " + actual);
            menos.setEnabled(actual > min);
            mas.setEnabled(actual < max);
            menos.setAlpha(actual > min ? 1f : 0.38f);
            mas.setAlpha(actual < max ? 1f : 0.38f);
        }
    }

    private final class Adaptador extends RecyclerView.Adapter<Adaptador.Fila> {

        final class Fila extends RecyclerView.ViewHolder {
            final TextView nombre, pauta;
            final View asa;
            final ImageButton mas;
            // Las acciones de TalkBack de esta fila, para quitarlas al volver a pintarla.
            final List<Integer> acciones = new ArrayList<>();

            Fila(View v) {
                super(v);
                nombre = v.findViewById(R.id.tvTuNombre);
                pauta = v.findViewById(R.id.tvTuPauta);
                asa = v.findViewById(R.id.ivAsa);
                mas = v.findViewById(R.id.btnTuOpciones);
            }
        }

        @NonNull
        @Override
        public Fila onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Fila(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tu_rutina_ejercicio, parent, false));
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(@NonNull Fila h, int position) {
            BorradorRutina.Ejercicio e = borrador.ejercicios.get(position);
            String pauta = TextosCrearRutina.pauta(TuRutinaActivity.this, e);
            h.nombre.setText(e.nombre);
            h.pauta.setText(pauta);
            int total = borrador.ejercicios.size();
            h.itemView.setContentDescription(getString(R.string.crear_fila_a11y, position + 1, total, e.nombre,
                    getString(R.string.crear_pauta_con_descanso, pauta, e.descanso)));
            h.mas.setContentDescription(getString(R.string.crear_opciones_a11y, e.nombre));
            h.mas.setOnClickListener(v -> {
                int p = h.getBindingAdapterPosition();
                if (p != RecyclerView.NO_POSITION) opciones(v, p);
            });
            // El asa arrastra en cuanto se toca, sin pulsación larga.
            h.asa.setOnTouchListener((v, ev) -> {
                if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) arrastre.startDrag(h);
                return false;
            });
            // Lo mismo sin arrastrar, para TalkBack y Switch Access: acciones de la fila.
            for (int id : h.acciones) ViewCompat.removeAccessibilityAction(h.itemView, id);
            h.acciones.clear();
            h.acciones.add(ViewCompat.addAccessibilityAction(h.itemView, getString(R.string.crear_cambiar_pauta),
                    (v, a) -> { cambiarPauta(h.getBindingAdapterPosition()); return true; }));
            if (position > 0) h.acciones.add(ViewCompat.addAccessibilityAction(h.itemView,
                    getString(R.string.crear_subir), (v, a) -> { subir(h.getBindingAdapterPosition()); return true; }));
            if (position < total - 1) h.acciones.add(ViewCompat.addAccessibilityAction(h.itemView,
                    getString(R.string.crear_bajar), (v, a) -> { bajar(h.getBindingAdapterPosition()); return true; }));
            h.acciones.add(ViewCompat.addAccessibilityAction(h.itemView, getString(R.string.crear_quitar),
                    (v, a) -> { quitar(h.getBindingAdapterPosition()); return true; }));
        }

        @Override
        public int getItemCount() {
            return borrador == null ? 0 : borrador.ejercicios.size();
        }
    }

    // ── Salir ───────────────────────────────────────────────────────────

    private boolean hayCambios() {
        return borrador != null && cargada && !borrador.aJson().equals(inicial);
    }

    // Al crear, vuelve al paso 2 con la lista tal como está: no se pierde nada.
    private void volverAlPaso2() {
        setResult(RESULT_CANCELED, new Intent().putExtra(AnadirEjerciciosActivity.EXTRA_BORRADOR, borrador.aJson()));
        finish();
    }

    // El aspa: al crear cierra el asistente entero (con algo elegido, avisa); al editar, sale.
    private void cerrar() {
        if (editar) {
            AvisoDescartar.salir(this, this::hayCambios);
            return;
        }
        // RESULT_FIRST_USER: el paso 2 y el paso 1 también se van. Si al final sigue editando,
        // volver al paso 2 o guardar ponen después su propio resultado.
        setResult(RESULT_FIRST_USER);
        AvisoDescartar.salir(this, () -> !borrador.ejercicios.isEmpty());
    }

    // ── Guardar ─────────────────────────────────────────────────────────

    private void guardar() {
        if (!borrador.sePuedeGuardar()) {
            if (borrador.nombre == null || borrador.nombre.trim().isEmpty()) {
                etNombre.setError(getString(R.string.error_campo_requerido));
                etNombre.requestFocus();
                Movimiento.temblar(etNombre);
            }
            return;
        }
        LoadingDialog.show(this);
        btnGuardar.setEnabled(false);
        guardarRutina();
    }

    private void fallo(int code, String message) {
        LoadingDialog.hide(this);
        btnGuardar.setEnabled(true);
        // Lo guardado hasta aquí se queda en el borrador con sus ids: «Guardar» sigue desde aquí.
        UiFeedback.toastError(this, code, message);
    }

    private Map<String, Object> datosRutina() {
        Map<String, Object> b = new HashMap<>();
        b.put("nombre", borrador.nombre.trim());
        b.put("descripcion", borrador.descripcion != null ? borrador.descripcion.trim() : "");
        b.put("nivel", borrador.nivel);
        // La que dice la pantalla: la fórmula de la API sobre lo que hay (GP-244, punto 9).
        b.put("duracionMinutos", borrador.minutosEstimados());
        return b;
    }

    // Paso 1: la rutina. Nueva, se crea; si ya existe (editar, o un guardado cortado), se cambia.
    private void guardarRutina() {
        Map<String, Object> b = datosRutina();
        if (borrador.rutinaId == null) {
            b.put("usuarioId", new PreferencesManager(this).getUsuarioId());
            b.put("esPredefinida", false);
            api.crear(b).enqueue(new ApiCallback<Rutina>() {
                @Override public void onOk(Rutina r) {
                    if (r == null) { fallo(0, getString(R.string.crear_error_guardar)); return; }
                    borrador.rutinaId = r.getId();
                    borrarQuitados(new ArrayList<>(originales.keySet()), 0);
                }
                @Override public void onFail(int code, String message) { fallo(code, message); }
            });
        } else {
            api.patch(borrador.rutinaId, b).enqueue(new ApiCallback<Void>() {
                @Override public void onOk(Void x) { borrarQuitados(new ArrayList<>(originales.keySet()), 0); }
                @Override public void onFail(int code, String message) { fallo(code, message); }
            });
        }
    }

    // Paso 2: las filas que tenía y ya no están.
    private void borrarQuitados(List<Integer> filas, int i) {
        if (i >= filas.size()) {
            guardarFila(0);
            return;
        }
        int fila = filas.get(i);
        boolean sigue = false;
        for (BorradorRutina.Ejercicio e : borrador.ejercicios) if (Integer.valueOf(fila).equals(e.filaId)) sigue = true;
        if (sigue) {
            borrarQuitados(filas, i + 1);
            return;
        }
        api.eliminarEjercicio(borrador.rutinaId, originales.get(fila)).enqueue(new ApiCallback<Void>() {
            @Override public void onOk(Void x) {
                originales.remove(fila);
                borrarQuitados(filas, i + 1);
            }
            @Override public void onFail(int code, String message) {
                // Ya no estaba (un reintento tras un corte): es lo que se quería.
                if (code == 404) onOk(null);
                else fallo(code, message);
            }
        });
    }

    // Paso 3: cada fila en su orden; las que ya existen se cambian y las nuevas se añaden.
    private void guardarFila(int i) {
        if (i >= borrador.ejercicios.size()) {
            terminar();
            return;
        }
        BorradorRutina.Ejercicio e = borrador.ejercicios.get(i);
        Map<String, Object> b = new HashMap<>();
        b.put("series", e.series);
        b.put("repeticionesMin", e.min);
        b.put("repeticionesMax", e.max);
        b.put("medida", e.segundos ? "SEGUNDOS" : "REPETICIONES");
        b.put("tiempoDescanso", e.descanso);
        b.put("orden", i + 1);
        if (e.filaId != null) {
            api.patchEjercicio(e.filaId, b).enqueue(new ApiCallback<Void>() {
                @Override public void onOk(Void x) { guardarFila(i + 1); }
                @Override public void onFail(int code, String message) { fallo(code, message); }
            });
            return;
        }
        b.put("rutinaId", borrador.rutinaId);
        b.put("ejercicioId", e.ejercicioId);
        api.anadirFila(b).enqueue(new ApiCallback<RutinaEjercicio>() {
            @Override public void onOk(RutinaEjercicio f) {
                if (f != null) {
                    e.filaId = f.getId();
                    originales.put(f.getId(), e.ejercicioId);
                }
                guardarFila(i + 1);
            }
            @Override public void onFail(int code, String message) { fallo(code, message); }
        });
    }

    private void terminar() {
        LoadingDialog.hide(this);
        if (editar) {
            UIHelper.mostrarToastExito(this, getString(R.string.editar_rutina_guardado));
        } else {
            NotificationHelper.notificarRutinaCreada(this, borrador.nombre.trim());
            UIHelper.mostrarToastExito(this, getString(R.string.rutinas_guardada_exito));
        }
        setResult(RESULT_OK);
        finish();
    }
}
