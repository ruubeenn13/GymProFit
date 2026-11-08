package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import es.pmdm.gymprofit.model.sesion.VolumenSemanal;

// ============================================================
// MedidaSemanal — qué cuentan las barras por semana de Progreso e Inicio (GP-249): los kilos
// o, para quien entrena con su peso, las series.
//
// Con kilos en alguna semana del período, kilos, como siempre: una semana a peso corporal en
// medio sale con su barra a cero. Sin kilos en ninguna, el bloque pasa a «Series por semana»,
// con la misma cifra, el mismo avance y las mismas barras. Sin kilos ni series, ninguna de las
// dos: Progreso dice que no hay series en esas semanas e Inicio esconde el bloque, en vez de
// enseñar «0 kg» (DEC-060). Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class MedidaSemanal {

    /** Lo que se enseña. */
    public enum Medida { KILOS, SERIES, NINGUNA }

    private MedidaSemanal() { }

    /** La medida del período: kilos si alguna semana los tiene; si no, series si las hay. */
    @NonNull
    public static Medida de(@Nullable List<VolumenSemanal> semanas) {
        if (semanas == null) return Medida.NINGUNA;
        boolean series = false;
        for (VolumenSemanal s : semanas) {
            if (s.getVolumenKg() > 0) return Medida.KILOS;
            if (s.getSeries() > 0) series = true;
        }
        return series ? Medida.SERIES : Medida.NINGUNA;
    }

    /** Lo de cada semana en esa medida, de la más antigua a la más reciente (ceros con NINGUNA). */
    @NonNull
    public static double[] valores(@NonNull Medida medida, @Nullable List<VolumenSemanal> semanas) {
        if (semanas == null) return new double[0];
        double[] v = new double[semanas.size()];
        for (int i = 0; i < v.length; i++) {
            VolumenSemanal s = semanas.get(i);
            v[i] = medida == Medida.KILOS ? s.getVolumenKg() : medida == Medida.SERIES ? s.getSeries() : 0;
        }
        return v;
    }
}
