package es.pmdm.gymprofit.model.logro;

public class Logro {

    private int id;
    private String nombre;
    private String descripcion;
    private String icono;
    private String criterio;
    private int valorObjetivo;

    public Logro() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getIcono() { return icono; }
    public void setIcono(String icono) { this.icono = icono; }

    public String getCriterio() { return criterio; }
    public void setCriterio(String criterio) { this.criterio = criterio; }

    public int getValorObjetivo() { return valorObjetivo; }
    public void setValorObjetivo(int valorObjetivo) { this.valorObjetivo = valorObjetivo; }
}
