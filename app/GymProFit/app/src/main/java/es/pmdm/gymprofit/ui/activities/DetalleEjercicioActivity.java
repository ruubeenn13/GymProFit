package es.pmdm.gymprofit.ui.activities;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.EjercicioRealizado;
import es.pmdm.gymprofit.model.record.PuntoProgresion;
import es.pmdm.gymprofit.model.record.Record;
import es.pmdm.gymprofit.model.record.Records;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.RecordApi;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.utils.ChartMarker;
import es.pmdm.gymprofit.utils.ChartStyler;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.Marcas;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.PasosEjercicio;
import es.pmdm.gymprofit.utils.PeriodoProgreso;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.ProximaVez;
import es.pmdm.gymprofit.utils.TiempoRelativo;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UltimasVeces;

// ============================================================
// DetalleEjercicioActivity — la ficha de un ejercicio (GP-216, tablero 30).
//
// Arriba, la demostración: los dos fotogramas del catálogo (imagen_url e imagen_url_2) se
// funden en bucle; con «Quitar animaciones» del sistema se queda el primero. Sin foto, la
// silueta de la zona. Debajo, el nombre con sus datos, «Tu mejor marca» en dorado (de /records,
// con el máximo estimado) y dos pestañas:
//   · Tu progreso: la gráfica del peso máximo por sesión con su periodo y «Tus últimas veces»,
//     una tarjeta por sesión con las series en barras. La pista «La próxima vez…» sale solo si
//     la ficha se abrió desde una rutina de programa (que trae el máximo de repeticiones) y la
//     última vez llegó a él en todas las series: es la regla de los programas.
//   · Cómo se hace: los pasos numerados.
// Todo sale de la API actual: /records, /records/ejercicio/{id}/progresion,
// /ejercicios-realizados/ejercicio/{id} y la lista de sesiones para las fechas.
// ============================================================
public class DetalleEjercicioActivity extends AppCompatActivity {

    /** Máximo de repeticiones de la pauta, si la ficha se abre desde una rutina de programa. */
    public static final String EXTRA_REPS_MAX = "repsMax";

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    // Milisegundos entre fotogramas de la demostración y lo que dura el fundido
    private static final long FRAME_MS = 900;
    private static final long FUNDIDO_MS = 250;
    // Cuántas sesiones enseña «Tus últimas veces»
    private static final int ULTIMAS = 5;

    private final Handler frameHandler = new Handler(Looper.getMainLooper());
    private Runnable frameRunnable;

