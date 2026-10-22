package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.model.medicion.MedicionCorporal;

/**
 * El % de grasa corporal más reciente del usuario, para la proteína sobre masa magra
 * (GP-206, DEC-054).
 *
 * <p>Se elige entre las mediciones la más reciente que traiga un % (un NULL no es un 0 %: se
 * salta) y se guarda con su fecha en las preferencias, ligado a la cuenta, para las pantallas
 * que calculan sin red. Si vale o no (rango, antigüedad) lo decide
 * {@link CalculadoraNutricional}.
 */
public final class GrasaReciente {

    private static final long DIA_MS = 24L * 60 * 60 * 1000;

    /** Un % de grasa y el día en que se midió (medianoche local). */
    public static final class Medida {
        public final double pct;
        public final long fechaMs;

        public Medida(double pct, long fechaMs) {
            this.pct = pct;
            this.fechaMs = fechaMs;
        }

        /** Días enteros desde la medición hasta {@code ahoraMs}; negativo si es futura. */
        public int dias(long ahoraMs) {
            return (int) Math.floorDiv(ahoraMs - fechaMs, DIA_MS);
        }
    }

    private GrasaReciente() { }

    /**
     * La medición más reciente con % de grasa.
     *
     * @return null si ninguna lo trae, o si la que lo trae no tiene una fecha legible
     */
    @Nullable
    public static Medida elegir(@Nullable List<MedicionCorporal> mediciones) {
        if (mediciones == null) return null;
        MedicionCorporal mejor = null;
        for (MedicionCorporal m : mediciones) {
            if (m == null || m.getGrasaCorporal() == null || m.getFecha() == null) continue;
            // Las fechas ISO se ordenan como texto.
            if (mejor == null || m.getFecha().compareTo(mejor.getFecha()) > 0) mejor = m;
        }
        if (mejor == null) return null;
        long ms = dia(mejor.getFecha());
        return ms < 0 ? null : new Medida(mejor.getGrasaCorporal(), ms);
    }

    // Medianoche local del día de «yyyy-MM-dd…»; -1 si no se entiende.
    static long dia(@NonNull String iso) {
        if (iso.length() < 10) return -1;
        try {
            java.util.Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso.substring(0, 10));
            return d == null ? -1 : d.getTime();
        } catch (ParseException e) {
            return -1;
        }
    }
}
