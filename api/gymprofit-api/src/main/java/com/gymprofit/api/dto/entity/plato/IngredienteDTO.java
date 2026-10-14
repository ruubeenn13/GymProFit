package com.gymprofit.api.dto.entity.plato;

import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// IngredienteDTO — un ingrediente de un plato, para leerlo (lote 1.6.6)
// El alimento entero (para editar su cantidad en la ficha), la cantidad con los mismos
// nombres que una línea de comida y lo que vale hoy (lo que sumará al guardar otra vez).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IngredienteDTO implements Serializable {
    private AlimentoDTO alimento;
    private BigDecimal cantidadGramos;
    // La ración y cuántas si se eligió así y aún pesa lo mismo (GP-177); si no, null.
    private Integer racionId;
    private String racionNombre;
    private BigDecimal racionGramos;
    private BigDecimal raciones;
    private String racionUnidad;
    private String racionUnidadPlural;
    private ValoresDTO valores;
}
