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
public class ObjetivoPersonalDTO implements Serializable {
    private Integer id;
    private Integer usuarioId;
    private String tipoObjetivo;
    private String descripcion;
    private BigDecimal valorActual;
    private BigDecimal valorObjetivo;
    private String unidad;
    private LocalDate fechaInicio;
    private LocalDate fechaLimite;
    private Boolean completado;
    private LocalDate fechaCompletado;
}
