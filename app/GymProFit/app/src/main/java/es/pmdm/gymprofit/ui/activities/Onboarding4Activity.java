package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.CalculadoraNutricional;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

public class Onboarding4Activity extends AppCompatActivity {

    // Objetivo seleccionado - valor exacto del enum TipoObjetivo de la API
    private String objetivoSeleccionado = null;

    // Cards
    private MaterialCardView cardPerderPeso, cardGanarMusculo, cardMantener, cardResistencia, cardFuerza,
                             cardReducirGrasa, cardFlexibilidad, cardVelocidad, cardAumentarCalorias, cardMovilidad;

    // Checks
    private ImageView ivCheckPerderPeso, ivCheckGanarMusculo, ivCheckMantener, ivCheckResistencia, ivCheckFuerza,
                      ivCheckReducirGrasa, ivCheckFlexibilidad, ivCheckVelocidad, ivCheckAumentarCalorias, ivCheckMovilidad;

    // Arrays para limpiar selección fácilmente
    private MaterialCardView[] todasLasCards;
    private ImageView[] todosLosChecks;

    private int colorBordeNormal;
    private int colorBordeSeleccionado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();
        aplicarIdioma(prefs);

        setContentView(R.layout.activity_onboarding4);

        resolverColores();
        inicializarVistas();
        condigurarCards();

        Bundle extras = getIntent().getExtras();

        findViewById(R.id.btnSiguiente4).setOnClickListener(v -> {
            if (objetivoSeleccionado == null) {
                UIHelper.mostrarToastError(this, getString(R.string.onboarding_selecciona_objetivo));
                return;
            }

            Intent intent = new Intent(this, OnboardingResumenActivity.class);

            if (extras != null) {
                intent.putExtras(extras);
            }

            intent.putExtra("objetivo", objetivoSeleccionado);

            startActivity(intent);
        });

        findViewById(R.id.btnAnterior4).setOnClickListener(v -> finish());
        findViewById(R.id.tvSaltar4).setOnClickListener(v -> saltarAlHome());
    }

    private void inicializarVistas() {
        cardPerderPeso = findViewById(R.id.cardPerderPeso);
        cardGanarMusculo = findViewById(R.id.cardGanarMusculo);
        cardMantener = findViewById(R.id.cardMantener);
        cardResistencia = findViewById(R.id.cardResistencia);
        cardFuerza = findViewById(R.id.cardFuerza);
        cardReducirGrasa = findViewById(R.id.cardReducirGrasa);
        cardFlexibilidad = findViewById(R.id.cardFlexibilidad);
        cardVelocidad = findViewById(R.id.cardVelocidad);
        cardAumentarCalorias = findViewById(R.id.cardAumentarCalorias);
        cardMovilidad = findViewById(R.id.cardMovilidad);

        ivCheckPerderPeso = findViewById(R.id.ivCheckPerderPeso);
        ivCheckGanarMusculo = findViewById(R.id.ivCheckGanarMusculo);
        ivCheckMantener = findViewById(R.id.ivCheckMantener);
        ivCheckResistencia = findViewById(R.id.ivCheckResistencia);
        ivCheckFuerza = findViewById(R.id.ivCheckFuerza);
        ivCheckReducirGrasa = findViewById(R.id.ivCheckReducirGrasa);
        ivCheckFlexibilidad = findViewById(R.id.ivCheckFlexibilidad);
        ivCheckVelocidad = findViewById(R.id.ivCheckVelocidad);
        ivCheckAumentarCalorias = findViewById(R.id.ivCheckAumentarCalorias);
        ivCheckMovilidad = findViewById(R.id.ivCheckMovilidad);

        todasLasCards = new MaterialCardView[]{
                cardPerderPeso, cardGanarMusculo, cardMantener, cardResistencia, cardFuerza, cardReducirGrasa, cardFlexibilidad, cardVelocidad,
                cardAumentarCalorias, cardMovilidad
        };

        todosLosChecks = new ImageView[]{
                ivCheckPerderPeso, ivCheckGanarMusculo, ivCheckMantener, ivCheckResistencia, ivCheckFuerza, ivCheckReducirGrasa,
                ivCheckFlexibilidad, ivCheckVelocidad, ivCheckAumentarCalorias, ivCheckMovilidad
        };
    }

    private void resolverColores() {
        TypedValue typedValue = new TypedValue();

        getTheme().resolveAttribute(
                com.google.android.material.R.attr.colorOutlineVariant, typedValue, true
        );

        colorBordeNormal = typedValue.data;

        getTheme().resolveAttribute(
                com.google.android.material.R.attr.colorPrimary, typedValue, true
        );

        colorBordeSeleccionado = typedValue.data;
    }

    private void condigurarCards() {
        cardPerderPeso.setOnClickListener(v -> seleccionarObjetivo(cardPerderPeso, ivCheckPerderPeso, CalculadoraNutricional.OBJETIVO_PERDER_PESO));
    }

    private void seleccionarObjetivo(MaterialCardView cardSeleccionada,
                                     ImageView checkSeleccionado, String objetivo) {
        objetivoSeleccionado = objetivo;

        for (MaterialCardView card : todasLasCards) {
            card.setStrokeColor(colorBordeNormal);
            card.setStrokeWidth(2);
        }
        for (ImageView check : todosLosChecks) {
            check.setVisibility(View.GONE);
        }

        cardSeleccionada.setStrokeColor(colorBordeSeleccionado);
        cardSeleccionada.setStrokeWidth(4);
        checkSeleccionado.setVisibility(View.VISIBLE);
    }

    private void saltarAlHome() {
        startActivity(new Intent(this, HomeActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));

        finish();
    }

    private void aplicarIdioma(PreferencesManager prefs) {
        String lang = prefs.getLanguage();

        if (!lang.isEmpty()) {
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);

            Resources resources = getResources();

            Configuration configuration = resources.getConfiguration();
            configuration.setLocale(locale);

            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }
    }
}