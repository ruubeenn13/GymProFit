package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import es.pmdm.gymprofit.model.alimento.Alimento;

// ============================================================
// CuentaPlato — lo que vale un plato, con la misma cuenta que la API (lote 1.6.6, DEC-047)
//
// Lo que dice «Por ración» en la pantalla del plato es lo que se apuntará, así que no
// puede ser una cuenta parecida: es la de la API (Platos y ValoresLinea), paso a paso, y
// las dos se prueban con la misma tabla (cuenta_plato_casos.csv, en los tests de la API).
//   · Cada ingrediente vale lo que valdría apuntado: kcal = redondeo de kcal × g / 100;
//     cada macro, valor × g / 100 a dos decimales, con la mitad hacia arriba.
//   · Sin peso hecho, el plato guarda una ración: el total entre las raciones (kcal
//     enteras, macros a dos decimales). «1 ración» son 100 g por dentro: lo guardado.
//   · Con peso hecho, guarda 100 g: el total por 100 entre el peso. «1 ración» pesa el
//     peso entre las raciones, a la décima, y vale como cualquier alimento con esos gramos.
// Sin vistas, para probarlo.
// ============================================================
public final class CuentaPlato {

    /** Como al crear un alimento: más es más que la grasa pura, y casi siempre son kJ. */
    public static final int KCAL_MAXIMAS_POR_100 = 900;
    /** Lo que pesa por dentro la ración de un plato sin peso hecho. */
    public static final double GRAMOS_SIN_PESO = 100;
    public static final int RACIONES_MIN = 1;
    public static final int RACIONES_MAX = 99;
    public static final int INGREDIENTES_MAX = 50;

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private CuentaPlato() {
    }

    /** Las kcal (enteras) y los macros (a dos decimales) de una cantidad. */
    public static final class Cifras {
        public final int kcal;
        @NonNull public final BigDecimal proteinas;
        @NonNull public final BigDecimal carbohidratos;
        @NonNull public final BigDecimal grasas;

        public Cifras(int kcal, @NonNull BigDecimal proteinas, @NonNull BigDecimal carbohidratos,
                      @NonNull BigDecimal grasas) {
            this.kcal = kcal;
            this.proteinas = proteinas;
            this.carbohidratos = carbohidratos;
            this.grasas = grasas;
        }

        public static final Cifras CERO = new Cifras(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        @NonNull
        public Cifras mas(@NonNull Cifras otra) {
            return new Cifras(kcal + otra.kcal, proteinas.add(otra.proteinas), carbohidratos.add(otra.carbohidratos),
                    grasas.add(otra.grasas));
        }
    }

    /** Lo que vale {@code gramos} de un alimento (o de un plato, con sus valores guardados). */
    @NonNull
    public static Cifras linea(@NonNull Alimento a, double gramos) {
        BigDecimal g = BigDecimal.valueOf(gramos);
        return new Cifras((int) Math.round(a.getCalorias() * gramos / 100.0), parte(a.getProteinas(), g),
                parte(a.getCarbohidratos(), g), parte(a.getGrasas(), g));
    }

    private static BigDecimal parte(double porCien, BigDecimal gramos) {
        return BigDecimal.valueOf(porCien).multiply(gramos).divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    /** La suma de los ingredientes, cada uno como se apuntaría. */
    @NonNull
    public static Cifras total(@NonNull List<Alimento> alimentos, @NonNull List<Double> gramos) {
        Cifras t = Cifras.CERO;
        for (int i = 0; i < alimentos.size(); i++) t = t.mas(linea(alimentos.get(i), gramos.get(i)));
        return t;
    }

    /** Lo que guarda el plato: por ración sin peso hecho; por 100 g con él. */
    @NonNull
    public static Cifras guardado(@NonNull Cifras total, int raciones, @Nullable Double peso) {
        BigDecimal entre = peso == null ? BigDecimal.valueOf(raciones) : BigDecimal.valueOf(peso);
        BigDecimal por = peso == null ? BigDecimal.ONE : CIEN;
        int kcal = BigDecimal.valueOf(total.kcal).multiply(por).divide(entre, 0, RoundingMode.HALF_UP).intValue();
        return new Cifras(kcal, reparte(total.proteinas, por, entre), reparte(total.carbohidratos, por, entre),
                reparte(total.grasas, por, entre));
    }

    private static BigDecimal reparte(BigDecimal valor, BigDecimal por, BigDecimal entre) {
        return valor.multiply(por).divide(entre, 2, RoundingMode.HALF_UP);
    }

    /** Lo que pesa una ración con peso hecho: el peso entre las raciones, a la décima. */
    public static double gramosRacion(double peso, int raciones) {
        return BigDecimal.valueOf(peso).divide(BigDecimal.valueOf(raciones), 1, RoundingMode.HALF_UP).doubleValue();
    }

    /** Lo que se apunta con «1 ración»: lo que dice «Por ración». */
    @NonNull
    public static Cifras porRacion(@NonNull Cifras total, int raciones, @Nullable Double peso) {
        Cifras g = guardado(total, raciones, peso);
        Alimento plato = new Alimento();
        plato.setCalorias(g.kcal);
        plato.setProteinas(g.proteinas.doubleValue());
        plato.setCarbohidratos(g.carbohidratos.doubleValue());
        plato.setGrasas(g.grasas.doubleValue());
        return linea(plato, peso == null ? GRAMOS_SIN_PESO : gramosRacion(peso, raciones));
    }

    /** ¿El peso no vale? Como en la API: al menos 1 g por ración y como mucho 50 kg. */
    public static boolean pesoMalo(double peso, int raciones) {
        return peso < raciones || peso > 50000;
    }

    /** ¿Salen más de 900 kcal por 100 g con ese peso? Entonces el peso está mal. */
    public static boolean demasiadasKcal(@NonNull Cifras total, int raciones, double peso) {
        return guardado(total, raciones, peso).kcal > KCAL_MAXIMAS_POR_100;
    }
}
