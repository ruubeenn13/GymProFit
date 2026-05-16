package es.pmdm.gymprofit.model.rutina;

public class Rutina {

    private int id;
    private String nombre;
    private String nivel;
    private String descripcion;
    private int numEjercicios;
    private int duracionMinutos;
    private int caloriasAproximadas;
    private boolean predefinida;
    private int usuarioId;
    private String categoria;
    private String diasSemana;
    private String fechaCreacion;
    private boolean activa;

    public Rutina() {}

    public Rutina(String nombre, String nivel, String descripcion,
                  int numEjercicios, int duracionMinutos, int caloriasAproximadas) {
        this.nombre = nombre;
        this.nivel = nivel;
        this.descripcion = descripcion;
        this.numEjercicios = numEjercicios;
        this.duracionMinutos = duracionMinutos;
        this.caloriasAproximadas = caloriasAproximadas;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getNumEjercicios() { return numEjercicios; }
    public void setNumEjercicios(int numEjercicios) { this.numEjercicios = numEjercicios; }

    public int getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(int duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public int getCaloriasAproximadas() { return caloriasAproximadas; }
    public void setCaloriasAproximadas(int caloriasAproximadas) { this.caloriasAproximadas = caloriasAproximadas; }

    public boolean isPredefinida() { return predefinida; }
    public void setPredefinida(boolean predefinida) { this.predefinida = predefinida; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getDiasSemana() { return diasSemana; }
    public void setDiasSemana(String diasSemana) { this.diasSemana = diasSemana; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
