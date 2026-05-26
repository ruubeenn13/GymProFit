package es.pmdm.gymprofit.model.alimento;

public class Alimento {

    private int id;
    private String nombre;
    private String categoria;
    private int calorias;
    private double proteinas;
    private double carbohidratos;
    private double grasas;
    private Integer usuarioId;
    private boolean activo;

    public Alimento() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public int getCalorias() { return calorias; }
    public void setCalorias(int calorias) { this.calorias = calorias; }

    public double getProteinas() { return proteinas; }
    public void setProteinas(double proteinas) { this.proteinas = proteinas; }

    public double getCarbohidratos() { return carbohidratos; }
    public void setCarbohidratos(double carbohidratos) { this.carbohidratos = carbohidratos; }

    public double getGrasas() { return grasas; }
    public void setGrasas(double grasas) { this.grasas = grasas; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
