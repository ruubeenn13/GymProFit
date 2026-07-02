package es.pmdm.gymprofit.model.rutina;

// ============================================================
// Rutina — modelo de datos de una rutina de entrenamiento.
// Puede ser predefinida (catálogo de la app) o creada por el usuario;
// agrupa metadatos como nivel, duración y calorías aproximadas usados
// en las pantallas de listado y detalle de rutinas.
// ============================================================
public class Rutina {

    // Identificador único de la rutina.
    private int id;
    // Nombre de la rutina.
    private String nombre;
    // Nivel de dificultad (principiante, intermedio, avanzado...).
    private String nivel;
    // Descripción de la rutina.
    private String descripcion;
    // Número de ejercicios que componen la rutina.
    private int numEjercicios;
    // Duración estimada de la rutina en minutos.
    private int duracionMinutos;
    // Calorías aproximadas que se queman al completarla.
    private int caloriasAproximadas;
    // Indica si es una rutina predefinida del sistema (no creada por el usuario).
    private boolean predefinida;
    // Id del usuario propietario, si la rutina es personalizada.
    private int usuarioId;
    // Categoría de la rutina (fuerza, cardio, etc.).
    private String categoria;
    // Días de la semana en los que se realiza la rutina.
    private String diasSemana;
    // Fecha de creación de la rutina.
    private String fechaCreacion;
    // Indica si la rutina está activa/en uso.
    private boolean activa;

    public Rutina() {}

    // Constructor con los datos básicos para crear una rutina nueva.
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
