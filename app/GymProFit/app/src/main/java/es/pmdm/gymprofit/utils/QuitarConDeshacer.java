package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// QuitarConDeshacer — quitar sin preguntar, con «Deshacer» (decisión 16, momento 20,
// lote 1.6.2)
//
// Quitar no llama a la API: la fila se va de la pantalla y queda pendiente. «Deshacer» la
// devuelve sin llamar a nada. El borrado se manda cuando el aviso se va —por tiempo,
// porque se quita otra o porque se sale de la pantalla— y, al salir, la pantalla espera
// la respuesta antes de volver, para que el diario no cuente lo que ya no está. Si la API
// falla, la fila vuelve a su sitio y se dice.
// Solo hay una pendiente a la vez: es la que puede deshacer el aviso que se ve. Lo
// pendiente y lo enviado sin responder quedan apartados: una recarga no lo devuelve
// (GP-179).
// Sin vistas ni red: quien lo usa pone el envío y lo que se hace en pantalla.
// ============================================================
public final class QuitarConDeshacer<T> {

    /** La respuesta del envío; la primera que llega es la que cuenta. */
    public interface Respuesta {
        void ok();

        void fallo(int code, @Nullable String message);
    }

    /** Manda el borrado de verdad. */
    public interface Envio<T> {
        void enviar(@NonNull T item, @NonNull Respuesta r);
    }

    /** Lo que pasa en pantalla. */
    public interface Vista<T> {
        /** Falló el borrado: la fila vuelve a su sitio y se dice por qué. */
        void devolver(@NonNull T item, int posicion, int code, @Nullable String message);

        /** Ya no queda nada por mandar ni por responder: se puede volver. */
        void listoParaSalir();
    }

    /** Lo quitado que aún se puede deshacer, con la posición que tenía. */
    public static final class Pendiente<T> {
        @NonNull public final T item;
        public final int posicion;

        Pendiente(@NonNull T item, int posicion) {
            this.item = item;
            this.posicion = posicion;
        }
    }

    private final Envio<T> envio;
    private final Vista<T> vista;
    @Nullable private Pendiente<T> pendiente;
    // Lo enviado que aún no ha respondido.
    private final java.util.List<T> enCamino = new java.util.ArrayList<>();
    private int enVuelo;
    private boolean saliendo;
    private boolean avisadoSalida;

    public QuitarConDeshacer(@NonNull Envio<T> envio, @NonNull Vista<T> vista) {
        this.envio = envio;
        this.vista = vista;
    }

    /** Quita sin mandar nada; si había otra pendiente, esa se manda ya. */
    public void quitar(@NonNull T item, int posicion) {
        confirmar();
        pendiente = new Pendiente<>(item, posicion);
    }

    /** «Deshacer»: la pendiente, para devolverla a su sitio, o null si no hay. */
    @Nullable
    public Pendiente<T> deshacer() {
        Pendiente<T> p = pendiente;
        pendiente = null;
        return p;
    }

    public boolean hayPendiente() {
        return pendiente != null;
    }

    /**
     * Lo que no tiene que volver a la lista si se recarga: la pendiente y lo enviado que
     * aún no ha respondido (GP-179).
     */
    @NonNull
    public java.util.List<T> apartados() {
        java.util.List<T> l = new java.util.ArrayList<>(enCamino);
        if (pendiente != null) l.add(pendiente.item);
        return l;
    }

    /** El aviso se ha ido: se manda la pendiente, si la hay. */
    public void confirmar() {
        Pendiente<T> p = pendiente;
        if (p == null) return;
        pendiente = null;
        enVuelo++;
        enCamino.add(p.item);
        envio.enviar(p.item, new Respuesta() {
            private boolean respondida;

            @Override
            public void ok() {
                if (respondida) return;
                respondida = true;
                enCamino.remove(p.item);
                terminado();
            }

            @Override
            public void fallo(int code, @Nullable String message) {
                if (respondida) return;
                respondida = true;
                enCamino.remove(p.item);
                vista.devolver(p.item, p.posicion, code, message);
                terminado();
            }
        });
    }

    /** Se sale de la pantalla: se manda lo pendiente y se avisa cuando todo ha respondido. */
    public void salir() {
        saliendo = true;
        confirmar();
        comprobarSalida();
    }

    private void terminado() {
        enVuelo--;
        comprobarSalida();
    }

    private void comprobarSalida() {
        if (saliendo && enVuelo == 0 && !avisadoSalida) {
            avisadoSalida = true;
            vista.listoParaSalir();
        }
    }
}
