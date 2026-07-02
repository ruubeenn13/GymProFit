package com.gymprofit.api.dto.entity.progresoejercicio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============================================================
// ProgresoEjercicioDTO — representación completa del progreso de un usuario
// en un ejercicio: fecha del registro y mejores marcas alcanzadas (peso,
// repeticiones, tiempo), tal como se devuelve al cliente.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProgresoEjercicioDTO implements Serializable {
    private Integer id;
    private Integer usuarioId;
    private Integer ejercicioId;
    private LocalDateTime fecha;
    private BigDecimal mejorPeso;
    private Integer mejorRepeticiones;
    private Integer mejorTiempoSegundos;
    private String notas;
}
