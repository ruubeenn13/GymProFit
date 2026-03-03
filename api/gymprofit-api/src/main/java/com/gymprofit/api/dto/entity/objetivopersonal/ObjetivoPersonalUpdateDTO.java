package com.gymprofit.api.dto.entity.objetivopersonal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjetivoPersonalUpdateDTO implements Serializable {
    private Integer id;
    private BigDecimal valorObjetivo;
    private boolean completado;
}
