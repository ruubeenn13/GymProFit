package es.pmdm.gymprofit.model.comida;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

// ============================================================
// CopiaRespuesta — lo que devuelve POST /comidas/copiar (lote 1.6.4)
// La comida de destino con sus totales y, por cada alimento copiado, su línea y lo que
// tenía antes (null si es nueva), como al añadir: con eso se quita exacto, uno a uno.
// ============================================================
public class CopiaRespuesta {

    private Comida comida;
    private List<Copiada> lineas;

    public CopiaRespuesta() {
    }

    public CopiaRespuesta(Comida comida, List<Copiada> lineas) {
        this.comida = comida;
        this.lineas = lineas;
    }

    public Comida getComida() { return comida; }

    @NonNull
    public List<Copiada> getLineas() { return lineas != null ? lineas : new ArrayList<>(); }

    /** Un alimento copiado: su línea en el destino y lo que tenía antes. */
    public static class Copiada {
        private AlimentoComida linea;
        @Nullable private CantidadAnterior anterior;

        public Copiada() {
        }

        public Copiada(AlimentoComida linea, @Nullable CantidadAnterior anterior) {
            this.linea = linea;
            this.anterior = anterior;
        }

        public AlimentoComida getLinea() { return linea; }

        @Nullable
        public CantidadAnterior getAnterior() { return anterior; }
    }
}
