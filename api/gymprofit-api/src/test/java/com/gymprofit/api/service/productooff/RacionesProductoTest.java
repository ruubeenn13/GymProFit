package com.gymprofit.api.service.productooff;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// RacionesProductoTest — qué ración propone un producto (lotes 1.6.1 y 1.6.4)
// La primera es la que proponen la ficha, la hoja del escáner y el «+». Desde la 1.6.4
// (GP-182), una ración declarada más pequeña que el envase va delante de él.
// ============================================================
class RacionesProductoTest {

    private static BigDecimal g(String n) {
        return new BigDecimal(n);
    }

    @Test
    @DisplayName("GP-182: unas galletas de 400 g con ración de 30 proponen la ración, y el envase después")
    void racion_antes_que_envase() {
        List<RacionesProducto.Racion> r = RacionesProducto.de("400 g", g("30"), "30 g");
        assertThat(r).extracting(RacionesProducto.Racion::nombre).containsExactly("1 ración", "1 envase");
        assertThat(r.get(0).gramos()).isEqualByComparingTo("30");
        assertThat(r.get(0).nombreEn()).isEqualTo("1 serving");
        assertThat(r.get(1).gramos()).isEqualByComparingTo("400");
        assertThat(r.get(1).nombreEn()).isEqualTo("1 pack");
    }

    @Test
    @DisplayName("GP-182: un yogur de 125 g, sin ración más pequeña, propone el envase")
    void yogur_envase() {
        assertThat(RacionesProducto.de("125 g", null, null))
                .extracting(RacionesProducto.Racion::nombre).containsExactly("1 envase");
        assertThat(RacionesProducto.de("125 g", g("125"), "125 g"))
                .extracting(RacionesProducto.Racion::nombre).containsExactly("1 envase");
    }

    @Test
    @DisplayName("una ración declarada mayor que el envase no se adelanta: como hasta ahora")
    void racion_mayor() {
        assertThat(RacionesProducto.de("200 g", g("250"), "250 g"))
                .extracting(RacionesProducto.Racion::nombre).containsExactly("1 envase", "1 ración");
    }

    @Test
    @DisplayName("un multipack da «1 unidad»")
    void multipack() {
        List<RacionesProducto.Racion> r = RacionesProducto.de("4 x 125 g", null, null);
        assertThat(r).extracting(RacionesProducto.Racion::nombre).containsExactly("1 unidad");
        assertThat(r.get(0).gramos()).isEqualByComparingTo("125");
        assertThat(r.get(0).nombreEn()).isEqualTo("1 unit");
    }

    @Test
    @DisplayName("GP-182: en un multipack, la unidad sigue primero aunque la ración sea más pequeña")
    void multipack_con_racion() {
        assertThat(RacionesProducto.de("6 x 40 g", g("20"), "20 g"))
                .extracting(RacionesProducto.Racion::nombre).containsExactly("1 unidad", "1 ración");
    }

    @Test
    @DisplayName("si la ración pesa lo mismo que el envase, solo el envase")
    void sin_repetir() {
        assertThat(RacionesProducto.de("330 ml", g("330"), null))
                .extracting(RacionesProducto.Racion::nombre).containsExactly("1 envase");
    }

    @Test
    @DisplayName("más de 500 g no es una ración: un kilo de arroz se queda con su ración")
    void envase_grande() {
        assertThat(RacionesProducto.de("1 kg", g("80"), "80 g"))
                .extracting(RacionesProducto.Racion::nombre).containsExactly("1 ración");
        assertThat(RacionesProducto.de("500 g", null, null))
                .extracting(RacionesProducto.Racion::nombre).containsExactly("1 envase");
    }

    @Test
    @DisplayName("sin envase legible ni ración, nada: la app ofrece 100 g")
    void nada() {
        assertThat(RacionesProducto.de("6 pcs", null, null)).isEmpty();
        assertThat(RacionesProducto.de(null, null, null)).isEmpty();
    }
}
