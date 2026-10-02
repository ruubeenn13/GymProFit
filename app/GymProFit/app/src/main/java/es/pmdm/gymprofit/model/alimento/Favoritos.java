package es.pmdm.gymprofit.model.alimento;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

// ============================================================
// Favoritos — lo que devuelve GET /favoritos (lote 1.6.3)
// Los favoritos de la cuenta, ordenados por uso, y como mucho una propuesta de uno
// nuevo: un alimento que aparece en 4 comidas o más de las dos últimas semanas.
// ============================================================
public class Favoritos {

    @Nullable private List<Alimento> favoritos;
    @Nullable private Propuesta propuesta;

    /** La propuesta: el alimento y en cuántas comidas lo has añadido en 14 días. */
    public static class Propuesta {
        private Alimento alimento;
        private int veces;

        public Propuesta() {
        }

        public Propuesta(Alimento alimento, int veces) {
            this.alimento = alimento;
            this.veces = veces;
        }

        public Alimento getAlimento() { return alimento; }

        public int getVeces() { return veces; }
    }

    public Favoritos() {
    }

    public Favoritos(@Nullable List<Alimento> favoritos, @Nullable Propuesta propuesta) {
        this.favoritos = favoritos;
        this.propuesta = propuesta;
    }

    @NonNull
    public List<Alimento> getFavoritos() { return favoritos != null ? favoritos : new ArrayList<>(); }

    @Nullable
    public Propuesta getPropuesta() { return propuesta; }
}
