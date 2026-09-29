package es.pmdm.gymprofit.model.record;

// ============================================================
// PuntoProgresion — un punto de la gráfica de un ejercicio (GP-088).
//
// La mejor serie de una sesión completada. La API los entrega en orden cronológico,
// del más antiguo al más reciente, que es como se dibujan.
// ============================================================
public class PuntoProgresion {

    private Integer sesionId;
    private String fecha;          // ISO date-time
    private String tipo;           // Record.TIPO_PESO o Record.TIPO_REPETICIONES
    private Double peso;
    private Integer repeticiones;
    private Integer segundos;      // solo en los de tiempo (GP-125)

    public String getFecha() { return fecha; }
    public String getTipo() { return tipo; }
    public double getPeso() { return peso == null ? 0 : peso; }
    public int getRepeticiones() { return repeticiones == null ? 0 : repeticiones; }

    public int getSegundos() { return segundos == null ? 0 : segundos; }

    /** @return {@code true} si el punto se mide en kilos. */
    public boolean esDePeso() { return Record.TIPO_PESO.equals(tipo); }

    /** @return {@code true} si el punto se mide en segundos (GP-125). */
    public boolean esDeTiempo() { return Record.TIPO_TIEMPO.equals(tipo); }
}