    private final RecordApi recordApi = ApiClient.service(RecordApi.class);
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);

    private int ejercicioId;
    @Nullable private Integer repsMax;
    private boolean yaResuelto;

    // Lo cargado, para repintar la gráfica al cambiar de periodo sin volver a pedirlo.
    @Nullable private List<PuntoProgresion> progresion;
    private int periodoSemanas = 8;

    private View panelProgreso, panelComo, btnTabProgreso, btnTabComo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new PreferencesManager(this).applyTheme();
        setContentView(R.layout.activity_detalle_ejercicio);

        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        es.pmdm.gymprofit.ui.widget.BarraSuperior.enlazar(findViewById(R.id.scrollDetalle), findViewById(R.id.barraSuperior));

        String nombre        = getIntent().getStringExtra("nombre");
        String descripcion   = getIntent().getStringExtra("descripcion");
        String instrucciones = getIntent().getStringExtra("instrucciones");
        String grupoMuscular = getIntent().getStringExtra("grupoMuscular");
        String musculoPrim   = getIntent().getStringExtra("musculoPrimario");
        String dificultad    = getIntent().getStringExtra("dificultad");
        String equipo        = getIntent().getStringExtra("equipoNecesario");
        String imagenUrl     = getIntent().getStringExtra("imagenUrl");
        String imagenUrl2    = getIntent().getStringExtra("imagenUrl2");
        ejercicioId = getIntent().getIntExtra("id", -1);
        int max = getIntent().getIntExtra(EXTRA_REPS_MAX, -1);
        repsMax = max > 0 ? max : null;

        poblarCabecera(nombre, grupoMuscular, musculoPrim, dificultad, equipo);
        configurarDemostracion(imagenUrl, imagenUrl2, musculoPrim, grupoMuscular);
        pintarPasos(descripcion, instrucciones);
        configurarPestanas();
        configurarPeriodos();

        if (ejercicioId > 0) cargarDatos();
        else mostrarSinDatos();
    }

    // ── Cabecera ────────────────────────────────────────────────────────────

    // Nombre y datos del ejercicio: el grupo en el contenedor naranja, los demás en neutro.
    private void poblarCabecera(String nombre, String grupoMuscular, String musculoPrimario,
                                String dificultad, String equipo) {
        ((TextView) findViewById(R.id.tvNombreDetalle)).setText(nombre != null ? nombre : "");
        ChipGroup chips = findViewById(R.id.chipsDetalle);
        String grupo = !isEmpty(grupoMuscular) ? UIHelper.traducirGrupoMuscular(this, grupoMuscular) : null;
        if (grupo != null) chips.addView(dato(grupo, true));
        if (!isEmpty(musculoPrimario) && !musculoPrimario.equalsIgnoreCase(grupo)) {
            chips.addView(dato(musculoPrimario, false));
        }
        if (!isEmpty(equipo)) chips.addView(dato(equipo.trim(), false));
        if (!isEmpty(dificultad)) chips.addView(dato(UIHelper.traducirNivel(this, dificultad), false));
    }

    // Una píldora de dato (solo texto; no se pulsa).
    private TextView dato(String texto, boolean destacado) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(13);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        float dp = getResources().getDisplayMetrics().density;
        int h = Math.round(10 * dp), v = Math.round(6 * dp);
        tv.setPadding(h, v, h, v);
        tv.setBackgroundResource(destacado ? R.drawable.bg_chip_dato_destacado : R.drawable.bg_chip_dato);
        tv.setTextColor(color(destacado ? com.google.android.material.R.attr.colorOnPrimaryContainer
                : com.google.android.material.R.attr.colorOnSurface));
        return tv;
    }

    // ── Demostración ────────────────────────────────────────────────────────

    // Con 2 fotogramas se funden en bucle; con 1, imagen estática; sin foto, la silueta de la
    // zona (el placeholder ya está debajo). Con las animaciones quitadas, solo el primero.
    private void configurarDemostracion(String url1, String url2, String musculoPrimario, String grupo) {
        ((ImageView) findViewById(R.id.ivSilueta))
                .setImageResource(UIHelper.cuerpoMuscular(musculoPrimario, grupo));
        if (isEmpty(url1)) return;

        ImageView uno = findViewById(R.id.ivImagenDetalle);
        uno.setVisibility(View.VISIBLE);
        Glide.with(this).load(url1).into(uno);

        if (isEmpty(url2) || Movimiento.quieto(this)) return;
        ImageView dos = findViewById(R.id.ivImagenDetalle2);
        dos.setVisibility(View.VISIBLE);
        Glide.with(this).load(url2).into(dos);
        frameRunnable = new Runnable() {
            private boolean mostrarSegundo = true;

            @Override
            public void run() {
                dos.animate().alpha(mostrarSegundo ? 1f : 0f).setDuration(FUNDIDO_MS).start();
                mostrarSegundo = !mostrarSegundo;
                frameHandler.postDelayed(this, FRAME_MS);
            }
        };
        frameHandler.postDelayed(frameRunnable, FRAME_MS);
    }

    // Detiene la animación de fotogramas al salir de la pantalla.
    @Override
    protected void onDestroy() {
        if (frameRunnable != null) frameHandler.removeCallbacks(frameRunnable);
        super.onDestroy();
    }

    // ── Pestañas ────────────────────────────────────────────────────────────

    private void configurarPestanas() {
        panelProgreso = findViewById(R.id.panelProgreso);
        panelComo = findViewById(R.id.panelComo);
        btnTabProgreso = findViewById(R.id.btnTabProgreso);
        btnTabComo = findViewById(R.id.btnTabComo);
        btnTabProgreso.setOnClickListener(v -> elegirPestana(true));
        btnTabComo.setOnClickListener(v -> elegirPestana(false));
        // Cada pestaña dice que lo es y si está elegida (el estado lo lee del propio selected).
        rol(btnTabProgreso, getString(R.string.nav_rol_pestana), null);
        rol(btnTabComo, getString(R.string.nav_rol_pestana), null);
        elegirPestana(true);
    }

    private void elegirPestana(boolean progreso) {
        btnTabProgreso.setSelected(progreso);
        btnTabComo.setSelected(!progreso);
        panelProgreso.setVisibility(progreso ? View.VISIBLE : View.GONE);
        panelComo.setVisibility(progreso ? View.GONE : View.VISIBLE);
    }

    // ── Cómo se hace ────────────────────────────────────────────────────────

    // Descripción arriba y las instrucciones en pasos numerados; sin ninguno de los dos, un aviso.
    private void pintarPasos(String descripcion, String instrucciones) {
        TextView tvDesc = findViewById(R.id.tvDescripcion);
        if (!isEmpty(descripcion)) {
            tvDesc.setText(descripcion.trim());
            tvDesc.setVisibility(View.VISIBLE);
        }
        LinearLayout lista = findViewById(R.id.listaPasos);
        List<String> pasos = PasosEjercicio.de(instrucciones);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < pasos.size(); i++) {
            View paso = inflater.inflate(R.layout.item_paso_ejercicio, lista, false);
            ((TextView) paso.findViewById(R.id.tvPasoNumero)).setText(String.valueOf(i + 1));
            ((TextView) paso.findViewById(R.id.tvPasoTexto)).setText(pasos.get(i));
            if (i == pasos.size() - 1) {
                ((ViewGroup.MarginLayoutParams) paso.getLayoutParams()).bottomMargin = 0;
            }
            lista.addView(paso);
        }
        boolean hay = !pasos.isEmpty();
        findViewById(R.id.cardPasos).setVisibility(hay ? View.VISIBLE : View.GONE);
        findViewById(R.id.tvSinPasos).setVisibility(hay || !isEmpty(descripcion) ? View.GONE : View.VISIBLE);
    }

    // ── Datos del usuario ───────────────────────────────────────────────────

    // Pide lo que depende de la cuenta: los récords, la progresión y las ejecuciones con las
    // fechas de las sesiones. Cada pieza se pinta cuando llega la suya; si una falla, la ficha
    // se lee igual sin ella y se deja rastro en el log (un toast encima estorbaría más).
    private void cargarDatos() {
        // Récords: «Tu mejor marca».
        recordApi.getRecords(null).enqueue(new ApiCallback<Records>() {
            @Override public void onOk(Records r) {
                if (isFinishing() || r == null) return;
                for (Record x : r.getRecords()) {
                    if (x.getEjercicioId() == ejercicioId) { recordVigente = x; break; }
                }
                pintarMarca(recordVigente);
                unaMas();
            }
            @Override public void onFail(int code, String message) {
                Log.w("GymProFit", "ficha: records falló (" + code + "): " + message);
                unaMas();
            }
        });
        // Progresión: la gráfica.
        recordApi.getProgresion(ejercicioId).enqueue(new ApiCallback<List<PuntoProgresion>>() {
            @Override public void onOk(List<PuntoProgresion> lista) {
                if (isFinishing()) return;
                progresion = lista != null ? lista : new ArrayList<>();
                pintarGrafica();
                unaMas();
            }
            @Override public void onFail(int code, String message) {
                // «Sin progreso» es 200 con []; lo que cae aquí es un fallo de verdad.
                Log.w("GymProFit", "ficha: progresión falló (" + code + "): " + message);
                unaMas();
            }
        });
        // Ejecuciones y fechas: «Tus últimas veces».
        sesionApi.ejecucionesDe(ejercicioId).enqueue(new ApiCallback<List<EjercicioRealizado>>() {
            @Override public void onOk(List<EjercicioRealizado> lista) {
                ejecuciones = lista != null ? lista : new ArrayList<>();
                pintarUltimasVeces();
                unaMas();
            }
            @Override public void onFail(int code, String message) {
                Log.w("GymProFit", "ficha: ejecuciones falló (" + code + "): " + message);
                unaMas();
            }
        });
        int uid = new PreferencesManager(this).getUsuarioId();
        if (uid == -1) {
            fechasSesion = new HashMap<>();
            pintarUltimasVeces();
            unaMas();
            return;
        }
        sesionApi.getDeUsuario(uid).enqueue(new ApiCallback<List<SesionEntrenamiento>>() {
            @Override public void onOk(List<SesionEntrenamiento> lista) {
                Map<Integer, String> m = new HashMap<>();
                if (lista != null) for (SesionEntrenamiento s : lista) m.put(s.getId(), s.getFechaInicio());
                fechasSesion = m;
                pintarUltimasVeces();
                unaMas();
            }
            @Override public void onFail(int code, String message) {
                Log.w("GymProFit", "ficha: sesiones falló (" + code + "): " + message);
                fechasSesion = new HashMap<>();
                pintarUltimasVeces();
                unaMas();
            }
        });
    }

    @Nullable private Record recordVigente;
    @Nullable private List<EjercicioRealizado> ejecuciones;
    @Nullable private Map<Integer, String> fechasSesion;
    private final AtomicInteger respondidas = new AtomicInteger();

    // Cuando han vuelto las cuatro, si no hay nada de nada se dice en vez de dejar el hueco.
    private void unaMas() {
        if (respondidas.incrementAndGet() < 4 || yaResuelto) return;
        yaResuelto = true;
        boolean sinProgresion = progresion == null || progresion.isEmpty();
        boolean sinEjecuciones = ejecuciones == null || ejecuciones.isEmpty();
        if (sinProgresion && sinEjecuciones && recordVigente == null) mostrarSinDatos();
    }

    private void mostrarSinDatos() {
        findViewById(R.id.tvSinDatos).setVisibility(View.VISIBLE);
    }

    // ── Tu mejor marca ──────────────────────────────────────────────────────

    private void pintarMarca(@Nullable Record r) {
        if (r == null) return;
        findViewById(R.id.cardMarca).setVisibility(View.VISIBLE);
        String valor = Marcas.texto(this, r);
        String cuando = TiempoRelativo.texto(this, r.getFecha());
        ((TextView) findViewById(R.id.tvMarcaFecha)).setText(cuando != null
                ? getString(R.string.detalle_ejercicio_mejor_marca, cuando)
                : getString(R.string.detalle_ejercicio_mejor_marca_sola));
        ((TextView) findViewById(R.id.tvMarcaValor)).setText(valor);
        Double rm = r.getUnoRmEstimado();
        if (r.esDePeso() && rm != null && rm > 0) {
            findViewById(R.id.bloqueMaximo).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvMaximoValor)).setText(Marcas.kilos(this, Math.round(rm * 2) / 2.0));
        }
        findViewById(R.id.cardMarca).setContentDescription(getString(R.string.detalle_ejercicio_marca_a11y,
                valor, cuando != null ? cuando : ""));
    }

    // ── Gráfica ─────────────────────────────────────────────────────────────

    private void configurarPeriodos() {
        View[] botones = {findViewById(R.id.btnPeriodo4), findViewById(R.id.btnPeriodo8), findViewById(R.id.btnPeriodoTodo)};
        int[] semanas = {4, 8, 0};
        for (int i = 0; i < botones.length; i++) {
            final int s = semanas[i];
            rol(botones[i], null, android.widget.RadioButton.class.getName());
            botones[i].setOnClickListener(v -> {
                periodoSemanas = s;
                pintarGrafica();
            });
        }
    }

    // El rol y el estado de un botón que no es de Material: pestaña (roleDescription) o una opción
    // de un grupo (RadioButton); los dos dicen si están elegidos.
    private static void rol(View v, @Nullable String rol, @Nullable String clase) {
        androidx.core.view.ViewCompat.setAccessibilityDelegate(v, new androidx.core.view.AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host,
                                                          androidx.core.view.accessibility.AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                if (rol != null) info.setRoleDescription(rol);
                if (clase != null) {
                    info.setClassName(clase);
                    info.setCheckable(true);
                    info.setChecked(host.isSelected());
                } else {
                    info.setSelected(host.isSelected());
                }
            }
        });
    }

    private void marcarPeriodo() {
        findViewById(R.id.btnPeriodo4).setSelected(periodoSemanas == 4);
        findViewById(R.id.btnPeriodo8).setSelected(periodoSemanas == 8);
        findViewById(R.id.btnPeriodoTodo).setSelected(periodoSemanas == 0);
    }

    /**
     * Dibuja la mejor serie de cada sesión dentro del periodo elegido; resalta el récord y da
     * tooltip al tocar. Si el ejercicio se ha hecho alguna vez con peso se dibujan los kilos; si
     * nunca, las repeticiones o los segundos: con peso, sin peso y por tiempo son marcas distintas.
     */
    private void pintarGrafica() {
        View card = findViewById(R.id.cardProgresion);
        LineChart chart = findViewById(R.id.chartProgresion);
        if (progresion == null) return;

        boolean hayPeso = false, hayTiempo = false;
        for (PuntoProgresion p : progresion) {
            if (p.esDePeso() && p.getPeso() > 0) hayPeso = true;
            if (p.esDeTiempo() && p.getSegundos() > 0) hayTiempo = true;
        }
        final boolean enKilos = hayPeso;
        final boolean enSegundos = !hayPeso && hayTiempo;

        // La API los entrega en orden cronológico, que es como se dibujan.
        List<String> fechas = new ArrayList<>();
        List<Float> valores = new ArrayList<>();
        for (PuntoProgresion p : progresion) {
            if (enKilos != p.esDePeso() || enSegundos != p.esDeTiempo()) continue;
            float y = enKilos ? (float) p.getPeso() : enSegundos ? p.getSegundos() : p.getRepeticiones();
            if (y <= 0) continue;
            fechas.add(p.getFecha());
            valores.add(y);
        }
        if (valores.size() < 2) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        marcarPeriodo();
        ((TextView) findViewById(R.id.tvProgresionTitulo)).setText(enKilos
                ? R.string.detalle_progresion_titulo
                : enSegundos ? R.string.detalle_progresion_titulo_tiempo : R.string.detalle_progresion_titulo_reps);

        // El récord es el máximo de todo lo hecho, no solo de lo que se ve.
        float max = 0f;
        for (float v : valores) max = Math.max(max, v);
        final float pr = max;

        int desde = PeriodoProgreso.primerIndice(fechas, TiempoRelativo.hoy(), periodoSemanas);
        final List<String> etiquetas = new ArrayList<>();
        List<Entry> entradas = new ArrayList<>();
        for (int i = desde; i < valores.size(); i++) {
            entradas.add(new Entry(entradas.size(), valores.get(i)));
            etiquetas.add(fechaCorta(fechas.get(i)));
        }
        if (entradas.isEmpty()) {
            chart.clear();
            chart.invalidate();
            pintarDelta(null);
            return;
        }

        ChartStyler.styleLine(chart, new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int idx = Math.round(value);
                return (idx >= 0 && idx < etiquetas.size()) ? etiquetas.get(idx) : "";
            }
        });
        // Tooltip: marca + fecha, con «· PR» en el récord.
        chart.setMarker(new ChartMarker(this, (e, h) -> {
            int idx = Math.round(e.getX());
            String fecha = (idx >= 0 && idx < etiquetas.size()) ? etiquetas.get(idx) : "";
            String sufijo = e.getY() >= pr ? " · " + getString(R.string.detalle_progresion_pr) : "";
            if (enKilos) return getString(R.string.grafica_kg_fecha, e.getY(), sufijo, fecha);
            if (enSegundos) {
                return getString(R.string.grafica_tiempo_fecha, Marcas.tiempo(Math.round(e.getY())), sufijo, fecha);
            }
            int reps = Math.round(e.getY());
            return getString(R.string.grafica_reps_fecha,
                    getResources().getQuantityString(R.plurals.record_repeticiones, reps, reps), sufijo, fecha);
        }));

        LineDataSet ds = new LineDataSet(entradas, "");
        ChartStyler.styleLineDataSet(ds, this);
        // El/los punto(s) de récord en dorado (DEC-018: lo conseguido); el resto, el naranja de marca.
        int marca = ds.getColor();
        int oro = ContextCompat.getColor(this, R.color.gp_gold);
        List<Integer> circulos = new ArrayList<>();
        for (Entry e : entradas) circulos.add(e.getY() >= pr ? oro : marca);
        ds.setCircleColors(circulos);
        ds.setCircleRadius(4.5f);
        // Con muchas sesiones los círculos tapan la línea: por encima de 30 solo la línea.
        ds.setDrawCircles(entradas.size() <= 30);
        chart.setData(new LineData(ds));
        chart.invalidate();

        // Lo que se ha subido en el periodo (solo en kilos): verde si sube, neutro si no (DEC-055).
        pintarDelta(enKilos && entradas.size() >= 2
                ? (double) entradas.get(entradas.size() - 1).getY() - entradas.get(0).getY() : null);
        chart.setContentDescription(getString(R.string.detalle_progresion_a11y,
                ((TextView) findViewById(R.id.tvProgresionTitulo)).getText(),
                etiquetas.get(0), etiquetas.get(etiquetas.size() - 1)));
    }

    private void pintarDelta(@Nullable Double kilos) {
        TextView tv = findViewById(R.id.tvProgresionDelta);
        if (kilos == null) {
            tv.setVisibility(View.GONE);
            return;
        }
        tv.setVisibility(View.VISIBLE);
        String n = numero(Math.abs(kilos));
        tv.setText(getString(kilos >= 0 ? R.string.detalle_kg_mas : R.string.detalle_kg_menos, n));
        tv.setTextColor(kilos > 0 ? ContextCompat.getColor(this, R.color.gp_success)
                : color(com.google.android.material.R.attr.colorOnSurfaceVariant));
    }

    // Fecha ISO («yyyy-MM-dd...») → etiqueta corta «dd/MM».
    private String fechaCorta(String iso) {
        if (iso == null || iso.length() < 10) return "";
        return iso.substring(8, 10) + "/" + iso.substring(5, 7);
    }

    // ── Tus últimas veces ───────────────────────────────────────────────────

    // Cuando han llegado las ejecuciones y las fechas: una tarjeta por sesión, con barras.
    private void pintarUltimasVeces() {
        if (ejecuciones == null || fechasSesion == null) return;

        // Una sesión puede traer varias filas del mismo ejercicio: se juntan sus series.
        Map<Integer, List<EjercicioRealizado.Serie>> porSesion = new HashMap<>();
        for (EjercicioRealizado er : ejecuciones) {
            if (er.getSesionId() == null) continue;
            List<EjercicioRealizado.Serie> l = porSesion.get(er.getSesionId());
            if (l == null) { l = new ArrayList<>(); porSesion.put(er.getSesionId(), l); }
            for (EjercicioRealizado.Serie s : er.getSeries()) if (s.isCompletada()) l.add(s);
        }
        boolean porTiempo = false;
        List<UltimasVeces.Sesion> sesiones = new ArrayList<>();
        for (Map.Entry<Integer, List<EjercicioRealizado.Serie>> e : porSesion.entrySet()) {
            String fecha = fechasSesion.get(e.getKey());
            if (fecha == null) continue;   // sin fecha no se sabe dónde ponerla
            List<EjercicioRealizado.Serie> orig = e.getValue();
            orig.sort((a, b) -> Integer.compare(a.getNumero(), b.getNumero()));
            List<UltimasVeces.Serie> series = new ArrayList<>();
            for (EjercicioRealizado.Serie s : orig) {
                if (s.getSegundos() > 0) {
                    porTiempo = true;
                    series.add(new UltimasVeces.Serie(0, s.getSegundos()));
                } else {
                    series.add(new UltimasVeces.Serie(s.getPeso(), s.getRepeticiones()));
                }
            }
            sesiones.add(new UltimasVeces.Sesion(e.getKey(), fecha, series));
        }
        Integer delRecord = recordVigente != null ? recordVigente.getSesionId() : null;
        List<UltimasVeces.Tarjeta> tarjetas = UltimasVeces.de(sesiones, delRecord, ULTIMAS);

        LinearLayout lista = findViewById(R.id.listaVeces);
        lista.removeAllViews();
        findViewById(R.id.bloqueVeces).setVisibility(tarjetas.isEmpty() ? View.GONE : View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (UltimasVeces.Tarjeta t : tarjetas) lista.addView(tarjeta(inflater, lista, t, porTiempo));

        // La pista: solo con la regla de los programas y la última vez completa.
        View pista = findViewById(R.id.cardPista);
        pista.setVisibility(View.GONE);
        if (!porTiempo && !tarjetas.isEmpty() && repsMax != null) {
            List<Integer> reps = new ArrayList<>();
            for (UltimasVeces.Serie s : tarjetas.get(0).series) reps.add(s.repeticiones);
            if (ProximaVez.subirPeso(reps, repsMax)) {
                ((TextView) findViewById(R.id.tvPista)).setText(getString(R.string.detalle_pista_subir, repsMax));
                pista.setVisibility(View.VISIBLE);
            }
        }
    }

    // Una sesión: día, hace cuánto y total, la diferencia con la anterior y las barras.
    private View tarjeta(LayoutInflater inflater, ViewGroup padre, UltimasVeces.Tarjeta t, boolean porTiempo) {
        View v = inflater.inflate(R.layout.item_vez_ejercicio, padre, false);
        ((TextView) v.findViewById(R.id.tvVezDia)).setText(diaDeLaSemana(t.fecha));
        String cuando = TiempoRelativo.texto(this, t.fecha);
        String total = porTiempo ? null : getString(R.string.detalle_kg_total, numero(t.totalKg));
        TextView sub = v.findViewById(R.id.tvVezSub);
        String subTexto = cuando != null && total != null && t.totalKg > 0
                ? getString(R.string.detalle_vez_sub, cuando, total)
                : cuando != null ? cuando : total != null && t.totalKg > 0 ? total : "";
        sub.setText(subTexto);
        sub.setVisibility(subTexto.isEmpty() ? View.GONE : View.VISIBLE);

        TextView dif = v.findViewById(R.id.tvVezDiferencia);
        if (!porTiempo && t.diferenciaKg != null && Math.round(t.diferenciaKg) != 0) {
            boolean mas = t.diferenciaKg > 0;
            dif.setVisibility(View.VISIBLE);
            dif.setText(getString(mas ? R.string.detalle_kg_mas : R.string.detalle_kg_menos, numero(Math.abs(t.diferenciaKg))));
            // Subir, en verde; bajar, neutro: una sesión más floja no es un fallo (DEC-055).
            dif.setTextColor(mas ? ContextCompat.getColor(this, R.color.gp_success)
                    : color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        }
        pintarBarras(v.findViewById(R.id.barrasVez), t, porTiempo);
        return v;
    }

    // Las series en barras: el peso arriba, la altura proporcional y «× reps» debajo. La serie del
    // récord, en dorado. Sin peso (peso corporal) manda la cifra de repeticiones; por tiempo, el cronómetro.
    private void pintarBarras(LinearLayout barras, UltimasVeces.Tarjeta t, boolean porTiempo) {
        float dp = getResources().getDisplayMetrics().density;
        double maxPeso = 0;
        double maxValor = 0;
        for (UltimasVeces.Serie s : t.series) {
            maxPeso = Math.max(maxPeso, s.peso);
            maxValor = Math.max(maxValor, s.peso > 0 ? s.peso : s.repeticiones);
        }
        final boolean conPeso = maxPeso > 0 && !porTiempo;
        @ColorInt int neutro = color(com.google.android.material.R.attr.colorOutline);
        @ColorInt int texto = color(com.google.android.material.R.attr.colorOnSurface);
        @ColorInt int apoyo = color(com.google.android.material.R.attr.colorOnSurfaceVariant);
        @ColorInt int oro = ContextCompat.getColor(this, R.color.gp_gold);
        @ColorInt int oroTexto = ContextCompat.getColor(this, R.color.gp_gold_text);
        StringBuilder voz = new StringBuilder();

        for (int i = 0; i < t.series.size(); i++) {
            UltimasVeces.Serie s = t.series.get(i);
            boolean dorada = t.esRecord && i == t.mejorIndice;

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            // Con una o dos series, la columna no se estira: una barra de ancho completo no dice nada.
            LinearLayout.LayoutParams lpCol = t.series.size() < 3
                    ? new LinearLayout.LayoutParams(Math.round(72 * dp), ViewGroup.LayoutParams.WRAP_CONTENT)
                    : new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lpCol.setMarginStart(Math.round(10 * dp));
            barras.addView(col, lpCol);

            String arriba = porTiempo ? Marcas.tiempo(s.repeticiones)
                    : conPeso ? numero(s.peso) : String.valueOf(s.repeticiones);
            TextView tvArriba = etiqueta(arriba, 13, true, dorada ? oroTexto : texto);
            col.addView(tvArriba);

            double valor = porTiempo || !conPeso ? s.repeticiones : s.peso;
            int alto = Math.max(Math.round(8 * dp), (int) Math.round(75 * dp * (maxValor > 0 ? valor / maxValor : 0)));
            View barra = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setColor(dorada ? oro : neutro);
            float r10 = 10 * dp, r4 = 4 * dp;
            g.setCornerRadii(new float[]{r10, r10, r10, r10, r4, r4, r4, r4});
            barra.setBackground(g);
            barra.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams lpBarra = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, alto);
            lpBarra.topMargin = Math.round(4 * dp);
            lpBarra.bottomMargin = Math.round(4 * dp);
            col.addView(barra, lpBarra);

            String abajo = conPeso ? getString(R.string.detalle_por_reps, s.repeticiones) : "";
            col.addView(etiqueta(abajo, 13, false, apoyo));

            if (i > 0) voz.append(", ");
            voz.append(porTiempo ? Marcas.tiempo(s.repeticiones)
                    : conPeso ? getString(R.string.detalle_serie_a11y, Marcas.kilos(this, s.peso), s.repeticiones)
                    : getResources().getQuantityString(R.plurals.record_repeticiones, s.repeticiones, s.repeticiones));
        }
        String desc = getString(R.string.detalle_series_a11y, voz.toString());
        barras.setContentDescription(t.esRecord ? getString(R.string.detalle_vez_record_a11y, desc) : desc);
        barras.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    private TextView etiqueta(String texto, int sp, boolean negrita, @ColorInt int color) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setGravity(Gravity.CENTER);
        tv.setMinHeight(Math.round(18 * getResources().getDisplayMetrics().density));
        if (negrita) tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return tv;
    }

    // ── Utilidades ──────────────────────────────────────────────────────────

    // «Lunes» a partir de una fecha ISO; en el idioma de la app y con mayúscula inicial.
    private String diaDeLaSemana(String iso) {
        if (iso == null || iso.length() < 10) return "";
        try {
            Locale loc = FechaUtils.localeDeLaApp(this);
            java.text.SimpleDateFormat entrada = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
            java.util.Date d = entrada.parse(iso.substring(0, 10));
            String dia = new java.text.SimpleDateFormat("EEEE", loc).format(d);
            return dia.isEmpty() ? dia : dia.substring(0, 1).toUpperCase(loc) + dia.substring(1);
        } catch (java.text.ParseException e) {
            return "";
        }
    }

    // Un número con coma decimal solo si hace falta («77,5», «1.880»), en el idioma de la app.
    private String numero(double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(FechaUtils.localeDeLaApp(this));
        nf.setMaximumFractionDigits(1);
        nf.setMinimumFractionDigits(0);
        return nf.format(v);
    }

    @ColorInt
    private int color(int attr) {
        return MaterialColors.getColor(this, attr, 0);
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
