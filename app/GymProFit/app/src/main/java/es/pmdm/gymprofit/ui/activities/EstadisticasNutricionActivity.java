package es.pmdm.gymprofit.ui.activities;

import android.graphics.Color;
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
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.comida.ResumenDiarioNutricion;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ComidaApi;
import es.pmdm.gymprofit.utils.ChartMarker;
import es.pmdm.gymprofit.utils.ChartStyler;
import es.pmdm.gymprofit.utils.PreferencesManager;

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
    private ChipGroup chipsPeriodo;
    private BarChart chartKcal;
    private PieChart chartMacros;
    private TextView tvMacrosVacia;
    private GridLayout gridHeatmap;

    // Colores del donut de macros (proteína / carbohidratos / grasa): 3 tonos distintos.
    private static final int[] MACRO_COLORS = {
            Color.parseColor("#FF6A00"), Color.parseColor("#2DD4BF"), Color.parseColor("#A78BFA")
    };
    // Heatmap de adherencia: en objetivo (verde) / fuera (ámbar); sin datos = surface del tema.
    private static final int COLOR_ON  = Color.parseColor("#22C55E");
    private static final int COLOR_OFF = Color.parseColor("#FFB300");

    // Inicio de la ventana temporal actual (para recorrer día a día en el heatmap).
    private long windowStartMillis;

    private PreferencesManager prefs;
    private final ComidaApi comidaApi = ApiClient.service(ComidaApi.class);

    // Longitud del periodo en días (7 = semana, 30 = mes) y desplazamiento hacia atrás
    // en periodos (0 = actual, 1 = anterior, ...). El objetivo diario de kcal del perfil.
    private int lenDias = 7;
    private int offset = 0;
    private int objetivoKcal = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PreferencesManager(this);
        prefs.applyTheme();
        setContentView(R.layout.activity_estadisticas_nutricion);

        objetivoKcal = prefs.getCaloriasDiarias();

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

        // Swatches de la leyenda del heatmap.
        int colorNone = getColorTema(com.google.android.material.R.attr.colorSurfaceVariant);
        pintarSwatch(R.id.legNone, colorNone);
        pintarSwatch(R.id.legOff, COLOR_OFF);
        pintarSwatch(R.id.legOn, COLOR_ON);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Cambio de periodo (semana/mes): reinicia el desplazamiento al periodo actual.
        chipsPeriodo.setOnCheckedStateChangeListener((g, ids) -> {
            lenDias = (chipsPeriodo.getCheckedChipId() == R.id.chipMes) ? 30 : 7;
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
        SimpleDateFormat corta = new SimpleDateFormat("dd/MM", Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -offset * lenDias);
        String fin = iso.format(cal.getTime());
        String finCorta = corta.format(cal.getTime());

        cal.add(Calendar.DAY_OF_YEAR, -(lenDias - 1));
        String inicio = iso.format(cal.getTime());
        String inicioCorta = corta.format(cal.getTime());
        windowStartMillis = cal.getTimeInMillis();

        tvPeriodo.setText(inicioCorta + " – " + finCorta);

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

        tvMedia.setText(String.valueOf(media));
        tvDias.setText(String.valueOf(diasReg));
        tvObjetivo.setText(String.valueOf(enObjetivo));
        tvAdherencia.setText(adherencia + "%");

        renderMacros(lista);
        construirHeatmap(lista);

        if (lista.isEmpty()) {
            chartKcal.setVisibility(View.GONE);
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
        meta.setLineColor(getColorTema(com.google.android.material.R.attr.colorPrimary));
        meta.setLineWidth(1.4f);
        meta.enableDashedLine(12f, 8f, 0f);
        meta.setTextColor(getColorTema(com.google.android.material.R.attr.colorOnSurfaceVariant));
        meta.setTextSize(9f);
        chartKcal.getAxisLeft().addLimitLine(meta);

        // Tooltip: kcal + fecha.
        chartKcal.setMarker(new ChartMarker(this, (e, h) -> {
            int idx = Math.round(e.getX());
            String fecha = (idx >= 0 && idx < etiquetas.size()) ? etiquetas.get(idx) : "";
            return String.format(Locale.getDefault(), "%d kcal\n%s", (int) e.getY(), fecha);
        }));

        BarDataSet ds = new BarDataSet(entradas, "kcal");
        ChartStyler.styleBarDataSet(ds, this);
        BarData data = new BarData(ds);
        data.setBarWidth(0.6f);
        chartKcal.setData(data);
        chartKcal.invalidate();
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
        chartMacros.setCenterText(String.format(Locale.getDefault(), "%.0f g", prot + carb + grasa));

        PieDataSet ds = new PieDataSet(entradas, "");
        ChartStyler.stylePieDataSet(ds, MACRO_COLORS);
        PieData data = new PieData(ds);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f g", value);
            }
        });
        chartMacros.setData(data);
        chartMacros.invalidate();
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

        int colorNone = getColorTema(com.google.android.material.R.attr.colorSurfaceVariant);
        double margen = objetivoKcal * 0.15;
        int cell = (int) dp(30), margin = (int) dp(3);

        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(windowStartMillis);

        for (int i = 0; i < lenDias; i++) {
            Integer kcal = porFecha.get(iso.format(cal.getTime()));
            int color;
            if (kcal == null) color = colorNone;
            else if (Math.abs(kcal - objetivoKcal) <= margen) color = COLOR_ON;
            else color = COLOR_OFF;

            View v = new View(this);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(color);
            bg.setCornerRadius(dp(6));
            v.setBackground(bg);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cell;
            lp.height = cell;
            lp.setMargins(margin, margin, margin, margin);
            gridHeatmap.addView(v, lp);

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
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

    // Fecha ISO ("yyyy-MM-dd...") → etiqueta corta "dd/MM".
    private String fechaCorta(String isoStr) {
        if (isoStr == null || isoStr.length() < 10) return "";
        return isoStr.substring(8, 10) + "/" + isoStr.substring(5, 7);
    }

    // Resuelve un color del tema.
    private int getColorTema(int attrRes) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attrRes, tv, true);
        return tv.data;
    }
}
