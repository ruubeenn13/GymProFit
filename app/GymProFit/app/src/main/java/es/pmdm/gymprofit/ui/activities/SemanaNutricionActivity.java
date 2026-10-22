package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.comida.AlimentoApuntado;
import es.pmdm.gymprofit.model.comida.DiaDiario;
import es.pmdm.gymprofit.model.comida.ResumenDiarioNutricion;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ComidaApi;
import es.pmdm.gymprofit.network.DiarioApi;
import es.pmdm.gymprofit.ui.widget.BarrasSemanaView;
import es.pmdm.gymprofit.utils.Agua;
import es.pmdm.gymprofit.utils.DiaCompleto;
import es.pmdm.gymprofit.utils.DiaNutricion;
import es.pmdm.gymprofit.utils.Enumeracion;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.ResultadoNutricional;
import es.pmdm.gymprofit.utils.SemanaNutricion;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// SemanaNutricionActivity — «Tu semana de nutrición» (tablero 12 del lienzo de nutrición,
// decisión 13; GP-210 paso 1).
//
// La semana de lunes a domingo: proteína (con lo que pasó), días completos, calorías, agua y
// lo que más has comido. Sin notificación: se llega desde la cabecera de Nutrición y desde la
// tarjeta de los lunes. Se puede pasar a otras semanas (nunca a una futura).
//
// Los números se agregan aquí (SemanaNutricion) con lo que ya dan /comidas/.../resumen y
// /diario: siete valores por cuenta. «Lo que más has comido» sí viene del servidor
// (/diario/mas-apuntados), y si falla la tarjeta no sale: la semana está completa sin ella.
// Cargando, error con reintentar y vacío, con VistaEstado.
// ============================================================
public class SemanaNutricionActivity extends AppCompatActivity {

    /** Extra opcional: el lunes de la semana que se abre, «yyyy-MM-dd» (la tarjeta de los lunes manda el pasado). */
    public static final String EXTRA_LUNES = "lunes";

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private PreferencesManager prefs;
    private final ComidaApi comidaApi = ApiClient.service(ComidaApi.class);
    private final DiarioApi diarioApi = ApiClient.service(DiarioApi.class);

    private VistaEstado estado;
    private View contenido;
    private View btnSiguiente;
    private TextView tvRango;

    /** El lunes de la semana que se mira. */
    private final Calendar lunes = Calendar.getInstance();
    private String hoy;
    private int turno;

    // Lo que llega de la red.
    private List<ResumenDiarioNutricion> resumen;
    private List<DiaDiario> diario;
    private List<AlimentoApuntado> masApuntados;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PreferencesManager(this);
        prefs.applyTheme();
        setContentView(R.layout.activity_semana_nutricion);

        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        estado = new VistaEstado(findViewById(R.id.estadoSemana));
        contenido = findViewById(R.id.contenidoSemana);
        tvRango = findViewById(R.id.tvRangoSemana);
        btnSiguiente = findViewById(R.id.btnSemanaSiguiente);
        hoy = ISO.format(new Date());

        lunes.setTime(new Date());
        irAlLunes(lunes);
        String pedido = getIntent().getStringExtra(EXTRA_LUNES);
        if (pedido != null) {
            try {
                Date d = ISO.parse(pedido);
                if (d != null) {
                    lunes.setTime(d);
                    irAlLunes(lunes);
                }
            } catch (java.text.ParseException e) {
                // Un lunes que no se entiende se queda en esta semana: no hay nada que decir.
            }
        }

