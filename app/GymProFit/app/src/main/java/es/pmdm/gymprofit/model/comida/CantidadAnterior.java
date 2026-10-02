package es.pmdm.gymprofit.model.comida;

import androidx.annotation.Nullable;

// ============================================================
// CantidadAnterior — lo que tenía una línea antes de sumarle (lote 1.6.3, A2)
// Viene en la respuesta de POST /comidas/anadir cuando el alimento ya estaba en la
// comida; deshacer manda esto tal cual en el PATCH de la línea.
// ============================================================
public class CantidadAnterior {

    private double cantidadGramos;
    @Nullable private Integer racionId;
    @Nullable private Double raciones;

    public CantidadAnterior() {
    }

    public CantidadAnterior(double cantidadGramos, @Nullable Integer racionId, @Nullable Double raciones) {
        this.cantidadGramos = cantidadGramos;
        this.racionId = racionId;
        this.raciones = raciones;
    }

    public double getCantidadGramos() { return cantidadGramos; }

    @Nullable public Integer getRacionId() { return racionId; }

    @Nullable public Double getRaciones() { return raciones; }
}
