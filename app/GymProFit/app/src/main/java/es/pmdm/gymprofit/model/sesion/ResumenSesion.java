package es.pmdm.gymprofit.model.sesion;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

// ============================================================
// ResumenSesion — el resumen del entreno (GP-210 paso 1, GET /sesiones/{id}/resumen)
// Los kilos y las series de la sesión, por ejercicio, y con qué compararlos: lo que movió
// el ejercicio la última vez que se hizo antes y la sesión anterior de la misma rutina.
// Salen del servidor con la definición de volumen de GP-011, también para sesiones del
// historial. Las comparaciones son null la primera vez; los kilos, siempre positivos o 0.
// ============================================================
public class ResumenSesion {

    private int sesionId;
    private double volumenKg;
    private int series;
    private SesionAnterior anterior;
    private List<Ejercicio> ejercicios;

    public int getSesionId() { return sesionId; }
    public double getVolumenKg() { return volumenKg; }
    public int getSeries() { return series; }

    /** La sesión anterior de la misma rutina; null en un entrenamiento libre o la primera vez. */
    @Nullable
    public SesionAnterior getAnterior() { return anterior; }

    @NonNull
    public List<Ejercicio> getEjercicios() { return ejercicios != null ? ejercicios : new ArrayList<>(); }

    /** Para los tests y para quien lo construya a mano; la red lo rellena con Gson. */
    public static ResumenSesion de(int sesionId, double volumenKg, int series,
                                   @Nullable SesionAnterior anterior, @NonNull List<Ejercicio> ejercicios) {
        ResumenSesion r = new ResumenSesion();
        r.sesionId = sesionId;
        r.volumenKg = volumenKg;
        r.series = series;
        r.anterior = anterior;
        r.ejercicios = ejercicios;
        return r;
    }

    /** La sesión terminada inmediatamente anterior de la misma rutina. */
    public static class SesionAnterior {
        private int sesionId;
        private String fecha;
        private double volumenKg;

        public int getSesionId() { return sesionId; }
        /** Inicio de esa sesión, «yyyy-MM-ddTHH:mm:ss». */
        public String getFecha() { return fecha; }
        public double getVolumenKg() { return volumenKg; }

        public static SesionAnterior de(int sesionId, String fecha, double volumenKg) {
            SesionAnterior s = new SesionAnterior();
            s.sesionId = sesionId;
            s.fecha = fecha;
            s.volumenKg = volumenKg;
            return s;
        }
    }

    /** Un ejercicio de la sesión, con sus kilos y series. */
    public static class Ejercicio {
        private int ejercicioId;
        private String nombre;
        private double volumenKg;
        private int series;
        private boolean porTiempo;
        private UltimaMarca anterior;

        public int getEjercicioId() { return ejercicioId; }
        public String getNombre() { return nombre; }
        public double getVolumenKg() { return volumenKg; }
        public int getSeries() { return series; }
        /** Todas sus series son por tiempo: no mueve kilos. */
        public boolean isPorTiempo() { return porTiempo; }

        /** Lo que movió la última vez que lo hizo antes de esta sesión; null si es la primera. */
        @Nullable
        public UltimaMarca getAnterior() { return anterior; }

        public static Ejercicio de(int ejercicioId, String nombre, double volumenKg, int series,
                                   boolean porTiempo, @Nullable UltimaMarca anterior) {
            Ejercicio e = new Ejercicio();
            e.ejercicioId = ejercicioId;
            e.nombre = nombre;
            e.volumenKg = volumenKg;
            e.series = series;
            e.porTiempo = porTiempo;
            e.anterior = anterior;
            return e;
        }
    }

    /** Los kilos de un ejercicio la última vez que se hizo antes de la sesión resumida. */
    public static class UltimaMarca {
        private String fecha;
        private double volumenKg;

        public String getFecha() { return fecha; }
        public double getVolumenKg() { return volumenKg; }

        public static UltimaMarca de(String fecha, double volumenKg) {
            UltimaMarca m = new UltimaMarca();
            m.fecha = fecha;
            m.volumenKg = volumenKg;
            return m;
        }
    }
}
