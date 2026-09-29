package es.pmdm.gymprofit.utils;

import android.content.Context;

import androidx.annotation.Nullable;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;

// ============================================================
// Pauta — cómo se escribe la pauta de un ejercicio de rutina (GP-074 / GP-125).
//
// Un solo sitio para «3 × 8–12», «3 × 10», «3 × 30–60 s» y su «por pierna», usado en
// el detalle de la rutina, el editor, el registro, los programas y «Tu programa». La
// parte que decide qué se escribe es pura (texto con los formatos que se le den) para
// probarla sin Android; los formatos están en strings.xml, en ES y EN, con espacio
// duro entre la cifra y la «s».
// ============================================================
public final class Pauta {

    private Pauta() { }

    /** Los formatos de strings.xml que usa la pauta. */
    public static final class Formatos {
        final String rango;      // «%1$d–%2$d»
        final String segundos;   // «%1$s s», con espacio duro
        final String series;     // «%1$d × %2$s»
        final String porPierna;  // «%1$s por pierna»
        final String porBrazo;
        final String porLado;

        public Formatos(String rango, String segundos, String series,
                        String porPierna, String porBrazo, String porLado) {
            this.rango = rango;
            this.segundos = segundos;
            this.series = series;
            this.porPierna = porPierna;
            this.porBrazo = porBrazo;
            this.porLado = porLado;
        }

        /** Los de la app, en su idioma. */
        public static Formatos de(Context ctx) {
            return new Formatos(ctx.getString(R.string.pauta_rango), ctx.getString(R.string.pauta_segundos),
                    ctx.getString(R.string.pauta_series), ctx.getString(R.string.pauta_por_pierna),
                    ctx.getString(R.string.pauta_por_brazo), ctx.getString(R.string.pauta_por_lado));
        }
    }

    /** La pauta de un ejercicio de rutina, en el idioma de la app. */
    public static String texto(Context ctx, RutinaEjercicio re) {
        return texto(Formatos.de(ctx), re.getSeries(), re.getRepeticionesMin(), re.getRepeticionesMax(),
                re.getRepeticiones(), re.getMedida(), re.getPorLado());
    }

    /**
     * «3 × 8–12», «3 × 10» sin rango, «3 × 30–60 s» por tiempo, más «por pierna»,
     * «por brazo» o «por lado».
     *
     * @param min      mínimo del rango, o null si no hay rango.
     * @param max      máximo del rango, o null.
     * @param reps     las repeticiones de siempre, si no hay rango.
     * @param medida   REPETICIONES, SEGUNDOS o null (repeticiones).
     * @param porLado  PIERNA, BRAZO, LADO o null.
     */
    public static String texto(Formatos f, int series, @Nullable Integer min, @Nullable Integer max, int reps,
                               @Nullable String medida, @Nullable String porLado) {
        String cantidad = cantidad(f, min, max, reps, medida);
        return lado(f, String.format(f.series, series, cantidad), porLado);
    }

    /**
     * Solo lo de cada serie, sin el número de series: «8–12», «10», «30–60 s». Es la
     * pista del campo al registrar.
     */
    public static String cantidad(Formatos f, @Nullable Integer min, @Nullable Integer max, int reps,
                                  @Nullable String medida) {
        String numero;
        if (min != null && max != null && !min.equals(max)) {
            numero = String.format(f.rango, min, max);
        } else {
            numero = String.valueOf(max != null ? max : reps);
        }
        return "SEGUNDOS".equals(medida) ? String.format(f.segundos, numero) : numero;
    }

    /**
     * El descanso como se dice: «Descanso 90 s», «Descanso 2 min». Minutos solo si son
     * minutos justos de 2 o más; 60 s se queda en segundos, que es como se cuenta.
     *
     * @return el texto, o null si no hay descanso.
     */
    @Nullable
    public static String descanso(Context ctx, @Nullable Integer segundos) {
        if (segundos == null || segundos <= 0) return null;
        return segundos >= 120 && segundos % 60 == 0
                ? ctx.getString(R.string.pauta_descanso_minutos, segundos / 60)
                : ctx.getString(R.string.pauta_descanso_segundos, segundos);
    }

    /** Añade «por pierna», «por brazo» o «por lado»; sin lado, el texto tal cual. */
    public static String lado(Formatos f, String texto, @Nullable String porLado) {
        if (porLado == null) return texto;
        switch (porLado) {
            case "PIERNA": return String.format(f.porPierna, texto);
            case "BRAZO":  return String.format(f.porBrazo, texto);
            case "LADO":   return String.format(f.porLado, texto);
            default:       return texto;
        }
    }
}
