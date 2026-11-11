package com.gymprofit.api.dto.entity.medicioncorporal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============================================================
// MedicionCorporalPatchDTO — DTO para actualización parcial de una medición corporal
// Contiene los campos opcionales que se pueden modificar de una medición
// corporal ya registrada (PATCH), sin exigir el usuarioId. El IMC no está:
// lo calcula siempre el servicio (GP-204).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MedicionCorporalPatchDTO implements Serializable {
    private LocalDateTime fecha;
    private BigDecimal peso;
    private BigDecimal altura;
    private BigDecimal grasaCorporal;
    private BigDecimal masaMuscular;
    private BigDecimal cintura;
    private BigDecimal pecho;
    private BigDecimal brazos;
    private BigDecimal piernas;
    private String notas;
}
