package es.pmdm.gymprofit.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.comida.Comida;
import es.pmdm.gymprofit.model.comida.ResumenDiarioNutricion;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ComidaApi;
import es.pmdm.gymprofit.ui.activities.ComidaActivity;
import es.pmdm.gymprofit.utils.CalculadoraNutricional;
import es.pmdm.gymprofit.utils.ChartMarker;
import es.pmdm.gymprofit.utils.ChartStyler;
import es.pmdm.gymprofit.utils.ResultadoNutricional;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// NutricionFragment — pestaña de seguimiento nutricional.
// Muestra calorías y macros del día, con cards por tipo de comida que abren
// ComidaActivity para registrar/editar los alimentos de cada comida.
// ============================================================
public class NutricionFragment extends BaseFragment {
    private ProgressBar progressCalorias, progressProteinas, progressCarbos, progressGrasas;
    private TextView tvCaloriasActuales, tvCaloriasObjetivo;
    private TextView tvProteinasActuales, tvCarbosActuales, tvGrasasActuales;
    private TextView tvSubDesayuno, tvSubAlmuerzo, tvSubComida, tvSubMerienda, tvSubCena;

    private int objetivoCalorias = 2000, objetivoProteinas = 150, objetivoCarbos = 250, objetivoGrasas = 65;
    private final Map<String, Comida> comidasHoy = new HashMap<>();

    // Gráfica de histórico de calorías por día (barras).
    private BarChart chartNutricion;
    private TextView tvNutricionVacia;
    private ChipGroup chipsRangoNutri;

    private final ComidaApi comidaApi = ApiClient.service(ComidaApi.class);

    private ActivityResultLauncher<Intent> comidaLauncher;

