package com.gymprofit.api.dto.entity.medicioncorporal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MedicionCorporalPatchDTO implements Serializable {
    private LocalDateTime fecha;
    private BigDecimal peso;
    private BigDecimal altura;
    private BigDecimal imc;
    private BigDecimal grasaCorporal;
    private BigDecimal masaMuscular;
    private BigDecimal cintura;
    private BigDecimal pecho;
    private BigDecimal brazos;
    private BigDecimal piernas;
    private String notas;
}
