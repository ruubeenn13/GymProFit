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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
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
    }

    private void inicializarVistas() {
        rvEjercicios = findViewById(R.id.rvEjercicios);
        etBuscar = findViewById(R.id.etBuscar);
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros);
    }

    private void configurarRecyclerView() {
        List<Ejercicio> ejercicios = obtenerEjercicios();
        adapter = new EjercicioAdapter(ejercicios);
        rvEjercicios.setLayoutManager(new LinearLayoutManager(this));
        rvEjercicios.setAdapter(adapter);
    }

    private void configurarBuscador() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {

            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                adapter.filtrarPorTexto(charSequence.toString());
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
        });
    }

    private void configurarChips() {
        chipGroupFiltros.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }

            int id = checkedIds.get(0);
            String grupo;

            if (id == R.id.chipTodos) {
                grupo = "Todos";
            } else if (id == R.id.chipPecho) {
                grupo = "Pecho";
            } else if (id == R.id.chipEspalda) {
                grupo = "Espalda";
            } else if (id == R.id.chipPiernas) {
                grupo = "Piernas";
            } else if (id == R.id.chipBrazos) {
                grupo = "Brazos";
            } else if (id == R.id.chipHombros) {
                grupo = "Hombros";
            } else if (id == R.id.chipCore) {
                grupo = "Core";
            } else {
                grupo = "Todos";
            }
            adapter.filtrarPorGrupo(grupo);
        });
    }

    private List<Ejercicio> obtenerEjercicios() {
        List<Ejercicio> lista = new ArrayList<>();
        lista.add(new Ejercicio("Press de Banca", "Pecho", "Intermedio", "Pectoral · Tríceps", 300, R.drawable.ic_ejercicios));
        lista.add(new Ejercicio("Fondos en Paralelas", "Pecho", "Avanzado", "Pectoral · Tríceps · Hombros", 280, R.drawable.ic_ejercicios));
        lista.add(new Ejercicio("Aperturas con Mancuernas", "Pecho", "Principiante", "Pectoral", 200, R.drawable.ic_ejercicios));
        lista.add(new Ejercicio("Dominadas", "Espalda", "Avanzado", "Dorsales · Bíceps", 350, R.drawable.ic_ejercicios));
        lista.add(new Ejercicio("Remo con Barra", "Espalda", "Intermedio", "Dorsales · Romboides", 320, R.drawable.ic_ejercicios));
        lista.add(new Ejercicio("Jalón al Pecho", "Espalda", "Principiante", "Dorsales · Bíceps", 270, R.drawable.ic_ejercicios));
        lista.add(new Ejercicio("Sentadilla", "Piernas", "Intermedio", "Cuádriceps · Glúteos", 400, R.drawable.ic_ejercicios));
        lista.add(new Ejercicio("Peso Muerto", "Piernas", "Avanzado", "Isquiotibiales · Glúteos", 450, R.drawable.ic_ejercicios));
        lista.add(new Ejercicio("Prensa de Piernas", "Piernas", "Principiante", "Cuádriceps · Isquiotibiales", 350, R.drawable.ic_ejercicios));
        lista.add(new Ejercicio("Curl de Bíceps", "Brazos", "Principiante", "Bíceps", 150, R.drawable.ic_ejercicios));
        lista.add(new Ejercicio("Extensión de Tríceps", "Brazos", "Principiante", "Tríceps", 140, R.drawable.ic_ejercicios));
        lista.add(new Ejercicio("Press Militar", "Hombros", "Intermedio", "Deltoides · Tríceps", 260, R.drawable.ic_ejercicios));
        lista.add(new Ejercicio("Elevaciones Laterales", "Hombros", "Principiante", "Deltoides", 180, R.drawable.ic_ejercicios));
        lista.add(new Ejercicio("Plancha", "Core", "Principiante", "Abdominales · Lumbar", 120, R.drawable.ic_ejercicios));
        lista.add(new Ejercicio("Crunch Abdominal", "Core", "Principiante", "Abdominales", 100, R.drawable.ic_ejercicios));
        return lista;
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