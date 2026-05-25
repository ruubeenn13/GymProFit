package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.UIHelper;

public class NutricionActivity extends BaseActivity {

    private BottomNavigationView bottomNavigationView;
    private ProgressBar progressCalorias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutricion);

        setupMenuButton();
        inicializarVistas();
        configurarProgreso();
        configurarCardsComida();
        configurarNavegacion();
    }

    private void inicializarVistas() {
        progressCalorias = findViewById(R.id.progressCalorias);
    }

    private void configurarProgreso() {
        int caloriasActuales = 0;
        int caloriasObjetivo = 2000;
        int progreso = (int) ((caloriasActuales / (float) caloriasObjetivo) * 100);
        progressCalorias.setProgress(progreso);
    }

    private void configurarCardsComida() {
        String proximamente = getString(R.string.nutricion_proximamente);
        findViewById(R.id.cardDesayuno).setOnClickListener(v ->
                UIHelper.mostrarToastInfo(this, proximamente)
        );
        findViewById(R.id.cardComida).setOnClickListener(v ->
                UIHelper.mostrarToastInfo(this, proximamente)
        );
        findViewById(R.id.cardCena).setOnClickListener(v ->
                UIHelper.mostrarToastInfo(this, proximamente)
        );
    }

    private void configurarNavegacion() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_nutricion);

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
                startActivity(new Intent(this, EjerciciosActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_nutricion) {
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