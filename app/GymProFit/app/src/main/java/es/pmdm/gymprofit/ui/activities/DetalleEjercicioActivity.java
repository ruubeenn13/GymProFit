package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.progreso.ProgresoEjercicio;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ProgresoEjercicioApi;
import es.pmdm.gymprofit.utils.ChartMarker;
import es.pmdm.gymprofit.utils.ChartStyler;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// DetalleEjercicioActivity — pantalla de detalle de un ejercicio.
// Muestra nombre, descripción, instrucciones, estadísticas (músculo,
// nivel, calorías, equipo) y la demostración visual: si el ejercicio
// trae 2 fotogramas (free-exercise-db) se alternan en bucle, animando
// al "monigote" haciendo el ejercicio.
// ============================================================
public class DetalleEjercicioActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    // Milisegundos entre fotogramas de la demostración animada
    private static final long FRAME_MS = 700;

    // Alternador de fotogramas de la demostración
    private final Handler frameHandler = new Handler(Looper.getMainLooper());
    private Runnable frameRunnable;

    // Interfaz Retrofit del progreso por ejercicio (para la gráfica de progresión).
    private final ProgresoEjercicioApi progresoApi = ApiClient.service(ProgresoEjercicioApi.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();
        setContentView(R.layout.activity_detalle_ejercicio);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        String nombre        = getIntent().getStringExtra("nombre");
        String descripcion   = getIntent().getStringExtra("descripcion");
        String instrucciones = getIntent().getStringExtra("instrucciones");
        String grupoMuscular = getIntent().getStringExtra("grupoMuscular");
        String musculoPrim   = getIntent().getStringExtra("musculoPrimario");
        String dificultad    = getIntent().getStringExtra("dificultad");
        int calorias         = getIntent().getIntExtra("calorias", 0);
        String equipo        = getIntent().getStringExtra("equipoNecesario");
        String imagenUrl     = getIntent().getStringExtra("imagenUrl");
        String imagenUrl2    = getIntent().getStringExtra("imagenUrl2");

        poblarVistas(nombre, descripcion, instrucciones, grupoMuscular, musculoPrim, dificultad, calorias, equipo);
        configurarDemostracion(imagenUrl, imagenUrl2);

        int ejercicioId = getIntent().getIntExtra("id", -1);
        cargarProgresion(prefs.getUsuarioId(), ejercicioId);
    }

    // Pide el histórico de progreso del usuario para este ejercicio y lo pinta.
    private void cargarProgresion(int usuarioId, int ejercicioId) {
        if (usuarioId <= 0 || ejercicioId <= 0) return;
        progresoApi.getHistorial(usuarioId, ejercicioId).enqueue(new ApiCallback<List<ProgresoEjercicio>>() {
            @Override
            public void onOk(List<ProgresoEjercicio> lista) {
                if (isFinishing()) return;
                pintarProgresion(lista);
            }
            @Override
            public void onFail(int code, String message) {
                // Sin progreso (404/errores) → la card se queda oculta, sin molestar.
            }
        });
    }

    // Dibuja la evolución del mejor peso; resalta el récord (PR) y da tooltip al tocar.
    private void pintarProgresion(List<ProgresoEjercicio> lista) {
        MaterialCardView card = findViewById(R.id.cardProgresion);
        LineChart chart = findViewById(R.id.chartProgresion);
        if (lista == null) return;

        // La API entrega el histórico DESC (más reciente primero) → se recorre al revés
        // para dibujar de antiguo a reciente (progresión ascendente en el tiempo).
        List<Entry> entradas = new ArrayList<>();
        final List<String> etiquetas = new ArrayList<>();
        float maxPeso = 0f;
        for (int i = lista.size() - 1; i >= 0; i--) {
            ProgresoEjercicio p = lista.get(i);
            if (p.getMejorPeso() <= 0) continue;
            entradas.add(new Entry(entradas.size(), (float) p.getMejorPeso()));
            etiquetas.add(fechaCorta(p.getFecha()));
            if (p.getMejorPeso() > maxPeso) maxPeso = (float) p.getMejorPeso();
        }

        if (entradas.size() < 2) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);

        ChartStyler.styleLine(chart, new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int idx = Math.round(value);
                return (idx >= 0 && idx < etiquetas.size()) ? etiquetas.get(idx) : "";
            }
        });

        // Tooltip: peso + fecha, con "· PR" en el récord.
        final float pr = maxPeso;
        chart.setMarker(new ChartMarker(this, (e, h) -> {
            int idx = Math.round(e.getX());
            String fecha = (idx >= 0 && idx < etiquetas.size()) ? etiquetas.get(idx) : "";
            String sufijo = e.getY() >= pr ? " · " + getString(R.string.detalle_progresion_pr) : "";
            return String.format(Locale.getDefault(), "%.1f kg%s\n%s", e.getY(), sufijo, fecha);
        }));

        LineDataSet ds = new LineDataSet(entradas, "peso");
        ChartStyler.styleLineDataSet(ds, this);

        // Resalta el/los punto(s) de récord en dorado; el resto en naranja de marca.
        int marca = ds.getColor();
        int oro = android.graphics.Color.parseColor("#FFC24B");
        List<Integer> circulos = new ArrayList<>();
        for (Entry e : entradas) circulos.add(e.getY() >= maxPeso ? oro : marca);
        ds.setCircleColors(circulos);
        ds.setCircleRadius(4.5f);

        chart.setData(new LineData(ds));
        chart.invalidate();
    }

    // Fecha ISO ("yyyy-MM-dd...") → etiqueta corta "dd/MM".
    private String fechaCorta(String iso) {
        if (iso == null || iso.length() < 10) return "";
        return iso.substring(8, 10) + "/" + iso.substring(5, 7);
    }

    // Muestra la demostración del ejercicio en la cabecera: con 2 fotogramas
    // se alternan en bucle (animación del monigote); con 1, imagen estática;
    // sin URL se mantiene el placeholder genérico.
    private void configurarDemostracion(String url1, String url2) {
        if (isEmpty(url1)) return;

        View placeholder = findViewById(R.id.layoutVideoPlaceholder);
        ImageView ivImagen = findViewById(R.id.ivImagenDetalle);
        ivImagen.setVisibility(View.VISIBLE);
        placeholder.setVisibility(View.GONE);
        Glide.with(this).load(url1).into(ivImagen);

        if (isEmpty(url2)) return;

        // Precarga el fotograma 2 y arranca la alternancia en bucle
        Glide.with(this).load(url2).preload();
        frameRunnable = new Runnable() {
            private boolean mostrarSegundo = true;

            @Override
            public void run() {
                // placeholder(drawable actual) evita el parpadeo entre fotogramas
                Glide.with(DetalleEjercicioActivity.this)
                        .load(mostrarSegundo ? url2 : url1)
                        .placeholder(ivImagen.getDrawable())
                        .into(ivImagen);
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

    // Rellena las vistas de la pantalla con los datos del ejercicio,
    // ocultando las secciones (equipamiento/descripción/instrucciones) vacías.
    private void poblarVistas(String nombre, String descripcion, String instrucciones,
                              String grupoMuscular, String musculoPrimario, String dificultad,
                              int calorias, String equipo) {
        ((TextView) findViewById(R.id.tvNombreDetalle)).setText(nombre != null ? nombre : "");

        // Músculo: el primario preciso (ej. "Aductores") si la API lo trae;
        // si no, el grupo grueso traducido como fallback.
        String musculo = !isEmpty(musculoPrimario)
                ? musculoPrimario
                : (!isEmpty(grupoMuscular)
                        ? es.pmdm.gymprofit.utils.UIHelper.traducirGrupoMuscular(this, grupoMuscular) : "—");
        ((TextView) findViewById(R.id.tvStatMusculo)).setText(musculo);
        ((TextView) findViewById(R.id.tvStatNivel)).setText(
                !isEmpty(dificultad) ? es.pmdm.gymprofit.utils.UIHelper.traducirNivel(this, dificultad) : "—");
        ((TextView) findViewById(R.id.tvStatCalorias)).setText(
                calorias > 0 ? calorias + " kcal" : "—");

        if (!isEmpty(equipo)) {
            ((TextView) findViewById(R.id.tvStatEquipamiento)).setText(equipo);
        } else {
            findViewById(R.id.rowEquipamiento).setVisibility(View.GONE);
        }

        TextView tvDesc = findViewById(R.id.tvDescripcion);
        if (!isEmpty(descripcion)) {
            tvDesc.setText(descripcion);
        } else {
            findViewById(R.id.cardDescripcion).setVisibility(View.GONE);
        }

        TextView tvInstr = findViewById(R.id.tvInstrucciones);
        if (!isEmpty(instrucciones)) {
            tvInstr.setText(instrucciones);
        } else {
            findViewById(R.id.cardInstrucciones).setVisibility(View.GONE);
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
