package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.EnumMap;
import java.util.Map;

// ============================================================
// EtiquetaAlimento — las cifras de un alimento nuevo, como en la etiqueta (decisión 17,
// tablero 13, GP-175, lote 1.6.2)
//
// Se escriben por 100 g o por la ración; la API guarda por 100 g y aquí se pasan. Las
// reglas (DEC-043):
//   · Solo la energía es obligatoria, y 0 vale. Un macro sin escribir es 0; la fibra sin
//     escribir no se sabe (null).
//   · Más de 900 kcal en 100 g no cabe —la grasa pura son 900—: casi seguro son kJ.
//   · Cada macro, de 0 a 100 g por 100 g, y grasas + hidratos + fibra + proteínas hasta
//     102 g: las etiquetas redondean cada cifra y la suma puede pasarse un poco.
//   · El aviso de las calorías compara la energía con 4·P + 4·C + 9·G (solo con los tres
//     escritos) y no impide guardar: la fibra y el alcohol cuentan aparte. Cuadra si se
//     separa como mucho 20 kcal o un 15 %, lo que sea mayor, siempre por 100 g (GP-178).
// Sin vistas, para probarlo.
// ============================================================
public final class EtiquetaAlimento {

    private EtiquetaAlimento() { }

    public static final int KCAL_MAX = 900;
    public static final double MACRO_MAX = 100;
    public static final double SUMA_MAX = 102;
    private static final int MARGEN_KCAL = 20;
    private static final double MARGEN_PCT = 0.15;

    /** Los campos, en el orden del envase. */
    public enum Campo { ENERGIA, GRASAS, HIDRATOS, FIBRA, PROTEINAS }

    /** Lo que puede estar mal en un campo. */
    public enum Error { FALTA, KJ, FUERA, SUMA }

    /** Lo escrito, en la base que se esté usando (100 g o la ración); null si está vacío. */
    public static final class Cifras {
        @Nullable final Double energia, grasas, hidratos, fibra, proteinas;

        public Cifras(@Nullable Double energia, @Nullable Double grasas, @Nullable Double hidratos,
                      @Nullable Double fibra, @Nullable Double proteinas) {
            this.energia = energia;
            this.grasas = grasas;
            this.hidratos = hidratos;
            this.fibra = fibra;
            this.proteinas = proteinas;
        }
    }

    /** Lo que se guarda, por 100 g, y los errores por campo (vacío si se puede guardar). */
    public static final class Resultado {
        public final Map<Campo, Error> errores = new EnumMap<>(Campo.class);
        public int calorias;
        public double grasas, carbohidratos, proteinas;
        @Nullable public Double fibra;
    }

    /**
     * Comprueba lo escrito y lo pasa a 100 g.
     *
     * @param gramosBase 100, o los gramos de la ración si las cifras son por ración.
     */
    @NonNull
    public static Resultado comprobar(@NonNull Cifras c, double gramosBase) {
        Resultado r = new Resultado();
        double f = 100 / gramosBase;
        if (c.energia == null) {
            r.errores.put(Campo.ENERGIA, Error.FALTA);
        } else {
            long kcal = Math.round(c.energia * f);
            if (c.energia < 0) r.errores.put(Campo.ENERGIA, Error.FUERA);
            else if (kcal > KCAL_MAX) r.errores.put(Campo.ENERGIA, Error.KJ);
            r.calorias = (int) Math.max(0, Math.min(Integer.MAX_VALUE, kcal));
        }
        r.grasas = macro(r, Campo.GRASAS, c.grasas, f);
        r.carbohidratos = macro(r, Campo.HIDRATOS, c.hidratos, f);
        r.proteinas = macro(r, Campo.PROTEINAS, c.proteinas, f);
        r.fibra = c.fibra == null ? null : macro(r, Campo.FIBRA, c.fibra, f);

        double suma = r.grasas + r.carbohidratos + r.proteinas + (r.fibra != null ? r.fibra : 0);
        boolean sinFuera = !r.errores.containsValue(Error.FUERA);
        if (sinFuera && suma > SUMA_MAX) {
            // El error va en el macro más grande, que es el que más probablemente esté mal.
            Campo mayor = Campo.GRASAS;
            double max = r.grasas;
            if (r.carbohidratos > max) { mayor = Campo.HIDRATOS; max = r.carbohidratos; }
            if (r.proteinas > max) { mayor = Campo.PROTEINAS; max = r.proteinas; }
            if (r.fibra != null && r.fibra > max) mayor = Campo.FIBRA;
            r.errores.put(mayor, Error.SUMA);
        }
        return r;
    }

    private static double macro(Resultado r, Campo campo, @Nullable Double valor, double f) {
        if (valor == null) return 0;
        double cien = Math.round(valor * f * 100) / 100.0;
        if (valor < 0 || cien > MACRO_MAX) r.errores.put(campo, Error.FUERA);
        return Math.max(0, cien);
    }

    /** Una cifra escrita por {@code de} gramos, expresada por {@code a} gramos. */
    public static double convertir(double valor, double de, double a) {
        return valor * a / de;
    }

    /** 4·P + 4·C + 9·G, o null si falta alguno de los tres. */
    @Nullable
    public static Integer kcalSegunMacros(@NonNull Cifras c) {
        if (c.proteinas == null || c.hidratos == null || c.grasas == null) return null;
        return (int) Math.round(4 * c.proteinas + 4 * c.hidratos + 9 * c.grasas);
    }

    /** Si la energía escrita cuadra con la de los macros. */
    public static boolean cuadra(int energia, int segunMacros) {
        return Math.abs(energia - segunMacros) <= Math.max(MARGEN_KCAL, MARGEN_PCT * segunMacros);
    }

    /**
     * Si cuadra lo escrito, comparado siempre por 100 g (GP-178): por una ración pequeña
     * las cifras son tan bajas que el margen mínimo de 20 kcal lo dejaba pasar casi todo.
     *
     * @param gramosBase 100, o los gramos de la ración si las cifras son por ración.
     * @return null si falta la energía o alguno de los tres macros.
     */
    @Nullable
    public static Boolean cuadra(@NonNull Cifras c, double gramosBase) {
        if (c.energia == null || c.proteinas == null || c.hidratos == null || c.grasas == null) return null;
        double f = 100 / gramosBase;
        double segun = 4 * c.proteinas + 4 * c.hidratos + 9 * c.grasas;
        return cuadra((int) Math.round(c.energia * f), (int) Math.round(segun * f));
    }

    /**
     * El nombre con que empieza un alimento creado desde «Créalo» (GP-181): lo que se
     * buscaba, sin espacios de más y con la primera letra en mayúscula, como los del
     * catálogo. Null si no se buscaba nada.
     */
    @Nullable
    public static String nombreDesdeBusqueda(@Nullable String buscado) {
        if (buscado == null) return null;
        String t = buscado.trim().replaceAll("\\s+", " ");
        if (t.isEmpty()) return null;
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    /** Lo que saldría, redondeado a 10 para el aviso («unas 430 kcal»). */
    public static int redondeoAviso(int kcal) {
        return (int) (Math.round(kcal / 10.0) * 10);
    }
}
