package com.gymprofit.api.dto.entity.rutinaejercicio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RutinaEjercicioDTO implements Serializable {
    private Integer id;
    private Integer rutinaId;
    private Integer ejercicioId;
    private Integer repeticiones;
    private Integer series;
    private BigDecimal pesoRecomendado;
    private Integer tiempoDescanso;
    private Integer orden;
    private String notas;
}
