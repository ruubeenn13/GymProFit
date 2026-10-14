package com.gymprofit.api.service.alimentocomida;

import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AlimentoComida;

import java.math.BigDecimal;
import java.math.RoundingMode;

// ============================================================
// ValoresLinea — lo que vale una cantidad de un alimento (lote 1.6.6, A1)
//
// La única cuenta de una línea: las kcal, redondeadas al entero como siempre
// (calorías por 100 g por los gramos entre 100), y cada macro con dos decimales y la
// mitad hacia arriba, como los devolvía AlimentoComidaMapper. La usan las líneas de
// las comidas al escribirse y los platos al sumar sus ingredientes, para que «1 ración»
// valga lo mismo en todas partes.
// ============================================================
public record ValoresLinea(int kcal, BigDecimal proteinas, BigDecimal carbohidratos, BigDecimal grasas,
                           BigDecimal fibra) {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    /** Nada: el punto de partida de una suma. La fibra, sin dato. */
    public static final ValoresLinea CERO = new ValoresLinea(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);

    /**
     * @param alimento con sus valores por 100 g (o, en un plato sin peso hecho, por ración
     *                 de 100 g por dentro).
     * @param gramos   la cantidad; null vale 0.
     */
    public static ValoresLinea de(Alimento alimento, BigDecimal gramos) {
        if (gramos == null) gramos = BigDecimal.ZERO;
        int kcal = alimento.getCalorias() == null ? 0
                : (int) Math.round((alimento.getCalorias() * gramos.doubleValue()) / 100.0);
        return new ValoresLinea(kcal, parte(alimento.getProteinas(), gramos), parte(alimento.getCarbohidratos(), gramos),
                parte(alimento.getGrasas(), gramos),
                alimento.getFibra() == null ? null : parte(alimento.getFibra(), gramos));
    }

    private static BigDecimal parte(BigDecimal porCien, BigDecimal gramos) {
        if (porCien == null) return BigDecimal.ZERO.setScale(2);
        return porCien.multiply(gramos).divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    /** La suma; la fibra solo se sabe si alguna de las dos la sabe. */
    public ValoresLinea mas(ValoresLinea otra) {
        BigDecimal f = fibra == null ? otra.fibra : (otra.fibra == null ? fibra : fibra.add(otra.fibra));
        return new ValoresLinea(kcal + otra.kcal, proteinas.add(otra.proteinas),
                carbohidratos.add(otra.carbohidratos), grasas.add(otra.grasas), f);
    }

    /** Escribe las kcal y los macros en la línea. */
    public void ponerEn(AlimentoComida linea) {
        linea.setCaloriasTotales(kcal);
        linea.setProteinasTotales(proteinas);
        linea.setCarbohidratosTotales(carbohidratos);
        linea.setGrasasTotales(grasas);
    }
}
