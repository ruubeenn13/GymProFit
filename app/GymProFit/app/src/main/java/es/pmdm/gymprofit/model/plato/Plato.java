package es.pmdm.gymprofit.model.plato;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.model.alimento.Alimento;

// ============================================================
// Plato — un plato propio con sus ingredientes (lote 1.6.6, DEC-047)
// alimento es el plato como lo ve cualquier lista; porRacion, lo que se apunta con
// «1 ración»; total, la suma de sus ingredientes con los valores de hoy.
// ============================================================
public class Plato {

    private int id;
    private String nombre;
    private int raciones;
    @Nullable private Double pesoGramos;
    private boolean activo;
    @Nullable private Alimento alimento;
    @Nullable private Valores total;
    @Nullable private Valores porRacion;
    @Nullable private List<Ingrediente> ingredientes;

    public Plato() {
    }

    public int getId() { return id; }

    public String getNombre() { return nombre; }

    public int getRaciones() { return raciones; }

    @Nullable public Double getPesoGramos() { return pesoGramos; }

    public boolean isActivo() { return activo; }

    @Nullable public Alimento getAlimento() { return alimento; }

    @Nullable public Valores getPorRacion() { return porRacion; }

    @NonNull public List<Ingrediente> getIngredientes() { return ingredientes != null ? ingredientes : new ArrayList<>(); }
}
