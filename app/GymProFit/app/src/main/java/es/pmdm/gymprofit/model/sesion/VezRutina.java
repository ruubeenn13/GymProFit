package es.pmdm.gymprofit.model.sesion;

import androidx.annotation.Nullable;

// ============================================================
// VezRutina — una vez que se hizo una rutina: una barra de su tarjeta (GP-232).
// Las barras de una sesión y la «última vez» de una rutina son lo mismo: sus sesiones
// terminadas, cada una con sus kilos (GP-011), sus series hechas y si batió algún récord.
// Las series son la cifra de una vez a 0 kilos (peso corporal o por tiempo, CifraTarjeta).
// ============================================================
public class VezRutina {

    private int sesionId;
    private String fecha;
    private double kilos;
    private boolean record;
    private int series;

    public VezRutina() { }

    public VezRutina(int sesionId, @Nullable String fecha, double kilos, boolean record) {
        this(sesionId, fecha, kilos, record, 0);
    }

    public VezRutina(int sesionId, @Nullable String fecha, double kilos, boolean record, int series) {
        this.sesionId = sesionId;
        this.fecha = fecha;
        this.kilos = kilos;
        this.record = record;
        this.series = series;
    }

    public int getSesionId() { return sesionId; }

    /** Inicio, ISO en la hora local. */
    @Nullable
    public String getFecha() { return fecha; }

    public double getKilos() { return kilos; }

    public boolean isRecord() { return record; }

    /** Series hechas; 0 si la API aún no las manda. */
    public int getSeries() { return series; }
}