    // Registra el launcher de ComidaActivity antes de STARTED.
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        comidaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        recalcularObjetivos();
                        cargarComidasHoy();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_nutricion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMenuButton();
        inicializarVistas();
        configurarCardsComida();
    }

    // Recalcula objetivos y recarga las comidas de hoy al volver a primer plano.
    @Override
    public void onResume() {
        super.onResume();
        recalcularObjetivos();
        cargarComidasHoy();
        cargarHistorial();
    }

    // ── Vistas ───────────────────────────────────────────────────────────────

    private void inicializarVistas() {
        progressCalorias   = findViewById(R.id.progressCalorias);
        progressProteinas  = findViewById(R.id.progressProteinas);
        progressCarbos     = findViewById(R.id.progressCarbos);
        progressGrasas     = findViewById(R.id.progressGrasas);
        tvCaloriasActuales = findViewById(R.id.tvCaloriasActuales);
        tvCaloriasObjetivo = findViewById(R.id.tvCaloriasObjetivo);
        tvProteinasActuales = findViewById(R.id.tvProteinasActuales);
        tvCarbosActuales    = findViewById(R.id.tvCarbosActuales);
        tvGrasasActuales    = findViewById(R.id.tvGrasasActuales);
        tvSubDesayuno  = findViewById(R.id.tvSubDesayuno);
        tvSubAlmuerzo  = findViewById(R.id.tvSubAlmuerzo);
        tvSubComida    = findViewById(R.id.tvSubComida);
        tvSubMerienda  = findViewById(R.id.tvSubMerienda);
        tvSubCena      = findViewById(R.id.tvSubCena);

        chartNutricion    = findViewById(R.id.chartNutricion);
        tvNutricionVacia  = findViewById(R.id.tvNutricionVacia);
        chipsRangoNutri   = findViewById(R.id.chipsRangoNutri);
        // Recarga el histórico al cambiar de rango (7/30 días).
        chipsRangoNutri.setOnCheckedStateChangeListener((g, ids) -> cargarHistorial());
    }

    // ── Objetivos nutricionales ──────────────────────────────────────────────

    // Recalcula los objetivos nutricionales a partir de los datos del perfil.
    private void recalcularObjetivos() {
        double peso      = prefsManager.getPeso();
        double altura    = prefsManager.getAltura();
        int edad         = prefsManager.getEdad();
        boolean hombre   = "HOMBRE".equals(prefsManager.getSexo());
        String actividad = prefsManager.getActividad();
        String objetivo  = prefsManager.getObjetivo();

        ResultadoNutricional r = CalculadoraNutricional.calcular(peso, altura, edad, hombre, actividad, objetivo);
        objetivoCalorias  = r.calorias;
        objetivoProteinas = r.proteinas;
        objetivoCarbos    = r.carbohidratos;
        objetivoGrasas    = r.grasas;

        prefsManager.saveResultadoNutricional(objetivoCalorias, objetivoProteinas, objetivoCarbos, objetivoGrasas, r.agua);
        tvCaloriasObjetivo.setText("/ " + objetivoCalorias + " kcal");
    }

    // ── Carga de comidas ─────────────────────────────────────────────────────

    // Obtiene las comidas del día actual desde la API y actualiza la UI.
    private void cargarComidasHoy() {
        int usuarioId = prefsManager.getUsuarioId();
        final String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        comidaApi.getDeUsuarioFecha(usuarioId, hoy).enqueue(new ApiCallback<List<Comida>>() {
            @Override
            public void onOk(List<Comida> lista) {
                if (!isAdded()) return;
                comidasHoy.clear();
                if (lista != null) {
                    for (Comida c : lista) {
                        comidasHoy.put(c.getTipoComida(), c);
                    }
                }
                actualizarUI(hoy);
            }

            @Override
            public void onFail(int code, String message) {
                if (!isAdded()) return;
                comidasHoy.clear();
                actualizarUI(hoy);
                if (code != 404) {
                    UiFeedback.toastError(requireActivity(), code, message);
                }
            }
        });
    }

    // ── Actualización de la UI ────────────────────────────────────────────────

    // Actualiza totales, barras de progreso, macros y subtítulos de cards.
    private void actualizarUI(String fecha) {
        int totalCal = 0;
        double totalProt = 0, totalCarb = 0, totalGras = 0;

        for (Comida c : comidasHoy.values()) {
            totalCal  += c.getTotalCalorias();
            totalProt += c.getTotalProteinas();
            totalCarb += c.getTotalCarbohidratos();
            totalGras += c.getTotalGrasas();
        }

        tvCaloriasActuales.setText(String.valueOf(totalCal));

        progressCalorias.setProgress(Math.min(100, (int) (totalCal * 100.0 / objetivoCalorias)));
        progressProteinas.setProgress(Math.min(100, (int) (totalProt * 100.0 / objetivoProteinas)));
        progressCarbos.setProgress(Math.min(100, (int) (totalCarb * 100.0 / objetivoCarbos)));
        progressGrasas.setProgress(Math.min(100, (int) (totalGras * 100.0 / objetivoGrasas)));

        int colorNormal = getAttrColor(com.google.android.material.R.attr.colorOnSurface);
        int colorError  = getAttrColor(com.google.android.material.R.attr.colorError);

        tvProteinasActuales.setText(String.format(Locale.getDefault(), "%.0fg", totalProt));
        tvProteinasActuales.setTextColor(totalProt > objetivoProteinas ? colorError : colorNormal);

        tvCarbosActuales.setText(String.format(Locale.getDefault(), "%.0fg", totalCarb));
        tvCarbosActuales.setTextColor(totalCarb > objetivoCarbos ? colorError : colorNormal);

        tvGrasasActuales.setText(String.format(Locale.getDefault(), "%.0fg", totalGras));
        tvGrasasActuales.setTextColor(totalGras > objetivoGrasas ? colorError : colorNormal);

        actualizarSubtituloCard("DESAYUNO", tvSubDesayuno, fecha);
        actualizarSubtituloCard("ALMUERZO", tvSubAlmuerzo, fecha);
        actualizarSubtituloCard("COMIDA",   tvSubComida,   fecha);
        actualizarSubtituloCard("MERIENDA", tvSubMerienda, fecha);
        actualizarSubtituloCard("CENA",     tvSubCena,     fecha);
    }

    // Actualiza el subtítulo de la card de un tipo de comida.
    private void actualizarSubtituloCard(String tipo, TextView tvSub, String fecha) {
        Comida c = comidasHoy.get(tipo);
        if (c != null && c.getTotalCalorias() > 0) {
            tvSub.setText(c.getTotalCalorias() + " kcal");
        } else {
            tvSub.setText(getString(R.string.sin_registrar));
        }
    }

    // Resuelve un color de atributo del tema actual.
    private int getAttrColor(int attrResId) {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(attrResId, tv, true);
        return tv.data;
    }

    // ── Histórico de calorías (gráfica de barras) ─────────────────────────────

    // Pide el resumen diario de kcal/macros del rango seleccionado (7/30 días) y lo pinta.
    private void cargarHistorial() {
        if (chartNutricion == null) return;
        int usuarioId = prefsManager.getUsuarioId();
        int dias = (chipsRangoNutri.getCheckedChipId() == R.id.chipNutri7) ? 7 : 30;

        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        String fin = f.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, -(dias - 1));
        String inicio = f.format(cal.getTime());

        comidaApi.getResumen(usuarioId, inicio, fin).enqueue(new ApiCallback<List<ResumenDiarioNutricion>>() {
            @Override
            public void onOk(List<ResumenDiarioNutricion> lista) {
                if (!isAdded()) return;
                pintarHistorial(lista);
            }
            @Override
            public void onFail(int code, String message) {
                if (!isAdded()) return;
                pintarHistorial(null);   // sin datos → estado vacío (no molesta con toast)
            }
        });
    }

    // Dibuja las barras de kcal/día con la línea de objetivo y tooltip al tocar.
    private void pintarHistorial(List<ResumenDiarioNutricion> lista) {
        if (lista == null || lista.isEmpty()) {
            chartNutricion.setVisibility(View.GONE);
            tvNutricionVacia.setVisibility(View.VISIBLE);
            return;
        }
        chartNutricion.setVisibility(View.VISIBLE);
        tvNutricionVacia.setVisibility(View.GONE);

        List<BarEntry> entradas = new ArrayList<>();
        final List<String> etiquetas = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            ResumenDiarioNutricion r = lista.get(i);
            entradas.add(new BarEntry(i, r.getCalorias()));
            etiquetas.add(fechaCorta(r.getFecha()));
        }

        ChartStyler.styleBar(chartNutricion, new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int idx = Math.round(value);
                return (idx >= 0 && idx < etiquetas.size()) ? etiquetas.get(idx) : "";
            }
        });

        // Línea de objetivo diario de calorías (referencia).
        chartNutricion.getAxisLeft().removeAllLimitLines();
        LimitLine meta = new LimitLine(objetivoCalorias, getString(R.string.nutricion_meta_kcal));
        meta.setLineColor(getAttrColor(com.google.android.material.R.attr.colorPrimary));
        meta.setLineWidth(1.4f);
        meta.enableDashedLine(12f, 8f, 0f);
        meta.setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        meta.setTextSize(9f);
        chartNutricion.getAxisLeft().addLimitLine(meta);

        // Tooltip: kcal + fecha del día tocado.
        chartNutricion.setMarker(new ChartMarker(requireContext(), (e, h) -> {
            int idx = Math.round(e.getX());
            String fecha = (idx >= 0 && idx < etiquetas.size()) ? etiquetas.get(idx) : "";
            return String.format(Locale.getDefault(), "%d kcal\n%s", (int) e.getY(), fecha);
        }));

        BarDataSet ds = new BarDataSet(entradas, "kcal");
        ChartStyler.styleBarDataSet(ds, requireContext());
        BarData data = new BarData(ds);
        data.setBarWidth(0.6f);
        chartNutricion.setData(data);
        chartNutricion.invalidate();
    }

    // Fecha ISO ("yyyy-MM-dd...") → etiqueta corta "dd/MM" para el eje X.
    private String fechaCorta(String iso) {
        if (iso == null || iso.length() < 10) return "";
        return iso.substring(8, 10) + "/" + iso.substring(5, 7);
    }

    // ── Cards de comida ───────────────────────────────────────────────────────

    // Configura los listeners de click en cada card de tipo de comida.
    private void configurarCardsComida() {
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        setupCardComida(R.id.cardDesayuno, "DESAYUNO", hoy);
        setupCardComida(R.id.cardAlmuerzo, "ALMUERZO", hoy);
        setupCardComida(R.id.cardComida,   "COMIDA",   hoy);
        setupCardComida(R.id.cardMerienda, "MERIENDA", hoy);
        setupCardComida(R.id.cardCena,     "CENA",     hoy);
    }

    // Asigna el listener de click a una card de comida y lanza ComidaActivity.
    private void setupCardComida(int cardId, String tipo, String fecha) {
        View card = findViewById(cardId);
        card.setOnClickListener(v -> {
            if (!verificarAccesoRegistrado()) return;
            Intent intent = new Intent(requireContext(), ComidaActivity.class);
            intent.putExtra("tipoComida", tipo);
            Comida c = comidasHoy.get(tipo);
            intent.putExtra("comidaId", c != null ? c.getId() : -1);
            intent.putExtra("fecha", fecha);
            comidaLauncher.launch(intent);
        });
    }
}
