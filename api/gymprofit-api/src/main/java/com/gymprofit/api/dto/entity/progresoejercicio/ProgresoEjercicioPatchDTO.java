package com.gymprofit.api.dto.entity.progresoejercicio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============================================================
// ProgresoEjercicioPatchDTO — DTO para actualización parcial (PATCH) de un progreso de ejercicio
// Todos los campos son opcionales; solo se actualizan los que llegan no nulos.
// Usado en el endpoint PATCH de progreso de ejercicio para modificar la mejor marca alcanzada.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProgresoEjercicioPatchDTO {
    // Nueva fecha del progreso
    private LocalDateTime fecha;
    // Nuevo mejor peso levantado
    private BigDecimal mejorPeso;
    // Nuevo mejor número de repeticiones
    private Integer mejorRepeticiones;
    // Nuevo mejor tiempo en segundos
    private Integer mejorTiempoSegundos;
    // Notas adicionales sobre el progreso
    private String notas;
}
