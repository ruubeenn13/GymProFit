package es.pmdm.gymprofit.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.envivo.UltimaVez;
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.record.Record;
import es.pmdm.gymprofit.model.record.Records;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.model.sesion.ResumenSesion;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.CatalogoEjercicios;
import es.pmdm.gymprofit.network.ProgramaApi;
import es.pmdm.gymprofit.network.RecordApi;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.adapters.EjercicioAdapter;
import es.pmdm.gymprofit.utils.EjercicioNavHelper;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.Marcas;
import es.pmdm.gymprofit.utils.Musculos;
import es.pmdm.gymprofit.utils.Pauta;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.TiempoRelativo;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.UltimaMarca;
import es.pmdm.gymprofit.utils.UltimasVeces;
import es.pmdm.gymprofit.utils.ZonasDelDia;
import es.pmdm.gymprofit.utils.Zonas;

// ============================================================
// DetalleRutinaActivity — detalle de una rutina (GP-216, tablero 32).
//
// Cabecera protagonista con el nombre, de qué programa es y qué día del ciclo (si lo es), las
// series por zona y la última vez que se hizo; debajo, cada ejercicio con su pauta, el descanso
// y tu última marca (en dorado si fue el récord vigente); «Empezar» fijo abajo. Editar solo en
// las rutinas propias.
// Combina el catálogo (fotos y músculo) con las relaciones rutina-ejercicio, y pide aparte lo
// que depende de la cuenta —última vez de cada ejercicio, récords, última sesión de la rutina y
// el programa que sigue—; ninguna de esas cuatro piezas es imprescindible: si falla, la
// cabecera o la fila se quedan sin ese dato y lo demás se lee igual.
// ============================================================
public class DetalleRutinaActivity extends AppCompatActivity {

    /** Nombre del programa del que es la rutina, si es de uno (lote 1.2.1). */
    public static final String EXTRA_PROGRAMA_NOMBRE = "programaNombre";

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private PreferencesManager prefsManager;

    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    private final RecordApi recordApi = ApiClient.service(RecordApi.class);
    private final ProgramaApi programaApi = ApiClient.service(ProgramaApi.class);

    private int rutinaId;
    private String nombre, descripcion, nivel, programaNombre;
    private int duracion;
    private boolean predefinida;
    private int rutinaUsuarioId;

    // Lanzador para recibir el resultado de EditarRutinaActivity.
    private ActivityResultLauncher<Intent> editarLauncher;

    private final List<RutinaEjercicio> relaciones = new ArrayList<>();
    private final Map<Integer, Ejercicio> catalogo = new HashMap<>();
    private final Map<Integer, UltimaVez> ultimas = new HashMap<>();
    private final Map<Integer, Record> records = new HashMap<>();
    private boolean ultimasListas;
    private boolean recordsListos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        setContentView(R.layout.activity_detalle_rutina);

