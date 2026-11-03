package es.pmdm.gymprofit.ui.activities;


import androidx.core.content.ContextCompat;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.widget.SelectorPildora;
import es.pmdm.gymprofit.model.comida.ResumenDiarioNutricion;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ComidaApi;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.ChartMarker;
import es.pmdm.gymprofit.utils.ChartStyler;
import es.pmdm.gymprofit.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;

// ============================================================
// EstadisticasNutricionActivity — pantalla dedicada de estadísticas de nutrición.
// Descarga el diario de Nutrición llevando aquí el histórico: selector de periodo
// (semana/mes) con navegación anterior/siguiente, tarjetas KPI (media kcal, días
// registrados, en objetivo, adherencia) y gráfica de barras de calorías por día.
// ============================================================
public class EstadisticasNutricionActivity extends BaseActivity {

    private ImageView btnNext;
    private TextView tvPeriodo, tvVacia;
    private TextView tvMedia, tvDias, tvObjetivo, tvAdherencia;
    private SelectorPildora chipsPeriodo;
    private BarChart chartKcal;
    private PieChart chartMacros;
    private TextView tvMacrosVacia;
    private GridLayout gridHeatmap;

    // Colores del sistema (GP-228, revisor): los macros con gp_macro_* (antes, hex a fuego con el
    // naranja de lo pulsable para la proteína); el mapa, «en objetivo» en gp_success y «fuera» en
    // gp_stroke, neutro (un día fuera no es un fallo, DEC-055); «sin datos», solo el contorno.
    private int[] coloresMacros;
    private int colorOn, colorOff, colorSinDatos;

    // Inicio de la ventana temporal actual (para recorrer día a día en el heatmap).
    private long windowStartMillis;

    private PreferencesManager prefs;
    private final ComidaApi comidaApi = ApiClient.service(ComidaApi.class);

    // Longitud del periodo en días (7 = semana, 30 = mes) y desplazamiento hacia atrás
    // en periodos (0 = actual, 1 = anterior, ...). El objetivo diario de kcal del perfil.
    private int lenDias = 7;

    // Semana (0) o mes (1) en el selector: 7 o 30 días.
    private int diasDelPeriodo() {
        return chipsPeriodo.getElegida() == 1 ? 30 : 7;
    }

    // Tras un giro, el selector recupera el periodo elegido: se recarga si no es el que se pintó.
    @Override
    protected void onRestoreInstanceState(@androidx.annotation.NonNull android.os.Bundle estado) {
        super.onRestoreInstanceState(estado);
        if (diasDelPeriodo() != lenDias) {
            lenDias = diasDelPeriodo();
            offset = 0;
            cargar();
        }
    }
    private int offset = 0;
    private int objetivoKcal = 2000;
    // Sin peso ni altura no hay objetivo (GP-103): ni línea de meta ni días «en objetivo».
    private boolean conObjetivo = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PreferencesManager(this);
        prefs.applyTheme();
        setContentView(R.layout.activity_estadisticas_nutricion);

        es.pmdm.gymprofit.utils.ResultadoNutricional objetivo = es.pmdm.gymprofit.utils.DiaNutricion.objetivo(prefs);
        conObjetivo = objetivo != null;
        objetivoKcal = conObjetivo ? objetivo.calorias : 0;

        tvPeriodo    = findViewById(R.id.tvPeriodo);
        tvVacia      = findViewById(R.id.tvVacia);
        tvMedia      = findViewById(R.id.tvKpiMediaValor);
        tvDias       = findViewById(R.id.tvKpiDiasValor);
        tvObjetivo   = findViewById(R.id.tvKpiObjetivoValor);
        tvAdherencia = findViewById(R.id.tvKpiAdherenciaValor);
        chipsPeriodo = findViewById(R.id.chipsPeriodo);
        chartKcal    = findViewById(R.id.chartKcal);
        chartMacros  = findViewById(R.id.chartMacros);
        tvMacrosVacia = findViewById(R.id.tvMacrosVacia);
        gridHeatmap  = findViewById(R.id.gridHeatmap);
        btnNext      = findViewById(R.id.btnNext);

        coloresMacros = new int[]{ ContextCompat.getColor(this, R.color.gp_macro_proteinas),
                ContextCompat.getColor(this, R.color.gp_macro_carbos), ContextCompat.getColor(this, R.color.gp_macro_grasas) };
        colorOn = ContextCompat.getColor(this, R.color.gp_success);
        colorOff = ContextCompat.getColor(this, R.color.gp_stroke);
        colorSinDatos = colorOff;

