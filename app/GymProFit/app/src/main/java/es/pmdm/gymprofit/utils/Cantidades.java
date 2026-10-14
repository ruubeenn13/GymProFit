package es.pmdm.gymprofit.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.comida.AlimentoComida;

// ============================================================
// Cantidades — cómo se escribe una cantidad de comida (GP-172, lote 1.6.2)
//
// Un solo sitio para la ficha, la fila de una comida, la búsqueda y la hoja del escáner.
// Con la unidad que manda la API en singular y en plural: «1 rebanada (28 g)»,
// «2 rebanadas (56 g)», «1,5 rebanadas (42 g)». Si la ración no tiene unidad («Media
// taza») o la API es anterior y no la manda, como en la 1.6.1: «2 × media taza (120 g)».
// Por gramos, «150 g». El plural es un dato de la API, no una regla de la app: así
// «flan» → «flanes» y «flans» sin que la app sepa gramática (DEC-043).
// La parte que decide es pura, con los formatos que se le den, para probarla sin Android.
// La ración de un plato sin peso hecho (lote 1.6.6) pesa 100 g por dentro: se escribe sin
// gramos, «1 ración», «2 raciones».
// ============================================================
public final class Cantidades {

    private Cantidades() { }

    /** Los formatos de strings.xml que usa la cantidad. */
    public static final class Formatos {
        final String unaRacion;   // «%1$s (%2$s g)»: «Media taza (60 g)»
        final String raciones;    // «%1$s × %2$s (%3$s g)»: «2 × media taza (120 g)»
        final String conUnidad;   // «%1$s %2$s (%3$s g)»: «2 rebanadas (56 g)»
        final String gramos;      // «%1$s g»
        final String sinGramosUnidad;   // «%1$s %2$s»: «2 raciones»
        final String sinGramosRaciones; // «%1$s × %2$s»: «2 × ración»

        public Formatos(String unaRacion, String raciones, String conUnidad, String gramos) {
            this(unaRacion, raciones, conUnidad, gramos, "%1$s %2$s", "%1$s × %2$s");
        }

        public Formatos(String unaRacion, String raciones, String conUnidad, String gramos, String sinGramosUnidad,
                        String sinGramosRaciones) {
            this.unaRacion = unaRacion;
            this.raciones = raciones;
            this.conUnidad = conUnidad;
            this.gramos = gramos;
            this.sinGramosUnidad = sinGramosUnidad;
            this.sinGramosRaciones = sinGramosRaciones;
        }

        /** Los de la app, en su idioma. */
        public static Formatos de(Context ctx) {
            return new Formatos(ctx.getString(R.string.cantidad_una_racion), ctx.getString(R.string.cantidad_raciones),
                    ctx.getString(R.string.cantidad_con_unidad), ctx.getString(R.string.cantidad_gramos),
                    ctx.getString(R.string.cantidad_sin_gramos_unidad), ctx.getString(R.string.cantidad_sin_gramos_raciones));
        }
    }

    /** La cantidad de una línea de comida, en el idioma de la app. */
    @NonNull
    public static String de(Context ctx, AlimentoComida linea) {
        return texto(Formatos.de(ctx), FechaUtils.localeDeLaApp(ctx), linea.getRacionNombre(), linea.getRacionUnidad(),
                linea.getRacionUnidadPlural(), linea.getRaciones(), linea.getCantidadGramos(), linea.isRacionSinPeso());
    }

    /** Una cantidad cualquiera, en el idioma de la app. */
    @NonNull
    public static String de(Context ctx, @Nullable String racionNombre, @Nullable String unidad,
                            @Nullable String unidadPlural, @Nullable Double raciones, double gramos) {
        return de(ctx, racionNombre, unidad, unidadPlural, raciones, gramos, false);
    }

    /** Como la de arriba; {@code sinGramos} en la ración de un plato sin peso hecho. */
    @NonNull
    public static String de(Context ctx, @Nullable String racionNombre, @Nullable String unidad,
                            @Nullable String unidadPlural, @Nullable Double raciones, double gramos, boolean sinGramos) {
        return texto(Formatos.de(ctx), FechaUtils.localeDeLaApp(ctx), racionNombre, unidad, unidadPlural, raciones,
                gramos, sinGramos);
    }

    /** Con gramos: lo de siempre. */
    @NonNull
    public static String texto(Formatos f, Locale locale, @Nullable String racionNombre, @Nullable String unidad,
                               @Nullable String unidadPlural, @Nullable Double raciones, double gramos) {
        return texto(f, locale, racionNombre, unidad, unidadPlural, raciones, gramos, false);
    }

    /**
     * La cantidad escrita.
     *
     * @param racionNombre nombre de la ración («1 rebanada»), o null si va por gramos.
     * @param unidad       la unidad en singular («rebanada»), o null si no tiene o la API no la manda.
     * @param raciones     cuántas raciones, o null si va por gramos.
     * @param gramos       los gramos de la cantidad entera.
     * @param sinGramos    la ración de un plato sin peso hecho: sin gramos («2 raciones»).
     */
    @NonNull
    public static String texto(Formatos f, Locale locale, @Nullable String racionNombre, @Nullable String unidad,
                               @Nullable String unidadPlural, @Nullable Double raciones, double gramos, boolean sinGramos) {
        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        nf.setMaximumFractionDigits(1);
        String g = nf.format(Math.round(gramos));
        if (racionNombre == null || raciones == null || raciones <= 0) return String.format(locale, f.gramos, g);
        boolean una = raciones == 1;
        if (sinGramos) {
            if (unidad != null && unidadPlural != null) {
                return String.format(locale, f.sinGramosUnidad, nf.format(raciones), una ? unidad : unidadPlural);
            }
            if (una) return racionNombre;
            return String.format(locale, f.sinGramosRaciones, nf.format(raciones), CantidadFicha.nombreUnidad(racionNombre));
        }
        if (unidad != null && unidadPlural != null) {
            return String.format(locale, f.conUnidad, nf.format(raciones), una ? unidad : unidadPlural, g);
        }
        if (una) return String.format(locale, f.unaRacion, racionNombre, g);
        return String.format(locale, f.raciones, nf.format(raciones), CantidadFicha.nombreUnidad(racionNombre), g);
    }
}
