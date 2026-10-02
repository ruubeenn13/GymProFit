package com.gymprofit.api.dto.entity.comida;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// ============================================================
// AnadirAlimentoDTO — POST /comidas/anadir: un alimento a la comida de un día (1.6.1)
//
// El alimento, por id (del catálogo o tuyo) o por código de barras (que se materializa
// si es un producto); uno de los dos. La cantidad, en gramos o en raciones: la ración
// por su id o, en un producto que aún no estaba en el catálogo, por su posición en la
// lista de raciones que dio la búsqueda (la misma al materializarlo). Con ración y sin
// gramos, los gramos salen de ella. Sin usuario: es el del token (DEC-013).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnadirAlimentoDTO {

    // Día de la comida.
    @NotNull
    private LocalDate fecha;

    // DESAYUNO, ALMUERZO, COMIDA, MERIENDA, CENA o SNACK.
    @NotBlank
    private String tipoComida;

    private Integer alimentoId;

    @Pattern(regexp = "[0-9]{1,32}")
    private String barcode;

    private BigDecimal cantidadGramos;

    private Integer racionId;

    // Posición de la ración en la lista del alimento, desde 0; en lugar de racionId.
    @PositiveOrZero
    private Integer racionIndice;

    private BigDecimal raciones;
}
