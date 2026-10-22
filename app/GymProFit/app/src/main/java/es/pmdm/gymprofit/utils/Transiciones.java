package es.pmdm.gymprofit.utils;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.transition.platform.MaterialContainerTransform;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;
import com.google.android.material.transition.platform.MaterialSharedAxis;

// ============================================================
// Transiciones — Material motion entre pantallas (GP-210 paso 2, DEC-039).
//
// Dos casos, con los números de Movimiento (nunca escritos aquí):
//   · Tarjeta → detalle: contenedor que crece (MaterialContainerTransform). 400 ms con la curva
//     enfatizada al abrir y 350 ms con la estándar al volver. Rutina, sesión del historial,
//     alimento → ficha y récord → progresión.
//   · Avanzar en un flujo: eje X compartido de 30 dp en 300 ms con la estándar. El alta, crear
//     rutina y seguir un programa. Dentro del alta, entre pasos, lo hace la misma clase con
//     fragmentos (ejeX).
//
// El origen pide la transición con abrirTarjeta o avanzar, que añaden un extra al Intent. El
// destino no hace nada: GymProFitApp llama a prepararDestino al crear cada Activity, que lee el
// extra y monta el lado de llegada. Así ninguna pantalla de detalle tiene que saber que le llega
// con una transición.
//
// Con «Quitar animaciones» nada de esto se mueve: se abre la pantalla como siempre, sin
// transición de escena, y Android deja el resto en su estado final.
// ============================================================
public final class Transiciones {

    /** Extra del Intent que dice al destino cómo ha llegado. */
    private static final String EXTRA = "gp_transicion";
    private static final String CONTENEDOR = "contenedor";
    private static final String EJE_X = "eje_x";
    /** Nombre de transición compartido por la tarjeta de origen y la raíz del detalle. */
    private static final String NOMBRE_CONTENEDOR = "gp_contenedor";

    private Transiciones() {}

    // ── Tarjeta → detalle ───────────────────────────────────────────────────

    /**
     * Abre un detalle desde una tarjeta: la tarjeta crece hasta ocupar la pantalla.
     *
     * @param origen  la pantalla que lanza.
     * @param tarjeta la vista que se pulsó; es el contenedor que crece.
     * @param intent  el destino, con sus extras.
     */
    public static void abrirTarjeta(@NonNull Activity origen, @NonNull View tarjeta, @NonNull Intent intent) {
        Bundle opciones = opcionesTarjeta(origen, tarjeta, intent);
        if (opciones == null) origen.startActivity(intent);
        else origen.startActivity(intent, opciones);
    }

    /** Igual que {@link #abrirTarjeta(Activity, View, Intent)}, para quien espera un resultado. */
    public static void abrirTarjeta(@NonNull Activity origen, @NonNull View tarjeta, @NonNull Intent intent,
                                    @NonNull ActivityResultLauncher<Intent> lanzador) {
        Bundle opciones = opcionesTarjeta(origen, tarjeta, intent);
        if (opciones == null) lanzador.launch(intent);
        else lanzador.launch(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(origen, tarjeta,
                NOMBRE_CONTENEDOR));
    }

    // Marca el Intent y devuelve las opciones de escena; null si no hay que animar.
    @Nullable
    private static Bundle opcionesTarjeta(Activity origen, View tarjeta, Intent intent) {
        if (Movimiento.quieto(origen)) return null;
        tarjeta.setTransitionName(NOMBRE_CONTENEDOR);
        intent.putExtra(EXTRA, CONTENEDOR);
        return ActivityOptions.makeSceneTransitionAnimation(origen, tarjeta, NOMBRE_CONTENEDOR).toBundle();
    }

    // ── Avanzar en un flujo ─────────────────────────────────────────────────

