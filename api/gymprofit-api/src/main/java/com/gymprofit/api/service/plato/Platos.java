package com.gymprofit.api.service.plato;

import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AlimentoComida;
import com.gymprofit.api.entity.AlimentoRacion;
import com.gymprofit.api.service.alimentocomida.RacionVigente;
import com.gymprofit.api.service.alimentocomida.ValoresLinea;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// ============================================================
// Platos — qué es un plato y cuánto vale (lote 1.6.6, DEC-047)
//
// Un plato es un alimento del usuario con plato_raciones. Sus valores salen de sus
// ingredientes al guardarlo:
//   · sin peso hecho, los del alimento son los de UNA ración (el total entre N), y su
//     ración pesa 100 g por dentro y no cambia nunca: «1 ración» son 100 g y vale
//     exactamente el total entre N, con la cuenta de cualquier alimento;
//   · con peso hecho, por 100 g (el total por 100 entre el peso), y su ración pesa el
//     peso entre N, en otra fila, para que las líneas viejas sigan con la suya.
// Las dos raciones se distinguen por su fuente. Se enseña solo la del estado actual.
// ============================================================
public final class Platos {

    /** Fuente de la ración de un plato sin peso hecho: 100 g por dentro, nunca gramos de verdad. */
    public static final String FUENTE_SIN_PESO = "Plato sin peso hecho";
    /** Fuente de la ración de un plato con peso hecho: el peso entre las raciones. */
    public static final String FUENTE_CON_PESO = "Plato con peso hecho";

    /** Lo que pesa por dentro la ración de un plato sin peso hecho. */
    public static final BigDecimal GRAMOS_SIN_PESO = BigDecimal.valueOf(100);

    /** Como al crear un alimento: más es más que la grasa pura, y casi siempre son kJ. */
    public static final int KCAL_MAXIMAS_POR_100 = 900;
    /** Lo que cabe en un macro de un alimento (DECIMAL(5,2)). */
    static final BigDecimal MACRO_MAXIMO = new BigDecimal("999.99");

    private Platos() {
    }

    public static boolean esPlato(Alimento a) {
        return a != null && a.getPlatoRaciones() != null;
    }

    public static boolean conPeso(Alimento a) {
        return esPlato(a) && a.getPlatoPesoGramos() != null;
    }

    public static boolean esRacionSinPeso(AlimentoRacion r) {
        return r != null && FUENTE_SIN_PESO.equals(r.getFuente());
    }

    public static boolean esRacionDePlato(AlimentoRacion r) {
        return r != null && (FUENTE_SIN_PESO.equals(r.getFuente()) || FUENTE_CON_PESO.equals(r.getFuente()));
    }

    /** La ración que hoy tiene el plato: la de sin peso o la de con peso; null si no es un plato. */
    public static AlimentoRacion racionActual(Alimento plato) {
        if (!esPlato(plato) || plato.getRaciones() == null) return null;
        String fuente = conPeso(plato) ? FUENTE_CON_PESO : FUENTE_SIN_PESO;
        return plato.getRaciones().stream().filter(r -> fuente.equals(r.getFuente())).findFirst().orElse(null);
    }

    /** Las raciones que se enseñan: las del alimento; de un plato, solo la actual. */
    public static List<AlimentoRacion> racionesVisibles(Alimento a) {
        if (a.getRaciones() == null) return List.of();
        if (!esPlato(a)) return a.getRaciones();
        AlimentoRacion actual = racionActual(a);
        return actual == null ? List.of() : List.of(actual);
    }

    /** Lo que pesa la ración de un plato con peso hecho: el peso entre las raciones, a la décima. */
    public static BigDecimal gramosRacion(BigDecimal peso, int raciones) {
        return peso.divide(BigDecimal.valueOf(raciones), 1, RoundingMode.HALF_UP);
    }

    /** Los valores que se guardan en el alimento del plato, con sus kcal enteras. */
    public record Valores(int kcal, BigDecimal proteinas, BigDecimal carbohidratos, BigDecimal grasas,
                          BigDecimal fibra) {
    }

    /**
     * Los valores del alimento del plato a partir de la suma de sus ingredientes.
     *
     * @param total    la suma de lo que valdría cada ingrediente apuntado.
     * @param raciones cuántas salen.
     * @param peso     el peso hecho, o null.
     * @return por ración (sin peso) o por 100 g (con peso).
     */
    public static Valores aGuardar(ValoresLinea total, int raciones, BigDecimal peso) {
        BigDecimal entre = peso == null ? BigDecimal.valueOf(raciones) : peso;
        BigDecimal por = peso == null ? BigDecimal.ONE : BigDecimal.valueOf(100);
        int kcal = BigDecimal.valueOf(total.kcal()).multiply(por).divide(entre, 0, RoundingMode.HALF_UP).intValue();
        return new Valores(kcal, reparte(total.proteinas(), por, entre), reparte(total.carbohidratos(), por, entre),
                reparte(total.grasas(), por, entre), total.fibra() == null ? null : reparte(total.fibra(), por, entre));
    }

    private static BigDecimal reparte(BigDecimal valor, BigDecimal por, BigDecimal entre) {
        return valor.multiply(por).divide(entre, 2, RoundingMode.HALF_UP);
    }

    /** ¿Caben en las columnas del alimento? (Un macro por ración de más de 999,99 g no.) */
    public static boolean caben(Valores v) {
        return v.proteinas().compareTo(MACRO_MAXIMO) <= 0 && v.carbohidratos().compareTo(MACRO_MAXIMO) <= 0
                && v.grasas().compareTo(MACRO_MAXIMO) <= 0
                && (v.fibra() == null || v.fibra().compareTo(MACRO_MAXIMO) <= 0);
    }

    /**
     * Cuántas raciones del plato son lo que dice una línea, o null si no se sabe: las de
     * su ración si es una del plato y cuadra (GP-177); si no, sus gramos entre los de la
     * ración con peso, que es la única con gramos de verdad.
     */
    public static BigDecimal racionesDe(AlimentoComida linea) {
        AlimentoRacion r = linea.getRacion();
        if (esRacionDePlato(r) && RacionVigente.de(linea) != null) return linea.getRaciones();
        if (esRacionSinPeso(r)) return linea.getRaciones();
        AlimentoRacion conPeso = linea.getAlimento().getRaciones() == null ? null
                : linea.getAlimento().getRaciones().stream().filter(x -> FUENTE_CON_PESO.equals(x.getFuente()))
                .findFirst().orElse(null);
        if (conPeso == null || linea.getCantidadGramos() == null || conPeso.getGramos().signum() <= 0) return null;
        return linea.getCantidadGramos().divide(conPeso.getGramos(), 2, RoundingMode.HALF_UP);
    }
}
