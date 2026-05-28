package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

public class Onboarding5Activity extends AppCompatActivity {

    private String nivelSeleccionado = null;

    private MaterialCardView cardPrincipiante, cardIntermedio, cardAvanzado, cardExperto;
    private ImageView ivCheckPrincipiante, ivCheckIntermedio, ivCheckAvanzado, ivCheckExperto;

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

        setContentView(R.layout.activity_onboarding5);

        resolverColores();
        inicializarVistas();
        configurarCards();

        Bundle extras = getIntent().getExtras();

        findViewById(R.id.btnSiguiente5).setOnClickListener(v -> {
            if (nivelSeleccionado == null) {
                UIHelper.mostrarToastError(this, getString(R.string.onboarding_selecciona_nivel));
                return;
            }

            Intent intent = new Intent(this, OnboardingResumenActivity.class);
            if (extras != null) intent.putExtras(extras);
            intent.putExtra("nivel", nivelSeleccionado);
            startActivity(intent);
        });

        findViewById(R.id.btnAnterior5).setOnClickListener(v -> finish());
        findViewById(R.id.tvSaltar5).setOnClickListener(v -> saltarAlHome());
    }

    private void inicializarVistas() {
        cardPrincipiante = findViewById(R.id.cardPrincipiante);
        cardIntermedio   = findViewById(R.id.cardIntermedio);
        cardAvanzado     = findViewById(R.id.cardAvanzado);
        cardExperto      = findViewById(R.id.cardExperto);

        ivCheckPrincipiante = findViewById(R.id.ivCheckPrincipiante);
        ivCheckIntermedio   = findViewById(R.id.ivCheckIntermedio);
        ivCheckAvanzado     = findViewById(R.id.ivCheckAvanzado);
        ivCheckExperto      = findViewById(R.id.ivCheckExperto);

        todasLasCards  = new MaterialCardView[]{ cardPrincipiante, cardIntermedio, cardAvanzado, cardExperto };
        todosLosChecks = new ImageView[]{ ivCheckPrincipiante, ivCheckIntermedio, ivCheckAvanzado, ivCheckExperto };
    }

    private void resolverColores() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, typedValue, true);
        colorBordeNormal = typedValue.data;
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
        colorBordeSeleccionado = typedValue.data;
    }

    private void configurarCards() {
        cardPrincipiante.setOnClickListener(v -> seleccionar(cardPrincipiante, ivCheckPrincipiante, "PRINCIPIANTE"));
        cardIntermedio.setOnClickListener(v ->   seleccionar(cardIntermedio,   ivCheckIntermedio,   "INTERMEDIO"));
        cardAvanzado.setOnClickListener(v ->     seleccionar(cardAvanzado,     ivCheckAvanzado,     "AVANZADO"));
        cardExperto.setOnClickListener(v ->      seleccionar(cardExperto,      ivCheckExperto,      "EXPERTO"));
    }

    private void seleccionar(MaterialCardView card, ImageView check, String nivel) {
        nivelSeleccionado = nivel;

        for (MaterialCardView c : todasLasCards) {
            c.setStrokeColor(colorBordeNormal);
            c.setStrokeWidth(2);
        }
        for (ImageView iv : todosLosChecks) {
            iv.setVisibility(View.GONE);
        }

        card.setStrokeColor(colorBordeSeleccionado);
        card.setStrokeWidth(4);
        check.setVisibility(View.VISIBLE);
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
