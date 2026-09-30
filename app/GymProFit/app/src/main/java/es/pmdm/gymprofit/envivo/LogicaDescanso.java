package es.pmdm.gymprofit.envivo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;

// ============================================================
// LogicaDescanso — las reglas del descanso entre series (GP-013), sin Android.
//
// Empieza al marcar una serie, con el descanso de la pauta de su ejercicio; sin pauta
// (entrenamiento libre o ejercicio añadido), el de Ajustes. Tras la última serie que
// quede por hacer no hay: no queda nada que esperar. Marcar otra serie lo empieza de
// nuevo con el suyo; desmarcar la serie que lo empezó lo quita.
//
// Lo que se guarda es la hora de fin, no lo que queda: la cuenta atrás se calcula
// desde ahí, así que sigue donde tocaba aunque Android cierre la app. Al terminar se
// avisa solo si se ve terminar (dentro de un margen); si ya acabó hace rato —la app
// estaba cerrada y avisó la alarma, o volvió después—, se quita sin volver a sonar.
// ============================================================
public final class LogicaDescanso {

    private LogicaDescanso() { }

    /** Lo que suman o quitan «−15 s» y «+15 s». */
    public static final int PASO_SEGUNDOS = 15;
    /** Lo más que puede quedar: 10 minutos. */
    public static final int MAX_SEGUNDOS = 600;
    /** Opciones del descanso sin pauta en Ajustes. */
    public static final int[] SIN_PAUTA_OPCIONES = {60, 90, 120, 180};
    public static final int SIN_PAUTA_DEFECTO = 90;
    /**
     * Pasado este margen desde el fin, el descanso ya había acabado cuando se miró: se
     * quita sin sonar. Un tic por segundo lo ve siempre antes.
     */
    public static final long MARGEN_AVISO_MS = 3000;

    /** Segundos de descanso tras una serie de este ejercicio. */
    public static int segundosPara(@NonNull SesionEnCurso.Ejercicio e, int sinPauta) {
        if (e.deRutina && e.descanso != null && e.descanso > 0) return Math.min(e.descanso, MAX_SEGUNDOS);
        return Math.max(1, Math.min(sinPauta, MAX_SEGUNDOS));
    }

    /** Lo que viene después: la primera serie sin marcar de la sesión. */
    public static final class Siguiente {
        @NonNull public final SesionEnCurso.Ejercicio ejercicio;
        /** Número de la serie en su ejercicio, desde 1. */
        public final int numero;
        public final long serieId;

        Siguiente(@NonNull SesionEnCurso.Ejercicio ejercicio, int numero, long serieId) {
            this.ejercicio = ejercicio;
            this.numero = numero;
            this.serieId = serieId;
        }
    }

    /** La primera serie sin marcar, en el orden de la sesión, o null si no queda ninguna. */
    @Nullable
    public static Siguiente siguiente(@NonNull SesionEnCurso s) {
        for (SesionEnCurso.Ejercicio e : s.ejercicios) {
            for (int i = 0; i < e.series.size(); i++) {
                SesionEnCurso.Serie serie = e.series.get(i);
                if (!serie.hecha) return new Siguiente(e, i + 1, serie.id);
            }
        }
        return null;
    }

    /**
     * Tras marcar una serie: empieza el descanso con el de su ejercicio, o lo quita si no
     * queda ninguna serie por hacer.
     *
     * @return si queda un descanso en marcha.
     */
    public static boolean empezar(@NonNull SesionEnCurso s, long serieId, long ahoraMs, int sinPauta) {
        SesionEnCurso.Ejercicio e = SesionEnCursoRepositorio.ejercicioDeSerie(s, serieId);
        if (e == null || siguiente(s) == null) {
            s.descanso = null;
            return false;
        }
        long duracion = segundosPara(e, sinPauta) * 1000L;
        SesionEnCurso.Descanso d = new SesionEnCurso.Descanso();
        d.serieId = serieId;
        d.finMs = ahoraMs + duracion;
        d.duracionMs = duracion;
        s.descanso = d;
        return true;
    }

