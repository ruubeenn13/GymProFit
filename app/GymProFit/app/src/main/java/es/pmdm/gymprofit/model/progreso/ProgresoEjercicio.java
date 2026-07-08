package es.pmdm.gymprofit.model.progreso;

// ============================================================
// ProgresoEjercicio — un registro de progreso de un ejercicio (POJO Gson).
// Espeja ProgresoEjercicioDTO de la API: el mejor peso, mejores repeticiones y
// mejor tiempo alcanzados en una fecha. Alimenta la gráfica de progresión del
// detalle de ejercicio (evolución del mejor peso + marca de récord).
// ============================================================
public class ProgresoEjercicio {
    private int id;
    private int usuarioId;
    private int ejercicioId;
    private String fecha;          // ISO date-time
    private double mejorPeso;      // kg
    private int mejorRepeticiones;
    private int mejorTiempoSegundos;
    private String notas;

    public int getId() { return id; }
    public int getUsuarioId() { return usuarioId; }
    public int getEjercicioId() { return ejercicioId; }
    public String getFecha() { return fecha; }
    public double getMejorPeso() { return mejorPeso; }
    public int getMejorRepeticiones() { return mejorRepeticiones; }
    public int getMejorTiempoSegundos() { return mejorTiempoSegundos; }
    public String getNotas() { return notas; }
}