        rutinaId        = getIntent().getIntExtra("rutinaId", -1);
        nombre          = getIntent().getStringExtra("nombre");
        descripcion     = getIntent().getStringExtra("descripcion");
        nivel           = getIntent().getStringExtra("nivel");
        duracion        = getIntent().getIntExtra("duracion", 0);
        predefinida     = getIntent().getBooleanExtra("predefinida", false);
        rutinaUsuarioId = getIntent().getIntExtra("usuarioId", -1);
        programaNombre  = getIntent().getStringExtra(EXTRA_PROGRAMA_NOMBRE);

        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        es.pmdm.gymprofit.ui.widget.BarraSuperior.enlazar(findViewById(R.id.scrollPantalla), findViewById(R.id.barraSuperior));
        editarLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Si se editó la rutina, propaga el OK y cierra el detalle.
                        setResult(RESULT_OK);
                        finish();
                    }
                });

        poblarCabecera();
        configurarBotones();
        cargarEjercicios();
        cargarSesionYPrograma();
    }

    // ── Cabecera ────────────────────────────────────────────────────────────

    // Nombre, resumen y, si tiene, la descripción. La etiqueta de arriba dice de qué programa es.
    private void poblarCabecera() {
        ((TextView) findViewById(R.id.tvNombreDetalle)).setText(nombre != null ? nombre : "");
        ((TextView) findViewById(R.id.tvEtiquetaRutina)).setText(
                programaNombre != null && !programaNombre.isEmpty() ? programaNombre : getString(R.string.detalle_rutina_tuya));
        pintarResumen(0);
        TextView desc = findViewById(R.id.tvDescripcionDetalle);
        if (descripcion != null && !descripcion.trim().isEmpty()) {
            desc.setText(descripcion.trim());
            desc.setVisibility(View.VISIBLE);
        }
        MaterialButton empezar = findViewById(R.id.btnEntrenarRutina);
        empezar.setText(getString(R.string.detalle_rutina_empezar, nombre != null ? nombre : ""));
    }

    // «7 ejercicios · 55 min · intermedio»; el número de ejercicios llega con la lista.
    private void pintarResumen(int numEjercicios) {
        int n = numEjercicios > 0 ? numEjercicios : getIntent().getIntExtra("numEjercicios", 0);
        String ejercicios = getResources().getQuantityString(R.plurals.rutina_num_ejercicios, n, n);
        String nivelTxt = UIHelper.traducirNivel(this, nivel).toLowerCase(FechaUtils.localeDeLaApp(this));
        ((TextView) findViewById(R.id.tvResumenRutina)).setText(getString(R.string.detalle_rutina_resumen,
                ejercicios, getString(R.string.duracion_min, duracion), nivelTxt));
    }

    private void configurarBotones() {
        findViewById(R.id.btnEntrenarRutina).setOnClickListener(v -> entrenarEstaRutina());
        // Editar solo si la rutina no es predefinida y es del usuario de la sesión.
        boolean esPropia = !predefinida && rutinaUsuarioId == prefsManager.getUsuarioId();
        View editar = findViewById(R.id.btnEditarRutina);
        if (esPropia) {
            editar.setVisibility(View.VISIBLE);
            editar.setOnClickListener(v -> abrirEditar());
        }
    }

    /** Empieza la sesión en vivo de esta rutina (GP-012), con el reloj en marcha desde este toque. */
    private void entrenarEstaRutina() {
        es.pmdm.gymprofit.envivo.EmpezarSesion.empezar(this, rutinaId, nombre, programaNombre);
    }

    // Abre EditarRutinaActivity pasando los datos actuales de la rutina.
    private void abrirEditar() {
        Intent intent = new Intent(this, EditarRutinaActivity.class);
        intent.putExtra("rutinaId",    rutinaId);
        intent.putExtra("nombre",      nombre);
        intent.putExtra("descripcion", descripcion);
        intent.putExtra("nivel",       nivel);
        intent.putExtra("duracion",    duracion);
        editarLauncher.launch(intent);
    }

    // ── Ejercicios ──────────────────────────────────────────────────────────

    // Lanza en paralelo el catálogo de ejercicios activos y las relaciones rutina-ejercicio, y
    // combina los resultados cuando ambas terminan.
    private void cargarEjercicios() {
        AtomicInteger pendientes = new AtomicInteger(2);
        // Spinner de carga durante las dos peticiones (la lista estaría en blanco mientras carga).
        LoadingDialog.show(this);

        // El catálogo (fotos y músculos) se pide una vez por proceso: Entrenar ya lo suele tener.
        CatalogoEjercicios.cargar(porId -> {
            if (porId != null) {
                catalogo.putAll(porId);
            } else {
                // El catálogo es la carga esencial: se dice (evita doble toast).
                UiFeedback.toastError(DetalleRutinaActivity.this, 0, getString(R.string.error_conexion));
            }
            if (pendientes.decrementAndGet() == 0) listaLista();
        });

        rutinaApi.getEjerciciosDeRutina(rutinaId).enqueue(new ApiCallback<List<RutinaEjercicio>>() {
            @Override public void onOk(List<RutinaEjercicio> lista) {
                if (lista != null) relaciones.addAll(lista);
                if (pendientes.decrementAndGet() == 0) listaLista();
            }
            @Override public void onFail(int code, String message) {
                // Una rutina sin ejercicios llega como 200 con [] (GP-069): aquí solo caen fallos de
                // verdad, y un 404 significa que la rutina ya no existe, no que esté vacía.
                UiFeedback.toastError(DetalleRutinaActivity.this, code, message);
                if (pendientes.decrementAndGet() == 0) listaLista();
            }
        });
    }

    // Con el catálogo y las relaciones: se pinta la lista, las zonas y se piden las marcas.
    private void listaLista() {
        LoadingDialog.hide(this);
        if (isFinishing()) return;
        Collections.sort(relaciones, (a, b) -> Integer.compare(a.getOrden(), b.getOrden()));
        ((TextView) findViewById(R.id.tvEjerciciosTituloDetalle)).setText(
                getString(R.string.detalle_rutina_ejercicios_fmt, relaciones.size()));
        findViewById(R.id.tvSinEjercicios).setVisibility(relaciones.isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.cardEjerciciosRutina).setVisibility(relaciones.isEmpty() ? View.GONE : View.VISIBLE);
        pintarResumen(relaciones.size());
        pintarZonas();
        pintarFilas();
        if (!relaciones.isEmpty()) pedirMarcas();
    }

    // Una barra por zona con sus series, de más a menos (las que no se tocan no salen).
    private void pintarZonas() {
        List<ZonasDelDia.Ejercicio> lista = new ArrayList<>();
        for (RutinaEjercicio re : relaciones) {
            Ejercicio e = catalogo.get(re.getEjercicioId());
            String musculo = e == null ? null : Musculos.normalizar(e.getMusculoPrimario(), e.getGrupoMuscular());
            lista.add(new ZonasDelDia.Ejercicio(musculo, re.getSeries()));
        }
        Map<Zonas.Zona, Integer> porZona = Zonas.seriesPorZona(ZonasDelDia.de(lista).seriesPorMusculo);
        List<Map.Entry<Zonas.Zona, Integer>> orden = new ArrayList<>();
        int max = 0;
        for (Map.Entry<Zonas.Zona, Integer> e : porZona.entrySet()) {
            if (e.getValue() > 0) { orden.add(e); max = Math.max(max, e.getValue()); }
        }
        // sort es estable: a igualdad queda el orden de los chips.
        Collections.sort(orden, (a, b) -> Integer.compare(b.getValue(), a.getValue()));

        LinearLayout cont = findViewById(R.id.listaZonas);
        cont.removeAllViews();
        cont.setVisibility(orden.isEmpty() ? View.GONE : View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(cont.getContext());
        StringBuilder voz = new StringBuilder();
        for (Map.Entry<Zonas.Zona, Integer> e : orden) {
            View fila = inflater.inflate(R.layout.item_zona_rutina, cont, false);
            ((TextView) fila.findViewById(R.id.tvZonaNombre)).setText(e.getKey().titulo);
            String series = getResources().getQuantityString(R.plurals.inicio_zona_series, e.getValue(), e.getValue());
            ((TextView) fila.findViewById(R.id.tvZonaSeries)).setText(series);
            ((LinearLayout.LayoutParams) fila.findViewById(R.id.rellenoZona).getLayoutParams()).weight = e.getValue() / (float) max;
            cont.addView(fila);
            if (voz.length() > 0) voz.append(", ");
            voz.append(getString(e.getKey().titulo)).append(' ').append(series);
        }
        cont.setContentDescription(getString(R.string.detalle_rutina_zona_a11y, voz));
    }

    // Las filas de los ejercicios; la última marca se añade cuando llega.
    private void pintarFilas() {
        LinearLayout lista = findViewById(R.id.listaEjerciciosRutina);
        lista.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int n = 0;
        for (RutinaEjercicio re : relaciones) {
            n++;
            Ejercicio e = catalogo.get(re.getEjercicioId());
            if (e == null) {
                // El catálogo que se pide no trae los ejercicios desactivados; la relación sí trae su nombre.
                e = new Ejercicio();
                e.setId(re.getEjercicioId());
                e.setNombre(re.getNombreEjercicio() != null && !re.getNombreEjercicio().isEmpty()
                        ? re.getNombreEjercicio() : getString(R.string.ejercicio_sin_nombre, re.getEjercicioId()));
            }
            final Ejercicio ej = e;
            View fila = inflater.inflate(R.layout.item_ejercicio_rutina, lista, false);
            ((TextView) fila.findViewById(R.id.tvNumeroEjercicio)).setText(String.valueOf(n));
            ((TextView) fila.findViewById(R.id.tvNombreEjercicio)).setText(ej.getNombre());
            String pauta = Pauta.texto(this, re);
            String descanso = Pauta.descanso(this, re.getTiempoDescanso());
            ((TextView) fila.findViewById(R.id.tvPautaEjercicio)).setText(descanso != null
                    ? getString(R.string.detalle_rutina_pauta_descanso, pauta, descanso) : pauta);
            EjercicioAdapter.pintarFoto((ImageView) fila.findViewById(R.id.ivIconoEjercicio), ej);
            fila.setTag(re.getEjercicioId());
            fila.setOnClickListener(v -> EjercicioNavHelper.abrir(this, v, ej,
                    re.getRepeticionesMax() != null ? re.getRepeticionesMax() : -1));
            lista.addView(fila);
            if (n < relaciones.size()) {
                View raya = new View(this);
                raya.setBackgroundColor(com.google.android.material.color.MaterialColors.getColor(raya,
                        com.google.android.material.R.attr.colorSurfaceVariant));
                raya.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Math.round(getResources().getDisplayMetrics().density));
                lp.setMarginStart(Math.round(98 * getResources().getDisplayMetrics().density));
                lista.addView(raya, lp);
            }
        }
    }

    // ── Tu última marca en cada ejercicio ───────────────────────────────────

    // La última vez de cada ejercicio y los récords vigentes; la fila se repinta con lo que llegue.
    private void pedirMarcas() {
        Set<Integer> ids = new LinkedHashSet<>();
        for (RutinaEjercicio re : relaciones) if (ids.size() < 30) ids.add(re.getEjercicioId());
        sesionApi.ultimaVez(android.text.TextUtils.join(",", ids)).enqueue(new ApiCallback<List<UltimaVez>>() {
            @Override public void onOk(List<UltimaVez> lista) {
                if (lista != null) for (UltimaVez u : lista) ultimas.put(u.ejercicioId, u);
                ultimasListas = true;
                pintarMarcas();
            }
            @Override public void onFail(int code, String message) {
                // Se ignora a propósito: sin la última marca cada fila enseña solo su pauta.
            }
        });
        recordApi.getRecords(null).enqueue(new ApiCallback<Records>() {
            @Override public void onOk(Records r) {
                if (r != null) for (Record x : r.getRecords()) if (!records.containsKey(x.getEjercicioId())) records.put(x.getEjercicioId(), x);
                recordsListos = true;
                pintarMarcas();
            }
            @Override public void onFail(int code, String message) {
                // Se ignora a propósito: sin los récords la marca sale en el color normal, no en dorado.
                recordsListos = true;
                pintarMarcas();
            }
        });
    }

    // Cuando están la última vez y los récords: «Última: 80 kg × 8 · récord» o «Primera vez».
    private void pintarMarcas() {
        if (!ultimasListas || !recordsListos || isFinishing()) return;
        LinearLayout lista = findViewById(R.id.listaEjerciciosRutina);
        int oro = ContextCompat.getColor(this, R.color.gp_gold_text);
        for (int i = 0; i < lista.getChildCount(); i++) {
            View fila = lista.getChildAt(i);
            if (!(fila.getTag() instanceof Integer)) continue;
            int id = (Integer) fila.getTag();
            TextView tv = fila.findViewById(R.id.tvUltimaMarca);
            UltimaVez u = ultimas.get(id);
            String texto;
            boolean record = false;
            if (u == null) {
                texto = getString(R.string.detalle_rutina_primera_vez);
            } else {
                texto = marcaTexto(u);
                Record r = records.get(id);
                UltimaMarca m = mejorDe(u);
                record = m != null && r != null && r.esDePeso()
                        && m.esRecord(u.fecha, r.getFecha(), r.getPeso(), r.getRepeticiones());
                if (texto != null) {
                    texto = getString(record ? R.string.detalle_rutina_ultima_marca_record
                            : R.string.detalle_rutina_ultima_marca, texto);
                }
            }
            if (texto == null) { tv.setVisibility(View.GONE); continue; }
            tv.setVisibility(View.VISIBLE);
            tv.setText(texto);
            if (record) {
                tv.setTextColor(oro);
                tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_ms_trophy_fill, 0, 0, 0);
                tv.setCompoundDrawablePadding(Math.round(4 * getResources().getDisplayMetrics().density));
                tv.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.gp_gold)));
                int d = Math.round(16 * getResources().getDisplayMetrics().density);
                android.graphics.drawable.Drawable ic = tv.getCompoundDrawablesRelative()[0];
                if (ic != null) ic.setBounds(0, 0, d, d);
                tv.setCompoundDrawablesRelative(ic, null, null, null);
            }
            fila.setContentDescription(getString(R.string.detalle_rutina_fila_a11y,
                    ((TextView) fila.findViewById(R.id.tvNombreEjercicio)).getText(),
                    ((TextView) fila.findViewById(R.id.tvPautaEjercicio)).getText(), texto));
        }
    }

    // La mejor serie de esa vez, en kilos × repeticiones, o por tiempo; null si no hay nada que decir.
    @Nullable
    private String marcaTexto(UltimaVez u) {
        if (u.series == null || u.series.isEmpty()) return null;
        boolean porTiempo = false;
        int mejorSeg = 0;
        for (UltimaVez.Serie s : u.series) {
            if (s.segundos != null && s.segundos > 0) { porTiempo = true; mejorSeg = Math.max(mejorSeg, s.segundos); }
        }
        if (porTiempo) return Marcas.tiempo(mejorSeg);
        UltimaMarca m = mejorDe(u);
        if (m == null) return null;
        return Marcas.texto(this, m.peso > 0, m.peso, m.repeticiones);
    }

    @Nullable
    private static UltimaMarca mejorDe(UltimaVez u) {
        if (u.series == null) return null;
        List<UltimasVeces.Serie> series = new ArrayList<>();
        for (UltimaVez.Serie s : u.series) {
            if (s.segundos != null && s.segundos > 0) continue;
            series.add(new UltimasVeces.Serie(s.peso != null ? s.peso.doubleValue() : 0,
                    s.repeticiones != null ? s.repeticiones : 0));
        }
        return UltimaMarca.mejorDe(series);
    }

    // ── Día del ciclo y última sesión ───────────────────────────────────────

    // «Torso y pierna · día 1 de 4» (solo si es de un programa que sigue) y «La última vez…»
    // con la duración y los kilos de la última sesión de esta rutina.
    private void cargarSesionYPrograma() {
        if (programaNombre != null && !programaNombre.isEmpty()) {
            programaApi.seguido().enqueue(new ApiCallback<ProgramaQueSigue>() {
                @Override public void onOk(ProgramaQueSigue s) {
                    if (s == null || s.getCiclo() == null || isFinishing()) return;
                    int dia = 0, total = s.getCiclo().size();
                    for (int i = 0; i < total; i++) {
                        Integer rid = s.getCiclo().get(i).getRutinaId();
                        if (rid != null && rid == rutinaId) { dia = i + 1; break; }
                    }
                    if (dia > 0) ((TextView) findViewById(R.id.tvEtiquetaRutina)).setText(
                            getString(R.string.detalle_rutina_programa_dia, programaNombre, dia, total));
                }
                @Override public void onFail(int code, String message) {
                    // Se ignora a propósito: la etiqueta se queda con el nombre del programa, sin el día.
                }
            });
        }
        int uid = prefsManager.getUsuarioId();
        if (uid == -1 || rutinaId <= 0) return;
        sesionApi.getDeUsuario(uid).enqueue(new ApiCallback<List<SesionEntrenamiento>>() {
            @Override public void onOk(List<SesionEntrenamiento> lista) {
                if (lista == null || isFinishing()) return;
                SesionEntrenamiento ultima = null;
                for (SesionEntrenamiento s : lista) {
                    if (s.getRutinaId() == null || s.getRutinaId() != rutinaId || s.getFechaInicio() == null) continue;
                    if (ultima == null || s.getFechaInicio().compareTo(ultima.getFechaInicio()) > 0) ultima = s;
                }
                if (ultima != null) pintarUltimaSesion(ultima);
            }
            @Override public void onFail(int code, String message) {
                // Se ignora a propósito: sin la última sesión la cabecera no dice «La última vez».
            }
        });
    }

    private void pintarUltimaSesion(SesionEntrenamiento s) {
        String cuando = TiempoRelativo.texto(this, s.getFechaInicio());
        if (cuando == null) return;
        String dur = getString(R.string.duracion_min, s.getDuracionMinutos());
        pintarUltimaVez(getString(R.string.detalle_rutina_ultima_vez_sin_kilos, cuando, dur));
        sesionApi.resumen(s.getId()).enqueue(new ApiCallback<ResumenSesion>() {
            @Override public void onOk(ResumenSesion r) {
                if (r == null || isFinishing() || r.getVolumenKg() <= 0) return;
                String kilos = getString(R.string.detalle_kg_total, java.text.NumberFormat
                        .getIntegerInstance(FechaUtils.localeDeLaApp(DetalleRutinaActivity.this))
                        .format(Math.round(r.getVolumenKg())));
                pintarUltimaVez(getString(R.string.detalle_rutina_ultima_vez, cuando, dur, kilos));
            }
            @Override public void onFail(int code, String message) {
                // Se ignora a propósito: la línea se queda con la duración, sin los kilos.
            }
        });
    }

    private void pintarUltimaVez(String texto) {
        ((TextView) findViewById(R.id.tvUltimaVezRutina)).setText(texto);
        findViewById(R.id.bloqueUltimaVez).setVisibility(View.VISIBLE);
    }
}
