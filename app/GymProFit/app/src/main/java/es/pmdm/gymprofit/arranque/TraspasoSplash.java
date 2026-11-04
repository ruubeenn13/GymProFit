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
// con la copia puesta. Si nadie avisa de que el splash se va (no llegó a enseñarse), la
// espera de ESPERA_SPLASH_MS lo da por ido; pero una vez que la copia está debajo de la
// réplica, esa espera ya no cuenta.
//
// Por debajo de Android 12 la copia es una vista de la librería sin superficie aparte: se
// quita en el acto, como siempre.
//
// Sin dependencias de Android ni reloj propio, como PlanArranque: la hora llega en cada
// llamada y siguienteTic dice cuándo volver.
// ============================================================
public final class TraspasoSplash {

    /** Lo más que se espera a que el sistema diga que su splash se va. */
    public static final long ESPERA_SPLASH_MS = 600;
    /** Lo más que la copia se queda debajo de la réplica esperando su fotograma. */
    public static final long LIMITE_MS = 1000;

    /** Lo que el traspaso pide a la pantalla. Cada cosa, una vez como mucho. */
    public interface Pantalla {
        /** La réplica ya se ve en su sitio: quitar la copia del splash. */
        void quitarCopia();
        /** El splash del sistema ya no está, ni su copia: puede empezar lo que esperaba. */
        void splashFuera();
    }

    private enum Fase { PUESTO, ENCIMA, ENTREGADO, FUERA }

    private final boolean esperaEntrega;
    private final Pantalla pantalla;
    private Fase fase = Fase.PUESTO;
    private boolean copiaQuitada;
    // Hasta cuándo se espera en la fase en curso; -1 si no se espera nada.
    private long limite = -1;

    private TraspasoSplash(boolean esperaEntrega, @NonNull Pantalla pantalla) {
        this.esperaEntrega = esperaEntrega;
        this.pantalla = pantalla;
    }

    /**
     * @param esperaEntrega si la copia espera al fotograma de la réplica (Android 12 o más);
     *                      si no, se quita en cuanto la réplica está encima.
     */
    @NonNull
    public static TraspasoSplash nuevo(boolean esperaEntrega, @NonNull Pantalla p) {
        return new TraspasoSplash(esperaEntrega, p);
    }

    /** Algo espera a que se vaya el splash: si a ESPERA_SPLASH_MS nadie avisa, se da por ido. */
    public void esperar(long ahora) {
        if (fase == Fase.PUESTO && limite < 0) limite = ahora + ESPERA_SPLASH_MS;
    }

    /** La escucha de salida: la réplica ya está encima de la copia, con la G casada. */
    public void encima(long ahora) {
        if (fase == Fase.FUERA) {
            // Llega tarde, con la salida ya en marcha: la copia se quita y nada más.
            quitar();
            return;
        }
        if (fase != Fase.PUESTO) return;
        if (!esperaEntrega) {
            salir();
            return;
        }
        fase = Fase.ENCIMA;
        limite = ahora + LIMITE_MS;
    }

    /** Se ha entregado el fotograma que pinta la réplica en su sitio. */
    public void entregado(long ahora) {
        if (fase == Fase.ENCIMA) fase = Fase.ENTREGADO;
    }

    /** Un fotograma (Choreographer). Solo cuenta el primero tras la entrega. */
    public void fotograma(long ahora) {
        if (fase == Fase.ENTREGADO) salir();
    }

    /** Lo que toque a esta hora: el límite de la fase en curso. */
    public void tic(long ahora) {
        if (fase == Fase.FUERA || limite < 0 || ahora < limite) return;
        if (fase == Fase.PUESTO) {
            // Nadie avisó: no hay copia que quitar.
            fase = Fase.FUERA;
            limite = -1;
            pantalla.splashFuera();
            return;
        }
        salir();
    }

    /** Cuándo volver a llamar a tic, en el mismo reloj; -1 si no hace falta. */
    public long siguienteTic() {
        return fase == Fase.FUERA ? -1 : limite;
    }

    /** Si el splash y su copia ya no están. */
    public boolean fuera() {
        return fase == Fase.FUERA;
    }

    private void salir() {
        fase = Fase.FUERA;
        limite = -1;
        quitar();
        pantalla.splashFuera();
    }

    private void quitar() {
        if (copiaQuitada) return;
        copiaQuitada = true;
        pantalla.quitarCopia();
    }
}
