package com.gymprofit.api.dto.entity.comida;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComidaDTO implements Serializable {
    private Integer id;
    private Integer usuarioId;
    private LocalDateTime fecha;
    private String tipoComida;
    private Integer totalCalorias;
    private BigDecimal totalProteinas;
    private BigDecimal totalCarbohidratos;
    private BigDecimal totalGrasas;
    private String notas;

}
