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
    // Lote 1.6.2 (GP-172): la unidad en singular y en plural («rebanada», «rebanadas»), o
    // null si el nombre no la tiene («Media taza»).
    @Nullable private String unidad;
    @Nullable private String unidadPlural;
    // La clave de una ración propia (UNIDAD, RACION, ENVASE, REBANADA); null en las demás.
    @Nullable private String clave;

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

    @Nullable
    public String getUnidad() { return unidad; }

    @Nullable
    public String getUnidadPlural() { return unidadPlural; }

    @Nullable
    public String getClave() { return clave; }
}
