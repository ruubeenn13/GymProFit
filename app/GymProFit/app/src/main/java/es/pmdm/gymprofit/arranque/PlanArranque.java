package es.pmdm.gymprofit.arranque;

import androidx.annotation.NonNull;

import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// PlanArranque — los tiempos del arranque (GP-223, punto 6).
//
// Sin dependencias de Android y sin reloj propio: quien lo usa le pasa la hora en cada
// llamada (SystemClock.uptimeMillis en la app, un reloj falso en PlanArranqueTest) y le
// pregunta cuándo quiere el siguiente tic. Todo se mide desde que se tocó el icono.
//
//   · La entrada de la G se ve entera: si ya se sabe adónde ir antes de los 700 ms, se
//     espera a que acabe. Con «Quitar animaciones», no se espera.
//   · A 1 s, si aún no se sabe, el splash del sistema se va (mantenerSplash pasa a
//     falso) y empieza el brillo de la pantalla de carga, que está debajo, igual.
//   · A 3 s, «Conectando…».
//   · A 15 s se rinde: la pantalla cancela la petición y dice «No conseguimos conectar».
//     Una respuesta que llegue después ya no cambia nada.
//
// Un reintento («Reintentar» de un error) empieza otro plan, sin splash ni entrada.
// ============================================================
public final class PlanArranque {

    /** Hasta cuándo se queda el splash del sistema si aún no se sabe adónde ir. */
    public static final long SPLASH_SISTEMA_MS = 1000;
    /** Cuándo sale «Conectando…» bajo la G. */
    public static final long CONECTANDO_MS = 3000;
    /** Cuándo se cancela la petición y sale «No conseguimos conectar». */
    public static final long LIMITE_MS = 15_000;
    /** La entrada de la G entera, la del momento de Movimiento. */
    public static final long ENTRADA_MS = Movimiento.G_ENTRADA;

    /** Lo que el plan pide a la pantalla. Cada cosa, una vez como mucho. */
    public interface Pantalla {
        /** A 1 s sin destino: la franja de luz empieza a cruzar la G. */
        void empezarBrillo();
        /** A 3 s sin destino: «Conectando…». */
        void mostrarConectando();
        /** A 15 s sin destino: cancelar la petición y enseñar el error. */
        void agotado();
        /** Ya se sabe adónde ir y la entrada se ha visto: la salida. */
        void salir();
    }

    private final long inicio;
    private final boolean quieto;
    private final boolean conSplash;
    private final Pantalla pantalla;

    private boolean decidido, brillo, conectando, cerrado;

    private PlanArranque(long inicio, boolean quieto, boolean conSplash, @NonNull Pantalla pantalla) {
        this.inicio = inicio;
        this.quieto = quieto;
        this.conSplash = conSplash;
        this.pantalla = pantalla;
    }

    /**
     * El plan de un arranque: con el splash del sistema y la entrada de la G.
     *
     * @param tocado cuándo se tocó el icono, en el reloj que se use después.
     * @param quieto si el sistema tiene las animaciones quitadas.
     */
    @NonNull
    public static PlanArranque alTocar(long tocado, boolean quieto, @NonNull Pantalla p) {
        return new PlanArranque(tocado, quieto, true, p);
    }

    /** El plan de «Reintentar»: el splash ya no está y la G ya entró. */
    @NonNull
    public static PlanArranque reintento(long ahora, boolean quieto, @NonNull Pantalla p) {
        return new PlanArranque(ahora, quieto, false, p);
    }

    /** Si el splash del sistema tiene que seguir en pantalla (setKeepOnScreenCondition). */
    public boolean mantenerSplash(long ahora) {
        if (!conSplash || cerrado) return false;
        if (decidido && entradaVista(ahora)) return false;
        return ahora - inicio < SPLASH_SISTEMA_MS;
    }

    /** Ya se sabe adónde ir. Sale ahora o cuando acabe la entrada. */
    public void decidido(long ahora) {
        if (cerrado) return;
        decidido = true;
        tic(ahora);
    }

    /** Lo que toque a esta hora. La pantalla lo llama cuando dijo siguienteTic. */
    public void tic(long ahora) {
        if (cerrado) return;
        long t = ahora - inicio;
        if (decidido) {
            if (entradaVista(ahora)) {
                cerrado = true;
                pantalla.salir();
            }
            return;
        }
        if (t >= LIMITE_MS) {
            cerrado = true;
            pantalla.agotado();
            return;
        }
        if (t >= SPLASH_SISTEMA_MS && !brillo) {
            brillo = true;
            // El brillo va en bucle: con las animaciones quitadas, la G se queda quieta.
            if (!quieto) pantalla.empezarBrillo();
        }
        if (t >= CONECTANDO_MS && !conectando) {
            conectando = true;
            pantalla.mostrarConectando();
        }
    }

    /** Cuándo hay que volver a llamar a tic, en el mismo reloj; -1 si ya no hace falta. */
    public long siguienteTic(long ahora) {
        if (cerrado) return -1;
        if (decidido) return entradaVista(ahora) ? ahora : inicio + ENTRADA_MS;
        if (!brillo) return inicio + SPLASH_SISTEMA_MS;
        if (!conectando) return inicio + CONECTANDO_MS;
        return inicio + LIMITE_MS;
    }

    private boolean entradaVista(long ahora) {
        return quieto || !conSplash || ahora - inicio >= ENTRADA_MS;
    }
}
