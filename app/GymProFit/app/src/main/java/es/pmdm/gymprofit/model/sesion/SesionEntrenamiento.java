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
    // Id de la rutina asociada a la sesión.
    private int rutinaId;
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

    public int getRutinaId() { return rutinaId; }
    public void setRutinaId(int rutinaId) { this.rutinaId = rutinaId; }

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
