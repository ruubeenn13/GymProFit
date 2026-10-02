package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

// ============================================================
// EncajeDia — «Cómo encaja en tu día» de la ficha del alimento (lote 1.6.1)
//
// De lo que llevas hoy a lo que llevarías con esta cantidad, en kcal y en proteína, y
// lo que te quedará. Si te pasas, lo dice con la cifra, sin rojo (el lienzo: pasarse no
// es un error, y la vista no lo pinta como tal). Sin objetivo en el perfil, no hay
// encaje y la ficha no lo enseña. Al editar una línea, lo que ya contaba esa línea se
// resta antes de sumar la cantidad nueva.
// También el reparto de las kcal del alimento entre los tres macros, que suma 100.
// ============================================================
public final class EncajeDia {

    public final int antesKcal, despuesKcal, objetivoKcal, quedanKcal, pasaKcalPor;
    public final double antesProteina, despuesProteina, quedanProteina;
    public final int objetivoProteina;
    public final boolean sePasaKcal, sePasaProteina;
    /** Barras: lo que ya llevas y lo que suma esto, en % del objetivo; juntos, hasta 100. */
    public final double antesKcalPct, sumaKcalPct, antesProteinaPct, sumaProteinaPct;

    private EncajeDia(int antesKcal, double antesProteina, int objetivoKcal, int objetivoProteina,
                      double kcal, double proteina) {
        this.antesKcal = antesKcal;
        this.antesProteina = antesProteina;
        this.objetivoKcal = objetivoKcal;
        this.objetivoProteina = objetivoProteina;
        this.despuesKcal = (int) Math.round(antesKcal + kcal);
        this.despuesProteina = antesProteina + proteina;
        this.sePasaKcal = despuesKcal > objetivoKcal;
        this.sePasaProteina = despuesProteina > objetivoProteina;
        this.quedanKcal = Math.max(0, objetivoKcal - despuesKcal);
        this.pasaKcalPor = Math.max(0, despuesKcal - objetivoKcal);
        this.quedanProteina = Math.max(0, objetivoProteina - despuesProteina);
        this.antesKcalPct = pct(antesKcal, objetivoKcal);
        this.sumaKcalPct = Math.min(100 - antesKcalPct, pct(kcal, objetivoKcal));
        this.antesProteinaPct = pct(antesProteina, objetivoProteina);
        this.sumaProteinaPct = Math.min(100 - antesProteinaPct, pct(proteina, objetivoProteina));
    }

    private static double pct(double valor, int objetivo) {
        return Math.min(100, Math.max(0, valor * 100.0 / objetivo));
    }

    /**
     * @param llevaKcal         kcal del día, como las cuenta el diario.
     * @param llevaProteina     proteína del día, en g.
     * @param objetivoKcal      objetivo del día; 0 si no hay.
     * @param objetivoProteina  objetivo de proteína; 0 si no hay.
     * @param kcal              kcal de la cantidad elegida.
     * @param proteina          proteína de la cantidad elegida.
     * @param kcalLinea         al editar, las kcal que ya contaba la línea; si no, 0.
     * @param proteinaLinea     al editar, su proteína; si no, 0.
     * @return el encaje, o null si no hay objetivo.
     */
    @Nullable
    public static EncajeDia de(int llevaKcal, double llevaProteina, int objetivoKcal, int objetivoProteina,
                               double kcal, double proteina, double kcalLinea, double proteinaLinea) {
        if (objetivoKcal <= 0 || objetivoProteina <= 0) return null;
        int antes = (int) Math.max(0, Math.round(llevaKcal - kcalLinea));
        double antesP = Math.max(0, llevaProteina - proteinaLinea);
        return new EncajeDia(antes, antesP, objetivoKcal, objetivoProteina, kcal, proteina);
    }

    /**
     * El reparto de las kcal entre proteína, carbohidratos y grasa (4, 4 y 9 kcal por g),
     * en enteros que suman 100 (resto mayor).
     *
     * @return {proteína, carbohidratos, grasa}, o null si no tiene macros.
     */
    @Nullable
    public static int[] reparto(double proteinas, double carbohidratos, double grasas) {
        double[] kcal = {Math.max(0, proteinas) * 4, Math.max(0, carbohidratos) * 4, Math.max(0, grasas) * 9};
        double total = kcal[0] + kcal[1] + kcal[2];
        if (total <= 0) return null;
        int[] r = new int[3];
        double[] resto = new double[3];
        int suma = 0;
        for (int i = 0; i < 3; i++) {
            double exacto = kcal[i] * 100 / total;
            r[i] = (int) Math.floor(exacto);
            resto[i] = exacto - r[i];
            suma += r[i];
        }
        while (suma < 100) {
            int mayor = 0;
            for (int i = 1; i < 3; i++) if (resto[i] > resto[mayor]) mayor = i;
            r[mayor]++;
            resto[mayor] = -1;
            suma++;
        }
        return r;
    }
}
