package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.logro.LogroProgreso;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.LogroApi;
import es.pmdm.gymprofit.ui.adapters.LogroAdapter;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UiFeedback;
import com.google.android.material.appbar.MaterialToolbar;

// ============================================================
// LogrosActivity — pantalla de logros/achievements del usuario.
// Muestra el catálogo completo con el estado de cada logro: conseguido y
// cuándo, o cuánto falta (GP-079).
//
// Una sola llamada, GET /logros/progreso, que ya cruza catálogo y obtenidos.
// Antes eran dos (catálogo + logros del usuario por id) y el cruce se hacía
// aquí; además no había forma de saber cuánto faltaba.
// ============================================================
public class LogrosActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private RecyclerView rvLogros;
    private View tvVacio;

    // Interfaz Retrofit tipada del dominio logros (cacheada por ApiClient).
    private final LogroApi api = ApiClient.service(LogroApi.class);

    // Aplica tema/idioma, infla el layout y carga los logros con su progreso.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new PreferencesManager(this).applyTheme();
        setContentView(R.layout.activity_logros);

        rvLogros = findViewById(R.id.rvLogros);
        tvVacio  = findViewById(R.id.tvVacio);

        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        rvLogros.setLayoutManager(new LinearLayoutManager(this));

        LoadingDialog.show(this);
        api.getProgreso().enqueue(new ApiCallback<List<LogroProgreso>>() {
            @Override
            public void onOk(List<LogroProgreso> lista) {
                LoadingDialog.hide(LogrosActivity.this);
                mostrar(lista != null ? lista : new ArrayList<>());
            }
            @Override
            public void onFail(int code, String message) {
                // Sin la respuesta no hay nada fiable que pintar: ni el catálogo ni
                // el estado. Se avisa y se enseña el vacío, no una lista a medias.
                LoadingDialog.hide(LogrosActivity.this);
                UiFeedback.toastError(LogrosActivity.this, code, message);
                mostrar(new ArrayList<>());
            }
        });
    }

    // Conseguidos primero, conservando el orden del catálogo dentro de cada grupo;
    // o el estado vacío si no hay logros.
    private void mostrar(List<LogroProgreso> logros) {
        if (logros.isEmpty()) {
            rvLogros.setVisibility(View.GONE);
            tvVacio.setVisibility(View.VISIBLE);
            return;
        }

        List<LogroProgreso> ordenados = new ArrayList<>();
        for (LogroProgreso l : logros) if (l.isConseguido()) ordenados.add(l);
        for (LogroProgreso l : logros) if (!l.isConseguido()) ordenados.add(l);

        tvVacio.setVisibility(View.GONE);
        rvLogros.setVisibility(View.VISIBLE);
        rvLogros.setAdapter(new LogroAdapter(ordenados));
    }
}
