package es.pmdm.gymprofit.ui.activities;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.logro.Logro;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.LogroAdapter;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// LogrosActivity — pantalla de logros/achievements del usuario.
// Muestra el catálogo completo de logros y marca visualmente cuáles
// están desbloqueados para el usuario actual dentro de GymProFit.
// ============================================================
public class LogrosActivity extends AppCompatActivity {

    private RecyclerView rvLogros;
    private View tvVacio;
    private PreferencesManager prefsManager;

    private List<Logro> todosLogros = new ArrayList<>();
    private Set<Integer> desbloqueados = new HashSet<>();
    // Contador de llamadas asíncronas pendientes (catálogo + desbloqueados) antes de renderizar
    private final AtomicInteger llamadasPendientes = new AtomicInteger(2);

    // Aplica tema/idioma, infla el layout y lanza en paralelo la carga del
    // catálogo de logros y de los logros desbloqueados por el usuario.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdioma();
        setContentView(R.layout.activity_logros);

        rvLogros = findViewById(R.id.rvLogros);
        tvVacio  = findViewById(R.id.tvVacio);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        rvLogros.setLayoutManager(new LinearLayoutManager(this));

        cargarTodosLogros();
        cargarLogrosDesbloqueados();
    }

    // Carga el catálogo completo de logros disponibles en la app.
    private void cargarTodosLogros() {
        API.getLogros(new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    todosLogros = UtilJSONParser.parseLogroList(response);
                } catch (JSONException ignored) {
                    todosLogros = new ArrayList<>();
                }
                if (llamadasPendientes.decrementAndGet() == 0) {
                    runOnUiThread(() -> mostrar());
                }
            }
            @Override
            public void onError(String message, int statusCode) {
                if (llamadasPendientes.decrementAndGet() == 0) {
                    runOnUiThread(() -> mostrar());
                }
            }
        });
    }

    // Carga el conjunto de ids de logros que el usuario actual ya ha desbloqueado.
    private void cargarLogrosDesbloqueados() {
        int usuarioId = prefsManager.getUsuarioId();
        API.getLogrosDeUsuario(usuarioId, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    desbloqueados = UtilJSONParser.parseLogrosDesbloqueados(response);
                } catch (JSONException ignored) {
                    desbloqueados = new HashSet<>();
                }
                if (llamadasPendientes.decrementAndGet() == 0) {
                    runOnUiThread(() -> mostrar());
                }
            }
            @Override
            public void onError(String message, int statusCode) {
                // 404 = ningún logro desbloqueado
                desbloqueados = new HashSet<>();
                if (llamadasPendientes.decrementAndGet() == 0) {
                    runOnUiThread(() -> mostrar());
                }
            }
        });
    }

    // Ordena los logros (desbloqueados primero) y los muestra en el RecyclerView,
    // o el estado vacío si no hay logros en el catálogo.
    private void mostrar() {
        if (todosLogros.isEmpty()) {
            rvLogros.setVisibility(View.GONE);
            tvVacio.setVisibility(View.VISIBLE);
            return;
        }

        // Desbloqueados primero
        List<Logro> ordenados = new ArrayList<>();
        for (Logro l : todosLogros) {
            if (desbloqueados.contains(l.getId())) ordenados.add(0, l);
            else ordenados.add(l);
        }

        tvVacio.setVisibility(View.GONE);
        rvLogros.setVisibility(View.VISIBLE);
        rvLogros.setAdapter(new LogroAdapter(ordenados, desbloqueados));
    }

    // Aplica el idioma guardado en preferencias a la configuración de recursos.
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
