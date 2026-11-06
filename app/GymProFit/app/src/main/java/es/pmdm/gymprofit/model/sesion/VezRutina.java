package es.pmdm.gymprofit.model.sesion;

import androidx.annotation.Nullable;

// ============================================================
// VezRutina — una vez que se hizo una rutina: una barra de su tarjeta (GP-232).
// Las barras de una sesión y la «última vez» de una rutina son lo mismo: sus sesiones
// terminadas, cada una con sus kilos (GP-011) y si batió algún récord.
// ============================================================
public class VezRutina {

    private int sesionId;
    private String fecha;
    private double kilos;
    private boolean record;

    public VezRutina() { }

    public VezRutina(int sesionId, @Nullable String fecha, double kilos, boolean record) {
        this.sesionId = sesionId;
        this.fecha = fecha;
        this.kilos = kilos;
        this.record = record;
    }

    public int getSesionId() { return sesionId; }

    /** Inicio, ISO en la hora local. */
    @Nullable
    public String getFecha() { return fecha; }

    public double getKilos() { return kilos; }

    public boolean isRecord() { return record; }
}
