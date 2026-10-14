package es.pmdm.gymprofit.model.plato;

import androidx.annotation.Nullable;

import es.pmdm.gymprofit.model.alimento.Alimento;

// ============================================================
// Ingrediente — un ingrediente de un plato, como lo devuelve la API (lote 1.6.6)
// El alimento entero, la cantidad con los nombres de una línea de comida y lo que vale.
// ============================================================
public class Ingrediente {

    private Alimento alimento;
    private double cantidadGramos;
    @Nullable private Integer racionId;
    @Nullable private String racionNombre;
    @Nullable private Double racionGramos;
    @Nullable private Double raciones;
    @Nullable private String racionUnidad;
    @Nullable private String racionUnidadPlural;
    @Nullable private Valores valores;

    public Ingrediente() {
    }

    public Alimento getAlimento() { return alimento; }

    public double getCantidadGramos() { return cantidadGramos; }

    @Nullable public Integer getRacionId() { return racionId; }

    @Nullable public Double getRaciones() { return raciones; }
}
