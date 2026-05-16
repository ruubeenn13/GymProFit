package es.pmdm.gymprofit.model.objetivo;

public class ObjetivoPersonal {

    private int id;
    private int usuarioId;
    private String tipo;
    private String descripcion;
    private double valorObjetivo;
    private double valorActual;
    private String fechaInicio;
    private String fechaFin;
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
