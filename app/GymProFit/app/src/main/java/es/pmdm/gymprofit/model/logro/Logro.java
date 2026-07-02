package es.pmdm.gymprofit.model.logro;

// ============================================================
// Logro — modelo de datos de un logro/insignia del sistema de gamificación.
// Define el criterio y el valor objetivo que el usuario debe alcanzar
// para desbloquear el logro dentro de GymProFit.
// ============================================================
public class Logro {

    // Identificador único del logro.
    private int id;
    // Nombre del logro.
    private String nombre;
    // Descripción del logro.
    private String descripcion;
    // Icono asociado al logro (nombre/recurso o URL).
    private String icono;
    // Criterio o tipo de métrica que evalúa el logro.
    private String criterio;
    // Valor que debe alcanzar el criterio para completar el logro.
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
