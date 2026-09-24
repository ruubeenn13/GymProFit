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

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    // Nivel de experiencia seleccionado
    private String nivelSeleccionado = null;
    private PreferencesManager prefs;

    private MaterialCardView cardPrincipiante, cardIntermedio, cardAvanzado, cardExperto;
    private ImageView ivCheckPrincipiante, ivCheckIntermedio, ivCheckAvanzado, ivCheckExperto;

    private MaterialCardView[] todasLasCards;
    private ImageView[] todosLosChecks;

    // setStrokeWidth() de MaterialCardView recibe PÍXELES, no dp. Poniendo 2 y 4
    // a pelo, en un móvil a x3 el borde quedaba en 0,67 y 1,33 dp: al seleccionar
    // una tarjeta el borde de todas ADELGAZABA respecto al 2dp que declara el XML.
    private int bordeNormalPx;
    private int bordeElegidoPx;

    private int colorBordeNormal;
    private int colorBordeSeleccionado;

    // Inicializa la pantalla: aplica tema/idioma, resuelve colores, monta vistas
    // y configura los listeners de navegación (siguiente, anterior y saltar).
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new PreferencesManager(this);
        prefs.applyTheme();

        setContentView(R.layout.activity_onboarding5);

        resolverColores();
        inicializarVistas();
        configurarCards();

        restaurarSeleccion(prefs.getBorradorNivel());

        findViewById(R.id.btnSiguiente5).setOnClickListener(v -> {
            if (nivelSeleccionado == null) {
                UIHelper.mostrarToastError(this, getString(R.string.onboarding_selecciona_nivel));
                return;
            }

            prefs.guardarBorradorNivel(nivelSeleccionado);
            startActivity(new Intent(this, OnboardingResumenActivity.class));
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

        // El medidor es información, no adorno: TalkBack tiene que decir en qué
        // punto de la escala está cada tarjeta (GP-078).
        int[] medidores = { R.id.ivNivelPrincipiante, R.id.ivNivelIntermedio, R.id.ivNivelAvanzado, R.id.ivNivelExperto };
        for (int i = 0; i < medidores.length; i++) {
            findViewById(medidores[i]).setContentDescription(
                    getString(R.string.nivel_escala, i + 1, medidores.length));
        }
    }

    // Resuelve del tema actual los colores de borde normal y de borde
    // seleccionado para las cards de nivel.
    private void resolverColores() {
        float densidad = getResources().getDisplayMetrics().density;
        bordeNormalPx = Math.round(2 * densidad);
        bordeElegidoPx = Math.round(3 * densidad);

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
            c.setStrokeWidth(bordeNormalPx);
            c.setSelected(false);
        }
        for (ImageView iv : todosLosChecks) {
            iv.setVisibility(View.GONE);
        }

        card.setStrokeColor(colorBordeSeleccionado);
        card.setStrokeWidth(bordeElegidoPx);
        // La elección se veía solo por el borde y el check, que TalkBack no lee.
        card.setSelected(true);
        check.setVisibility(View.VISIBLE);
    }

    // Permite saltar el onboarding e ir directamente al Home, limpiando el
    // Vuelve a marcar el nivel ya elegido, por el mismo camino que el toque real.
    private void restaurarSeleccion(String nivel) {
        if (nivel == null || nivel.isEmpty()) return;

        switch (nivel) {
            case "PRINCIPIANTE": seleccionar(cardPrincipiante, ivCheckPrincipiante, nivel); break;
            case "INTERMEDIO":   seleccionar(cardIntermedio,   ivCheckIntermedio,   nivel); break;
            case "AVANZADO":     seleccionar(cardAvanzado,     ivCheckAvanzado,     nivel); break;
            case "EXPERTO":      seleccionar(cardExperto,      ivCheckExperto,      nivel); break;
            default: break;
        }
    }

    // Saltar el onboarding es una DECISION del usuario, no un abandono: se marca
    // como visto para no volver a pedirselo en cada arranque (el splash lo
    // reabriria si no) y se tira el borrador, que ya no hay nada que reanudar.
    private void saltarAlHome() {
        prefs.setOnboardingCompletado(true);
        prefs.setOnboardingCompletadoParaUsuario(prefs.getUsername());
        prefs.limpiarBorradorOnboarding();
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
