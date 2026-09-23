package com.gymprofit.api.dto.entity.usuario;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// UsuarioEstadisticasDTO — DTO con estadísticas agregadas de un usuario
// Agrupa métricas calculadas (sesiones, minutos, rachas, IMC...)
// para mostrar el resumen de progreso del usuario en la app Android,
// típicamente obtenidas mediante consultas jOOQ agregadas.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioEstadisticasDTO implements Serializable {

    private Integer totalSesiones;
    private Integer sesionesCompletadas;
    private Integer totalMinutosEntrenados;
    // NO HAY CAMPO DE CALORÍAS AQUÍ, Y ES A PROPÓSITO (DEC-004 / GP-010).
    // El gasto calórico de un entrenamiento no se puede estimar con los datos que
    // tiene la app, así que no se estima. La columna sigue en la base de datos hasta
    // la migración que la retire, pero ni se lee ni se escribe.
    private String ejercicioMasFrecuente;
    private Integer rachaActualDias;
    private Integer mejorRachaDias;
    private Integer totalEjerciciosRealizados;
    private BigDecimal pesoCorporalActual;
    private BigDecimal imcActual;
    private Integer totalObjetivosCompletados;
}
