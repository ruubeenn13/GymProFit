package com.gymprofit.api.dto.entity.record;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============================================================
// RecordDTO — una marca de un ejercicio: un récord, o una primera marca (GP-088)
//
// Sirve para las tres cosas que enseña la app: el récord vigente de cada ejercicio
// (pantalla Récords), los récords batidos en un periodo (tarjeta de inicio) y los
// cambios de marca de una sesión (resumen). Lleva el nombre del ejercicio en los
// dos idiomas del catálogo y la clave del músculo para que la app agrupe por zona
// sin una segunda llamada.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordDTO implements Serializable {

    private Integer ejercicioId;
    private String ejercicioNombre;
    private String ejercicioNombreEn;

    // Clave de músculo normalizada (la misma de la silueta de inicio), o null en cardio.
    private String musculo;

    // PESO: la marca son los kilos con sus repeticiones. REPETICIONES: ejercicio sin peso.
    private String tipo;

    // Kilos de la serie. Nulo en un ejercicio sin peso.
    private BigDecimal peso;
    private Integer repeticiones;

    // 1RM estimado con Epley. Nulo en un ejercicio sin peso.
    private BigDecimal unoRmEstimado;

    private LocalDateTime fecha;
    private Integer sesionId;

    // La marca que se superó. Nulos en una primera marca, que no supera nada.
    private BigDecimal pesoAnterior;
    private Integer repeticionesAnterior;
}
