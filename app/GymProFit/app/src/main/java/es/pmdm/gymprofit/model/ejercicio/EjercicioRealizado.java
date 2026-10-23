package es.pmdm.gymprofit.model.ejercicio;

import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// ============================================================
// EjercicioRealizado — un elemento de GET /ejercicios-realizados/ejercicio/{id} (GP-216,
// ficha del ejercicio): lo que el usuario hizo de ese ejercicio en una sesión, con cada
// serie. La fecha no viene: se pone con la sesión (sesionId).
// ============================================================
public class EjercicioRealizado {
    private Integer sesionId;
    private List<Serie> series;

    @Nullable public Integer getSesionId() { return sesionId; }
    public List<Serie> getSeries() { return series != null ? series : new ArrayList<>(); }

    /** Una serie hecha; el peso es null en peso corporal y segundos solo en las de tiempo. */
    public static class Serie {
        private Integer numero;
        private Integer repeticiones;
        private Integer segundos;
        private BigDecimal peso;
        private Boolean completada;

        public int getNumero() { return numero == null ? 0 : numero; }
        public int getRepeticiones() { return repeticiones == null ? 0 : repeticiones; }
        public int getSegundos() { return segundos == null ? 0 : segundos; }
        public double getPeso() { return peso == null ? 0 : peso.doubleValue(); }
        /** Si se marcó como completada; las que no, no cuentan en «Tus últimas veces». */
        public boolean isCompletada() { return completada == null || completada; }
    }
}
