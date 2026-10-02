package com.gymprofit.api.service.productooff;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// RacionesProductoTest — el envase primero, después la ración declarada (lote 1.6.1)
// ============================================================
class RacionesProductoTest {

    private static BigDecimal g(String n) {
        return new BigDecimal(n);
    }

    @Test
    @DisplayName("un yogur de 200 g con ración de 125: «1 envase» y después «1 ración»")
    void envase_y_racion() {
        List<RacionesProducto.Racion> r = RacionesProducto.de("200 g", g("125"), "125 g");
        assertThat(r).extracting(RacionesProducto.Racion::nombre).containsExactly("1 envase", "1 ración");
        assertThat(r.get(0).gramos()).isEqualByComparingTo("200");
        assertThat(r.get(0).nombreEn()).isEqualTo("1 pack");
        assertThat(r.get(1).nombreEn()).isEqualTo("1 serving");
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
