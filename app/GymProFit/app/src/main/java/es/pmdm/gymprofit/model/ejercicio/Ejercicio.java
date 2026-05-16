package es.pmdm.gymprofit.model.ejercicio;

public class Ejercicio {

    private int id;
    private String nombre;
    private String grupoMuscular;
    private String dificultad;
    private String descripcion;
    private String instrucciones;
    private String imagenUrl;
    private String equipoNecesario;
    private int calorias;
    private boolean activo;

    public Ejercicio() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getGrupoMuscular() { return grupoMuscular; }
    public void setGrupoMuscular(String grupoMuscular) { this.grupoMuscular = grupoMuscular; }

    public String getDificultad() { return dificultad; }
    public void setDificultad(String dificultad) { this.dificultad = dificultad; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getInstrucciones() { return instrucciones; }
    public void setInstrucciones(String instrucciones) { this.instrucciones = instrucciones; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public String getEquipoNecesario() { return equipoNecesario; }
    public void setEquipoNecesario(String equipoNecesario) { this.equipoNecesario = equipoNecesario; }

    public int getCalorias() { return calorias; }
    public void setCalorias(int calorias) { this.calorias = calorias; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
