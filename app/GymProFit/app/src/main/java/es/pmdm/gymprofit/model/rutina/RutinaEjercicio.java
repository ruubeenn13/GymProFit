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
    // NO HAY CAMPO DE CALORÍAS AQUÍ, Y ES A PROPÓSITO (DEC-004 / GP-010).
    // La API dejó de enviarlo: el gasto calórico de un entrenamiento no se puede
    // estimar con los datos que hay, así que no se estima ni se enseña.
    // Nombre del ejercicio (dato enriquecido desde el catálogo por la API).
    private String nombreEjercicio;
    // Descanso entre series, en segundos, y notas de la rutina.
    private Integer tiempoDescanso;
    private String notas;
    // Pauta de las rutinas de programa (GP-074/GP-125); nulos en las propias. El rango es
    // de repeticiones, o de segundos si la medida es SEGUNDOS. repeticiones lleva el máximo.
    private Integer repeticionesMin;
    private Integer repeticionesMax;
    private String medida;
    private String tipo;
    private String porLado;

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


    public String getNombreEjercicio() { return nombreEjercicio; }
    public void setNombreEjercicio(String nombreEjercicio) { this.nombreEjercicio = nombreEjercicio; }

    public Integer getTiempoDescanso() { return tiempoDescanso; }
    public void setTiempoDescanso(Integer tiempoDescanso) { this.tiempoDescanso = tiempoDescanso; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public Integer getRepeticionesMin() { return repeticionesMin; }
    public void setRepeticionesMin(Integer repeticionesMin) { this.repeticionesMin = repeticionesMin; }

    public Integer getRepeticionesMax() { return repeticionesMax; }
    public void setRepeticionesMax(Integer repeticionesMax) { this.repeticionesMax = repeticionesMax; }

    public String getMedida() { return medida; }
    public void setMedida(String medida) { this.medida = medida; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getPorLado() { return porLado; }
    public void setPorLado(String porLado) { this.porLado = porLado; }

    /** Si la serie se mide en segundos (plancha…) y no en repeticiones (GP-125). */
    public boolean esPorTiempo() { return "SEGUNDOS".equals(medida); }

    /** Si es uno de los básicos de la rutina (se enseña como etiqueta). */
    public boolean esBasico() { return "BASICO".equals(tipo); }
}
