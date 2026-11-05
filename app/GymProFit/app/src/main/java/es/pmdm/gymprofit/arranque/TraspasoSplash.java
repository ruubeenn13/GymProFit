package es.pmdm.gymprofit.arranque;

import androidx.annotation.NonNull;

// ============================================================
// TraspasoSplash — cuándo se quita la copia del splash del sistema (GP-223).
//
// En Android 12 o más, al irse el splash, Android lo pasa a una copia en la ventana de la
// app y su icono animado sigue siendo una superficie del sistema por encima de todo. Al
// quitar la copia, la shell suelta ese icono en el acto, sin esperar a ningún fotograma de
// la app: si el siguiente tarda, lo último a la vista es la copia sin icono y la G
// desaparece. Por eso la réplica (la pantalla de carga, con su G casada al icono) se pone
// encima y la copia se queda debajo hasta que el fotograma que pinta la réplica en su sitio
// se ha entregado (entregado) y ha pasado otro (fotograma). Si eso no llega, a LIMITE_MS.
//
// Lo que espera al splash (la salida, el montaje de Inicio) empieza con splashFuera, nunca
// con la copia puesta. Si nadie avisa de que el splash se va (sin splash, o si Android no
// consigue pasarlo a la app), el respaldo lo da por ido; una vez que la copia está debajo
// de la réplica, el respaldo ya no cuenta.
//
// El respaldo, en Android 12 o más, cuenta desde que la app entrega su primer fotograma
// (antes, el sistema no puede pasarle el splash) y dura RESPALDO_MS; si la salida se pide
// después de ese fotograma, cuenta igual desde él. Medido en el emulador: la escucha llegó
// 630 ms después del primer fotograma, y contar 600 ms desde la petición de salida daba el
// splash por ido antes de que la app se viera (traza de 9963274).
//
// Por debajo de Android 12 la copia es una vista de la librería sin superficie aparte: se
// quita en el acto, y el respaldo es el de siempre, ESPERA_SPLASH_MS desde la petición.
//
// Sin dependencias de Android ni reloj propio, como PlanArranque: la hora llega en cada
// llamada y siguienteTic dice cuándo volver.
// ============================================================
public final class TraspasoSplash {

    /** El respaldo por debajo de Android 12: desde que se pide la salida. */
    public static final long ESPERA_SPLASH_MS = 600;
    /** El respaldo en Android 12 o más: desde el primer fotograma entregado de la app. */
    public static final long RESPALDO_MS = 2000;
    /** Lo más que la copia se queda debajo de la réplica esperando su fotograma. */
    public static final long LIMITE_MS = 1000;

    /** Lo que el traspaso pide a la pantalla. Cada cosa, una vez como mucho. */
    public interface Pantalla {
        /** La réplica ya se ve en su sitio: quitar la copia del splash. */
        void quitarCopia();
        /** El splash del sistema ya no está, ni su copia: puede empezar lo que esperaba. */
        void splashFuera();
        /** El splash no avisó al irse y salta el respaldo; justo antes de splashFuera. */
        void respaldo();
    }

    private enum Fase { PUESTO, ENCIMA, ENTREGADO, FUERA }

    private final boolean esperaEntrega;
    private final Pantalla pantalla;
    private Fase fase = Fase.PUESTO;
    private boolean copiaQuitada;
    // Cuándo se pidió la salida y cuándo se entregó el primer fotograma; -1 si aún no.
    private long pedidoEn = -1;
    private long primerFotogramaEn = -1;
    // Hasta cuándo se queda la copia bajo la réplica (ENCIMA o ENTREGADO).
    private long limiteCopia = -1;

    private TraspasoSplash(boolean esperaEntrega, @NonNull Pantalla pantalla) {
        this.esperaEntrega = esperaEntrega;
        this.pantalla = pantalla;
    }

    /**
     * @param esperaEntrega Android 12 o más: la copia espera al fotograma de la réplica y el
     *                      respaldo cuenta desde el primer fotograma. Si no, la copia se
     *                      quita en cuanto la réplica está encima y el respaldo cuenta
     *                      desde la petición de salida.
     */
    @NonNull
    public static TraspasoSplash nuevo(boolean esperaEntrega, @NonNull Pantalla p) {
        return new TraspasoSplash(esperaEntrega, p);
    }

    /** Algo espera a que se vaya el splash: corre el respaldo, por si no avisa. */
    public void esperar(long ahora) {
        if (pedidoEn < 0) pedidoEn = ahora;
    }

    /** La app ha entregado su primer fotograma. */
    public void primerFotograma(long ahora) {
        if (primerFotogramaEn < 0) primerFotogramaEn = ahora;
    }

    /** La escucha de salida: la réplica ya está encima de la copia, con la G casada. */
    public void encima(long ahora) {
        if (fase == Fase.FUERA) {
            // Llega tras el respaldo, con la salida ya en marcha: la copia se quita y nada más.
            quitar();
            return;
        }
        if (fase != Fase.PUESTO) return;
        if (!esperaEntrega) {
            salir();
            return;
        }
        fase = Fase.ENCIMA;
        limiteCopia = ahora + LIMITE_MS;
    }

    /** Se ha entregado el fotograma que pinta la réplica en su sitio. */
    public void entregado(long ahora) {
        if (fase == Fase.ENCIMA) fase = Fase.ENTREGADO;
    }

    /** Un fotograma (Choreographer). Solo cuenta el primero tras la entrega. */
    public void fotograma(long ahora) {
        if (fase == Fase.ENTREGADO) salir();
    }

    /** Lo que toque a esta hora: el respaldo, o el límite de la copia. */
    public void tic(long ahora) {
        long limite = siguienteTic();
        if (limite < 0 || ahora < limite) return;
        if (fase == Fase.PUESTO) {
            // Nadie avisó: no hay copia que quitar.
            fase = Fase.FUERA;
            pantalla.respaldo();
            pantalla.splashFuera();
            return;
        }
        salir();
    }

    /** Cuándo volver a llamar a tic, en el mismo reloj; -1 si no hace falta. */
    public long siguienteTic() {
        switch (fase) {
            case PUESTO:
                if (pedidoEn < 0) return -1;
                if (!esperaEntrega) return pedidoEn + ESPERA_SPLASH_MS;
                return primerFotogramaEn < 0 ? -1 : primerFotogramaEn + RESPALDO_MS;
            case FUERA:
                return -1;
            default:
                return limiteCopia;
        }
    }

    /** Si el splash y su copia ya no están. */
    public boolean fuera() {
        return fase == Fase.FUERA;
    }

    private void salir() {
        fase = Fase.FUERA;
        quitar();
        pantalla.splashFuera();
    }

    private void quitar() {
        if (copiaQuitada) return;
        copiaQuitada = true;
        pantalla.quitarCopia();
    }
}
