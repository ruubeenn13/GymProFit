package com.gymprofit.api.dto.entity.medicioncorporal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============================================================
// MedicionCorporalDTO — DTO de salida con una medición corporal completa
// Representa la medición tal como se devuelve al cliente, incluyendo
// el IMC calculado y la fecha en que se realizó.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MedicionCorporalDTO implements Serializable {
    private Integer id;
    private Integer usuarioId;
    private LocalDateTime fecha;
    private BigDecimal peso;
    private BigDecimal altura;
    // Índice de masa corporal, calculado a partir de peso y altura
    private BigDecimal imc;
    private BigDecimal grasaCorporal;
    private BigDecimal masaMuscular;
    private BigDecimal cintura;
    private BigDecimal pecho;
    private BigDecimal brazos;
    private BigDecimal piernas;
    private String notas;
}
