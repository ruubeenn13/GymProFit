package es.pmdm.gymprofit.network.dto;

public class RutinaCreateDTO {
    private Integer usuarioId;
    private String nombre;
    private String descripcion;
    private Integer duracionMinutos;
    private String nivel;
    private Boolean esPredefinida;

    public RutinaCreateDTO(Integer usuarioId, String nombre, String descripcion,
                           Integer duracionMinutos, String nivel) {
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracionMinutos = duracionMinutos;
        this.nivel = nivel;
        this.esPredefinida = false;
    }

    public Integer getUsuarioId() { return usuarioId; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Integer getDuracionMinutos() { return duracionMinutos; }
    public String getNivel() { return nivel; }
    public Boolean getEsPredefinida() { return esPredefinida; }
}