package com.gymprofit.api.dto.entity.sesionentrenamiento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============================================================
// VolumenSemanalDTO — kilos movidos y series hechas en una semana (GP-216 y GP-249)
// Una entrada por semana pedida, también las que no se entrenó (a cero), para que la
// app dibuje las barras sin rellenar huecos. El inicio de cada semana es el lunes a las
// 00:00 de la hora local que mandó la app.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VolumenSemanalDTO implements Serializable {

    // Inicio de la semana (lunes a las 00:00, hora del usuario).
    private LocalDateTime inicio;

    // Kilos movidos en las sesiones terminadas que empezaron esa semana (definición de GP-011).
    private BigDecimal volumenKg;

    // Sesiones terminadas que empezaron esa semana.
    private Integer sesiones;

    // Series hechas en esas sesiones, contadas como en el resumen y el Historial (GP-249):
    // quien entrena con su peso mueve 0 kg y Progreso e Inicio enseñan entonces sus series.
    private Integer series;
}
