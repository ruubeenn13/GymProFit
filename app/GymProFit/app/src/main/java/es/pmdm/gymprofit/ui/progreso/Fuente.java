package es.pmdm.gymprofit.ui.progreso;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import es.pmdm.gymprofit.network.ApiCallback;
import retrofit2.Call;

// ============================================================
// Fuente — una petición de Progreso (GP-216, lote 4) y lo que se sabe de ella: si ha llegado, si
// ha fallado y con qué. Varias secciones leen la misma fuente (la lista de sesiones sirve al
// resumen, a «Todo» de los kilos y al historial), así que se pide una sola vez y avisa a quien la
// escucha al cambiar. Una respuesta que llega después de una petición más nueva se descarta.
// ============================================================
final class Fuente<T> {

    @Nullable private T dato;
    private boolean lista;
    private boolean fallo;
    private int code;
    @Nullable private String mensaje;
    private int generacion;

    /**
     * Pide de nuevo. Si lo anterior llegó bien, lo conserva mientras tanto (no parpadea al volver a
     * la pestaña); si fue un fallo, lo olvida, y la sección enseña que carga tras «Reintentar».
     */
    void pedir(@NonNull Call<T> call, @NonNull Fragment fragment, @NonNull Runnable alCambiar) {
        if (fallo) {
            lista = false;
            fallo = false;
        }
        final int esta = ++generacion;
        call.enqueue(new ApiCallback<T>() {
            @Override
            public void onOk(T body) {
                if (esta != generacion || !fragment.isAdded()) return;
                dato = body;
                lista = true;
                fallo = false;
                alCambiar.run();
            }

            @Override
            public void onFail(int c, String m) {
                if (esta != generacion || !fragment.isAdded()) return;
                // El fallo lo enseña la sección que lee esta fuente, con su «Reintentar».
                lista = true;
                fallo = true;
                code = c;
                mensaje = m;
                alCambiar.run();
            }
        });
    }

    /** Ha respondido (bien o mal) al menos una vez. */
    boolean lista() { return lista; }

    /** La última respuesta fue un fallo. */
    boolean fallo() { return fallo; }

    /** Llegó bien: hay dato que pintar (puede ser null si la API no mandó cuerpo). */
    boolean ok() { return lista && !fallo; }

    @Nullable T dato() { return dato; }

    int code() { return code; }

    @Nullable String mensaje() { return mensaje; }
}
