package com.gymprofit.api.service.productooff;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// EnvaseProductoTest — leer el envase de Open Food Facts (lote 1.6.1)
// Solo lo que se lee sin ambigüedad: una cantidad con su unidad, o un multipack de
// unidades iguales. Lo demás (cifras sueltas, piezas, onzas, neto y escurrido) no da
// ración, y el producto se queda con su ración declarada o con 100 g.
// ============================================================
class EnvaseProductoTest {

    @ParameterizedTest(name = "«{0}» → envase de {1} g")
    @CsvSource(delimiter = '|', value = {
            "500 g|500",
            "200g|200",
            "1 kg|1000",
            "0,5 kg|500",
            "1 l|1000",
            "1 litro|1000",
            "1,5L|1500",
            "500 ml|500",
            "33 cl|330",
            "200 gr|200",
            "200 gram|200",
            "200 g.|200",
            "1 L.|1000",
            "1 x 30 g|30",
            "125 gramos|125",
    })
    void envase(String texto, String gramos) {
        Optional<EnvaseProducto> e = EnvaseProducto.leer(texto);
        assertThat(e).isPresent();
        assertThat(e.get().gramos()).isEqualByComparingTo(new BigDecimal(gramos));
        assertThat(e.get().unidad()).isFalse();
    }

    @ParameterizedTest(name = "«{0}» → unidad de {1} g")
    @CsvSource(delimiter = '|', value = {
            "4 x 125 g|125",
            "4x125g|125",
            "6 x 1 l|1000",
            "500 g (4 x 125 g)|125",
            "500 g (4x125g)|125",
            "480 g (120 g x 4)|120",
            "4 x 200 ml|200",
            "2 x 62,5 g|62.5",
    })
    void multipack(String texto, String gramos) {
        Optional<EnvaseProducto> e = EnvaseProducto.leer(texto);
        assertThat(e).isPresent();
        assertThat(e.get().gramos()).isEqualByComparingTo(new BigDecimal(gramos));
        assertThat(e.get().unidad()).isTrue();
    }

    @ParameterizedTest(name = "«{0}» no se lee")
    @ValueSource(strings = {"", "  ", "250", "1", "6 pcs", "1pcs", "7 oz", "52 g (10 capsules)",
            "345 g (neto), 180 g (escurrido), 370 ml", "0 g", "500 g (3 x 125 g)", "20 x 0 g",
            "100000 g", "abc g"})
    void ambiguo(String texto) {
        assertThat(EnvaseProducto.leer(texto)).isEmpty();
    }

    @Test
    @DisplayName("sin envase, nada")
    void nulo() {
        assertThat(EnvaseProducto.leer(null)).isEmpty();
    }
}
