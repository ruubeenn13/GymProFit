package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.ChartMarker;
import es.pmdm.gymprofit.utils.ChartStyler;
import es.pmdm.gymprofit.utils.InputDialog;
import es.pmdm.gymprofit.model.medicion.MedicionCorporal;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.MedicionApi;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// MedicionesActivity — pantalla de mediciones corporales del usuario.
// Muestra la última medición registrada (peso, altura, grasa, músculo,
// perímetros...) y permite editar cada campo individualmente o crear una
// medición inicial a partir de los datos del perfil dentro de GymProFit.
// ============================================================
public class MedicionesActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private View tvVacio;
    private View scrollMediciones;
    private TextView tvFechaUltima;
    private TextView tvPesoVal, tvGrasaVal, tvMusculoVal;
    private TextView tvCinturaVal, tvPechoVal, tvBrazosVal, tvPiernasVal, tvNotasVal;
    private TextView tvMasDetalles;
    private View layoutMasDetalles;
    private boolean masDetallesAbierto = false;
    private MaterialCardView cardGraficaPeso;
    private LineChart chartPeso;
    private TextView tvGraficaTitulo;
    private TextView tvGraficaVacia;
    private ChipGroup chipsMetrica, chipsRango;
    private List<MedicionCorporal> historial;
    // Métricas representables en la gráfica (solo las que la gente mide de verdad).
    private static final int MET_PESO = 0, MET_GRASA = 1, MET_CINTURA = 2;
    private PreferencesManager prefsManager;
    private MedicionCorporal ultimaMedicion;
    // Interfaz Retrofit tipada del dominio mediciones (etapa 2)
    private final MedicionApi medicionApi = ApiClient.service(MedicionApi.class);
    // Interfaz Retrofit tipada del dominio usuarios (etapa 2)
    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);

    // Aplica tema/idioma, referencia las vistas, registra el launcher para
    // el registro de nuevas mediciones y carga la última medición existente.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        setContentView(R.layout.activity_mediciones);

        tvVacio         = findViewById(R.id.tvVacio);
        scrollMediciones = findViewById(R.id.scrollMediciones);
        tvFechaUltima   = findViewById(R.id.tvFechaUltima);
        tvPesoVal       = findViewById(R.id.tvPesoVal);
        tvGrasaVal      = findViewById(R.id.tvGrasaVal);
        tvMusculoVal    = findViewById(R.id.tvMusculoVal);
        tvCinturaVal    = findViewById(R.id.tvCinturaVal);
        tvPechoVal      = findViewById(R.id.tvPechoVal);
        tvBrazosVal     = findViewById(R.id.tvBrazosVal);
        tvPiernasVal    = findViewById(R.id.tvPiernasVal);
        tvNotasVal      = findViewById(R.id.tvNotasVal);
        cardGraficaPeso = findViewById(R.id.cardGraficaPeso);
        chartPeso       = findViewById(R.id.chartPeso);
        tvGraficaVacia  = findViewById(R.id.tvGraficaVacia);
        tvGraficaTitulo = findViewById(R.id.tvGraficaTitulo);
        chipsMetrica    = findViewById(R.id.chipsMetrica);
        chipsRango      = findViewById(R.id.chipsRango);
        tvMasDetalles   = findViewById(R.id.tvMasDetalles);
        layoutMasDetalles = findViewById(R.id.layoutMasDetalles);

        // Re-pinta la gráfica al cambiar de métrica (peso/grasa/cintura) o de rango (30/90/todo).
        chipsMetrica.setOnCheckedStateChangeListener((g, ids) -> pintarGrafica());
        chipsRango.setOnCheckedStateChangeListener((g, ids) -> pintarGrafica());

        // Feedback háptico sutil al seleccionar un punto (aparece el tooltip).
        chartPeso.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override public void onValueSelected(Entry e, Highlight h) {
                chartPeso.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
            @Override public void onNothingSelected() { }
        });

        // Toggle de medidas avanzadas (músculo/pecho/brazos/piernas): la mayoría no
        // las mide, así que van ocultas tras "Más detalles" para no ensuciar la vista.
        findViewById(R.id.rowMasDetalles).setOnClickListener(v -> {
            masDetallesAbierto = !masDetallesAbierto;
            layoutMasDetalles.setVisibility(masDetallesAbierto ? View.VISIBLE : View.GONE);
            tvMasDetalles.setText(masDetallesAbierto
                    ? R.string.medicion_menos_detalles : R.string.medicion_mas_detalles);
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        // Sin FAB: registrar la primera medición desde el estado vacío; el resto se
        // edita in-place tocando cada fila (upsert por día al guardar).
        findViewById(R.id.btnRegistrarPrimera).setOnClickListener(v -> registrarPrimeraMedicion());

        configurarFilas();
        cargarMedicion();
    }

    // Configura los listeners de cada fila (peso, altura, grasa, etc.) para
    // abrir el diálogo de edición del campo correspondiente al pulsarla.
    private void configurarFilas() {
        findViewById(R.id.rowPeso).setOnClickListener(v ->
                editarCampoNumerico("peso", getString(R.string.perfil_peso),
                        ultimaMedicion != null ? ultimaMedicion.getPeso() : 0));

        findViewById(R.id.rowGrasa).setOnClickListener(v ->
                editarCampoNumerico("grasaCorporal", getString(R.string.medicion_label_grasa),
                        ultimaMedicion != null ? ultimaMedicion.getGrasaCorporal() : 0));

        findViewById(R.id.rowMusculo).setOnClickListener(v ->
                editarCampoNumerico("masaMuscular", getString(R.string.medicion_label_musculo),
                        ultimaMedicion != null ? ultimaMedicion.getMasaMuscular() : 0));

        findViewById(R.id.rowCintura).setOnClickListener(v ->
                editarCampoNumerico("cintura", getString(R.string.medicion_label_cintura),
                        ultimaMedicion != null ? ultimaMedicion.getCintura() : 0));

        findViewById(R.id.rowPecho).setOnClickListener(v ->
                editarCampoNumerico("pecho", getString(R.string.medicion_label_pecho),
                        ultimaMedicion != null ? ultimaMedicion.getPecho() : 0));

        findViewById(R.id.rowBrazos).setOnClickListener(v ->
                editarCampoNumerico("brazos", getString(R.string.medicion_label_brazos),
                        ultimaMedicion != null ? ultimaMedicion.getBrazos() : 0));

        findViewById(R.id.rowPiernas).setOnClickListener(v ->
                editarCampoNumerico("piernas", getString(R.string.medicion_label_piernas),
                        ultimaMedicion != null ? ultimaMedicion.getPiernas() : 0));

        findViewById(R.id.rowNotas).setOnClickListener(v ->
                editarCampoTexto("notas", getString(R.string.medicion_label_notas),
                        ultimaMedicion != null && ultimaMedicion.getNotas() != null
                                ? ultimaMedicion.getNotas() : ""));
    }

    // Obtiene la lista de mediciones del usuario y muestra la más reciente;
    // si no hay ninguna, intenta crear una a partir de los datos del perfil.
    private void cargarMedicion() {
        int usuarioId = prefsManager.getUsuarioId();
        if (usuarioId == -1) return;

        // Spinner durante la carga (pantalla en blanco hasta responder).
        LoadingDialog.show(this);

        // Respuesta ya deserializada a POJOs por Gson (sin UtilJSONParser).
        medicionApi.getOrdenadas(usuarioId).enqueue(new ApiCallback<List<MedicionCorporal>>() {
            @Override
            public void onOk(List<MedicionCorporal> lista) {
                if (lista == null || lista.isEmpty()) {
                    intentarCrearDesdePerfil(usuarioId);
                } else {
                    ultimaMedicion = lista.get(0);
                    historial = lista;
                    mostrarMedicion();
                    pintarGrafica();
                }
            }

            @Override
            public void onFail(int code, String message) {
                // 404 = sin mediciones (vacío benigno) → intenta sembrar desde el perfil.
                // -1/500 = error real de lectura → toast (UiFeedback silencia el 404).
                UiFeedback.toastError(MedicionesActivity.this, code, message);
                intentarCrearDesdePerfil(usuarioId);
            }
        });
    }

    // Si el usuario no tiene mediciones, crea una inicial usando el peso/altura
    // guardados en su perfil (si existen); si no, muestra el estado vacío.
    private void intentarCrearDesdePerfil(int usuarioId) {
        // Perfil ya deserializado a Usuario por Gson (sin UtilJSONParser); ApiCallback entrega en hilo UI.
        usuarioApi.getPorId(usuarioId).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                try {
                    String pesoStr = (u != null && u.getPeso() != null) ? u.getPeso().trim() : "";
                    if (!pesoStr.isEmpty() && !pesoStr.equals("null")) {
                        Map<String, Object> body = new HashMap<>();
                        body.put("usuarioId", usuarioId);
                        body.put("peso", new BigDecimal(pesoStr));
                        if (u.getAltura() > 0) body.put("altura", u.getAltura());

                        medicionApi.crear(body).enqueue(new ApiCallback<MedicionCorporal>() {
                            @Override
                            public void onOk(MedicionCorporal m) {
                                cargarMedicion();
                            }

                            @Override
                            public void onFail(int code, String msg) {
                                Log.e("MedicionesActivity", "Error al crear medición desde perfil: " + msg);
                                mostrarVacio();
                            }
                        });
                    } else {
                        mostrarVacio();
                    }
                } catch (Exception e) {
                    mostrarVacio();
                }
            }

            @Override
            public void onFail(int code, String message) {
                mostrarVacio();
            }
        });
    }

    // Pinta la gráfica según la métrica (peso/grasa/cintura) y el rango (30/90/todo)
    // seleccionados en los chips. La API entrega el histórico DESC por fecha → se
    // recorre al revés para dibujar de antiguo a reciente. Solo se muestra si quedan
    // ≥2 puntos con valor (una línea necesita 2). Tooltip al tocar cada punto.
    private void pintarGrafica() {
        if (historial == null || historial.isEmpty()) {
            cardGraficaPeso.setVisibility(View.GONE);
            return;
        }
        cardGraficaPeso.setVisibility(View.VISIBLE);

        // Métrica + unidad según el chip activo.
        final int metrica;
        final String unidad;
        if (chipsMetrica.getCheckedChipId() == R.id.chipMetGrasa) { metrica = MET_GRASA; unidad = "%"; }
        else if (chipsMetrica.getCheckedChipId() == R.id.chipMetCintura) { metrica = MET_CINTURA; unidad = "cm"; }
        else { metrica = MET_PESO; unidad = "kg"; }

        // Rango: fecha de corte (null = "Todo").
        String corte = null;
        if (chipsRango.getCheckedChipId() == R.id.chipRango30) corte = fechaCutoff(30);
        else if (chipsRango.getCheckedChipId() == R.id.chipRango90) corte = fechaCutoff(90);

        List<Entry> entradas = new ArrayList<>();
        final List<String> etiquetas = new ArrayList<>();
        for (int i = historial.size() - 1; i >= 0; i--) {
            MedicionCorporal m = historial.get(i);
            double v = valorMetrica(m, metrica);
            if (v <= 0) continue;
            if (corte != null) {
                String f = m.getFecha();
                if (f == null || f.length() < 10 || f.substring(0, 10).compareTo(corte) < 0) continue;
            }
            entradas.add(new Entry(entradas.size(), (float) v));
            etiquetas.add(fechaCorta(m.getFecha()));
        }

        tvGraficaTitulo.setText(tituloMetrica(metrica));

        // La card y los chips SIEMPRE se ven (hay histórico); si la métrica elegida no
        // tiene ≥2 puntos, se muestra el estado vacío dentro del área en vez de la línea
        // (así el usuario puede volver a otra métrica sin que desaparezca la card).
        if (entradas.size() < 2) {
            chartPeso.setVisibility(View.GONE);
            tvGraficaVacia.setVisibility(View.VISIBLE);
            return;
        }
        chartPeso.setVisibility(View.VISIBLE);
        tvGraficaVacia.setVisibility(View.GONE);

        ChartStyler.styleLine(chartPeso, new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int idx = Math.round(value);
                return (idx >= 0 && idx < etiquetas.size()) ? etiquetas.get(idx) : "";
            }
        });

        // Tooltip: valor + unidad + fecha del punto tocado.
        chartPeso.setMarker(new ChartMarker(this, (e, h) -> {
            int idx = Math.round(e.getX());
            String fecha = (idx >= 0 && idx < etiquetas.size()) ? etiquetas.get(idx) : "";
            return String.format(Locale.getDefault(), "%.1f %s\n%s", e.getY(), unidad, fecha);
        }));

        LineDataSet ds = new LineDataSet(entradas, "m");
        ChartStyler.styleLineDataSet(ds, this);
        chartPeso.setData(new LineData(ds));
        chartPeso.invalidate();
    }

    // Valor de la métrica pedida para una medición (0 si no registrada).
    private double valorMetrica(MedicionCorporal m, int metrica) {
        switch (metrica) {
            case MET_GRASA:   return m.getGrasaCorporal();
            case MET_CINTURA: return m.getCintura();
            default:          return m.getPeso();
        }
    }

    // Título (eyebrow) de la gráfica según la métrica.
    private String tituloMetrica(int metrica) {
        switch (metrica) {
            case MET_GRASA:   return getString(R.string.grafica_titulo_grasa);
            case MET_CINTURA: return getString(R.string.grafica_titulo_cintura);
            default:          return getString(R.string.medicion_grafica_peso);
        }
    }

    // Fecha de corte "yyyy-MM-dd" = hoy menos N días (para el filtro de rango por comparación de strings ISO).
    private String fechaCutoff(int dias) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -dias);
        return String.format(Locale.US, "%tF", c);
    }

    // Convierte una fecha ISO ("yyyy-MM-ddTHH:mm:ss") a etiqueta corta "dd/MM" para el eje X.
    private String fechaCorta(String iso) {
        if (iso == null || iso.length() < 10) return "";
        return iso.substring(8, 10) + "/" + iso.substring(5, 7);
    }

    // Oculta el scroll de datos y muestra el mensaje de "sin mediciones".
    private void mostrarVacio() {
        LoadingDialog.hide(this);
        scrollMediciones.setVisibility(View.GONE);
        tvVacio.setVisibility(View.VISIBLE);
    }

    // Rellena todos los TextView con los valores de la última medición,
    // mostrando el texto "añadir" en los campos aún no registrados.
    private void mostrarMedicion() {
        LoadingDialog.hide(this);
        tvVacio.setVisibility(View.GONE);
        scrollMediciones.setVisibility(View.VISIBLE);

        String anadir = getString(R.string.medicion_anadir);

        if (ultimaMedicion.getFecha() != null && !ultimaMedicion.getFecha().isEmpty()) {
            tvFechaUltima.setText(getString(R.string.medicion_fecha_ultima,
                    FechaUtils.formatearFechaHora(ultimaMedicion.getFecha())));
        }

        tvPesoVal.setText(ultimaMedicion.getPeso() > 0
                ? String.format(Locale.getDefault(), "%.1f kg", ultimaMedicion.getPeso()) : anadir);
        tvGrasaVal.setText(ultimaMedicion.getGrasaCorporal() > 0
                ? String.format(Locale.getDefault(), "%.1f%%", ultimaMedicion.getGrasaCorporal()) : anadir);
        tvMusculoVal.setText(ultimaMedicion.getMasaMuscular() > 0
                ? String.format(Locale.getDefault(), "%.1f kg", ultimaMedicion.getMasaMuscular()) : anadir);
        tvCinturaVal.setText(ultimaMedicion.getCintura() > 0
                ? String.format(Locale.getDefault(), "%.1f cm", ultimaMedicion.getCintura()) : anadir);
        tvPechoVal.setText(ultimaMedicion.getPecho() > 0
                ? String.format(Locale.getDefault(), "%.1f cm", ultimaMedicion.getPecho()) : anadir);
        tvBrazosVal.setText(ultimaMedicion.getBrazos() > 0
                ? String.format(Locale.getDefault(), "%.1f cm", ultimaMedicion.getBrazos()) : anadir);
        tvPiernasVal.setText(ultimaMedicion.getPiernas() > 0
                ? String.format(Locale.getDefault(), "%.1f cm", ultimaMedicion.getPiernas()) : anadir);
        tvNotasVal.setText(ultimaMedicion.getNotas() != null && !ultimaMedicion.getNotas().isEmpty()
                ? ultimaMedicion.getNotas() : anadir);
    }

    // Diálogo estilizado (InputDialog) para editar un campo numérico y guardarlo.
    // Título = nombre del campo; label del campo = instrucción (no se duplica el nombre);
    // la unidad va como sufijo dentro del input.
    private void editarCampoNumerico(String campo, String label, double valorActual) {
        if (ultimaMedicion == null) return;

        String inicial = valorActual > 0 ? String.format(Locale.getDefault(), "%.2f", valorActual) : null;
        InputDialog.numerico(this, label, getString(R.string.dialogo_nuevo_valor), inicial, unidadDe(campo), valor -> {
            if (valor.isEmpty()) {
                if ("peso".equals(campo)) {
                    UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido));
                }
                return;
            }
            patchCampo(campo, valor, false);
        });
    }

    // Diálogo estilizado (InputDialog) para editar un campo de texto (notas).
    private void editarCampoTexto(String campo, String label, String valorActual) {
        if (ultimaMedicion == null) return;

        InputDialog.texto(this, label, getString(R.string.dialogo_escribe_nota), valorActual,
                valor -> patchCampo(campo, valor, true));
    }

    // Unidad del campo de medición (para el sufijo del input).
    private String unidadDe(String campo) {
        switch (campo) {
            case "grasaCorporal": return "%";
            case "peso":
            case "masaMuscular": return "kg";
            default:              return "cm";   // cintura, pecho, brazos, piernas
        }
    }

    // Guarda el campo editado con UPSERT POR DÍA: si la última medición es de HOY,
    // hace PATCH sobre ella; si es de otro día, crea una NUEVA medición de hoy clonando
    // la última + el campo editado (así cada día es un punto nuevo en la gráfica sin FAB).
    private void patchCampo(String campo, String valorStr, boolean esTexto) {
        try {
            // Valor del campo (texto vacío → null explícito para borrarlo; Gson serializa nulls).
            Object valor = esTexto ? (valorStr.isEmpty() ? null : valorStr) : new BigDecimal(valorStr);

            LoadingDialog.show(this);
            if (esDeHoy(ultimaMedicion)) {
                Map<String, Object> body = new HashMap<>();
                body.put(campo, valor);
                medicionApi.patch(ultimaMedicion.getId(), body).enqueue(recargaCallback());
            } else {
                Map<String, Object> body = clonarUltima();
                body.put(campo, valor);
                medicionApi.crear(body).enqueue(recargaCallback());
            }
        } catch (NumberFormatException e) {
            UIHelper.mostrarToastError(this, getString(R.string.error_conexion));
        }
    }

    // ¿La medición es de hoy? (compara la parte de fecha yyyy-MM-dd con la de hoy).
    private boolean esDeHoy(MedicionCorporal m) {
        if (m == null || m.getFecha() == null || m.getFecha().length() < 10) return false;
        return m.getFecha().substring(0, 10).equals(fechaCutoff(0));
    }

    // Cuerpo para crear la medición de hoy clonando los valores actuales de la última.
    private Map<String, Object> clonarUltima() {
        Map<String, Object> body = new HashMap<>();
        body.put("usuarioId", prefsManager.getUsuarioId());
        if (ultimaMedicion.getPeso() > 0)          body.put("peso", BigDecimal.valueOf(ultimaMedicion.getPeso()));
        if (ultimaMedicion.getAltura() > 0)        body.put("altura", BigDecimal.valueOf(ultimaMedicion.getAltura()));
        if (ultimaMedicion.getGrasaCorporal() > 0) body.put("grasaCorporal", BigDecimal.valueOf(ultimaMedicion.getGrasaCorporal()));
        if (ultimaMedicion.getMasaMuscular() > 0)  body.put("masaMuscular", BigDecimal.valueOf(ultimaMedicion.getMasaMuscular()));
        if (ultimaMedicion.getCintura() > 0)       body.put("cintura", BigDecimal.valueOf(ultimaMedicion.getCintura()));
        if (ultimaMedicion.getPecho() > 0)         body.put("pecho", BigDecimal.valueOf(ultimaMedicion.getPecho()));
        if (ultimaMedicion.getBrazos() > 0)        body.put("brazos", BigDecimal.valueOf(ultimaMedicion.getBrazos()));
        if (ultimaMedicion.getPiernas() > 0)       body.put("piernas", BigDecimal.valueOf(ultimaMedicion.getPiernas()));
        if (ultimaMedicion.getNotas() != null && !ultimaMedicion.getNotas().isEmpty())
            body.put("notas", ultimaMedicion.getNotas());
        return body;
    }

    // Callback común: recarga la pantalla al terminar (éxito) o muestra el error.
    private ApiCallback<MedicionCorporal> recargaCallback() {
        return new ApiCallback<MedicionCorporal>() {
            @Override
            public void onOk(MedicionCorporal m) {
                setResult(RESULT_OK);
                cargarMedicion();   // recarga y oculta el spinner en su punto terminal
            }
            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(MedicionesActivity.this);
                UiFeedback.toastError(MedicionesActivity.this, code, message);
            }
        };
    }

    // Abre el diálogo para registrar la PRIMERA medición (peso) cuando no hay ninguna.
    private void registrarPrimeraMedicion() {
        InputDialog.numerico(this, getString(R.string.perfil_peso),
                getString(R.string.dialogo_nuevo_valor), null, "kg", valor -> {
            if (valor.isEmpty()) return;
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("usuarioId", prefsManager.getUsuarioId());
                body.put("peso", new BigDecimal(valor));
                LoadingDialog.show(this);
                medicionApi.crear(body).enqueue(recargaCallback());
            } catch (NumberFormatException e) {
                UIHelper.mostrarToastError(this, getString(R.string.error_conexion));
            }
        });
    }
}
