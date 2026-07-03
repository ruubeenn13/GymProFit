package es.pmdm.gymprofit.model.rutina;

// ============================================================
// RutinaEjercicio — relación entre una rutina y uno de sus ejercicios.
// Modela cada fila devuelta por el endpoint "rutinas-ejercicios/rutina/{id}"
// (ejercicioId + su configuración de series/repeticiones/orden dentro de la
// rutina). Deserializado por Gson; las claves JSON coinciden con los
// atributos camelCase, por lo que no requiere @SerializedName.
// ============================================================
public class RutinaEjercicio {

    // Identificador único de la relación rutina-ejercicio.
    private int id;
    // Id de la rutina a la que pertenece la relación.
    private int rutinaId;
    // Id del ejercicio asociado.
    private int ejercicioId;
    // Número de series configuradas para el ejercicio en esta rutina.
    private int series;
    // Número de repeticiones por serie.
    private int repeticiones;
    // Posición del ejercicio dentro de la rutina.
    private int orden;
    // Calorías estimadas del ejercicio (dato enriquecido desde el catálogo por la API).
    private int caloriasEjercicio;
    // Nombre del ejercicio (dato enriquecido desde el catálogo por la API).
    private String nombreEjercicio;

    public RutinaEjercicio() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRutinaId() { return rutinaId; }
    public void setRutinaId(int rutinaId) { this.rutinaId = rutinaId; }

    public int getEjercicioId() { return ejercicioId; }
    public void setEjercicioId(int ejercicioId) { this.ejercicioId = ejercicioId; }

    public int getSeries() { return series; }
    public void setSeries(int series) { this.series = series; }

    public int getRepeticiones() { return repeticiones; }
    public void setRepeticiones(int repeticiones) { this.repeticiones = repeticiones; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }

    public int getCaloriasEjercicio() { return caloriasEjercicio; }
    public void setCaloriasEjercicio(int caloriasEjercicio) { this.caloriasEjercicio = caloriasEjercicio; }

    public String getNombreEjercicio() { return nombreEjercicio; }
    public void setNombreEjercicio(String nombreEjercicio) { this.nombreEjercicio = nombreEjercicio; }
}
