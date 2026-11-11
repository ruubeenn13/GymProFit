package com.gymprofit.api.service;

import com.gymprofit.api.service.medicioncorporal.CalculoImc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// CalculoImcTest — el IMC con la altura en centímetros (GP-204)
// Antes se dividía el peso entre la altura en cm al cuadrado y salía ≈0,00. Lo que
// se comprueba aquí: la cuenta en metros a un decimal, y que sin un dato con el que
// calcular el IMC es nulo, nunca 0.
// ============================================================
@DisplayName("CalculoImc — IMC con la altura en cm (GP-204)")
class CalculoImcTest {

    private static BigDecimal imc(String peso, String altura) {
        return CalculoImc.calcular(peso == null ? null : new BigDecimal(peso),
                altura == null ? null : new BigDecimal(altura));
    }

    @Test
    @DisplayName("78 kg y 178 cm → 24,6")
    void caso_del_backlog() {
        assertThat(imc("78", "178")).isEqualByComparingTo("24.6");
        assertThat(imc("78", "178").scale()).isEqualTo(1);
    }

    @Test
    @DisplayName("redondea a un decimal con la mitad hacia arriba")
    void redondeo() {
        // 70 / 1,75² = 22,857… → 22,9; 80 / 1,80² = 24,691… → 24,7
        assertThat(imc("70.00", "175.00")).isEqualByComparingTo("22.9");
        assertThat(imc("80", "180")).isEqualByComparingTo("24.7");
    }

    @Test
    @DisplayName("sin altura, con altura 0 o sin peso → nulo, nunca 0")
    void sinDatos_nulo() {
        assertThat(imc("78", null)).isNull();
        assertThat(imc("78", "0")).isNull();
        assertThat(imc("78", "-170")).isNull();
        assertThat(imc(null, "178")).isNull();
        assertThat(imc("0", "178")).isNull();
    }

    @Test
    @DisplayName("una altura que no puede estar en cm (1,78) → nulo, no un IMC de miles")
    void alturaEnMetros_nulo() {
        // 78 / 0,0178² ≈ 246 000: no es un IMC y no cabe en la columna DECIMAL(4,2).
        assertThat(imc("78", "1.78")).isNull();
    }
}
