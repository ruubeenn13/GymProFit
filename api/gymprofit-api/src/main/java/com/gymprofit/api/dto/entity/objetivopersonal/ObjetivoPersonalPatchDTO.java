package com.gymprofit.api.dto.entity.objetivopersonal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjetivoPersonalPatchDTO implements Serializable {
    private String descripcion;
    private BigDecimal valorActual;
    private BigDecimal valorObjetivo;
    private String unidad;
    private LocalDate fechaLimite;
}
