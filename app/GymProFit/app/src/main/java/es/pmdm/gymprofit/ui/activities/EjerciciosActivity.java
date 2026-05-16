package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.EjercicioAdapter;
import es.pmdm.gymprofit.utils.PreferencesManager;

public class EjerciciosActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private RecyclerView rvEjercicios;
    private EjercicioAdapter adapter;
    private TextInputEditText etBuscar;
    private ChipGroup chipGroupFiltros;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesManager prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdiomaGuardado(prefsManager);

        setContentView(R.layout.activity_ejercicios);

        inicializarVistas();
        configurarRecyclerView();
        configurarBuscador();
        configurarChips();
        configurarNavegacion();
        cargarEjercicios();
    }

    private void inicializarVistas() {
        rvEjercicios = findViewById(R.id.rvEjercicios);
        etBuscar = findViewById(R.id.etBuscar);
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros);
    }

    private void configurarRecyclerView() {
        adapter = new EjercicioAdapter(new ArrayList<>());
        rvEjercicios.setLayoutManager(new LinearLayoutManager(this));
        rvEjercicios.setAdapter(adapter);
    }

    private void cargarEjercicios() {
        API.getEjerciciosActivos(new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    List<Ejercicio> lista = UtilJSONParser.parseEjercicioList(response);
                    adapter.setEjercicios(lista);
                } catch (JSONException e) {
                    android.util.Log.e("EjerciciosActivity", "Error parseando ejercicios", e);
                }
            }

            @Override
            public void onError(String message, int statusCode) {
                android.util.Log.e("EjerciciosActivity", "Error cargando ejercicios: " + message);
            }
        });
    }

    private void configurarBuscador() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filtrarPorTexto(s.toString());
            }
        });
    }

    private void configurarChips() {
        chipGroupFiltros.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int id = checkedIds.get(0);
            String grupo;

            if (id == R.id.chipTodos)        grupo = "Todos";
            else if (id == R.id.chipPecho)   grupo = "Pecho";
            else if (id == R.id.chipEspalda) grupo = "Espalda";
            else if (id == R.id.chipPiernas) grupo = "Piernas";
            else if (id == R.id.chipBrazos)  grupo = "Brazos";
            else if (id == R.id.chipHombros) grupo = "Hombros";
            else if (id == R.id.chipCore)    grupo = "Core";
            else                             grupo = "Todos";

            adapter.filtrarPorGrupo(grupo);
        });
    }

    private void configurarNavegacion() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_ejercicios);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_rutinas) {
                startActivity(new Intent(this, RutinasActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_ejercicios) {
                return true;
            } else if (itemId == R.id.nav_nutricion) {
                startActivity(new Intent(this, NutricionActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void aplicarIdiomaGuardado(PreferencesManager prefsManager) {
        String savedLanguage = prefsManager.getLanguage();
        if (!savedLanguage.isEmpty()) {
            Locale locale = new Locale(savedLanguage);
            Locale.setDefault(locale);
            Resources resources = getResources();
            Configuration config = resources.getConfiguration();
            config.setLocale(locale);
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        }
    }
}
