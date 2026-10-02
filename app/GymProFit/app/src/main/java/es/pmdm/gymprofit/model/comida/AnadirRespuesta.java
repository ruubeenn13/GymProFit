package es.pmdm.gymprofit.model.comida;

// ============================================================
// AnadirRespuesta — lo que devuelve POST /comidas/anadir (lote 1.6.1)
// La comida con sus totales ya recalculados, y la línea creada o sumada. Desde la 1.6.3,
// también lo que tenía la línea antes (null si es nueva), para deshacer exacto.
// ============================================================
public class AnadirRespuesta {

    private Comida comida;
    private AlimentoComida linea;
    @androidx.annotation.Nullable private CantidadAnterior anterior;

    public AnadirRespuesta() {
    }

    public AnadirRespuesta(Comida comida, AlimentoComida linea, @androidx.annotation.Nullable CantidadAnterior anterior) {
        this.comida = comida;
        this.linea = linea;
        this.anterior = anterior;
    }

    public Comida getComida() { return comida; }

    public AlimentoComida getLinea() { return linea; }

    @androidx.annotation.Nullable
    public CantidadAnterior getAnterior() { return anterior; }
}
