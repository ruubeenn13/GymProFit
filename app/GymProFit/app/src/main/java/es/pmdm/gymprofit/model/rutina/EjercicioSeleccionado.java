package es.pmdm.gymprofit.model.rutina;

import es.pmdm.gymprofit.model.ejercicio.Ejercicio;

// ============================================================
// EjercicioSeleccionado — asociación entre un Ejercicio y su configuración
// (series/repeticiones) dentro de la construcción de una rutina.
// Objeto inmutable usado en las pantallas de creación/edición de rutinas
// antes de persistir la rutina completa en la API.
// ============================================================
public class EjercicioSeleccionado {

    // Ejercicio seleccionado.
    private final Ejercicio ejercicio;
    // Número de series configuradas para el ejercicio.
    private final int series;
    // Número de repeticiones por serie.
    private final int repeticiones;

    // Crea la asociación ejercicio-series-repeticiones seleccionada por el usuario.
    public EjercicioSeleccionado(Ejercicio ejercicio, int series, int repeticiones) {
        this.ejercicio = ejercicio;
        this.series = series;
        this.repeticiones = repeticiones;
    }

    public Ejercicio getEjercicio() { return ejercicio; }
    public int getSeries() { return series; }
    public int getRepeticiones() { return repeticiones; }
}
