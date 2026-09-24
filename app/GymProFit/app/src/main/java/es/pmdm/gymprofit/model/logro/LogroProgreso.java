package es.pmdm.gymprofit.model.logro;

// ============================================================
// LogroProgreso — un logro del catálogo visto por el usuario (GP-079).
// Refleja LogroProgresoDTO de GET /logros/progreso: si está conseguido y
// cuándo, y si no, la métrica, el umbral y lo que lleva. Sustituye al cruce
// de dos llamadas (catálogo + obtenidos) que hacían las pantallas.
// ============================================================
public class LogroProgreso {

    private int logroId;
    private String nombre;
    private String descripcion;
    // Código estable del logro (enum TipoLogro de la API); decide el icono.
    private String tipo;
    // SESIONES_COMPLETADAS, EJERCICIOS_REALIZADOS u OBJETIVOS_COMPLETADOS; decide la palabra.
    private String metrica;
    private int umbral;
    // Lo que lleva el usuario, ya recortado al umbral por la API.
    private int progreso;
    private boolean conseguido;
    // ISO-8601 sin zona; null si no está conseguido.
    private String fechaObtenido;

    public LogroProgreso() {}

    public int getLogroId() { return logroId; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getTipo() { return tipo; }
    public String getMetrica() { return metrica; }
    public int getUmbral() { return umbral; }
    public int getProgreso() { return progreso; }
    public boolean isConseguido() { return conseguido; }
    public String getFechaObtenido() { return fechaObtenido; }

    /**
     * Si tiene sentido enseñar una barra de progreso: solo con más de un paso.
     * En un logro de umbral 1, «0 de 1» no dice nada que la descripción no diga ya.
     */
    public boolean tienePasos() { return umbral > 1; }
}
