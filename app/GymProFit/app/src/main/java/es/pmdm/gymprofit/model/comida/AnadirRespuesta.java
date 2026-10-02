package es.pmdm.gymprofit.model.comida;

// ============================================================
// AnadirRespuesta — lo que devuelve POST /comidas/anadir (lote 1.6.1)
// La comida con sus totales ya recalculados, y la línea creada o sumada.
// ============================================================
public class AnadirRespuesta {

    private Comida comida;
    private AlimentoComida linea;

    public Comida getComida() { return comida; }

    public AlimentoComida getLinea() { return linea; }
}
