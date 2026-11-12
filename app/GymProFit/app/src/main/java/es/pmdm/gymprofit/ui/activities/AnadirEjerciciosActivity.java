package es.pmdm.gymprofit.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.PageDTO;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.rutina.BorradorRutina;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.EjercicioApi;
import es.pmdm.gymprofit.ui.rutina.TextosCrearRutina;
import es.pmdm.gymprofit.ui.widget.HojaElegir;
import es.pmdm.gymprofit.utils.AvisoDescartar;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.PaginacionScrollListener;
import es.pmdm.gymprofit.utils.Transiciones;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// AnadirEjerciciosActivity — «Elige ejercicios» (GP-244, paso 2 de crear una rutina, tablero
// Mezcla-2), y la biblioteca para añadir ejercicios a una sesión (GP-012).
//
// El buscador del catálogo (GET /ejercicios/buscar, en el servidor y por páginas) filtrado por
// las zonas del paso 1, una píldora por zona que se quita y «+ Zona» para añadir otra; sin
// zonas, busca entre todos. Cada fila dice el nombre, la zona y el equipo y lleva el botón de
// añadir, que pasa a check: entra con 3 series de 8 a 12 y 90 s de descanso, y se cambia en el
// paso 3. El pie dice cuántos llevas y, al crear, cuánto dura contra el tiempo elegido.
//
// Tres modos:
//   · crear: «Listo» abre el paso 3 (TuRutinaActivity) con lo elegido; si vuelve sin guardar,
//     trae su lista, y si guarda o se cierra, esta también se cierra;
//   · devolver: desde el paso 3 («Añadir ejercicios» al editar), «Listo» devuelve la lista;
//   · sesión (EXTRA_MODO_SESION): devuelve «ejerciciosJson» como siempre a Apuntar entreno y a
//     la sesión en vivo, sin zonas ni tiempo.
// ============================================================
public class AnadirEjerciciosActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    /** El borrador de la rutina, en JSON (BorradorRutina.aJson). */
    public static final String EXTRA_BORRADOR = "borrador";
    /** true: «Listo» devuelve el borrador en vez de abrir el paso 3. */
    public static final String EXTRA_DEVOLVER = "devolver";
    /**
     * Se añade a la sesión en vivo o a Apuntar entreno (GP-012): va con editMode y devuelve
     * «ejerciciosJson» (ejercicioId, nombre, series y repeticiones).
     */
    public static final String EXTRA_MODO_SESION = "modoSesion";

    private static final int TAM_PAGINA = 30;
    private static final long DEBOUNCE_MS = 400;

    /** Para el paso 2 del asistente, con lo elegido en el paso 1. */
    @NonNull
    public static Intent paraCrear(@NonNull Context ctx, @NonNull BorradorRutina b) {
        return new Intent(ctx, AnadirEjerciciosActivity.class).putExtra(EXTRA_BORRADOR, b.aJson());
    }

    /** Desde el paso 3: devuelve el borrador con lo que se añada o quite. */
    @NonNull
    public static Intent paraDevolver(@NonNull Context ctx, @NonNull BorradorRutina b) {
        return paraCrear(ctx, b).putExtra(EXTRA_DEVOLVER, true);
    }

    private final EjercicioApi api = ApiClient.service(EjercicioApi.class);
    private BorradorRutina borrador;
    private List<Integer> idsAlEntrar;
    private boolean modoSesion, devolver;

    private EditText etBuscar;
    private ChipGroup grupoZonas;
    private VistaEstado estado;
    private final List<Ejercicio> resultados = new ArrayList<>();
    private Adaptador adaptador;
    private TextView tvLlevas, tvTiempo;
    private LinearProgressIndicator barra;
    private MaterialButton btnListo;

    // La búsqueda por páginas.
    private String texto = "";
    private int pagina = 0;
    private boolean cargando, ultima;
    // Solo cuenta la respuesta de la última búsqueda: escribir rápido no mezcla páginas.
    private int pedida = 0;
    private final Handler debounce = new Handler(Looper.getMainLooper());
    private Runnable buscarLuego;

    private ActivityResultLauncher<Intent> paso3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anadir_ejercicios);
        modoSesion = getIntent().getBooleanExtra(EXTRA_MODO_SESION, false);
        devolver = getIntent().getBooleanExtra(EXTRA_DEVOLVER, false);
        String json = savedInstanceState != null ? savedInstanceState.getString(EXTRA_BORRADOR)
                : getIntent().getStringExtra(EXTRA_BORRADOR);
        borrador = BorradorRutina.deJson(json);
        idsAlEntrar = BorradorRutina.deJson(getIntent().getStringExtra(EXTRA_BORRADOR)).ids();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Salir con algo elegido pregunta antes de tirarlo (GP-098). Desde el paso 3, solo si ha
        // cambiado algo en esta visita: lo de antes sigue allí.
        // La pregunta dice qué se pierde: la rutina al crearla, los cambios al volver del paso 3, los
        // ejercicios elegidos en una sesión.
        int pregunta = modoSesion ? R.string.descartar_elegidos_titulo
                : devolver ? R.string.descartar_cambios_titulo : R.string.descartar_rutina_titulo;
        AvisoDescartar.instalar(this, toolbar, pregunta, () -> devolver
                ? !borrador.ids().equals(idsAlEntrar) : !borrador.ejercicios.isEmpty());

        etBuscar = findViewById(R.id.etBuscarEjercicio);
        grupoZonas = findViewById(R.id.grupoZonas);
        estado = new VistaEstado(findViewById(R.id.estadoElegir));
        tvLlevas = findViewById(R.id.tvLlevas);
        tvTiempo = findViewById(R.id.tvTiempoElegir);
        barra = findViewById(R.id.barraTiempoElegir);
        btnListo = findViewById(R.id.btnContinuar);

        RecyclerView rv = findViewById(R.id.rvEjerciciosBusqueda);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        rv.setLayoutManager(lm);
        adaptador = new Adaptador();
        rv.setAdapter(adaptador);
        rv.addOnScrollListener(new PaginacionScrollListener(lm) {
            @Override protected void cargarMas() { cargarPagina(pagina + 1); }
            @Override protected boolean isCargando() { return cargando; }
            @Override protected boolean esUltimaPagina() { return ultima; }
        });

        paso3 = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() == RESULT_OK) {
                setResult(RESULT_OK);
                finish();
            } else if (r.getResultCode() == Activity.RESULT_FIRST_USER) {
                setResult(Activity.RESULT_FIRST_USER);
                finish();
            } else if (r.getData() != null && r.getData().getStringExtra(EXTRA_BORRADOR) != null) {
                // Vuelve del paso 3 sin guardar: con su lista, que puede haber cambiado allí.
                borrador = BorradorRutina.deJson(r.getData().getStringExtra(EXTRA_BORRADOR));
                adaptador.notifyDataSetChanged();
                pintarPie();
            }
        });

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                texto = s.toString().trim();
                if (buscarLuego != null) debounce.removeCallbacks(buscarLuego);
                buscarLuego = AnadirEjerciciosActivity.this::buscar;
                debounce.postDelayed(buscarLuego, DEBOUNCE_MS);
            }
        });
        btnListo.setOnClickListener(v -> listo());

        pintarZonas();
        pintarPie();
        buscar();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putString(EXTRA_BORRADOR, borrador.aJson());
    }

    @Override
    protected void onDestroy() {
        if (buscarLuego != null) debounce.removeCallbacks(buscarLuego);
        super.onDestroy();
    }

    // ── Las zonas ───────────────────────────────────────────────────────

    // Una píldora por zona, que se quita con su aspa, y «+ Zona» con las que faltan.
    private void pintarZonas() {
        grupoZonas.removeAllViews();
        if (modoSesion) {
            grupoZonas.setVisibility(View.GONE);
            return;
        }
        java.util.Locale idioma = es.pmdm.gymprofit.utils.FechaUtils.localeDeLaApp(this);
        for (String g : BorradorRutina.ZONAS) {
            if (!borrador.zonas.contains(g)) continue;
            String nombre = getString(TextosCrearRutina.zona(g));
            MaterialButton b = pildora(nombre, R.drawable.ic_ms_close, MaterialButton.ICON_GRAVITY_END,
                    R.color.gp_primary_container, R.color.gp_on_primary_container, false);
            b.setContentDescription(getString(R.string.elegir_ej_quitar_zona_a11y, nombre.toLowerCase(idioma)));
            b.setOnClickListener(v -> {
                borrador.zonas.remove(g);
                pintarZonas();
                buscar();
            });
            grupoZonas.addView(b);
        }
        if (borrador.zonas.size() < BorradorRutina.ZONAS.size()) {
            MaterialButton mas = pildora(getString(R.string.elegir_ej_mas_zona), R.drawable.ic_ms_add,
                    MaterialButton.ICON_GRAVITY_TEXT_START, android.R.color.transparent, R.color.gp_on_surface, true);
            mas.setContentDescription(getString(R.string.elegir_ej_mas_zona_a11y));
            mas.setOnClickListener(v -> elegirZona());
            grupoZonas.addView(mas);
        }
        grupoZonas.setVisibility(View.VISIBLE);
    }

    /**
     * Una píldora de acción (quitar una zona, añadir otra), no de filtro: un botón de Material y no
     * un Chip, que es un CompoundButton y le decía a TalkBack «no seleccionado» aunque no se pudiera
     * marcar, y que pinta su texto desde su drawable, sin dejar cambiar la fuente. 44 dp a la vista
     * y 48 de zona táctil (2 dp de margen por dentro arriba y abajo), Barlow 600 a 14 sp.
     */
    private MaterialButton pildora(String texto, int icono, int lado, int fondo, int color, boolean borde) {
        MaterialButton b = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle);
        b.setText(texto);
        b.setAllCaps(false);
        android.graphics.Typeface barlow = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.barlow_semibold);
        if (barlow != null) b.setTypeface(barlow);
        b.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_caption));
        b.setTextColor(ContextCompat.getColor(this, color));
        b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, fondo)));
        b.setIconResource(icono);
        b.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, color)));
        b.setIconGravity(lado);
        int d4 = getResources().getDimensionPixelSize(R.dimen.espacio_4);
        b.setIconPadding(d4);
        b.setIconSize(Math.round(18 * getResources().getDisplayMetrics().density));
        int alto = getResources().getDimensionPixelSize(R.dimen.chip_zona_alto);
        int tactil = getResources().getDimensionPixelSize(R.dimen.zona_tactil);
        int margen = (tactil - alto) / 2;
        b.setInsetTop(margen);
        b.setInsetBottom(margen);
        b.setMinHeight(tactil);
        b.setMinimumHeight(tactil);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        int lados = getResources().getDimensionPixelSize(R.dimen.espacio_16);
        int corto = getResources().getDimensionPixelSize(R.dimen.espacio_12);
        if (lado == MaterialButton.ICON_GRAVITY_END) b.setPaddingRelative(lados, 0, corto, 0);
        else b.setPaddingRelative(corto, 0, lados, 0);
        b.setCornerRadius(alto / 2);
        b.setStrokeWidth(borde ? getResources().getDimensionPixelSize(R.dimen.separador_alto) : 0);
        b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gp_stroke)));
        b.setElevation(0f);
        b.setStateListAnimator(android.animation.AnimatorInflater.loadStateListAnimator(this, R.animator.toque));
        return b;
    }

    private void elegirZona() {
        List<String> faltan = new ArrayList<>();
        List<HojaElegir.Opcion> opciones = new ArrayList<>();
        for (String g : BorradorRutina.ZONAS) {
            if (borrador.zonas.contains(g)) continue;
            faltan.add(g);
            opciones.add(HojaElegir.Opcion.de(getString(TextosCrearRutina.zona(g))));
        }
        HojaElegir.mostrar(this, R.string.elegir_ej_mas_zona_a11y, opciones, -1, 0, i -> {
            borrador.zonas.add(faltan.get(i));
            pintarZonas();
            buscar();
        });
    }

    // ── La búsqueda ─────────────────────────────────────────────────────

    private void buscar() {
        pagina = 0;
        ultima = false;
        cargarPagina(0);
    }

    private void cargarPagina(int p) {
        cargando = true;
        int esta = ++pedida;
        if (p == 0) {
            resultados.clear();
            adaptador.notifyDataSetChanged();
            estado.cargando();
        }
        List<String> zonas = modoSesion || borrador.zonas.isEmpty() ? null : new ArrayList<>(borrador.zonas);
        api.buscarEnZonas(texto.isEmpty() ? null : texto, zonas, p, TAM_PAGINA)
                .enqueue(new ApiCallback<PageDTO<Ejercicio>>() {
                    @Override public void onOk(PageDTO<Ejercicio> r) {
                        if (esta != pedida || isFinishing()) return;
                        cargando = false;
                        List<Ejercicio> l = r != null && r.getContent() != null ? r.getContent() : new ArrayList<>();
                        pagina = r != null ? r.getPage() : p;
                        ultima = r == null || r.isLast();
                        int desde = resultados.size();
                        resultados.addAll(l);
                        if (p == 0) adaptador.notifyDataSetChanged();
                        else adaptador.notifyItemRangeInserted(desde, l.size());
                        if (resultados.isEmpty()) estado.vacio(R.drawable.ic_ms_search, R.string.elegir_ej_vacio);
                        else estado.oculto();
                    }
                    @Override public void onFail(int code, String message) {
                        if (esta != pedida || isFinishing()) return;
                        cargando = false;
                        // Una página siguiente que falla se vuelve a pedir al desplazar; la primera se
                        // dice en la pantalla, con «Reintentar».
                        if (p == 0) estado.error(VistaEstado.mensaje(AnadirEjerciciosActivity.this,
                                R.string.error_conexion, code, message), AnadirEjerciciosActivity.this::buscar);
                    }
                });
    }

    // ── Elegir ──────────────────────────────────────────────────────────

    private void alternar(Ejercicio e, int posicion, View fila) {
        boolean anadido;
        if (borrador.contiene(e.getId())) {
            borrador.quitarId(e.getId());
            anadido = false;
        } else {
            borrador.anadir(e.getId(), e.getNombre(), e.getGrupoMuscular(), e.getEquipoNecesario(), e.esPorTiempo());
            anadido = true;
        }
        Movimiento.vibrar(fila, Movimiento.Vibracion.LIGERA);
        adaptador.notifyItemChanged(posicion, anadido ? "anadido" : "quitado");
        pintarPie();
    }

    // «4 elegidos» y, al crear, «Unos 35 de tus 45 min» con su barra.
    private void pintarPie() {
        int n = borrador.ejercicios.size();
        tvLlevas.setText(getResources().getQuantityString(R.plurals.elegir_ej_llevas, n, n));
        boolean conTiempo = !modoSesion && borrador.minutos != null && n > 0;
        tvTiempo.setVisibility(conTiempo ? View.VISIBLE : View.GONE);
        barra.setVisibility(!modoSesion && borrador.minutos != null ? View.VISIBLE : View.GONE);
        String frase = tvLlevas.getText().toString();
        if (conTiempo) {
            int m = borrador.minutosEstimados();
            tvTiempo.setText(borrador.minutos >= BorradorRutina.MINUTOS_SIN_TECHO
                    ? getString(R.string.elegir_ej_pie_tiempo_75, m)
                    : getString(R.string.elegir_ej_pie_tiempo, m, borrador.minutos));
            frase += ". " + tvTiempo.getText();
        }
        // Sin ejercicios la barra va vacía: los 8 minutos de calentamiento no son nada elegido.
        barra.setProgressCompat(n > 0 ? Math.round(borrador.partesDelTiempo() * 100) : 0, !Movimiento.quieto(this));
        findViewById(R.id.filaPieElegir).setContentDescription(frase);
        // Al crear, sin ejercicios no hay nada que llevar al paso 3.
        btnListo.setEnabled(devolver || modoSesion || n > 0);
    }

    private void listo() {
        if (modoSesion) {
            Intent r = new Intent();
            r.putExtra("ejerciciosJson", paraLaSesion());
            setResult(RESULT_OK, r);
            finish();
            return;
        }
        if (devolver) {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_BORRADOR, borrador.aJson()));
            finish();
            return;
        }
        Transiciones.avanzar(this, TuRutinaActivity.paraCrear(this, borrador), paso3);
    }

    // Lo que esperan Apuntar entreno y la sesión en vivo: entran con 3 series de 10.
    private String paraLaSesion() {
        JSONArray arr = new JSONArray();
        try {
            for (BorradorRutina.Ejercicio e : borrador.ejercicios) {
                JSONObject o = new JSONObject();
                o.put("ejercicioId", e.ejercicioId);
                o.put("nombre", e.nombre);
                // La medida del catálogo (GP-130): en una sesión libre, la plancha pide segundos.
                o.put("medida", e.segundos ? "SEGUNDOS" : "REPETICIONES");
                o.put("series", 3);
                o.put("repeticiones", 10);
                arr.put(o);
            }
        } catch (JSONException e) {
            // JSONObject.put solo falla con un número no finito o una clave nula: aquí no hay ninguno.
            throw new IllegalStateException(e);
        }
        return arr.toString();
    }

    // ── La lista ────────────────────────────────────────────────────────

    private final class Adaptador extends RecyclerView.Adapter<Adaptador.Fila> {

        final class Fila extends RecyclerView.ViewHolder {
            final TextView nombre, detalle;
            final ImageView boton;

            Fila(View v) {
                super(v);
                nombre = v.findViewById(R.id.tvElegirNombre);
                detalle = v.findViewById(R.id.tvElegirDetalle);
                boton = v.findViewById(R.id.ivElegirBoton);
            }
        }

        @NonNull
        @Override
        public Fila onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Fila(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_elegir_ejercicio, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Fila h, int position, @NonNull List<Object> payloads) {
            onBindViewHolder(h, position);
            // Momento 15: el «+» pasa a check con un salto; quieto con «Quitar animaciones».
            if (payloads.contains("anadido") && !Movimiento.quieto(AnadirEjerciciosActivity.this)) {
                Movimiento.saltar(h.boton, Movimiento.CHECK);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull Fila h, int position) {
            Ejercicio e = resultados.get(position);
            boolean elegido = borrador.contiene(e.getId());
            h.nombre.setText(e.getNombre());
            h.detalle.setText(TextosCrearRutina.detalle(AnadirEjerciciosActivity.this, e.getGrupoMuscular(),
                    e.getEquipoNecesario()));
            h.itemView.setSelected(elegido);
            h.boton.setImageResource(elegido ? R.drawable.ic_ms_check : R.drawable.ic_ms_add);
            h.boton.setBackgroundResource(elegido ? R.drawable.bg_boton_elegir_si : R.drawable.bg_boton_elegir_no);
            h.boton.setImageTintList(ContextCompat.getColorStateList(AnadirEjerciciosActivity.this,
                    elegido ? R.color.gp_on_primary : R.color.gp_on_surface));
            h.itemView.setContentDescription(e.getNombre() + ", " + h.detalle.getText());
            ViewCompat.setAccessibilityDelegate(h.itemView, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName(android.widget.Switch.class.getName());
                    info.setCheckable(true);
                    info.setChecked(borrador.contiene(e.getId()));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            AccessibilityNodeInfoCompat.ACTION_CLICK, getString(borrador.contiene(e.getId())
                            ? R.string.elegir_ej_quitar_a11y : R.string.elegir_ej_anadir_a11y, e.getNombre())));
                }
            });
            h.itemView.setOnClickListener(v -> {
                int p = h.getBindingAdapterPosition();
                if (p != RecyclerView.NO_POSITION) alternar(resultados.get(p), p, v);
            });
        }

        @Override
        public int getItemCount() {
            return resultados.size();
        }
    }
}
