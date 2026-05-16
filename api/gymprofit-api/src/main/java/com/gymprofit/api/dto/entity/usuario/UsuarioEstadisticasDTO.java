package com.gymprofit.api.dto.entity.usuario;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioEstadisticasDTO implements Serializable {

    private Integer totalSesiones;
    private Integer sesionesCompletadas;
    private Integer totalMinutosEntrenados;
    private Integer totalCaloriasQuemadas;
    private String ejercicioMasFrecuente;
    private Integer rachaActualDias;
    private Integer mejorRachaDias;
    private Integer totalEjerciciosRealizados;
    private BigDecimal pesoCorporalActual;
    private BigDecimal imcActual;
    private Integer totalObjetivosCompletados;
}
