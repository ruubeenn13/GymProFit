package es.pmdm.gymprofit.ui.alta;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// AnchoCreando — el ancho del botón de «Guarda tu plan» mientras se crea la cuenta (GP-163).
//
// Al pulsar «Crear cuenta» el botón encoge hasta un círculo de 52 dp en 300 ms (momento 9
// de DEC-039). Esa animación tiene que pararse en cuanto se sale de «creando»: si la API
// responde antes de que acabe —«correo en uso» con la API despierta lo hace—, el fallo
// devolvía el ancho entero y la animación, que seguía en marcha, lo volvía a encoger, y
// el botón se quedaba hecho un círculo con «Crear cuenta» cortado dentro.
//
// Por eso la animación vive aquí y no suelta: cada salida la para y deja el ancho final,
// el entero si falla y el círculo si sale bien, sea cual sea el tiempo de respuesta.
// No sabe nada de vistas: el Animador lo pone la pantalla, y los tests uno a mano.
// ============================================================
public final class AnchoCreando {

    /** Pone el ancho del botón, en px (o el valor de «entero» del layout). */
    public interface Ancho {
        void poner(int px);
    }

    /** Una animación de ancho en marcha. */
    public interface EnMarcha {
        /** La para donde esté: no vuelve a poner ningún ancho. */
        void cancelar();

        /** La lleva a su valor final de golpe y la para. */
        void acabar();
    }

    /** Arranca una animación de ancho de {@code desde} a {@code hasta}. */
    public interface Animador {
        @NonNull
        EnMarcha animar(int desde, int hasta, @NonNull Ancho alPaso);
    }

    private final Animador animador;
    private final Ancho ancho;
    private final int entero;
    @Nullable private EnMarcha enMarcha;

    /**
     * @param animador cómo se anima el ancho.
     * @param ancho    dónde se pone.
     * @param entero   el ancho del botón entero tal y como lo pide el layout.
     */
    public AnchoCreando(@NonNull Animador animador, @NonNull Ancho ancho, int entero) {
        this.animador = animador;
        this.ancho = ancho;
        this.entero = entero;
    }

    /** Empieza a encoger hasta el círculo. Una animación anterior, si la hay, se para. */
    public void encoger(int desde, int circulo) {
        parar();
        enMarcha = animador.animar(desde, circulo, ancho);
    }

    /** Ha fallado: el botón vuelve entero y nada lo vuelve a encoger. */
    public void volverEntero() {
        parar();
        ancho.poner(entero);
    }

    /** Ha salido bien: el botón se queda en círculo, acabe o no de encoger. */
    public void quedarseEnCirculo() {
        if (enMarcha != null) enMarcha.acabar();
        enMarcha = null;
    }

    /** La pantalla se va: la animación no sigue tocando una vista que ya no se ve. */
    public void parar() {
        if (enMarcha != null) enMarcha.cancelar();
        enMarcha = null;
    }
}
