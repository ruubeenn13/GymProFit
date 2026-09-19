package es.pmdm.gymprofit.model.sesion;

import java.util.List;

// ============================================================
// SesionEntrenamiento — modelo de datos de una sesión de entrenamiento.
// Registra la ejecución concreta de una rutina por parte de un usuario
// (inicio, fin, duración, calorías quemadas) para el historial y las
// estadísticas de entrenamiento de GymProFit.
// ============================================================
public class SesionEntrenamiento {

    // Identificador único de la sesión.
    private int id;
    // Id del usuario que realizó la sesión.
    private int usuarioId;
    // Id de la rutina asociada, o null si la sesión es un ENTRENAMIENTO LIBRE.
    //
    // Es Integer y no int a propósito (GP-057): sesiones_entrenamiento.rutina_id es
    // INT NULL desde la migración inicial y la API lo documenta como opcional. Con un
    // int primitivo, Gson no tiene dónde escribir el null y deja el campo a 0, que no
    // es «sin rutina» sino «la rutina número cero»: la app se quedaba buscando en el
    // mapa de nombres una rutina que no existe y no podía distinguir los dos casos.
    //
    // Se llega aquí creando una sesión sin elegir rutina, y también cuando se borra la
    // cuenta del dueño de una rutina que alguien más estaba usando: esa sesión se
    // desvincula en vez de borrarse (DEC-031) y queda como entrenamiento libre.
    private Integer rutinaId;
    // Fecha/hora de inicio de la sesión.
    private String fechaInicio;
    // Fecha/hora de fin de la sesión.
    private String fechaFin;
    // Duración total de la sesión en minutos.
    private int duracionMinutos;
    // Calorías quemadas durante la sesión.
    private int caloriasQuemadas;
    // Notas u observaciones de la sesión.
    private String notas;
    // Indica si la sesión se completó.
    private boolean completada;
    // Logros nuevos desbloqueados al crear la sesión (solo lo emite el POST de
    // creación; en las lecturas viene null). La clave JSON coincide con el campo.
    private List<String> nuevosLogros;

    public SesionEntrenamiento() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    /** @return el id de la rutina, o {@code null} si fue un entrenamiento libre. */
    public Integer getRutinaId() { return rutinaId; }
    public void setRutinaId(Integer rutinaId) { this.rutinaId = rutinaId; }

    /** Atajo legible para las pantallas: la sesión no se hizo sobre ninguna rutina. */
    public boolean esEntrenamientoLibre() { return rutinaId == null; }

    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getFechaFin() { return fechaFin; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }

    public int getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(int duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public int getCaloriasQuemadas() { return caloriasQuemadas; }
    public void setCaloriasQuemadas(int caloriasQuemadas) { this.caloriasQuemadas = caloriasQuemadas; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public boolean isCompletada() { return completada; }
    public void setCompletada(boolean completada) { this.completada = completada; }

    public List<String> getNuevosLogros() { return nuevosLogros; }
    public void setNuevosLogros(List<String> nuevosLogros) { this.nuevosLogros = nuevosLogros; }
}
