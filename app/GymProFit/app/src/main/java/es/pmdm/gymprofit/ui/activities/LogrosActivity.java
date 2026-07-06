package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.logro.Logro;
import es.pmdm.gymprofit.model.logro.UsuarioLogro;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.LogroApi;
import es.pmdm.gymprofit.ui.adapters.LogroAdapter;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UiFeedback;

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

    // Interfaz Retrofit tipada del dominio logros (cacheada por ApiClient).
    private final LogroApi api = ApiClient.service(LogroApi.class);

    // Aplica tema/idioma, infla el layout y lanza en paralelo la carga del
    // catálogo de logros y de los logros desbloqueados por el usuario.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        setContentView(R.layout.activity_logros);

        rvLogros = findViewById(R.id.rvLogros);
        tvVacio  = findViewById(R.id.tvVacio);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        rvLogros.setLayoutManager(new LinearLayoutManager(this));

        // Spinner mientras se resuelven en paralelo catálogo + desbloqueados.
        LoadingDialog.show(this);
        cargarTodosLogros();
        cargarLogrosDesbloqueados();
    }

    // Carga el catálogo completo de logros disponibles en la app.
    private void cargarTodosLogros() {
        api.getLogros().enqueue(new ApiCallback<List<Logro>>() {
            @Override
            public void onOk(List<Logro> lista) {
                todosLogros = lista != null ? lista : new ArrayList<>();
                if (llamadasPendientes.decrementAndGet() == 0) mostrar();
            }
            @Override
            public void onFail(int code, String message) {
                // Fallo del catálogo (no del progreso): avisa (404 silenciado por UiFeedback).
                UiFeedback.toastError(LogrosActivity.this, code, message);
                if (llamadasPendientes.decrementAndGet() == 0) mostrar();
            }
        });
    }

    // Carga el conjunto de ids de logros que el usuario actual ya ha desbloqueado.
    private void cargarLogrosDesbloqueados() {
        int usuarioId = prefsManager.getUsuarioId();
        api.getLogrosDeUsuario(usuarioId).enqueue(new ApiCallback<List<UsuarioLogro>>() {
            @Override
            public void onOk(List<UsuarioLogro> lista) {
                // Se extrae solo el logroId de cada relación para marcar el catálogo.
                Set<Integer> ids = new HashSet<>();
                if (lista != null) {
                    for (UsuarioLogro ul : lista) ids.add(ul.getLogroId());
                }
                desbloqueados = ids;
                if (llamadasPendientes.decrementAndGet() == 0) mostrar();
            }
            @Override
            public void onFail(int code, String message) {
                // 404 = ningún logro desbloqueado
                desbloqueados = new HashSet<>();
                if (llamadasPendientes.decrementAndGet() == 0) mostrar();
            }
        });
    }

    // Ordena los logros (desbloqueados primero) y los muestra en el RecyclerView,
    // o el estado vacío si no hay logros en el catálogo.
    private void mostrar() {
        // Ambas llamadas resueltas → oculta el spinner.
        LoadingDialog.hide(this);
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
}
