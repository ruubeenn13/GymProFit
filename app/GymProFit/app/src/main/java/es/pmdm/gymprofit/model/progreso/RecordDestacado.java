package es.pmdm.gymprofit.model.progreso;

// ============================================================
// RecordDestacado — el mejor levantamiento del usuario, tal y como lo manda la API.
// Trae el nombre del ejercicio ya resuelto para que la tarjeta de la pantalla de
// inicio pueda escribirse con una sola llamada.
// ============================================================
public class RecordDestacado {

    private int ejercicioId;
    private String ejercicioNombre;
    private Double peso;
    private Integer repeticiones;
    private String fecha;          // ISO date-time

    public int getEjercicioId() { return ejercicioId; }
    public String getEjercicioNombre() { return ejercicioNombre; }
    public double getPeso() { return peso == null ? 0 : peso; }
    public int getRepeticiones() { return repeticiones == null ? 0 : repeticiones; }
    public String getFecha() { return fecha; }
}
