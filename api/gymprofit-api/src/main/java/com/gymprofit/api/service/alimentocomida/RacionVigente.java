package com.gymprofit.api.service.alimentocomida;

import com.gymprofit.api.entity.AlimentoComida;
import com.gymprofit.api.entity.AlimentoRacion;

import java.math.BigDecimal;

// ============================================================
// RacionVigente — una línea solo va por raciones mientras sus gramos cuadren (GP-177)
//
// Los gramos mandan (DEC-042): la ración solo dice cómo enseñarlos. Si después alguien
// cambia el peso de esa ración (la rebanada de un pan propio pasa de 40 a 50 g), la
// línea sigue teniendo 40 g, pero ya no es «1 rebanada». Se decide al leer, sin
// reescribir nada: si la ración vuelve a pesar lo que pesaba, la línea vuelve a salir
// con ella.
// ============================================================
public final class RacionVigente {

    /** Margen entre los gramos de la línea y los de la ración por cuántas. */
    static final BigDecimal MARGEN = new BigDecimal("0.5");

    private RacionVigente() {
    }

    /**
     * @return true si {@code gramos} son los de una ración de {@code racionGramos} por
     * {@code raciones}, con medio gramo de margen; false si falta algún dato.
     */
    public static boolean cuadra(BigDecimal gramos, BigDecimal racionGramos, BigDecimal raciones) {
        if (gramos == null || racionGramos == null || raciones == null) return false;
        return gramos.subtract(racionGramos.multiply(raciones)).abs().compareTo(MARGEN) <= 0;
    }

    /** La ración de la línea si sus gramos cuadran con ella; si no, null. */
    public static AlimentoRacion de(AlimentoComida linea) {
        AlimentoRacion racion = linea.getRacion();
        if (racion == null) return null;
        return cuadra(linea.getCantidadGramos(), racion.getGramos(), linea.getRaciones()) ? racion : null;
    }
}
