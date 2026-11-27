package es.pmdm.gymprofit.ui.widget;

import androidx.annotation.NonNull;

// ============================================================
// EncajeCarrusel — dónde se para un carrusel al soltarlo (GP-285, lote 1b).
//
// Un carrusel de Inicio encaja por elemento: al soltar, se queda con el canto izquierdo de un
// elemento en su sitio, nunca con uno partido por la izquierda. Aquí solo está la cuenta, sin
// nada de Android, para poder probarla (EncajeCarruselTest); quien la usa es CarruselView.
// ============================================================
public final class EncajeCarrusel {

    // Cuánto adelanta la posición un lanzamiento: lo que recorrería en este tiempo a su velocidad.
    private static final float SEGUNDOS = 0.2f;

    private EncajeCarrusel() { }

    /**
     * La posición en la que tiene que quedarse el carrusel.
     *
     * @param bordes    el canto izquierdo de cada elemento, de menor a mayor: las posiciones de
     *                  desplazamiento en las que un elemento queda encajado.
     * @param posicion  el desplazamiento actual.
     * @param velocidad la del lanzamiento, en px/s: positiva hacia el final; 0 si se soltó sin lanzar.
     * @param maximo    el desplazamiento máximo (con el último elemento entero a la vista).
     * @return el desplazamiento de destino, entre 0 y {@code maximo}.
     */
    public static int destino(@NonNull int[] bordes, int posicion, int velocidad, int maximo) {
        if (bordes.length == 0 || maximo <= 0) return 0;
        float prevista = posicion + velocidad * SEGUNDOS;
        int elegido = masCercano(bordes, prevista, maximo);
        // Un lanzamiento avanza al menos un elemento en su sentido, aunque sea corto.
        if (velocidad > 0 && elegido <= posicion) elegido = siguiente(bordes, posicion, maximo);
        if (velocidad < 0 && elegido >= posicion) elegido = anterior(bordes, posicion);
        return elegido;
    }

    private static int masCercano(int[] bordes, float a, int maximo) {
        int mejor = 0;
        float distancia = Float.MAX_VALUE;
        for (int b : bordes) {
            int tope = Math.min(b, maximo);
            float d = Math.abs(tope - a);
            if (d < distancia) {
                distancia = d;
                mejor = tope;
            }
        }
        return mejor;
    }

    private static int siguiente(int[] bordes, int posicion, int maximo) {
        for (int b : bordes) if (b > posicion) return Math.min(b, maximo);
        return maximo;
    }

    private static int anterior(int[] bordes, int posicion) {
        int mejor = 0;
        for (int b : bordes) if (b < posicion) mejor = Math.max(mejor, b);
        return mejor;
    }
}
