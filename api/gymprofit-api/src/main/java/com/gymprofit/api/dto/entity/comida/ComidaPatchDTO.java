package com.gymprofit.api.dto.entity.comida;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============================================================
// ComidaPatchDTO — datos para actualización parcial (PATCH) de una comida
// Todos los campos son opcionales; solo se aplican los que llegan informados
// (no nulos) sobre la comida existente identificada por su id.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComidaPatchDTO implements Serializable {
    // Nueva fecha/hora de la comida (opcional)
    private LocalDateTime fecha;
    // Nuevo tipo de comida (opcional)
    private String tipoComida;
    // Nuevo total de calorías (opcional)
    private Integer totalCalorias;
    // Nuevo total de proteínas (opcional)
    private BigDecimal totalProteinas;
    // Nuevo total de carbohidratos (opcional)
    private BigDecimal totalCarbohidratos;
    // Nuevo total de grasas (opcional)
    private BigDecimal totalGrasas;
    // Nuevas notas (opcional)
    private String notas;
}
