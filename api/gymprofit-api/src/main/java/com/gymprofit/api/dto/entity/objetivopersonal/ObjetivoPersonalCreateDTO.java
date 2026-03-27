package com.gymprofit.api.dto.entity.objetivopersonal;

import com.gymprofit.api.enums.TipoObjetivo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjetivoPersonalCreateDTO implements Serializable {
    private Integer usuarioId;
    private TipoObjetivo tipoObjetivo;
    private String descripcion;
    private BigDecimal valorActual;
    private BigDecimal valorObjetivo;
    private String unidad;
    private LocalDate fechaLimite;
}