    /** Milisegundos que quedan (0 si ya acabó o no hay). */
    public static long restanteMs(@Nullable SesionEnCurso.Descanso d, long ahoraMs) {
        return d == null ? 0 : Math.max(0, d.finMs - ahoraMs);
    }

    /** Segundos que quedan, redondeando hacia arriba: «1:42» hasta que llega a 1:41. */
    public static int restanteSegundos(@Nullable SesionEnCurso.Descanso d, long ahoraMs) {
        long ms = restanteMs(d, ahoraMs);
        return (int) ((ms + 999) / 1000);
    }

    /** Lo que va de descanso, de 0 a 100, para la barra. */
    public static int progreso(@Nullable SesionEnCurso.Descanso d, long ahoraMs) {
        if (d == null || d.duracionMs <= 0) return 0;
        long hecho = d.duracionMs - restanteMs(d, ahoraMs);
        return (int) Math.max(0, Math.min(100, 100 * hecho / d.duracionMs));
    }

    /**
     * «−15 s» y «+15 s»: mueve el fin sin que queden menos de 0 ni más de 10 minutos. La
     * barra conserva lo que ya va: el total pasa a ser lo hecho más lo que queda.
     *
     * @return si había un descanso que ajustar.
     */
    public static boolean ajustar(@NonNull SesionEnCurso s, int segundos, long ahoraMs) {
        SesionEnCurso.Descanso d = s.descanso;
        if (d == null) return false;
        long queda = restanteMs(d, ahoraMs);
        long hecho = Math.max(0, d.duracionMs - queda);
        long nuevo = Math.max(0, Math.min(MAX_SEGUNDOS * 1000L, queda + segundos * 1000L));
        d.finMs = ahoraMs + nuevo;
        d.duracionMs = Math.max(1, hecho + nuevo);
        return true;
    }

    /** Si «+15 s» aún puede sumar algo. */
    public static boolean puedeSumar(@Nullable SesionEnCurso.Descanso d, long ahoraMs) {
        return d != null && restanteMs(d, ahoraMs) < MAX_SEGUNDOS * 1000L;
    }

    /** Qué pasa con el descanso al mirarlo. */
    public enum Estado {
        /** No hay descanso. */
        NINGUNO,
        /** Sigue contando. */
        EN_MARCHA,
        /** Acaba de terminar: hay que avisar. */
        TERMINA,
        /** Terminó hace rato (sin la app delante): se quita sin sonar. */
        TERMINO_ANTES
    }

    @NonNull
    public static Estado estado(@Nullable SesionEnCurso.Descanso d, long ahoraMs) {
        if (d == null) return Estado.NINGUNO;
        if (d.finMs > ahoraMs) return Estado.EN_MARCHA;
        return ahoraMs - d.finMs <= MARGEN_AVISO_MS ? Estado.TERMINA : Estado.TERMINO_ANTES;
    }

    /** Modo de timbre del móvil. */
    public enum Timbre { NORMAL, VIBRACION, SILENCIO }

    /** Cómo avisar con la app delante. */
    public static final class Aviso {
        public final boolean vibrar;
        public final boolean sonar;

        Aviso(boolean vibrar, boolean sonar) {
            this.vibrar = vibrar;
            this.sonar = sonar;
        }
    }

    /**
     * Con la app delante no hay notificación, así que se respeta a mano lo que haría el
     * sistema: en normal, suena y vibra; en vibración, solo vibra; en silencio, nada; y
     * con No molestar, nada (el aviso se ve en la pantalla igual).
     */
    @NonNull
    public static Aviso aviso(@NonNull Timbre timbre, boolean noMolestar) {
        if (noMolestar) return new Aviso(false, false);
        switch (timbre) {
            case NORMAL: return new Aviso(true, true);
            case VIBRACION: return new Aviso(true, false);
            default: return new Aviso(false, false);
        }
    }
}
