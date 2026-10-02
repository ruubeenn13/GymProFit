package es.pmdm.gymprofit.model.alimento;

import androidx.annotation.Nullable;

// ============================================================
// UltimaCantidad — lo último que apuntaste de un alimento (lote 1.6.3, A1)
// Los gramos de su línea más reciente y, si se apuntó por raciones y la ración sigue
// pesando lo mismo, la ración y cuántas. Los mismos nombres que la línea de una comida.
// ============================================================
public class UltimaCantidad {

    private double cantidadGramos;
    @Nullable private Integer racionId;
    @Nullable private String racionNombre;
    @Nullable private Double racionGramos;
    @Nullable private Double raciones;
    @Nullable private String racionUnidad;
    @Nullable private String racionUnidadPlural;

    public UltimaCantidad() {
    }

    public UltimaCantidad(double cantidadGramos, @Nullable Integer racionId, @Nullable String racionNombre,
                          @Nullable Double raciones, @Nullable String racionUnidad, @Nullable String racionUnidadPlural) {
        this.cantidadGramos = cantidadGramos;
        this.racionId = racionId;
        this.racionNombre = racionNombre;
        this.raciones = raciones;
        this.racionUnidad = racionUnidad;
        this.racionUnidadPlural = racionUnidadPlural;
    }

    public double getCantidadGramos() { return cantidadGramos; }

    @Nullable public Integer getRacionId() { return racionId; }

    @Nullable public String getRacionNombre() { return racionNombre; }

    @Nullable public Double getRacionGramos() { return racionGramos; }

    @Nullable public Double getRaciones() { return raciones; }

    @Nullable public String getRacionUnidad() { return racionUnidad; }

    @Nullable public String getRacionUnidadPlural() { return racionUnidadPlural; }
}
