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
            this(porMinuto, 1000, 1000);
        }

        ConReloj(int porMinuto, int porMinutoCuenta, int porDiaCuenta) {
            super(porMinuto, porMinutoCuenta, porDiaCuenta);
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
        assertThat(limite.pedirLectura(1)).isZero();
        limite.ahora += 20_000;
        assertThat(limite.pedirLectura(1)).isZero();
        assertThat(limite.pedirLectura(1)).isZero();
        // La primera fue hace 20 s: faltan 40.
        assertThat(limite.pedirLectura(1)).isEqualTo(40);
        limite.ahora += 40_000;
        assertThat(limite.pedirLectura(1)).isZero();
        assertThat(limite.pedirLectura(1)).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("GP-167: 3 lecturas por minuto y por cuenta; otra cuenta sigue leyendo")
    void por_cuenta_por_minuto() {
        ConReloj limite = new ConReloj(10, 3, 30);
        assertThat(limite.pedirLectura(1)).isZero();
        limite.ahora += 10_000;
        assertThat(limite.pedirLectura(1)).isZero();
        assertThat(limite.pedirLectura(1)).isZero();
        // La primera de la cuenta 1 fue hace 10 s: faltan 50.
        assertThat(limite.pedirLectura(1)).isEqualTo(50);
        assertThat(limite.pedirLectura(2)).isZero();
        limite.ahora += 50_000;
        assertThat(limite.pedirLectura(1)).isZero();
    }

    @Test
    @DisplayName("GP-167: 30 lecturas al día por cuenta, en ventana de 24 h")
    void por_cuenta_por_dia() {
        ConReloj limite = new ConReloj(10, 3, 30);
        long inicio = limite.ahora;
        for (int i = 0; i < 30; i++) {
            assertThat(limite.pedirLectura(1)).as("lectura %d", i).isZero();
            limite.ahora += 30_000; // dos por minuto: nunca toca el cupo de minuto
        }
        long espera = limite.pedirLectura(1);
        // Hasta que la primera salga de las 24 h.
        long faltan = (inicio + LimiteOpenFoodFacts.DIA_MS - limite.ahora + 999) / 1000;
        assertThat(espera).isEqualTo(faltan);
        assertThat(limite.pedirLectura(2)).isZero();
        limite.ahora = inicio + LimiteOpenFoodFacts.DIA_MS;
        assertThat(limite.pedirLectura(1)).isZero();
    }

    @Test
    @DisplayName("GP-167: la lectura que niega la cuenta no gasta cupo de la API")
    void negada_no_gasta_global() {
        ConReloj limite = new ConReloj(4, 3, 30);
        for (int i = 0; i < 3; i++) assertThat(limite.pedirLectura(1)).isZero();
        for (int i = 0; i < 5; i++) assertThat(limite.pedirLectura(1)).isPositive();
        // La API lleva 3 de 4: a la cuenta 2 le queda una.
        assertThat(limite.pedirLectura(2)).isZero();
        assertThat(limite.pedirLectura(2)).isPositive();
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
