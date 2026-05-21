package es.pmdm.gymprofit.model.rutina;

import es.pmdm.gymprofit.model.ejercicio.Ejercicio;

public class EjercicioSeleccionado {

    private final Ejercicio ejercicio;
    private final int series;
    private final int repeticiones;

    public EjercicioSeleccionado(Ejercicio ejercicio, int series, int repeticiones) {
        this.ejercicio = ejercicio;
        this.series = series;
        this.repeticiones = repeticiones;
    }

    public Ejercicio getEjercicio() { return ejercicio; }
    public int getSeries() { return series; }
    public int getRepeticiones() { return repeticiones; }
}
