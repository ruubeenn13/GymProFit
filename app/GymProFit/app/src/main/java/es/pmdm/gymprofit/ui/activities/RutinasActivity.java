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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.ui.adapters.RutinaAdapter;
import es.pmdm.gymprofit.utils.PreferencesManager;

public class RutinasActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private RecyclerView rvRutinas;
    private RutinaAdapter adapter;
    private ChipGroup chipGroupNivel;
    private FloatingActionButton fabCrearRutina;

    private ActivityResultLauncher<Intent> crearRutinaLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesManager prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdiomaGuardado(prefsManager);

        setContentView(R.layout.activity_rutinas);

        registrarLauncher();
        inicializarVistas();
        configurarRecyclerView();
        configurarChips();
        configurarFab();
        configurarNavegacion();
    }

    private void registrarLauncher() {
        crearRutinaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();

                        Rutina nuevaRutina = new Rutina(
                                data.getStringExtra("nombre"),
                                data.getStringExtra("nivel"),
                                data.getStringExtra("descripcion"),
                                data.getIntExtra("numEjercicios", 0),
                                data.getIntExtra("duracion", 0),
                                data.getIntExtra("calorias", 0)
                        );
                        adapter.addRutina(nuevaRutina);
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
        List<Rutina> rutinas = obtenerRutinas();
        adapter = new RutinaAdapter(rutinas);
        rvRutinas.setLayoutManager(new LinearLayoutManager(this));
        rvRutinas.setAdapter(adapter);
    }

    private void configurarChips() {
        chipGroupNivel.setOnCheckedStateChangeListener(((chipGroup, list) -> {
            if (list.isEmpty()) {
                return;
            }

            int id = list.get(0);
            String nivel;

            if (id == R.id.chipTodos) {
                nivel = "Todos";
            } else if (id == R.id.chipPrincipiante) {
                nivel = "Principiante";
            } else if (id == R.id.chipIntermedio) {
                nivel = "Intermedio";
            } else if (id == R.id.chipAvanzado) {
                nivel = "Avanzado";
            } else {
                nivel = "Todos";
            }

            adapter.filtrarPorNivel(nivel);
        }));
    }

    private void configurarFab() {
        fabCrearRutina.setOnClickListener(v -> {
            crearRutinaLauncher.launch(new Intent(this, CrearRutinaActivity.class));
        });
    }

    private List<Rutina> obtenerRutinas() {
        List<Rutina> lista = new ArrayList<>();
        lista.add(new Rutina("Full Body", "Principiante", "Trabaja todo el cuerpo en una sesión", 5, 45, 350));
        lista.add(new Rutina("Cardio y Resistencia", "Principiante", "Mejora tu resistencia cardiovascular", 6, 40, 400));
        lista.add(new Rutina("Fuerza Upper Body", "Intermedio", "Pecho, espalda y brazos", 8, 60, 500));
        lista.add(new Rutina("Piernas y Glúteos", "Intermedio", "Cuádriceps, isquiotibiales y glúteos", 7, 55, 480));
        lista.add(new Rutina("Push Pull Legs", "Avanzado", "División clásica de empuje, tirón y piernas", 10, 75, 600));
        lista.add(new Rutina("HIIT Intenso", "Avanzado", "Alta intensidad por intervalos", 8, 35, 550));
        lista.add(new Rutina("Core y Movilidad", "Principiante", "Abdominales, lumbar y flexibilidad", 6, 30, 200));
        lista.add(new Rutina("Fuerza y Potencia", "Avanzado", "Movimientos compuestos pesados", 9, 70, 650));
        return lista;
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
}