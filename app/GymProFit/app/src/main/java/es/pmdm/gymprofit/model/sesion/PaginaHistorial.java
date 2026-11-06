package es.pmdm.gymprofit.model.sesion;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

// ============================================================
// PaginaHistorial — una página de GET /sesiones/historial (GP-232).
// Hasta 8 semanas con sesiones, de la más nueva a la más vieja, y el cursor de la siguiente.
// Cada sesión trae lo que pinta su tarjeta: kilos, series, ejercicios, récords que batió ese
// día, los kilos de la anterior de la misma rutina, las series por músculo y sus barras. Una
// sola petición por página: ninguna tarjeta pide nada más.
// ============================================================
public class PaginaHistorial {

    private List<Semana> semanas;
    private String siguiente;

    @NonNull
    public List<Semana> getSemanas() { return semanas != null ? semanas : new ArrayList<>(); }

    /** El cursor de la página siguiente (se pasa como {@code antes}); null si no hay más. */
    @Nullable
    public String getSiguiente() { return siguiente; }

    /** Las sesiones de la página, de la más nueva a la más vieja. */
    @NonNull
    public List<Sesion> sesiones() {
        List<Sesion> todas = new ArrayList<>();
        for (Semana w : getSemanas()) todas.addAll(w.getSesiones());
        return todas;
    }

    /** Para los tests; la red lo rellena con Gson. */
    public static PaginaHistorial de(@NonNull List<Semana> semanas, @Nullable String siguiente) {
        PaginaHistorial p = new PaginaHistorial();
        p.semanas = semanas;
        p.siguiente = siguiente;
        return p;
    }

    /** Una semana, de lunes a domingo en la hora local. */
    public static class Semana {
        private String lunes;
        private int entrenos;
        private double kilos;
        private List<Sesion> sesiones;

        /** El lunes, «yyyy-MM-dd». */
        @Nullable
        public String getLunes() { return lunes; }
        public int getEntrenos() { return entrenos; }
        public double getKilos() { return kilos; }

        @NonNull
        public List<Sesion> getSesiones() { return sesiones != null ? sesiones : new ArrayList<>(); }

        /** Para los tests; la red lo rellena con Gson. */
        public static Semana de(@NonNull String lunes, @NonNull List<Sesion> sesiones) {
            Semana s = new Semana();
            s.lunes = lunes;
            s.sesiones = sesiones;
            s.entrenos = sesiones.size();
            for (Sesion x : sesiones) s.kilos += x.kilos;
            return s;
        }
    }

    /** Una sesión terminada con lo que pinta su tarjeta. */
    public static class Sesion {
        private int id;
        private Integer rutinaId;
        private String rutinaNombre;
        private String fechaInicio;
        private Integer duracionMinutos;
        private double kilos;
        private int series;
        private int ejercicios;
        private int records;
        private Double kilosAnterior;
        private List<SeriesMusculo> musculos;
        private List<VezRutina> barras;

        public int getId() { return id; }

        /** null en un entrenamiento libre. */
        @Nullable
        public Integer getRutinaId() { return rutinaId; }
        @Nullable
        public String getRutinaNombre() { return rutinaNombre; }
        public boolean esLibre() { return rutinaId == null; }
        @Nullable
        public String getFechaInicio() { return fechaInicio; }
        @Nullable
        public Integer getDuracionMinutos() { return duracionMinutos; }
        public double getKilos() { return kilos; }
        public int getSeries() { return series; }
        public int getEjercicios() { return ejercicios; }
        public int getRecords() { return records; }

        /** Los kilos de la anterior de la misma rutina; null la primera vez o sin rutina. */
        @Nullable
        public Double getKilosAnterior() { return kilosAnterior; }

        @NonNull
        public List<SeriesMusculo> getMusculos() { return musculos != null ? musculos : new ArrayList<>(); }

        /** Sus últimas 6 veces con la misma rutina, de la más vieja a ella; vacío si es libre. */
        @NonNull
        public List<VezRutina> getBarras() { return barras != null ? barras : new ArrayList<>(); }

        /** Para los tests; la red lo rellena con Gson. */
        public static Sesion de(int id, @Nullable Integer rutinaId, @Nullable String rutinaNombre,
                                @Nullable String fechaInicio, @Nullable Integer minutos, double kilos, int series,
                                int ejercicios, int records, @Nullable Double kilosAnterior,
                                @NonNull List<SeriesMusculo> musculos, @NonNull List<VezRutina> barras) {
            Sesion s = new Sesion();
            s.id = id;
            s.rutinaId = rutinaId;
            s.rutinaNombre = rutinaNombre;
            s.fechaInicio = fechaInicio;
            s.duracionMinutos = minutos;
            s.kilos = kilos;
            s.series = series;
            s.ejercicios = ejercicios;
            s.records = records;
            s.kilosAnterior = kilosAnterior;
            s.musculos = musculos;
            s.barras = barras;
            return s;
        }
    }
}
