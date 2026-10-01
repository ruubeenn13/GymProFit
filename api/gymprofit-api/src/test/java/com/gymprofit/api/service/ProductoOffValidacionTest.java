package com.gymprofit.api.service;

import com.gymprofit.api.dto.entity.productooff.ProductoOffImportDTO;
import com.gymprofit.api.service.productooff.ProductoOffValidacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// ProductoOffValidacionTest — qué producto de Open Food Facts se acepta (GP-164)
// Las mismas reglas que datos/productos/filtrar_off.py.
// ============================================================
class ProductoOffValidacionTest {

    private static ProductoOffImportDTO galletas() {
        return new ProductoOffImportDTO("8400000000011", "  Galletas  ", "Marca", 458.0, 7.0, 70.0, 16.0, 3.0,
                30.0, "30 g", "500 g", 10, null, null);
    }

    @Test
    @DisplayName("un producto correcto pasa limpio: textos sin espacios sobrantes")
    void correcto() {
        assertThat(ProductoOffValidacion.limpiar(galletas())).hasValueSatisfying(p -> {
            assertThat(p.getNombre()).isEqualTo("Galletas");
            assertThat(p.getKcal()).isEqualTo(458.0);
        });
    }

    @Test
    @DisplayName("sin nombre, sin código numérico o sin alguno de los cuatro valores, fuera")
    void incompleto() {
        ProductoOffImportDTO p = galletas();
        p.setNombre(" ");
        assertThat(ProductoOffValidacion.limpiar(p)).isEmpty();
        p = galletas();
        p.setCodigo("12a4");
        assertThat(ProductoOffValidacion.limpiar(p)).isEmpty();
        p = galletas();
        p.setGrasas(null);
        assertThat(ProductoOffValidacion.limpiar(p)).isEmpty();
    }

    @Test
    @DisplayName("más de 100 g de algo por cada 100 g, o macros que suman más de 100, fuera")
    void cifras_imposibles() {
        ProductoOffImportDTO p = galletas();
        p.setCarbohidratos(120.0);
        assertThat(ProductoOffValidacion.limpiar(p)).isEmpty();
        p = galletas();
        p.setProteinas(40.0);
        p.setCarbohidratos(50.0);
        p.setGrasas(20.0);
        p.setKcal(540.0);
        assertThat(ProductoOffValidacion.limpiar(p)).isEmpty();
    }

    @Test
    @DisplayName("kcal que no cuadran con los macros, fuera; con alcohol, cuadran")
    void kcal() {
        ProductoOffImportDTO p = galletas();
        p.setKcal(110.0);
        assertThat(ProductoOffValidacion.limpiar(p)).isEmpty();

        ProductoOffImportDTO cerveza = new ProductoOffImportDTO("8410000000001", "Cerveza", null, 42.0, 0.4, 3.0,
                0.0, null, null, null, "33 cl", 5, 3.8, null);
        assertThat(ProductoOffValidacion.limpiar(cerveza)).isPresent();
        cerveza.setAlcohol(null);
        assertThat(ProductoOffValidacion.limpiar(cerveza)).isEmpty();
    }

    @Test
    @DisplayName("un nombre de más de 200 caracteres se recorta sin partir un emoji")
    void recorte() {
        ProductoOffImportDTO p = galletas();
        p.setNombre("a".repeat(199) + "😀" + "b");
        assertThat(ProductoOffValidacion.limpiar(p)).hasValueSatisfying(l ->
                assertThat(l.getNombre()).isEqualTo("a".repeat(199)));
    }
}
