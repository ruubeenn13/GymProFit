package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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
        btnNext      = findViewById(R.id.btnNext);

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
