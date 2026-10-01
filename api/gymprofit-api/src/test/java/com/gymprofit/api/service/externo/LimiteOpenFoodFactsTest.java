package com.gymprofit.api.service.externo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// LimiteOpenFoodFactsTest — el cupo y la memoria de lo que no existe (GP-160)
// Con un reloj que se mueve a mano.
// ============================================================
class LimiteOpenFoodFactsTest {

    private static final class ConReloj extends LimiteOpenFoodFacts {
        long ahora = 1_000_000;

        ConReloj(int porMinuto) {
            super(porMinuto);
        }

        @Override
        long ahora() {
            return ahora;
        }
    }

    @Test
    @DisplayName("ventana deslizante: la lectura vuelve cuando sale de la ventana la más antigua")
    void ventana_deslizante() {
        ConReloj limite = new ConReloj(3);
        assertThat(limite.pedirLectura()).isZero();
        limite.ahora += 20_000;
        assertThat(limite.pedirLectura()).isZero();
        assertThat(limite.pedirLectura()).isZero();
        // La primera fue hace 20 s: faltan 40.
        assertThat(limite.pedirLectura()).isEqualTo(40);
        limite.ahora += 40_000;
        assertThat(limite.pedirLectura()).isZero();
        assertThat(limite.pedirLectura()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("un código desconocido se recuerda un día y después se olvida")
    void desconocidos() {
        ConReloj limite = new ConReloj(10);
        limite.apuntarDesconocido("123");
        assertThat(limite.esDesconocido("123")).isTrue();
        assertThat(limite.esDesconocido("456")).isFalse();
        limite.ahora += LimiteOpenFoodFacts.OLVIDO_MS;
        assertThat(limite.esDesconocido("123")).isFalse();
    }

    @Test
    @DisplayName("como mucho 10 000 desconocidos: se olvida el más viejo")
    void tope() {
        ConReloj limite = new ConReloj(10);
        for (int i = 0; i <= LimiteOpenFoodFacts.MAX_DESCONOCIDOS; i++) limite.apuntarDesconocido("c" + i);
        assertThat(limite.esDesconocido("c0")).isFalse();
        assertThat(limite.esDesconocido("c" + LimiteOpenFoodFacts.MAX_DESCONOCIDOS)).isTrue();
    }
}
