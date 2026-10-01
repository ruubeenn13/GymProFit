package es.pmdm.gymprofit.ui.alta;

// ============================================================
// Alta — lo que las pantallas del cuestionario piden a quien las aloja (GP-103).
//
// Las preguntas guardan cada respuesta en el borrador al elegirla; para avanzar avisan
// aquí, y es la actividad la que decide qué viene después, mueve la barra y anima el
// cambio de pantalla.
// ============================================================
public interface Alta {

    /** Pasa a la siguiente pantalla del cuestionario. */
    void siguiente();

    /**
     * Si se completa el perfil de una cuenta que ya existe (entró sin onboarding): el
     * plan acaba en «Empezar» y no en «Guarda tu plan».
     */
    boolean esCuentaExistente();
}
