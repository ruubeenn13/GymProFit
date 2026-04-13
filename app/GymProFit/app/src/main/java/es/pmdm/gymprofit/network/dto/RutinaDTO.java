package es.pmdm.gymprofit.network.dto;

public class RutinaDTO {
    private Integer id;
    private String usuarioId;
    private String nombre;
    private String descripcion;
    private Integer duracionMinutos;
    private String nivel;
    private Boolean esPredefinida;
    private String categoria;
    private String diasSemana;
    private String fechaCreacion;
    private Boolean activa;

    public Integer getId() { return id; }
    public String getUsuarioId() { return usuarioId; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Integer getDuracionMinutos() { return duracionMinutos; }
    public String getNivel() { return nivel; }
    public Boolean getEsPredefinida() { return esPredefinida; }
    public String getCategoria() { return categoria; }
    public String getDiasSemana() { return diasSemana; }
    public String getFechaCreacion() { return fechaCreacion; }
    public Boolean getActiva() { return activa; }
}