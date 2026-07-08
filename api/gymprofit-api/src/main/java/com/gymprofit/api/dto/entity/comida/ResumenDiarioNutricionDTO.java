package com.gymprofit.api.dto.entity.comida;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

// ============================================================
// ResumenDiarioNutricionDTO — totales nutricionales agregados de UN día
// Fila del histórico nutricional por rango de fechas: suma de calorías y
// macros de todas las comidas registradas por el usuario ese día. Alimenta
// las gráficas de kcal/macros (7-30 días) y el futuro historial de nutrición.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResumenDiarioNutricionDTO implements Serializable {
    // Día al que corresponde el resumen
    private LocalDate fecha;
    // Calorías totales del día (suma de todas las comidas)
    private Integer calorias;
    // Proteínas totales del día (g)
    private BigDecimal proteinas;
    // Carbohidratos totales del día (g)
    private BigDecimal carbohidratos;
    // Grasas totales del día (g)
    private BigDecimal grasas;
}
