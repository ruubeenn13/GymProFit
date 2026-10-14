package es.pmdm.gymprofit.model.plato;

import androidx.annotation.Nullable;

// ============================================================
// Valores — kcal y macros de una cantidad (lote 1.6.6)
// En un plato: el total de sus ingredientes y lo que vale una ración (lo que se apunta
// con «1 ración»). gramos, solo en la ración de un plato con peso hecho.
// ============================================================
public class Valores {

    private int calorias;
    private double proteinas;
    private double carbohidratos;
    private double grasas;
    @Nullable private Double fibra;
    @Nullable private Double gramos;

    public Valores() {
    }

    public int getCalorias() { return calorias; }

    public double getProteinas() { return proteinas; }

    public double getCarbohidratos() { return carbohidratos; }

    public double getGrasas() { return grasas; }

    @Nullable public Double getFibra() { return fibra; }

    @Nullable public Double getGramos() { return gramos; }
}
