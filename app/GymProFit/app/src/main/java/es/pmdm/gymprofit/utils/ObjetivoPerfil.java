package es.pmdm.gymprofit.utils;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import es.pmdm.gymprofit.R;

// ============================================================
// ObjetivoPerfil — la tarjeta del objetivo del Perfil (GP-242): su icono, el mismo de la
// rejilla de Tus datos, y la frase de lo que supone, por objetivo de CalculadoraNutricional.
//
// Con 17 años o menos «Perder grasa» se queda en mantenimiento (GP-103, DEC-054): la frase
// de comer menos de lo que gastas sería mentira, y en su sitio va la nota de menores del alta.
// Lógica pura: se prueba con ObjetivoPerfilTest.
// ============================================================
public final class ObjetivoPerfil {

    private ObjetivoPerfil() { }

    /** El icono del objetivo, el de su casilla en Tus datos; el de mantener si no se conoce. */
    @DrawableRes
    public static int icono(@Nullable String objetivo) {
        if (objetivo == null) return R.drawable.ic_ms_balance;
        switch (objetivo) {
            case CalculadoraNutricional.OBJETIVO_PERDER_PESO: return R.drawable.ic_ms_local_fire_department;
            case CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR: return R.drawable.ic_ms_fitness_center;
            case CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA: return R.drawable.ic_ms_bolt;
            default: return R.drawable.ic_ms_balance;
        }
    }

    /**
     * Lo que supone el objetivo, en una frase.
     *
     * @param objetivo  el objetivo guardado.
     * @param porEdad   si el plan se quedó en mantenimiento por la edad
     *                  ({@link ResultadoNutricional#mantenimientoPorEdad}).
     * @return la frase, la nota de menores o 0 si el objetivo no se conoce.
     */
    @StringRes
    public static int frase(@Nullable String objetivo, boolean porEdad) {
        if (objetivo == null) return 0;
        switch (objetivo) {
            case CalculadoraNutricional.OBJETIVO_PERDER_PESO:
                return porEdad ? R.string.alta_plan_nota_menores : R.string.perfil_objetivo_frase_perder;
            case CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR: return R.string.perfil_objetivo_frase_ganar;
            case CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA: return R.string.perfil_objetivo_frase_fuerza;
            case CalculadoraNutricional.OBJETIVO_MANTENER_PESO: return R.string.perfil_objetivo_frase_mantener;
            default: return 0;
        }
    }

    /**
     * Los pesos de las columnas de los gramos bajo la barra: los del reparto, pero ninguna por debajo
     * de {@code minimo} por ciento del ancho (a letra 1,3, «de proteína» no cabe en menos). Lo que se
     * da a las estrechas sale de las anchas, en proporción. Suman 100.
     */
    public static int[] columnas(int[] reparto, int minimo) {
        int n = reparto.length;
        double[] w = new double[n];
        boolean[] fija = new boolean[n];
        for (int i = 0; i < n; i++) w[i] = Math.max(0, reparto[i]);
        double total = 0;
        for (double x : w) total += x;
        if (total <= 0) {
            int[] iguales = new int[n];
            for (int i = 0; i < n; i++) iguales[i] = 100 / n + (i < 100 % n ? 1 : 0);
            return iguales;
        }
        for (int i = 0; i < n; i++) w[i] = w[i] * 100 / total;
        boolean cambia = true;
        while (cambia) {
            cambia = false;
            double libre = 100, resto = 0;
            for (int i = 0; i < n; i++) {
                if (!fija[i] && w[i] < minimo) {
                    fija[i] = true;
                    cambia = true;
                }
            }
            for (int i = 0; i < n; i++) if (fija[i]) libre -= minimo; else resto += w[i];
            for (int i = 0; i < n; i++) w[i] = fija[i] ? minimo : (resto > 0 ? w[i] * libre / resto : 0);
        }
        int[] r = new int[n];
        int suma = 0, mayor = 0;
        for (int i = 0; i < n; i++) {
            r[i] = (int) Math.round(w[i]);
            suma += r[i];
            if (r[i] > r[mayor]) mayor = i;
        }
        r[mayor] += 100 - suma;
        return r;
    }

    /**
     * El reparto de las calorías entre los macros, en porcentajes enteros que suman 100: la
     * proteína y los hidratos a 4 kcal por gramo y la grasa a 9.
     *
     * @return {proteína, hidratos, grasa}; ceros si no hay nada que repartir.
     */
    public static int[] reparto(int proteinas, int hidratos, int grasas) {
        double p = Math.max(0, proteinas) * 4.0, h = Math.max(0, hidratos) * 4.0, g = Math.max(0, grasas) * 9.0;
        double total = p + h + g;
        if (total <= 0) return new int[]{0, 0, 0};
        int pp = (int) Math.round(p * 100 / total);
        int ph = (int) Math.round(h * 100 / total);
        return new int[]{pp, ph, 100 - pp - ph};
    }
}
