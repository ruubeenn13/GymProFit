package com.gymprofit.api.dto.entity.medicioncorporal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MedicionCorporalCreateDTO implements Serializable {
    private Integer usuarioId;
    private Double peso;
    private Double altura;
    private BigDecimal grasaCorporal;
    private BigDecimal masaMuscular;
    private BigDecimal cintura;
    private BigDecimal pecho;
    private BigDecimal brazos;
    private BigDecimal piernas;
    private String notas;
}
