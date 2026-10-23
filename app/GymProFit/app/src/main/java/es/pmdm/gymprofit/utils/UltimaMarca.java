package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

import java.util.List;

// ============================================================
// UltimaMarca — «Última: 80 kg × 8 · récord» de cada ejercicio en el detalle de la rutina
// (GP-216, tablero 32): la mejor serie de la última vez que se hizo, y si fue el récord vigente.
// La API de «última vez» no dice si hubo récord: se deduce comparando con /records, y solo cuenta
// si es el mismo día y la misma marca (un récord posterior ya no es de esa vez). Sin dependencias
// de Android.
// ============================================================
public final class UltimaMarca {

    public final double peso;
    public final int repeticiones;

    public UltimaMarca(double peso, int repeticiones) {
        this.peso = peso;
        this.repeticiones = repeticiones;
    }

    /** La serie de más peso; a igualdad, la de más repeticiones. Null si no hay series. */
    @Nullable
    public static UltimaMarca mejorDe(@Nullable List<UltimasVeces.Serie> series) {
        if (series == null || series.isEmpty()) return null;
        UltimasVeces.Serie mejor = series.get(0);
        for (UltimasVeces.Serie s : series) {
            if (s.peso > mejor.peso || (s.peso == mejor.peso && s.repeticiones > mejor.repeticiones)) mejor = s;
        }
        return new UltimaMarca(mejor.peso, mejor.repeticiones);
    }

    /**
     * Si esa vez dio el récord vigente del ejercicio.
     *
     * @param fecha         inicio de esa vez, ISO.
     * @param fechaRecord   fecha del récord vigente, ISO.
     * @param pesoRecord    peso del récord, o null si no hay récord.
     * @param repsRecord    repeticiones del récord.
     */
    public boolean esRecord(@Nullable String fecha, @Nullable String fechaRecord,
                            @Nullable Double pesoRecord, int repsRecord) {
        if (pesoRecord == null || fecha == null || fechaRecord == null
                || fecha.length() < 10 || fechaRecord.length() < 10) return false;
        return fecha.substring(0, 10).equals(fechaRecord.substring(0, 10))
                && Math.abs(peso - pesoRecord) < 0.001 && repeticiones == repsRecord;
    }
}
