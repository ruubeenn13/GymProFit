package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
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

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.CalculadoraNutricional;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// Onboarding4Activity — Paso 4 del onboarding: selección del objetivo del usuario.
// Muestra tarjetas seleccionables con los distintos objetivos disponibles (perder
// peso, ganar músculo, mejorar resistencia, etc.) y pasa el valor elegido al
// siguiente paso del onboarding (Onboarding5Activity) junto con los datos previos.
// ============================================================
public class Onboarding4Activity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    // Objetivo seleccionado - valor exacto del enum TipoObjetivo de la API
    private String objetivoSeleccionado = null;
    private PreferencesManager prefs;

    // Cards
    private MaterialCardView cardPerderPeso, cardGanarMusculo, cardMantener, cardFuerza;

    // Checks
    private ImageView ivCheckPerderPeso, ivCheckGanarMusculo, ivCheckMantener, ivCheckFuerza;

    // Iconos de cada objetivo: se tinen de naranja al seleccionar, como el borde.
    private ImageView ivIconoPerderPeso, ivIconoGanarMusculo, ivIconoMantener, ivIconoFuerza;
    private ImageView[] todosLosIconos;

    // Arrays para limpiar selección fácilmente
    private MaterialCardView[] todasLasCards;
    private ImageView[] todosLosChecks;

    private int colorBordeNormal;
    private int colorBordeSeleccionado;
    // Gris de reposo del icono. No vale el del borde: colorOutlineVariant sobre
    // fondo oscuro deja el dibujo casi invisible.
    private int colorBordeNormalIcono;

    // Inicializa la pantalla: aplica tema/idioma, resuelve colores, monta vistas y
    // configura los listeners de navegación (siguiente, anterior y saltar).
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new PreferencesManager(this);
        prefs.applyTheme();

        setContentView(R.layout.activity_onboarding4);

        resolverColores();
        inicializarVistas();
        condigurarCards();

        restaurarSeleccion(prefs.getBorradorObjetivo());

        findViewById(R.id.btnSiguiente4).setOnClickListener(v -> {
            if (objetivoSeleccionado == null) {
                UIHelper.mostrarToastError(this, getString(R.string.onboarding_selecciona_objetivo));
                return;
            }

            prefs.guardarBorradorObjetivo(objetivoSeleccionado);
            startActivity(new Intent(this, Onboarding5Activity.class));
        });

        findViewById(R.id.btnAnterior4).setOnClickListener(v -> finish());
        findViewById(R.id.tvSaltar4).setOnClickListener(v -> saltarAlHome());
    }

    // Enlaza las cards y los iconos de check con sus vistas del layout y arma los
    // arrays auxiliares usados para limpiar la selección al elegir un nuevo objetivo.
    private void inicializarVistas() {
        cardPerderPeso = findViewById(R.id.cardPerderPeso);
        cardGanarMusculo = findViewById(R.id.cardGanarMusculo);
        cardMantener = findViewById(R.id.cardMantener);
        cardFuerza = findViewById(R.id.cardFuerza);

        ivCheckPerderPeso = findViewById(R.id.ivCheckPerderPeso);
        ivCheckGanarMusculo = findViewById(R.id.ivCheckGanarMusculo);
        ivCheckMantener = findViewById(R.id.ivCheckMantener);
        ivCheckFuerza = findViewById(R.id.ivCheckFuerza);

        ivIconoPerderPeso = findViewById(R.id.ivIconoPerderPeso);
        ivIconoGanarMusculo = findViewById(R.id.ivIconoGanarMusculo);
        ivIconoMantener = findViewById(R.id.ivIconoMantener);
        ivIconoFuerza = findViewById(R.id.ivIconoFuerza);

        todasLasCards = new MaterialCardView[]{
                cardPerderPeso, cardGanarMusculo, cardMantener, cardFuerza
        };

        todosLosChecks = new ImageView[]{
                ivCheckPerderPeso, ivCheckGanarMusculo, ivCheckMantener, ivCheckFuerza
        };

        // Mismo orden que todasLasCards: el icono i pertenece a la tarjeta i.
        todosLosIconos = new ImageView[]{
                ivIconoPerderPeso, ivIconoGanarMusculo, ivIconoMantener, ivIconoFuerza
        };
    }

    // Resuelve del tema actual los colores de borde normal y de borde seleccionado
    // para las cards de objetivos.
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

        getTheme().resolveAttribute(
                com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true
        );

        colorBordeNormalIcono = typedValue.data;
    }

    // Asocia cada card con su objetivo correspondiente (valores del enum
    // TipoObjetivo de la API definidos en CalculadoraNutricional).
    private void condigurarCards() {
        cardPerderPeso.setOnClickListener(v -> seleccionarObjetivo(cardPerderPeso, ivCheckPerderPeso, CalculadoraNutricional.OBJETIVO_PERDER_PESO));
        cardGanarMusculo.setOnClickListener(v -> seleccionarObjetivo(cardGanarMusculo, ivCheckGanarMusculo, CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR));
        cardMantener.setOnClickListener(v -> seleccionarObjetivo(cardMantener, ivCheckMantener, CalculadoraNutricional.OBJETIVO_MANTENER_PESO));
        cardFuerza.setOnClickListener(v -> seleccionarObjetivo(cardFuerza, ivCheckFuerza, CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA));
    }

    // Marca la card elegida como seleccionada (borde y check visibles) y limpia
    // el estado visual del resto de cards.
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

        // El icono de la tarjeta elegida pasa de gris a naranja. Se localiza por
        // posicion porque los tres arrays van en el mismo orden.
        for (int i = 0; i < todasLasCards.length; i++) {
            boolean elegida = todasLasCards[i] == cardSeleccionada;
            todosLosIconos[i].setColorFilter(elegida ? colorBordeSeleccionado : colorBordeNormalIcono);
        }
    }

    // Vuelve a marcar el objetivo que el usuario ya habia elegido. Pasa por el
    // mismo metodo que el toque real para que el resaltado quede identico.
    private void restaurarSeleccion(String objetivo) {
        if (objetivo == null || objetivo.isEmpty()) return;

        if (objetivo.equals(CalculadoraNutricional.OBJETIVO_PERDER_PESO)) {
            seleccionarObjetivo(cardPerderPeso, ivCheckPerderPeso, objetivo);
        } else if (objetivo.equals(CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR)) {
            seleccionarObjetivo(cardGanarMusculo, ivCheckGanarMusculo, objetivo);
        } else if (objetivo.equals(CalculadoraNutricional.OBJETIVO_MANTENER_PESO)) {
            seleccionarObjetivo(cardMantener, ivCheckMantener, objetivo);
        } else if (objetivo.equals(CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA)) {
            seleccionarObjetivo(cardFuerza, ivCheckFuerza, objetivo);
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
