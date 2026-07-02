package es.pmdm.gymprofit.model.usuario;

// ============================================================
// UsuarioEstadisticas — modelo de datos con las estadísticas de entrenamiento de un usuario
// Agrupa métricas agregadas calculadas por la API (sesiones totales y
// completadas, minutos entrenados, calorías, racha de días y ejercicio
// favorito) para mostrarlas en pantallas de progreso/perfil.
// ============================================================
public class UsuarioEstadisticas {

    private int totalSesiones;
    private int sesionesCompletadas;
    private int totalMinutosEntrenados;
    private int totalCaloriasQuemadas;
    private String ejercicioMasFrecuente;
    private int rachaActualDias;
    private int mejorRachaDias;

    // Constructor vacío requerido para deserialización JSON (Gson/Retrofit)
    public UsuarioEstadisticas() {}

    public int getTotalSesiones() { return totalSesiones; }
    public void setTotalSesiones(int v) { this.totalSesiones = v; }

    public int getSesionesCompletadas() { return sesionesCompletadas; }
    public void setSesionesCompletadas(int v) { this.sesionesCompletadas = v; }

    public int getTotalMinutosEntrenados() { return totalMinutosEntrenados; }
    public void setTotalMinutosEntrenados(int v) { this.totalMinutosEntrenados = v; }

    public int getTotalCaloriasQuemadas() { return totalCaloriasQuemadas; }
    public void setTotalCaloriasQuemadas(int v) { this.totalCaloriasQuemadas = v; }

    public String getEjercicioMasFrecuente() { return ejercicioMasFrecuente; }
    public void setEjercicioMasFrecuente(String v) { this.ejercicioMasFrecuente = v; }

    public int getRachaActualDias() { return rachaActualDias; }
    public void setRachaActualDias(int v) { this.rachaActualDias = v; }

    public int getMejorRachaDias() { return mejorRachaDias; }
    public void setMejorRachaDias(int v) { this.mejorRachaDias = v; }
}
