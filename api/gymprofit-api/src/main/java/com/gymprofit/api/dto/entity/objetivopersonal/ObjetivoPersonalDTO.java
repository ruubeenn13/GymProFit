package com.gymprofit.api.dto.entity.objetivopersonal;

import com.gymprofit.api.enums.TipoObjetivo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjetivoPersonalDTO implements Serializable {
    private Integer id;
    private Integer usuarioId;
    private TipoObjetivo tipoObjetivo;
    private String descripcion;
    private BigDecimal valorActual;
    private BigDecimal valorObjetivo;
    private String unidad;
    private LocalDate fechaInicio;
    private LocalDate fechaLimite;
    private Boolean completado;
    private LocalDateTime fechaCompletado;
}