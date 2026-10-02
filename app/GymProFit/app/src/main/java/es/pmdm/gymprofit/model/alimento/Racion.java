package es.pmdm.gymprofit.model.alimento;

import androidx.annotation.Nullable;

// ============================================================
// Racion — una ración con nombre de un alimento (GP-127, lote 1.6.1)
// «1 rebanada», 28 g, en el idioma de la petición. El id falta en un producto que aún
// no está en el catálogo: entonces se elige por su posición en la lista.
// ============================================================
public class Racion {

    @Nullable
    private Integer id;
    private String nombre;
    private double gramos;

    public Racion() {
    }

    public Racion(@Nullable Integer id, String nombre, double gramos) {
        this.id = id;
        this.nombre = nombre;
        this.gramos = gramos;
    }

    @Nullable
    public Integer getId() { return id; }

    public String getNombre() { return nombre; }

    public double getGramos() { return gramos; }
}
