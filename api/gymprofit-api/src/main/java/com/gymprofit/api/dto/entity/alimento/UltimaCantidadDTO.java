package com.gymprofit.api.dto.entity.alimento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// UltimaCantidadDTO — lo último que apuntaste de un alimento (lote 1.6.3, A1)
// La cantidad de su línea más reciente (por la fecha de la comida y, a igualdad, la
// última creada), con los mismos nombres que la línea de una comida (AlimentoComidaDTO)
// para que la app la escriba igual. Con la regla de GP-177: si esa ración ya no pesa
// lo mismo, solo los gramos.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UltimaCantidadDTO implements Serializable {
    private BigDecimal cantidadGramos;
    // La ración y cuántas, si se apuntó por raciones; null, en gramos.
    private Integer racionId;
    private String racionNombre;
    private BigDecimal racionGramos;
    private BigDecimal raciones;
    private String racionUnidad;
    private String racionUnidadPlural;
}
