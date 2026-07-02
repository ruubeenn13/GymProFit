package es.pmdm.gymprofit.model.objetivo;

// ============================================================
// ObjetivoPersonal — modelo de datos de un objetivo personal del usuario.
// Representa una meta (peso, calorías, etc.) con un valor objetivo, el
// progreso actual y el rango de fechas, usada en el seguimiento de metas.
// ============================================================
public class ObjetivoPersonal {

    // Identificador único del objetivo.
    private int id;
    // Id del usuario propietario del objetivo.
    private int usuarioId;
    // Tipo de objetivo (peso, grasa corporal, calorías, etc.).
    private String tipo;
    // Descripción del objetivo.
    private String descripcion;
    // Valor que se desea alcanzar.
    private double valorObjetivo;
    // Valor actual/progreso hacia el objetivo.
    private double valorActual;
    // Fecha de inicio del objetivo.
    private String fechaInicio;
    // Fecha límite/fin del objetivo.
    private String fechaFin;
    // Indica si el objetivo ya se ha completado.
    private boolean completado;

    public ObjetivoPersonal() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getValorObjetivo() { return valorObjetivo; }
    public void setValorObjetivo(double valorObjetivo) { this.valorObjetivo = valorObjetivo; }

    public double getValorActual() { return valorActual; }
    public void setValorActual(double valorActual) { this.valorActual = valorActual; }

    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getFechaFin() { return fechaFin; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }

    public boolean isCompletado() { return completado; }
    public void setCompletado(boolean completado) { this.completado = completado; }
}
