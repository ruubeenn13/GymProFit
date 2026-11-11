package com.gymprofit.api.service.medicioncorporal;

import java.math.BigDecimal;
import java.math.RoundingMode;

// ============================================================
// CalculoImc — el único cálculo del IMC de una medición corporal (GP-204)
// La altura llega en centímetros (V202605251000 amplió la columna para eso), así
// que se pasa a metros antes de elevarla al cuadrado. Antes cada método del servicio
// hacía su cuenta con la altura en cm y el IMC se guardaba como 0,00.
// La migración V202611111800 repite esta misma regla en SQL: si cambia una, cambia
// la otra.
// ============================================================
public final class CalculoImc {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    // Un IMC de 100 o más no es de una persona, y no cabe en DECIMAL(4,2): sale de una
    // altura que no puede estar en cm (1,78 en metros da ≈246 000).
    private static final BigDecimal TOPE = CIEN;

    private CalculoImc() {
    }

    /**
     * Calcula el IMC a un decimal, con la mitad hacia arriba.
     *
     * @param pesoKg   peso en kilos
     * @param alturaCm altura en centímetros
     * @return el IMC, o {@code null} si falta el peso o la altura, si alguno no es
     *         positivo o si el resultado no es un IMC posible; nunca 0
     */
    public static BigDecimal calcular(BigDecimal pesoKg, BigDecimal alturaCm) {
        if (pesoKg == null || alturaCm == null || pesoKg.signum() <= 0 || alturaCm.signum() <= 0) {
            return null;
        }
        BigDecimal alturaM = alturaCm.divide(CIEN);
        BigDecimal imc = pesoKg.divide(alturaM.multiply(alturaM), 1, RoundingMode.HALF_UP);
        return imc.compareTo(TOPE) < 0 ? imc : null;
    }
}
