package es.pmdm.gymprofit.ui.cristal;

// ============================================================
// RendimientoCristal — el interruptor de rendimiento del cristal vivo (GP-285, DEC-070).
//
// Recibe lo que tardó en dibujarse cada fotograma mientras hay cristal vivo en pantalla y dice
// cuándo el móvil no lo aguanta: VENTANAS_MALAS ventanas seguidas de VENTANA fotogramas en las
// que más de UMBRAL se han perdido. Un fotograma perdido es aquel cuyo dibujo tarda más de dos
// fotogramas de 60 Hz: a cualquier frecuencia de pantalla es un tirón que se ve.
//
// Los números salen de lo medido (DEC-021): en el emulador de API 36 el dibujo pasó de
// PERDIDO_NS en 0 a 7 fotogramas de cada 120 (como mucho, el 6 %), igual con desenfoque que
// sin él. El umbral, el 25 %, queda cuatro veces por encima de ese ruido, y hacen falta tres
// ventanas seguidas (unos seis segundos de scroll) para que un tirón suelto no cuente.
//
// Solo cuenta; quien lo usa (VigiaCristal) baja el techo guardado y avisa al cristal. No
// vuelve a subir solo: un móvil que no pudo hoy tampoco podrá mañana.
// No toca Android: se prueba sola (RendimientoCristalTest).
// ============================================================
public final class RendimientoCristal {

    /** Fotogramas por ventana: unos dos segundos de scroll a 60 Hz. */
    public static final int VENTANA = 120;
    /** Más que esto es un fotograma perdido: dos fotogramas de 60 Hz. */
    public static final long PERDIDO_NS = 33_400_000L;
    /** Parte de fotogramas perdidos a partir de la cual la ventana es mala. */
    public static final double UMBRAL = 0.25;
    /** Ventanas malas seguidas que hacen bajar de nivel. */
    public static final int VENTANAS_MALAS = 3;

    private int enVentana, perdidos, malasSeguidas;

    /**
     * Apunta un fotograma.
     *
     * @param dibujoNs lo que tardó en dibujarse (órdenes y GPU), en nanosegundos.
     * @return true si toca bajar un nivel; el contador vuelve a cero.
     */
    public boolean fotograma(long dibujoNs) {
        if (dibujoNs > PERDIDO_NS) perdidos++;
        if (++enVentana < VENTANA) return false;
        boolean mala = perdidos > VENTANA * UMBRAL;
        enVentana = 0;
        perdidos = 0;
        malasSeguidas = mala ? malasSeguidas + 1 : 0;
        if (malasSeguidas < VENTANAS_MALAS) return false;
        malasSeguidas = 0;
        return true;
    }
}