    /** Avanza a la siguiente pantalla de un flujo con el eje X compartido. */
    public static void avanzar(@NonNull Activity origen, @NonNull Intent intent) {
        if (!prepararAvance(origen, intent)) {
            origen.startActivity(intent);
            return;
        }
        origen.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(origen).toBundle());
    }

    /** Igual que {@link #avanzar(Activity, Intent)}, para quien espera un resultado. */
    public static void avanzar(@NonNull Activity origen, @NonNull Intent intent,
                               @NonNull ActivityResultLauncher<Intent> lanzador) {
        if (!prepararAvance(origen, intent)) {
            lanzador.launch(intent);
            return;
        }
        lanzador.launch(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(origen));
    }

    // La pantalla de origen se va hacia atrás por el eje X y vuelve igual al regresar.
    private static boolean prepararAvance(Activity origen, Intent intent) {
        if (Movimiento.quieto(origen)) return false;
        origen.getWindow().setExitTransition(ejeX(true));
        origen.getWindow().setReenterTransition(ejeX(false));
        intent.putExtra(EXTRA, EJE_X);
        return true;
    }

    // ── Entre pasos de un flujo con fragmentos ──────────────────────────────

    /**
     * Pone el eje X compartido a un fragmento que entra o sale, para pasar de un paso al
     * siguiente (adelante) o al anterior (atrás).
     *
     * @param contexto  el de la pantalla que monta el paso.
     * @param fragmento el fragmento que entra o el que sale.
     * @param entra     true si es el que entra.
     * @param adelante  true si se avanza en el flujo.
     */
    public static void ejeX(@NonNull Context contexto, @NonNull Fragment fragmento, boolean entra, boolean adelante) {
        // El contexto lo da quien monta el paso: el fragmento nuevo aún no está añadido y no tiene el suyo.
        if (Movimiento.quieto(contexto)) return;
        com.google.android.material.transition.MaterialSharedAxis t =
                new com.google.android.material.transition.MaterialSharedAxis(
                        com.google.android.material.transition.MaterialSharedAxis.X, adelante);
        t.setDuration(Movimiento.EJE_X);
        t.setInterpolator(Movimiento.ESTANDAR);
        if (entra) fragmento.setEnterTransition(t);
        else fragmento.setExitTransition(t);
    }

    // ── Lado del destino y del origen (GymProFitApp) ────────────────────────

    /**
     * Monta el lado de llegada si la pantalla vino con una transición. Se llama al crear
     * cada Activity, con su contenido ya puesto.
     */
    public static void prepararDestino(@NonNull Activity destino) {
        // Esta pantalla puede ser también el ORIGEN de otra transición: con esto su tarjeta
        // sale de la capa superpuesta y se entrega a la que crece.
        destino.setExitSharedElementCallback(new MaterialContainerTransformSharedElementCallback());
        destino.getWindow().setSharedElementsUseOverlay(false);

        String tipo = destino.getIntent() == null ? null : destino.getIntent().getStringExtra(EXTRA);
        if (CONTENEDOR.equals(tipo)) {
            View raiz = destino.findViewById(android.R.id.content);
            raiz.setTransitionName(NOMBRE_CONTENEDOR);
            destino.setEnterSharedElementCallback(new MaterialContainerTransformSharedElementCallback());
            int fondo = MaterialColors.getColor(raiz, com.google.android.material.R.attr.colorSurface);
            destino.getWindow().setSharedElementEnterTransition(contenedor(raiz, fondo, true));
            destino.getWindow().setSharedElementReturnTransition(contenedor(raiz, fondo, false));
        } else if (EJE_X.equals(tipo)) {
            destino.getWindow().setEnterTransition(ejeX(true));
            destino.getWindow().setReturnTransition(ejeX(false));
        }
    }

    private static MaterialContainerTransform contenedor(View raiz, int fondo, boolean abre) {
        MaterialContainerTransform t = new MaterialContainerTransform();
        t.addTarget(android.R.id.content);
        t.setAllContainerColors(fondo);
        t.setScrimColor(android.graphics.Color.TRANSPARENT);
        t.setDuration(abre ? Movimiento.CONTENEDOR_ABRE : Movimiento.CONTENEDOR_VUELVE);
        t.setInterpolator(abre ? Movimiento.ENFATIZADA : Movimiento.ESTANDAR);
        return t;
    }

    private static MaterialSharedAxis ejeX(boolean adelante) {
        MaterialSharedAxis t = new MaterialSharedAxis(MaterialSharedAxis.X, adelante);
        t.setDuration(Movimiento.EJE_X);
        t.setInterpolator(Movimiento.ESTANDAR);
        return t;
    }
}
