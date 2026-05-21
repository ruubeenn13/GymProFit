package es.pmdm.gymprofit.ui.activities;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.TextView;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.logro.Logro;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.model.usuario.UsuarioEstadisticas;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.LogroAdapter;
import es.pmdm.gymprofit.utils.PreferencesManager;

public class ResumenSesionActivity extends AppCompatActivity {

    private TextView tvFecha, tvDuracion, tvCalorias, tvRutina, tvNotas, tvCompletada;
    private View layoutNotas;
    private TextView tvStatSesiones, tvStatCompletadas, tvStatMinutos, tvStatCalorias;
    private TextView tvStatRacha, tvStatMejorRacha;
    private RecyclerView rvLogros;
    private TextView tvLogrosVacio;

    private PreferencesManager prefsManager;
    private final AtomicInteger pendientes = new AtomicInteger(4);

    private SesionEntrenamiento sesion;
    private UsuarioEstadisticas estadisticas;
    private List<Logro> todosLogros = new ArrayList<>();
    private Set<Integer> desbloqueados = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdioma();
        setContentView(R.layout.activity_resumen_sesion);

        int sesionId = getIntent().getIntExtra("sesionId", -1);
        if (sesionId == -1) { finish(); return; }

        String rutinaNombre = getIntent().getStringExtra("rutinaNombre");

        inicializarVistas(rutinaNombre);

        int usuarioId = prefsManager.getUsuarioId();
        cargarSesion(sesionId);
        cargarEstadisticas(usuarioId);
        cargarTodosLogros();
        cargarLogrosDesbloqueados(usuarioId);
    }

    private void inicializarVistas(String rutinaNombre) {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvFecha      = findViewById(R.id.tvFechaResumen);
        tvDuracion   = findViewById(R.id.tvDuracionResumen);
        tvCalorias   = findViewById(R.id.tvCaloriasResumen);
        tvRutina     = findViewById(R.id.tvRutinaResumen);
        layoutNotas  = findViewById(R.id.layoutNotasResumen);
        tvNotas      = findViewById(R.id.tvNotasResumen);
        tvCompletada = findViewById(R.id.tvCompletadaResumen);

        tvStatSesiones   = findViewById(R.id.tvStatSesiones);
        tvStatCompletadas = findViewById(R.id.tvStatCompletadas);
        tvStatMinutos    = findViewById(R.id.tvStatMinutos);
        tvStatCalorias   = findViewById(R.id.tvStatCalorias);
        tvStatRacha      = findViewById(R.id.tvStatRacha);
        tvStatMejorRacha = findViewById(R.id.tvStatMejorRacha);

        rvLogros      = findViewById(R.id.rvLogrosResumen);
        tvLogrosVacio = findViewById(R.id.tvLogrosVacioResumen);
        rvLogros.setLayoutManager(new LinearLayoutManager(this));

        if (rutinaNombre != null && !rutinaNombre.isEmpty()) {
            tvRutina.setText(rutinaNombre);
        } else {
            tvRutina.setText(getString(R.string.sesiones_sin_rutina));
        }
    }

    private void cargarSesion(int sesionId) {
        API.getSesionPorId(sesionId, new UtilREST.OnResponseListener() {
            @Override public void onSuccess(String response, int statusCode) {
                try { sesion = UtilJSONParser.parseSesion(response); } catch (JSONException ignored) {}
                comprobarYMostrar();
            }
            @Override public void onError(String message, int statusCode) { comprobarYMostrar(); }
        });
    }

    private void cargarEstadisticas(int usuarioId) {
        API.getEstadisticasUsuario(usuarioId, new UtilREST.OnResponseListener() {
            @Override public void onSuccess(String response, int statusCode) {
                try { estadisticas = UtilJSONParser.parseEstadisticas(response); } catch (JSONException ignored) {}
                comprobarYMostrar();
            }
            @Override public void onError(String message, int statusCode) { comprobarYMostrar(); }
        });
    }

    private void cargarTodosLogros() {
        API.getLogros(new UtilREST.OnResponseListener() {
            @Override public void onSuccess(String response, int statusCode) {
                try { todosLogros = UtilJSONParser.parseLogroList(response); } catch (JSONException ignored) {}
                comprobarYMostrar();
            }
            @Override public void onError(String message, int statusCode) { comprobarYMostrar(); }
        });
    }

    private void cargarLogrosDesbloqueados(int usuarioId) {
        API.getLogrosDeUsuario(usuarioId, new UtilREST.OnResponseListener() {
            @Override public void onSuccess(String response, int statusCode) {
                try { desbloqueados = UtilJSONParser.parseLogrosDesbloqueados(response); } catch (JSONException ignored) {}
                comprobarYMostrar();
            }
            @Override public void onError(String message, int statusCode) {
                desbloqueados = new HashSet<>();
                comprobarYMostrar();
            }
        });
    }

    private void comprobarYMostrar() {
        if (pendientes.decrementAndGet() == 0) {
            runOnUiThread(this::mostrarContenido);
        }
    }

    private void mostrarContenido() {
        if (sesion != null) {
            tvFecha.setText(sesion.getFechaInicio().isEmpty() ? "—" : sesion.getFechaInicio());
            tvDuracion.setText(getString(R.string.sesiones_min, sesion.getDuracionMinutos()));
            tvCalorias.setText(getString(R.string.sesiones_kcal, sesion.getCaloriasQuemadas()));
            if (!sesion.getNotas().isEmpty()) {
                tvNotas.setText(sesion.getNotas());
                layoutNotas.setVisibility(View.VISIBLE);
            }
            tvCompletada.setVisibility(sesion.isCompletada() ? View.VISIBLE : View.GONE);
        }

        if (estadisticas != null) {
            tvStatSesiones.setText(String.valueOf(estadisticas.getTotalSesiones()));
            tvStatCompletadas.setText(String.valueOf(estadisticas.getSesionesCompletadas()));
            tvStatMinutos.setText(String.valueOf(estadisticas.getTotalMinutosEntrenados()));
            tvStatCalorias.setText(String.valueOf(estadisticas.getTotalCaloriasQuemadas()));
            tvStatRacha.setText(String.valueOf(estadisticas.getRachaActualDias()));
            tvStatMejorRacha.setText(String.valueOf(estadisticas.getMejorRachaDias()));
        }

        List<Logro> filtrados = new ArrayList<>();
        for (Logro l : todosLogros) {
            if (desbloqueados.contains(l.getId())) filtrados.add(l);
        }

        if (filtrados.isEmpty()) {
            tvLogrosVacio.setVisibility(View.VISIBLE);
            rvLogros.setVisibility(View.GONE);
        } else {
            tvLogrosVacio.setVisibility(View.GONE);
            rvLogros.setVisibility(View.VISIBLE);
            rvLogros.setAdapter(new LogroAdapter(filtrados, desbloqueados));
        }
    }

    private void aplicarIdioma() {
        String lang = prefsManager.getLanguage();
        if (!lang.isEmpty()) {
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            Resources res = getResources();
            Configuration cfg = res.getConfiguration();
            cfg.setLocale(locale);
            res.updateConfiguration(cfg, res.getDisplayMetrics());
        }
    }
}
