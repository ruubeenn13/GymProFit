package es.pmdm.gymprofit.network.dto;

public class EjercicioDTO {
    private Integer id;
    private String nombre;
    private String descripcion;
    private String grupoMuscular;
    private String dificultad;
    private String imagenUrl;
    private String instrucciones;
    private Integer caloriasQuemadas;
    private String equipoNecesario;
    private Boolean activo;

    public Integer getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getGrupoMuscular() { return grupoMuscular; }
    public String getDificultad() { return dificultad; }
    public String getImagenUrl() { return imagenUrl; }
    public String getInstrucciones() { return instrucciones; }
    public Integer getCaloriasQuemadas() { return caloriasQuemadas; }
    public String getEquipoNecesario() { return equipoNecesario; }
    public Boolean getActivo() { return activo; }
}