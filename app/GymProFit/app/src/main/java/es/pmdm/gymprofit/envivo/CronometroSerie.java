package es.pmdm.gymprofit.envivo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;

// ============================================================
// CronometroSerie — el cronómetro de una serie por tiempo (GP-012), sin Android.
//
// Cuenta hacia arriba desde el momento en que se tocó, guardado en la sesión: si
// Android cierra la app con él en marcha, al volver sigue contando desde entonces, no
// desde cero. Al llegar al mínimo de la pauta avisa con una vibración corta; al
// máximo, con una doble. Cada aviso se da una vez y queda apuntado en la sesión, para
// que volver a la pantalla no lo repita. A la hora se para solo: una serie de una hora
// es un cronómetro olvidado, no una plancha.
// ============================================================
public final class CronometroSerie {

    private CronometroSerie() { }

    /** Lo que toca hacer en este instante. */
    public enum Aviso { NINGUNO, VIBRAR_CORTA, VIBRAR_DOBLE, PARAR }

    /** Segundos transcurridos. */
    public static int segundos(@NonNull SesionEnCurso.Cronometro c, long ahoraMs) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, ahoraMs - c.inicioMs) / 1000L);
    }

    /**
     * Mira el cronómetro y apunta el aviso que toque. Con mínimo y máximo iguales (una
     * pauta sin rango) solo hay un aviso, el doble: es el final.
     *
     * @param minimo mínimo de la pauta en segundos, o null sin pauta.
     * @param maximo máximo de la pauta en segundos, o null.
     */
    @NonNull
    public static Aviso comprobar(@NonNull SesionEnCurso.Cronometro c, long ahoraMs,
                                  @Nullable Integer minimo, @Nullable Integer maximo) {
        int s = segundos(c, ahoraMs);
        if (s >= LogicaSesion.MAX_SEGUNDOS) return Aviso.PARAR;
        if (maximo != null && s >= maximo && !c.avisoMaximo) {
            c.avisoMaximo = true;
            c.avisoMinimo = true;
            return Aviso.VIBRAR_DOBLE;
        }
        boolean hayRango = minimo != null && (maximo == null || minimo < maximo);
        if (hayRango && s >= minimo && !c.avisoMinimo) {
            c.avisoMinimo = true;
            return Aviso.VIBRAR_CORTA;
        }
        return Aviso.NINGUNO;
    }
}
