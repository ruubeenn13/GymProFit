package es.pmdm.gymprofit.model.admin;

// ============================================================
// EstadisticasGlobales — POJO de las métricas globales del panel de administración.
// Mapea la respuesta de GET admin/estadisticas-globales (AdminEstadisticasDTO en la
// API) con los contadores agregados de la plataforma (usuarios, sesiones, rutinas,
// ejercicios, objetivos y logros). Se deserializa vía Gson en AdminActivity.
// Los nombres de los campos coinciden con las claves JSON de la API (sin @SerializedName).
// ============================================================
public class EstadisticasGlobales {

    // Total de usuarios registrados.
    private long totalUsuarios;
    // Usuarios actualmente activos.
    private long usuariosActivos;
    // Total de sesiones de entrenamiento registradas.
    private long totalSesiones;
    // Sesiones de entrenamiento realizadas hoy.
    private long sesionesHoy;
    // Total de ejercicios realizados por todos los usuarios.
    private long totalEjerciciosRealizados;
    // Total de objetivos personales completados.
    private long totalObjetivosCompletados;
    // Total de logros otorgados a usuarios.
    private long totalLogrosOtorgados;
    // Número de rutinas predefinidas disponibles.
    private long rutinasPredefinidas;
    // Número de ejercicios activos en el catálogo.
    private long ejerciciosActivos;

    public EstadisticasGlobales() {}

    public long getTotalUsuarios() { return totalUsuarios; }
    public void setTotalUsuarios(long totalUsuarios) { this.totalUsuarios = totalUsuarios; }

    public long getUsuariosActivos() { return usuariosActivos; }
    public void setUsuariosActivos(long usuariosActivos) { this.usuariosActivos = usuariosActivos; }

    public long getTotalSesiones() { return totalSesiones; }
    public void setTotalSesiones(long totalSesiones) { this.totalSesiones = totalSesiones; }

    public long getSesionesHoy() { return sesionesHoy; }
    public void setSesionesHoy(long sesionesHoy) { this.sesionesHoy = sesionesHoy; }

    public long getTotalEjerciciosRealizados() { return totalEjerciciosRealizados; }
    public void setTotalEjerciciosRealizados(long totalEjerciciosRealizados) { this.totalEjerciciosRealizados = totalEjerciciosRealizados; }

    public long getTotalObjetivosCompletados() { return totalObjetivosCompletados; }
    public void setTotalObjetivosCompletados(long totalObjetivosCompletados) { this.totalObjetivosCompletados = totalObjetivosCompletados; }

    public long getTotalLogrosOtorgados() { return totalLogrosOtorgados; }
    public void setTotalLogrosOtorgados(long totalLogrosOtorgados) { this.totalLogrosOtorgados = totalLogrosOtorgados; }

    public long getRutinasPredefinidas() { return rutinasPredefinidas; }
    public void setRutinasPredefinidas(long rutinasPredefinidas) { this.rutinasPredefinidas = rutinasPredefinidas; }

    public long getEjerciciosActivos() { return ejerciciosActivos; }
    public void setEjerciciosActivos(long ejerciciosActivos) { this.ejerciciosActivos = ejerciciosActivos; }
}
