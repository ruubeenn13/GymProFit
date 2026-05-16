package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.RutinaAdapter;
import es.pmdm.gymprofit.utils.PreferencesManager;

public class RutinasActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private RecyclerView rvRutinas;
    private RutinaAdapter adapter;
    private ChipGroup chipGroupNivel;
    private FloatingActionButton fabCrearRutina;
    private PreferencesManager prefsManager;

    private ActivityResultLauncher<Intent> crearRutinaLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdiomaGuardado(prefsManager);

        setContentView(R.layout.activity_rutinas);

        registrarLauncher();
        inicializarVistas();
        configurarRecyclerView();
        configurarChips();
        configurarFab();
        configurarNavegacion();
        cargarRutinas();
    }

    private void registrarLauncher() {
        crearRutinaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        cargarRutinas();
                    }
                }
        );
    }

    private void inicializarVistas() {
        rvRutinas = findViewById(R.id.rvRutinas);
        chipGroupNivel = findViewById(R.id.chipGroupNivel);
        fabCrearRutina = findViewById(R.id.fabCrearRutina);
    }

    private void configurarRecyclerView() {
        adapter = new RutinaAdapter(new ArrayList<>());
        rvRutinas.setLayoutManager(new LinearLayoutManager(this));
        rvRutinas.setAdapter(adapter);
    }

    private void cargarRutinas() {
        int usuarioId = prefsManager.getUsuarioId();

        API.getRutinasPredefinidas(new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    List<Rutina> predefinidas = UtilJSONParser.parseRutinaList(response);
                    List<Rutina> todas = new ArrayList<>(predefinidas);

                    if (usuarioId != -1) {
                        API.getRutinasDeUsuario(usuarioId, new UtilREST.OnResponseListener() {
                            @Override
                            public void onSuccess(String response2, int statusCode2) {
                                try {
                                    todas.addAll(UtilJSONParser.parseRutinaList(response2));
                                } catch (JSONException e) {
                                    android.util.Log.e("RutinasActivity", "Error parseando rutinas usuario", e);
                                }
                                adapter.setRutinas(todas);
                            }

                            @Override
                            public void onError(String message, int statusCode2) {
                                adapter.setRutinas(todas);
                            }
                        });
                    } else {
                        adapter.setRutinas(todas);
                    }
                } catch (JSONException e) {
                    android.util.Log.e("RutinasActivity", "Error parseando rutinas predefinidas", e);
                }
            }

            @Override
            public void onError(String message, int statusCode) {
                android.util.Log.e("RutinasActivity", "Error cargando rutinas: " + message);
            }
        });
    }

    private void configurarChips() {
        chipGroupNivel.setOnCheckedStateChangeListener(((chipGroup, list) -> {
            if (list.isEmpty()) return;

            int id = list.get(0);
            String nivel;

            if (id == R.id.chipTodos)              nivel = "Todos";
            else if (id == R.id.chipPrincipiante)  nivel = "Principiante";
            else if (id == R.id.chipIntermedio)    nivel = "Intermedio";
            else if (id == R.id.chipAvanzado)      nivel = "Avanzado";
            else                                   nivel = "Todos";

            adapter.filtrarPorNivel(nivel);
        }));
    }

    private void configurarFab() {
        fabCrearRutina.setOnClickListener(v ->
                crearRutinaLauncher.launch(new Intent(this, CrearRutinaActivity.class)));
    }

    private void configurarNavegacion() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_rutinas);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_rutinas) {
                return true;
            } else if (itemId == R.id.nav_ejercicios) {
                startActivity(new Intent(this, EjerciciosActivity.class));
                overridePendingTransition(0, 0);
                finish();
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
