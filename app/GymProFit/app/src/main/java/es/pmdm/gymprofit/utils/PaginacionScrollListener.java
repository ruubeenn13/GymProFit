package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// ============================================================
// PaginacionScrollListener — scroll infinito para listados paginados
// Listener reutilizable de RecyclerView que dispara cargarMas() cuando el
// usuario se acerca al final de la lista (umbral de 5 ítems), evitando
// peticiones duplicadas mientras hay una carga en curso o si ya se
// recibió la última página del servidor.
// ============================================================
public abstract class PaginacionScrollListener extends RecyclerView.OnScrollListener {

    // Ítems restantes por debajo del viewport que disparan la siguiente página
    private static final int UMBRAL = 5;

    private final LinearLayoutManager layoutManager;

    public PaginacionScrollListener(LinearLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        // Solo interesa el scroll hacia abajo
        if (dy <= 0) return;

        int visibles = layoutManager.getChildCount();
        int total = layoutManager.getItemCount();
        int primeraVisible = layoutManager.findFirstVisibleItemPosition();

        // Cerca del final, sin carga en curso y con páginas pendientes → pedir más
        if (!isCargando() && !esUltimaPagina()
                && primeraVisible >= 0
                && (visibles + primeraVisible) >= total - UMBRAL) {
            cargarMas();
        }
    }

    // Pide la siguiente página al servidor.
    protected abstract void cargarMas();

    // true mientras hay una petición de página en vuelo.
    protected abstract boolean isCargando();

    // true si el servidor ya devolvió last=true.
    protected abstract boolean esUltimaPagina();
}
