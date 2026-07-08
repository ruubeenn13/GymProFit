package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// Onboarding5Activity — Paso 5 del onboarding: selección del nivel de experiencia.
// Muestra tarjetas seleccionables (principiante, intermedio, avanzado, experto)
// y pasa el nivel elegido, junto con el resto de datos del onboarding, a
// OnboardingResumenActivity para el envío final de los datos.
// ============================================================
public class Onboarding5Activity extends AppCompatActivity {

    // Nivel de experiencia seleccionado
    private String nivelSeleccionado = null;

    private MaterialCardView cardPrincipiante, cardIntermedio, cardAvanzado, cardExperto;
    private ImageView ivCheckPrincipiante, ivCheckIntermedio, ivCheckAvanzado, ivCheckExperto;

    private MaterialCardView[] todasLasCards;
    private ImageView[] todosLosChecks;

    private int colorBordeNormal;
    private int colorBordeSeleccionado;

    // Inicializa la pantalla: aplica tema/idioma, resuelve colores, monta vistas
    // y configura los listeners de navegación (siguiente, anterior y saltar).
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();

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

    // Enlaza las cards y los iconos de check de cada nivel con sus vistas del
    // layout y arma los arrays auxiliares para limpiar la selección.
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

    // Resuelve del tema actual los colores de borde normal y de borde
    // seleccionado para las cards de nivel.
    private void resolverColores() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, typedValue, true);
        colorBordeNormal = typedValue.data;
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
        colorBordeSeleccionado = typedValue.data;
    }

    // Asocia cada card con su nivel de experiencia correspondiente.
    private void configurarCards() {
        cardPrincipiante.setOnClickListener(v -> seleccionar(cardPrincipiante, ivCheckPrincipiante, "PRINCIPIANTE"));
        cardIntermedio.setOnClickListener(v ->   seleccionar(cardIntermedio,   ivCheckIntermedio,   "INTERMEDIO"));
        cardAvanzado.setOnClickListener(v ->     seleccionar(cardAvanzado,     ivCheckAvanzado,     "AVANZADO"));
        cardExperto.setOnClickListener(v ->      seleccionar(cardExperto,      ivCheckExperto,      "EXPERTO"));
    }

    // Marca la card elegida como seleccionada (borde y check visibles) y
    // limpia el estado visual del resto de cards.
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

    // Permite saltar el onboarding e ir directamente al Home, limpiando el
    // back stack.
    private void saltarAlHome() {
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