        findViewById(R.id.btnSemanaAnterior).setOnClickListener(v -> cambiarSemana(-7));
        btnSiguiente.setOnClickListener(v -> cambiarSemana(7));
        findViewById(R.id.btnVerMas).setOnClickListener(
                v -> startActivity(new Intent(this, EstadisticasNutricionActivity.class)));
        ViewCompat.setAccessibilityHeading(tvRango, false);
        cargar();
    }

    // Lleva el calendario al lunes de su semana, a las 00:00.
    private static void irAlLunes(Calendar c) {
        int dia = c.get(Calendar.DAY_OF_WEEK);
        c.add(Calendar.DAY_OF_YEAR, -((dia + 5) % 7));
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private void cambiarSemana(int dias) {
        lunes.add(Calendar.DAY_OF_YEAR, dias);
        cargar();
    }

    private boolean esEstaSemana() {
        Calendar c = (Calendar) lunes.clone();
        c.add(Calendar.DAY_OF_YEAR, 6);
        return ISO.format(c.getTime()).compareTo(hoy) >= 0;
    }

    private String fecha(int offset) {
        Calendar c = (Calendar) lunes.clone();
        c.add(Calendar.DAY_OF_YEAR, offset);
        return ISO.format(c.getTime());
    }

    // ── Carga ───────────────────────────────────────────────────────────────

    private void cargar() {
        int miTurno = ++turno;
        pintarRango();
        btnSiguiente.setVisibility(esEstaSemana() ? View.INVISIBLE : View.VISIBLE);
        resumen = null;
        diario = null;
        masApuntados = null;
        contenido.setVisibility(View.GONE);
        estado.cargando();
        String desde = fecha(0), hasta = fecha(6);
        AtomicInteger pendientes = new AtomicInteger(3);
        boolean[] fallo = {false};
        String[] porque = {null};

        comidaApi.getResumen(prefs.getUsuarioId(), desde, hasta).enqueue(new ApiCallback<List<ResumenDiarioNutricion>>() {
            @Override public void onOk(List<ResumenDiarioNutricion> lista) {
                resumen = lista != null ? lista : new ArrayList<>();
                llego(miTurno, pendientes, fallo, porque);
            }
            @Override public void onFail(int code, String message) {
                fallo[0] = true;
                porque[0] = VistaEstado.mensaje(SemanaNutricionActivity.this, R.string.sn_error, code, message).toString();
                llego(miTurno, pendientes, fallo, porque);
            }
        });
        diarioApi.dias(desde, hasta).enqueue(new ApiCallback<List<DiaDiario>>() {
            @Override public void onOk(List<DiaDiario> lista) {
                diario = lista != null ? lista : new ArrayList<>();
                llego(miTurno, pendientes, fallo, porque);
            }
            @Override public void onFail(int code, String message) {
                fallo[0] = true;
                porque[0] = VistaEstado.mensaje(SemanaNutricionActivity.this, R.string.sn_error, code, message).toString();
                llego(miTurno, pendientes, fallo, porque);
            }
        });
        diarioApi.masApuntados(desde, hasta).enqueue(new ApiCallback<List<AlimentoApuntado>>() {
            @Override public void onOk(List<AlimentoApuntado> lista) {
                masApuntados = lista != null ? lista : new ArrayList<>();
                llego(miTurno, pendientes, fallo, porque);
            }
            @Override public void onFail(int code, String message) {
                // Se ignora a propósito: «Lo que más has comido» es un extra y la semana está
                // completa sin él. Sin la tarjeta no falta nada que el resto necesite.
                masApuntados = new ArrayList<>();
                llego(miTurno, pendientes, fallo, porque);
            }
        });
    }

    private void llego(int miTurno, AtomicInteger pendientes, boolean[] fallo, String[] porque) {
        if (miTurno != turno || isDestroyed()) return;
        if (pendientes.decrementAndGet() > 0) return;
        if (fallo[0]) {
            estado.error(porque[0] != null ? porque[0] : getString(R.string.sn_error), this::cargar);
            return;
        }
        pintar();
    }

    // ── Pintar ──────────────────────────────────────────────────────────────

    // «21 – 27 de septiembre» o «28 de septiembre – 4 de octubre».
    private void pintarRango() {
        Locale idioma = FechaUtils.localeDeLaApp(this);
        Calendar fin = (Calendar) lunes.clone();
        fin.add(Calendar.DAY_OF_YEAR, 6);
        SimpleDateFormat mes = new SimpleDateFormat("MMMM", idioma);
        String m1 = mes.format(lunes.getTime()), m2 = mes.format(fin.getTime());
        String texto = lunes.get(Calendar.MONTH) == fin.get(Calendar.MONTH)
                ? getString(R.string.sn_rango_mismo_mes, lunes.get(Calendar.DAY_OF_MONTH), fin.get(Calendar.DAY_OF_MONTH), m1)
                : getString(R.string.sn_rango_distinto_mes, lunes.get(Calendar.DAY_OF_MONTH), m1,
                fin.get(Calendar.DAY_OF_MONTH), m2);
        tvRango.setText(texto);
    }

    private SemanaNutricion semana(@Nullable ResultadoNutricional objetivo) {
        Map<String, ResumenDiarioNutricion> resumenes = new HashMap<>();
        for (ResumenDiarioNutricion r : resumen) if (r.getFecha() != null) resumenes.put(r.getFecha(), r);
        Map<String, DiaDiario> dias = new HashMap<>();
        for (DiaDiario d : diario) if (d.getFecha() != null) dias.put(d.getFecha(), d);

        List<SemanaNutricion.Entrada> siete = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            String f = fecha(i);
            ResumenDiarioNutricion r = resumenes.get(f);
            DiaDiario d = dias.get(f);
            boolean hayComida = r != null && (r.getCalorias() > 0 || r.getProteinas() > 0);
            siete.add(new SemanaNutricion.Entrada(f, hayComida, r != null ? r.getProteinas() : 0,
                    r != null ? r.getCalorias() : 0, DiaCompleto.completo(d), d != null && d.getEntrenos() > 0,
                    d != null ? d.getAguaMl() : 0));
        }
        return SemanaNutricion.de(siete, hoy,
                objetivo != null ? objetivo.proteinas : 0, objetivo != null ? objetivo.calorias : 0,
                objetivo != null ? (int) Math.round(objetivo.agua * 1000) : 0);
    }

    private void pintar() {
        ResultadoNutricional objetivo = DiaNutricion.objetivo(prefs);
        SemanaNutricion s = semana(objetivo);
        if (s.vacia()) {
            contenido.setVisibility(View.GONE);
            estado.vacio(R.string.sn_vacio);
            return;
        }
        estado.oculto();
        contenido.setVisibility(View.VISIBLE);
        Locale idioma = FechaUtils.localeDeLaApp(this);
        String[] letras = getResources().getStringArray(R.array.dias_semana_iniciales);
        String[] nombres = nombresDeLosDias(idioma);

        pintarProteina(s, letras, nombres);
        pintarCompletos(s, nombres);
        pintarKcal(s, letras, nombres, idioma);
        pintarAgua(s, idioma);
        pintarMasComido();

        // Momento 3 (entrar): las tarjetas suben una tras otra.
        Movimiento.entrar(0, findViewById(R.id.cardProteina), findViewById(R.id.cardCompletos),
                findViewById(R.id.cardKcal), findViewById(R.id.cardAgua), findViewById(R.id.cardMasComido));
    }

    private void pintarProteina(SemanaNutricion s, String[] letras, String[] nombres) {
        int oro = ContextCompat.getColor(this, R.color.gp_gold);
        int azul = ContextCompat.getColor(this, R.color.gp_macro_proteinas);
        float[] valores = new float[7];
        boolean[] hay = new boolean[7];
        boolean[] entreno = new boolean[7];
        int[] colores = new int[7];
        List<SemanaNutricion.Entrada> dias = s.dias();
        for (int i = 0; i < 7; i++) {
            SemanaNutricion.Entrada e = dias.get(i);
            hay[i] = s.haLlegado(e) && e.hayComida;
            valores[i] = (float) e.proteina;
            entreno[i] = s.haLlegado(e) && e.entreno;
            colores[i] = s.hayObjetivoProteina() && e.proteina >= s.objetivoProteinas() ? oro : azul;
        }
        BarrasSemanaView barras = findViewById(R.id.barrasProteina);
        barras.formato(120, 8, 4, true);
        barras.poner(valores, hay, colores, entreno, letras, s.objetivoProteinas(),
                s.hayObjetivoProteina() ? getString(R.string.sn_objetivo_g, s.objetivoProteinas()) : null);
        barras.animarEntrada(200);

        TextView dias_ = findViewById(R.id.tvProteinaDias);
        TextView unidad = findViewById(R.id.tvProteinaUnidad);
        View medalla = findViewById(R.id.ivProteinaMedalla);
        if (s.hayObjetivoProteina()) {
            int n = s.proteinaConseguida(), de = s.diasQueHanLlegado();
            dias_.setText(getString(R.string.sn_proteina_dias, n, de));
            unidad.setText(getResources().getQuantityString(R.plurals.sn_proteina_unidad, de));
            medalla.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
            if (n > 0) Movimiento.saltar(medalla, Movimiento.CHECK);
        } else {
            // Sin objetivo no hay días «conseguidos»: se enseña la media y nada más.
            dias_.setText(getString(R.string.sn_objetivo_g, s.mediaProteina() != null ? s.mediaProteina() : 0));
            unidad.setText("");
            medalla.setVisibility(View.GONE);
        }
        // El dorado es de lo conseguido (DEC-018): con 0 días conseguidos, el color de siempre.
        dias_.setTextColor(s.hayObjetivoProteina() && s.proteinaConseguida() > 0
                ? ContextCompat.getColor(this, R.color.gp_gold_text) : color(com.google.android.material.R.attr.colorOnSurface));

        Integer media = s.mediaProteina();
        TextView tvMedia = findViewById(R.id.tvProteinaMedia);
        if (media == null) {
            tvMedia.setVisibility(View.GONE);
        } else {
            tvMedia.setVisibility(View.VISIBLE);
            tvMedia.setText(s.hayObjetivoProteina()
                    ? getString(R.string.sn_proteina_media, media, s.objetivoProteinas())
                    : getString(R.string.sn_proteina_media_sin_objetivo, media));
        }

        StringBuilder a11y = new StringBuilder();
        List<String> entrenos = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            if (a11y.length() > 0) a11y.append(", ");
            a11y.append(hay[i] ? getString(R.string.sn_a11y_dia_valor, nombres[i], Math.round(valores[i]))
                    : getString(R.string.sn_a11y_dia_sin_datos, nombres[i]));
            if (entreno[i]) entrenos.add(nombres[i]);
        }
        String descripcion = s.hayObjetivoProteina()
                ? getString(R.string.sn_proteina_a11y, s.objetivoProteinas(), a11y)
                : getString(R.string.sn_proteina_a11y_sin_objetivo, a11y);
        if (!entrenos.isEmpty()) descripcion += " " + getString(R.string.sn_a11y_entrenaste, unir(entrenos));
        barras.setContentDescription(descripcion);

        LinearLayout lista = findViewById(R.id.observacionesSemana);
        lista.removeAllViews();
        for (SemanaNutricion.Observacion o : s.observaciones()) {
            boolean entreno_ = o.tipo == SemanaNutricion.Observacion.Tipo.ENTRENO_Y_PROTEINA;
            String texto = entreno_
                    ? getResources().getQuantityString(R.plurals.sn_obs_entreno, o.valor, o.valor)
                    : getString(R.string.sn_obs_finde, o.valor);
            lista.addView(observacion(texto, entreno_ ? R.drawable.ic_ms_fitness_center : R.drawable.ic_ms_calendar_month,
                    entreno_ ? attrPrimario() : color(com.google.android.material.R.attr.colorOnPrimaryContainer)));
        }
        lista.setVisibility(lista.getChildCount() == 0 ? View.GONE : View.VISIBLE);
    }

    private View observacion(String texto, int icono, int tinte) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        int arriba = (int) (6 * getResources().getDisplayMetrics().density);
        fila.setPadding(0, arriba, 0, arriba);
        ImageView iv = new ImageView(this);
        int lado = (int) (20 * getResources().getDisplayMetrics().density);
        iv.setLayoutParams(new LinearLayout.LayoutParams(lado, lado));
        iv.setImageResource(icono);
        iv.setImageTintList(android.content.res.ColorStateList.valueOf(tinte));
        iv.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginStart((int) (10 * getResources().getDisplayMetrics().density));
        tv.setLayoutParams(lp);
        tv.setText(texto);
        tv.setTextColor(color(com.google.android.material.R.attr.colorOnSurface));
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_body));
        tv.setLineSpacing(0, 1.1f);
        fila.addView(iv);
        fila.addView(tv);
        return fila;
    }

    private void pintarCompletos(SemanaNutricion s, String[] nombres) {
        int llegados = s.diasQueHanLlegado(), completos = s.diasCompletos();
        TextView tvCompletos = findViewById(R.id.tvCompletosDias);
        tvCompletos.setText(getString(R.string.sn_proteina_dias, completos, llegados));
        tvCompletos.setTextColor(completos > 0 ? ContextCompat.getColor(this, R.color.gp_success)
                : color(com.google.android.material.R.attr.colorOnSurface));
        LinearLayout puntos = findViewById(R.id.puntosCompletos);
        puntos.removeAllViews();
        // Con letra grande (1,3 o más) el texto no cabía al lado de los siete puntos y se cortaba:
        // los puntos pasan debajo, como el anillo de Nutrición.
        LinearLayout fila = findViewById(R.id.filaCompletos);
        View texto = findViewById(R.id.textoCompletos);
        boolean apilar = getResources().getConfiguration().fontScale >= 1.3f;
        fila.setOrientation(apilar ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        fila.setGravity(apilar ? android.view.Gravity.START : android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lpTexto = (LinearLayout.LayoutParams) texto.getLayoutParams();
        lpTexto.width = apilar ? LinearLayout.LayoutParams.MATCH_PARENT : 0;
        lpTexto.weight = apilar ? 0 : 1;
        texto.setLayoutParams(lpTexto);
        LinearLayout.LayoutParams lpPuntos = (LinearLayout.LayoutParams) puntos.getLayoutParams();
        lpPuntos.topMargin = apilar ? Math.round(12 * getResources().getDisplayMetrics().density) : 0;
        puntos.setLayoutParams(lpPuntos);
        String[] letras = getResources().getStringArray(R.array.dias_semana_iniciales);
        int verde = ContextCompat.getColor(this, R.color.gp_success);
        int ambar = ContextCompat.getColor(this, R.color.gp_macro_carbos);
        List<String> completosLista = new ArrayList<>();
        List<String> aMediasLista = new ArrayList<>();
        List<SemanaNutricion.Entrada> dias = s.dias();
        float dp = getResources().getDisplayMetrics().density;
        for (int i = 0; i < 7; i++) {
            SemanaNutricion.Entrada e = dias.get(i);
            boolean llego = s.haLlegado(e);
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams lpCol = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) lpCol.setMarginStart((int) (6 * dp));
            col.setLayoutParams(lpCol);
            View punto = new View(this);
            punto.setLayoutParams(new LinearLayout.LayoutParams((int) (14 * dp), (int) (14 * dp)));
            android.graphics.drawable.GradientDrawable fondo = new android.graphics.drawable.GradientDrawable();
            fondo.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            // El estado va por la forma y no solo por el color: completo, círculo lleno y verde;
            // a medias, aro ámbar vacío; un día que no ha llegado, un aro fino y gris. Nunca rojo,
            // y el ámbar de un día a medias no se confunde con el dorado de lo conseguido.
            if (!llego) {
                fondo.setColor(android.graphics.Color.TRANSPARENT);
                fondo.setStroke((int) dp, color(com.google.android.material.R.attr.colorOutline));
            } else if (e.completo) {
                fondo.setColor(verde);
            } else {
                fondo.setColor(android.graphics.Color.TRANSPARENT);
                fondo.setStroke((int) (2 * dp), ambar);
            }
            punto.setBackground(fondo);
            punto.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            TextView letra = new TextView(this);
            letra.setText(letras[i]);
            letra.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_eyebrow));
            letra.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
            letra.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            col.addView(punto);
            col.addView(letra);
            puntos.addView(col);
            if (llego && e.completo) {
                completosLista.add(nombres[i]);
            } else if (llego) {
                aMediasLista.add(nombres[i]);
            }
            if (!Movimiento.quieto(this)) {
                punto.setScaleX(0.4f);
                punto.setScaleY(0.4f);
                punto.animate().scaleX(1f).scaleY(1f).setStartDelay(300 + (long) i * Movimiento.DIA_ESCALON)
                        .setDuration(Movimiento.CHECK).setInterpolator(Movimiento.REBOTE).start();
            }
        }
        StringBuilder detalle = new StringBuilder();
        if (!completosLista.isEmpty()) detalle.append(getString(R.string.sn_completos_lista, unir(completosLista)));
        if (!aMediasLista.isEmpty()) {
            if (detalle.length() > 0) detalle.append(' ');
            detalle.append(getString(R.string.sn_a_medias_lista, unir(aMediasLista)));
        }
        puntos.setContentDescription(getString(R.string.sn_completos_a11y, completos, llegados, detalle.toString()).trim());
    }

    private void pintarKcal(SemanaNutricion s, String[] letras, String[] nombres, Locale idioma) {
        NumberFormat miles = NumberFormat.getIntegerInstance(idioma);
        float[] valores = new float[7];
        boolean[] hay = new boolean[7];
        int[] colores = new int[7];
        int naranja = androidx.core.graphics.ColorUtils.setAlphaComponent(
                color(androidx.appcompat.R.attr.colorPrimary), 217);
        StringBuilder a11y = new StringBuilder();
        List<SemanaNutricion.Entrada> dias = s.dias();
        for (int i = 0; i < 7; i++) {
            SemanaNutricion.Entrada e = dias.get(i);
            hay[i] = s.haLlegado(e) && e.hayComida;
            valores[i] = e.kcal;
            colores[i] = naranja;
            if (a11y.length() > 0) a11y.append(", ");
            a11y.append(hay[i] ? getString(R.string.sn_a11y_dia_valor, nombres[i], e.kcal)
                    : getString(R.string.sn_a11y_dia_sin_datos, nombres[i]));
        }
        BarrasSemanaView barras = findViewById(R.id.barrasKcal);
        barras.formato(80, 6, 3, false);
        barras.poner(valores, hay, colores, null, letras, s.objetivoKcal(), null);
        barras.animarEntrada(300);
        barras.setContentDescription(s.objetivoKcal() > 0
                ? getString(R.string.sn_kcal_a11y_objetivo, s.objetivoKcal(), a11y)
                : getString(R.string.sn_kcal_a11y, a11y));

        Integer media = s.mediaKcal();
        ((TextView) findViewById(R.id.tvKcalMedia)).setText(media != null ? miles.format(media) : "");
        TextView pct = findViewById(R.id.tvKcalPorcentaje);
        if (s.objetivoKcal() > 0 && media != null) {
            pct.setVisibility(View.VISIBLE);
            pct.setText(getString(R.string.sn_kcal_pct, s.porcentajeKcal(), miles.format(s.objetivoKcal())));
        } else {
            pct.setVisibility(View.GONE);
        }
    }

    private void pintarAgua(SemanaNutricion s, Locale idioma) {
        Integer media = s.mediaAguaMl();
        TextView tvMedia = findViewById(R.id.tvAguaMedia);
        TextView unidad = findViewById(R.id.tvAguaUnidad);
        TextView dias = findViewById(R.id.tvAguaDias);
        LinearProgressIndicator barra = findViewById(R.id.progresoAgua);
        // A cien en cien mililitros: «1,9 L» y no «1,89 L», como en el lienzo.
        int mediaRedondeada = media == null ? 0 : (int) (Math.round(media / 100.0) * 100);
        String litros = getString(R.string.sn_agua_litros, Agua.litros(mediaRedondeada, idioma));
        tvMedia.setText(media != null ? litros : "—");
        boolean hayObjetivo = s.objetivoAguaMl() > 0;
        if (hayObjetivo) {
            String objetivo = getString(R.string.sn_agua_litros, Agua.litros(s.objetivoAguaMl(), idioma));
            unidad.setText(getString(R.string.sn_agua_unidad_objetivo, objetivo));
            dias.setVisibility(View.VISIBLE);
            dias.setText(getString(R.string.sn_agua_dias, s.diasConAguaCumplida(), s.diasQueHanLlegado()));
            int pct = media == null ? 0 : DiaNutricion.porcentaje(media, s.objetivoAguaMl());
            barra.setVisibility(View.VISIBLE);
            barra.setProgressCompat(pct, !Movimiento.quieto(this));
            barra.setContentDescription(getString(R.string.sn_agua_a11y_objetivo,
                    Agua.litros(mediaRedondeada, idioma), Agua.litros(s.objetivoAguaMl(), idioma)));
        } else {
            unidad.setText(R.string.sn_agua_unidad);
            dias.setVisibility(View.GONE);
            barra.setVisibility(View.GONE);
        }
    }

    private void pintarMasComido() {
        View tarjeta = findViewById(R.id.cardMasComido);
        LinearLayout lista = findViewById(R.id.listaMasComido);
        lista.removeAllViews();
        if (masApuntados == null || masApuntados.isEmpty()) {
            tarjeta.setVisibility(View.GONE);
            return;
        }
        tarjeta.setVisibility(View.VISIBLE);
        for (int i = 0; i < masApuntados.size(); i++) {
            AlimentoApuntado a = masApuntados.get(i);
            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(android.view.Gravity.CENTER_VERTICAL);
            float dp = getResources().getDisplayMetrics().density;
            fila.setMinimumHeight((int) (48 * dp));
            fila.setPadding(0, (int) (6 * dp), 0, (int) (6 * dp));
            ImageView iv = new ImageView(this);
            iv.setLayoutParams(new LinearLayout.LayoutParams((int) (24 * dp), (int) (24 * dp)));
            iv.setImageResource(R.drawable.ic_ms_nutrition);
            iv.setImageTintList(android.content.res.ColorStateList.valueOf(
                    color(com.google.android.material.R.attr.colorOnSurfaceVariant)));
            iv.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            TextView nombre = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMarginStart((int) (12 * dp));
            nombre.setLayoutParams(lp);
            nombre.setText(a.getNombre());
            nombre.setTextColor(color(com.google.android.material.R.attr.colorOnSurface));
            nombre.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_fila));
            TextView veces = new TextView(this);
            LinearLayout.LayoutParams lv = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lv.setMarginStart((int) (12 * dp));
            veces.setLayoutParams(lv);
            veces.setText(getResources().getQuantityString(R.plurals.sn_veces, a.getVeces(), a.getVeces()));
            veces.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
            veces.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_body));
            fila.addView(iv);
            fila.addView(nombre);
            fila.addView(veces);
            fila.setFocusable(true);
            fila.setContentDescription(a.getNombre() + ", "
                    + getResources().getQuantityString(R.plurals.sn_veces, a.getVeces(), a.getVeces()));
            lista.addView(fila);
            if (i < masApuntados.size() - 1) {
                View raya = new View(this);
                raya.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) dp));
                raya.setBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant));
                raya.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                lista.addView(raya);
            }
        }
    }

    // ── Utilidades ──────────────────────────────────────────────────────────

    // Los nombres de los siete días, para TalkBack: «lunes», «martes»…
    private String[] nombresDeLosDias(Locale idioma) {
        String[] nombres = new String[7];
        SimpleDateFormat f = new SimpleDateFormat("EEEE", idioma);
        for (int i = 0; i < 7; i++) {
            Calendar c = (Calendar) lunes.clone();
            c.add(Calendar.DAY_OF_YEAR, i);
            nombres[i] = f.format(c.getTime());
        }
        return nombres;
    }

    // «lunes, miércoles y viernes».
    private String unir(List<String> nombres) {
        return Enumeracion.unir(nombres, getString(R.string.enumeracion_y));
    }

    private int attrPrimario() {
        return color(androidx.appcompat.R.attr.colorPrimary);
    }

    private int color(int atributo) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(atributo, tv, true);
        return tv.data;
    }
}
