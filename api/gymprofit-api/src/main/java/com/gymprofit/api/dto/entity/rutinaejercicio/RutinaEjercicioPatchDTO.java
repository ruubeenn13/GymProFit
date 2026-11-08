package com.gymprofit.api.dto.entity.rutinaejercicio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// RutinaEjercicioPatchDTO — DTO para actualización parcial (PATCH) de un ejercicio de rutina
// Todos los campos son opcionales; solo se actualizan los que llegan no nulos.
// Usado en el endpoint PATCH de rutina-ejercicio.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RutinaEjercicioPatchDTO implements Serializable {
    private Integer series;
    private Integer repeticiones;
    private BigDecimal pesoRecomendado;
    private Integer tiempoDescanso;
    private Integer orden;
    private String notas;

    // La pauta como rango (GP-244), opcional: «de 8 a 12» repeticiones o «de 30 a 45» s.
    // Con el rango, repeticiones se guarda como el máximo, que es lo que leen las builds
    // viejas; con solo repeticiones, el rango pasa a ser ese número (RutinaEjercicioService).
    private Integer repeticionesMin;
    private Integer repeticionesMax;

    // REPETICIONES o SEGUNDOS; cualquier otra cosa es un 400.
    private String medida;
}
