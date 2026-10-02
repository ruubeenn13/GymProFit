package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import es.pmdm.gymprofit.model.comida.AlimentoComida;

// ============================================================
// ResumenComida — las cifras del resumen de una comida (decisión 16, tablero 2, lote 1.6.2)
//
// Suma las líneas; reparte sus kcal entre los macros con el mismo cálculo que la ficha
// (EncajeDia.reparto: 4, 4 y 9 kcal por gramo, enteros que suman 100), y dice qué parte
// es de lo del día: cada macro frente a su objetivo y las kcal de la comida frente a las
// del día. Sin objetivo (perfil sin peso o altura), esas partes no existen (-1) y la
// pantalla deja solo el anillo y los gramos. Sin vistas, para probarlo.
// ============================================================
public final class ResumenComida {

    public final int kcal;
    public final double proteinas, carbohidratos, grasas;
    /** {proteína, carbohidratos, grasa} en % de las kcal, o null si no hay macros. */
    @Nullable public final int[] reparto;
    public final boolean sinObjetivo;
    /** Sin líneas: la pantalla enseña la comida vacía (tablero 2b). */
    public final boolean vacia;
    private final int[] objetivos;
    private final int objetivoKcal;

    private ResumenComida(int kcal, double p, double c, double g, boolean vacia, @Nullable ResultadoNutricional o) {
        this.kcal = kcal;
        this.proteinas = p;
        this.carbohidratos = c;
        this.grasas = g;
        this.vacia = vacia;
        this.reparto = EncajeDia.reparto(p, c, g);
        this.sinObjetivo = o == null || o.calorias <= 0;
        this.objetivoKcal = o == null ? 0 : o.calorias;
        this.objetivos = o == null ? new int[3] : new int[]{o.proteinas, o.carbohidratos, o.grasas};
    }

    /** El resumen de estas líneas, con el objetivo del día o sin él. */
    @NonNull
    public static ResumenComida de(@NonNull List<AlimentoComida> lineas, @Nullable ResultadoNutricional objetivo) {
        int kcal = 0;
        double p = 0, c = 0, g = 0;
        for (AlimentoComida a : lineas) {
            kcal += a.getCaloriasTotales();
            p += a.getProteinasTotales();
            c += a.getCarbohidratosTotales();
            g += a.getGrasasTotales();
        }
        return new ResumenComida(kcal, p, c, g, lineas.isEmpty(), objetivo);
    }

    /** Los gramos de un macro (0 proteína, 1 carbohidratos, 2 grasa). */
    public double gramos(int macro) {
        return macro == 0 ? proteinas : macro == 1 ? carbohidratos : grasas;
    }

    /** Qué % del objetivo del día es este macro; -1 sin objetivo. */
    public int pctMacro(int macro) {
        if (sinObjetivo || objetivos[macro] <= 0) return -1;
        return (int) Math.round(gramos(macro) * 100 / objetivos[macro]);
    }

    /** Qué % de las kcal del día es la comida; -1 sin objetivo. */
    public int pctDia() {
        if (sinObjetivo) return -1;
        return (int) Math.round(kcal * 100.0 / objetivoKcal);
    }

    /** Lo lleno de la barra del día (0..1). */
    public float barraDia() {
        if (sinObjetivo) return 0f;
        return Math.min(1f, Math.max(0f, kcal / (float) objetivoKcal));
    }

    public int objetivoKcal() {
        return objetivoKcal;
    }
}