        // Swatches de la leyenda del heatmap.
        findViewById(R.id.legNone).setBackground(celda(colorSinDatos, false, dp(4)));
        pintarSwatch(R.id.legOff, colorOff);
        pintarSwatch(R.id.legOn, colorOn);
        // Sin objetivo no hay «en objetivo» ni «fuera»: la leyenda no aplica.
        if (!conObjetivo) ((View) findViewById(R.id.legOn).getParent()).setVisibility(View.GONE);

        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());

        // Cambio de periodo (semana/mes): reinicia el desplazamiento al periodo actual.
        chipsPeriodo.setAlElegir(i -> {
            lenDias = diasDelPeriodo();
            offset = 0;
            cargar();
        });

        // Navegación: anterior retrocede un periodo; siguiente avanza (nunca al futuro).
        findViewById(R.id.btnPrev).setOnClickListener(v -> { offset++; cargar(); });
        btnNext.setOnClickListener(v -> { if (offset > 0) { offset--; cargar(); } });

        cargar();
    }

    // Calcula la ventana [inicio, fin] del periodo actual, actualiza la etiqueta y pide los datos.
    private void cargar() {
        // Siguiente deshabilitado en el periodo actual (no se puede ir al futuro).
        btnNext.setVisibility(offset > 0 ? View.VISIBLE : View.INVISIBLE);

        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        // En el idioma de la app (GP-228): «28 sept – 4 oct» / «Sep 28 – Oct 4».
        SimpleDateFormat corta = new SimpleDateFormat(getString(R.string.inicio_fecha_corta_patron),
                FechaUtils.localeDeLaApp(this));

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -offset * lenDias);
        String fin = iso.format(cal.getTime());
        String finCorta = corta.format(cal.getTime());

        cal.add(Calendar.DAY_OF_YEAR, -(lenDias - 1));
        String inicio = iso.format(cal.getTime());
        String inicioCorta = corta.format(cal.getTime());
        windowStartMillis = cal.getTimeInMillis();

        tvPeriodo.setText(getString(R.string.fecha_rango, inicioCorta, finCorta));

        int usuarioId = prefs.getUsuarioId();
        comidaApi.getResumen(usuarioId, inicio, fin).enqueue(new ApiCallback<List<ResumenDiarioNutricion>>() {
            @Override
            public void onOk(List<ResumenDiarioNutricion> lista) {
                if (isFinishing()) return;
                render(lista);
            }
            @Override
            public void onFail(int code, String message) {
                if (isFinishing()) return;
                render(null);
            }
        });
    }

    // Calcula los KPIs y pinta las barras a partir del resumen diario del periodo.
    private void render(List<ResumenDiarioNutricion> lista) {
        if (lista == null) lista = new ArrayList<>();

        // KPIs sobre los días CON registro.
        int diasReg = lista.size();
        long suma = 0;
        int enObjetivo = 0;
        double margen = objetivoKcal * 0.15;
        for (ResumenDiarioNutricion r : lista) {
            suma += r.getCalorias();
            if (Math.abs(r.getCalorias() - objetivoKcal) <= margen) enObjetivo++;
        }
        int media = diasReg > 0 ? Math.round(suma / (float) diasReg) : 0;
        int adherencia = diasReg > 0 ? Math.round(enObjetivo * 100f / diasReg) : 0;

        // Un período sin comidas: el vacío, sin ceros (DESIGN.md); los números, en el idioma de la app.
        java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(this));
        String nada = getString(R.string.sin_valor);
        tvMedia.setText(diasReg > 0 ? nf.format(media) : nada);
        tvDias.setText(diasReg > 0 ? nf.format(diasReg) : nada);
        tvObjetivo.setText(conObjetivo && diasReg > 0 ? nf.format(enObjetivo) : nada);
        tvAdherencia.setText(conObjetivo && diasReg > 0 ? getString(R.string.estadisticas_porcentaje, adherencia) : nada);

        renderMacros(lista);
        construirHeatmap(lista);

        if (lista.isEmpty()) {
            chartKcal.setVisibility(View.GONE);
            // Una cuenta con comidas en otras semanas no «aún no ha registrado comidas».
            tvVacia.setText(R.string.estadisticas_periodo_vacio);
            tvVacia.setVisibility(View.VISIBLE);
            return;
        }
        chartKcal.setVisibility(View.VISIBLE);
        tvVacia.setVisibility(View.GONE);

        List<BarEntry> entradas = new ArrayList<>();
        final List<String> etiquetas = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            ResumenDiarioNutricion r = lista.get(i);
            entradas.add(new BarEntry(i, r.getCalorias()));
            etiquetas.add(fechaCorta(r.getFecha()));
        }

        ChartStyler.styleBar(chartKcal, new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int idx = Math.round(value);
                return (idx >= 0 && idx < etiquetas.size()) ? etiquetas.get(idx) : "";
            }
        });

        // Línea de objetivo de kcal.
        chartKcal.getAxisLeft().removeAllLimitLines();
        LimitLine meta = new LimitLine(objetivoKcal, getString(R.string.nutricion_meta_kcal));
        if (conObjetivo) {
        meta.setLineColor(getColorTema(androidx.appcompat.R.attr.colorPrimary));
        meta.setLineWidth(1.4f);
        meta.enableDashedLine(12f, 8f, 0f);
        meta.setTextColor(getColorTema(com.google.android.material.R.attr.colorOnSurfaceVariant));
        meta.setTextSize(ChartStyler.textoSp(this));
        chartKcal.getAxisLeft().addLimitLine(meta);
        }

        // Tooltip: kcal + fecha.
        chartKcal.setMarker(new ChartMarker(this, (e, h) -> {
            int idx = Math.round(e.getX());
            String fecha = (idx >= 0 && idx < etiquetas.size()) ? etiquetas.get(idx) : "";
            return getString(R.string.grafica_kcal_fecha, (int) e.getY(), fecha);
        }));

        BarDataSet ds = new BarDataSet(entradas, "kcal");
        ChartStyler.styleBarDataSet(ds, this);
        BarData data = new BarData(ds);
        data.setBarWidth(0.6f);
        // El eje empieza en 0 y llega al objetivo: desde el mínimo, 139 kcal parecían diez veces 135 y la
        // línea del objetivo caía fuera (revisor, GP-228). Cifras enteras, en el idioma de la app.
        int maxKcal = 0, minKcal = Integer.MAX_VALUE;
        for (ResumenDiarioNutricion r : lista) {
            maxKcal = Math.max(maxKcal, r.getCalorias());
            minKcal = Math.min(minKcal, r.getCalorias());
        }
        chartKcal.getAxisLeft().setAxisMinimum(0f);
        chartKcal.getAxisLeft().setAxisMaximum(Math.max(maxKcal, conObjetivo ? objetivoKcal : 0) * 1.1f);
        chartKcal.getAxisLeft().setGranularity(1f);
        chartKcal.getAxisLeft().setGranularityEnabled(true);
        chartKcal.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return nf.format(Math.round(value));
            }
        });
        chartKcal.setData(data);
        chartKcal.invalidate();
        // Las barras no son nada para TalkBack sin descripción (DEC-019).
        chartKcal.setContentDescription(conObjetivo
                ? getString(R.string.estadisticas_kcal_a11y, etiquetas.get(0), etiquetas.get(etiquetas.size() - 1),
                        nf.format(minKcal), nf.format(maxKcal), nf.format(objetivoKcal))
                : getString(R.string.estadisticas_kcal_a11y_sin_objetivo, etiquetas.get(0),
                        etiquetas.get(etiquetas.size() - 1), nf.format(minKcal), nf.format(maxKcal)));
    }

    // Donut del reparto de macros (gramos totales de proteína/carbohidratos/grasa del periodo).
    private void renderMacros(List<ResumenDiarioNutricion> lista) {
        double prot = 0, carb = 0, grasa = 0;
        for (ResumenDiarioNutricion r : lista) {
            prot += r.getProteinas();
            carb += r.getCarbohidratos();
            grasa += r.getGrasas();
        }

        if (prot + carb + grasa <= 0) {
            chartMacros.setVisibility(View.GONE);
            tvMacrosVacia.setVisibility(View.VISIBLE);
            return;
        }
        chartMacros.setVisibility(View.VISIBLE);
        tvMacrosVacia.setVisibility(View.GONE);

        List<PieEntry> entradas = new ArrayList<>();
        entradas.add(new PieEntry((float) prot, getString(R.string.macro_prot)));
        entradas.add(new PieEntry((float) carb, getString(R.string.macro_carb)));
        entradas.add(new PieEntry((float) grasa, getString(R.string.macro_grasa)));

        ChartStyler.stylePie(chartMacros);
        chartMacros.setCenterText(getString(R.string.unidad_g_redondeado, prot + carb + grasa));

        PieDataSet ds = new PieDataSet(entradas, "");
        ChartStyler.stylePieDataSet(ds, coloresMacros, this);
        // Blanco sobre los macros de claro y casi negro sobre los de oscuro (el blanco sobre el
        // turquesa daba 1,86:1).
        ds.setValueTextColor(ContextCompat.getColor(this, R.color.gp_texto_sobre_macro));
        PieData data = new PieData(ds);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return getString(R.string.unidad_g_redondeado, value);
            }
        });
        chartMacros.setData(data);
        chartMacros.invalidate();
        chartMacros.setContentDescription(getString(R.string.estadisticas_macros_a11y,
                getString(R.string.unidad_g_redondeado, prot), getString(R.string.unidad_g_redondeado, carb),
                getString(R.string.unidad_g_redondeado, grasa)));
    }

    // Construye el heatmap: una celda por día del periodo, coloreada según si ese día
    // se registró y quedó en objetivo (verde), fuera (ámbar) o sin datos (surface).
    private void construirHeatmap(List<ResumenDiarioNutricion> lista) {
        gridHeatmap.removeAllViews();

        Map<String, Integer> porFecha = new HashMap<>();
        for (ResumenDiarioNutricion r : lista) {
            if (r.getFecha() != null && r.getFecha().length() >= 10) {
                porFecha.put(r.getFecha().substring(0, 10), r.getCalorias());
            }
        }

        double margen = objetivoKcal * 0.15;
        int enObjetivo = 0, fuera = 0, sinDatos = 0;
        int cell = (int) dp(30), margin = (int) dp(3);

        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(windowStartMillis);

        for (int i = 0; i < lenDias; i++) {
            Integer kcal = porFecha.get(iso.format(cal.getTime()));
            int color;
            if (kcal == null) { color = colorSinDatos; sinDatos++; }
            // Sin objetivo (GP-103), un día con comidas es solo eso: registrado.
            else if (!conObjetivo) { color = getColorTema(com.google.android.material.R.attr.colorOnSurfaceVariant); enObjetivo++; }
            else if (Math.abs(kcal - objetivoKcal) <= margen) { color = colorOn; enObjetivo++; }
            else { color = colorOff; fuera++; }

            View v = new View(this);
            // «Sin datos» es solo el contorno: no se distingue solo por el color.
            v.setBackground(celda(color, kcal != null, dp(6)));
            v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cell;
            lp.height = cell;
            lp.setMargins(margin, margin, margin, margin);
            gridHeatmap.addView(v, lp);

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        // El mapa entero para TalkBack: cuántos días hay de cada (DEC-019).
        gridHeatmap.setContentDescription(conObjetivo
                ? getString(R.string.estadisticas_mapa_a11y, enObjetivo, fuera, sinDatos)
                : getString(R.string.estadisticas_mapa_a11y_sin_objetivo, enObjetivo, sinDatos));
    }

    // Una celda del mapa: rellena con su color o, sin datos, solo el contorno.
    private GradientDrawable celda(int color, boolean rellena, float radio) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(radio);
        if (rellena) bg.setColor(color);
        else bg.setStroke(Math.max(1, Math.round(dp(1))), color);
        return bg;
    }

    // Pinta un cuadradito de la leyenda con el color indicado.
    private void pintarSwatch(int viewId, int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(4));
        findViewById(viewId).setBackground(bg);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    // Fecha ISO («yyyy-MM-dd...») → etiqueta corta en el idioma de la app («28 sept» / «Sep 28»).
    private String fechaCorta(String isoStr) {
        String r = FechaUtils.fechaCorta(isoStr, FechaUtils.localeDeLaApp(this),
                getString(R.string.inicio_fecha_corta_patron));
        return r != null ? r : "";
    }

    // Resuelve un color del tema.
    private int getColorTema(int attrRes) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attrRes, tv, true);
        return tv.data;
    }
}
