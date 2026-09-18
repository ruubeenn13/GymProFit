package com.gymprofit.api.dto.entity.serierealizada;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
// ============================================================
// SerieRealizadaDTO — una serie concreta, tal y como se devuelve al cliente.
// ============================================================
@Schema(description = "Una serie concreta de un ejercicio dentro de una sesión")
public class SerieRealizadaDTO implements Serializable {

    @Schema(description = "Identificador de la serie", example = "412")
    private Integer id;

    @Schema(description = "Orden dentro del ejercicio, empezando en 1", example = "3")
    private Integer numero;

    @Schema(description = "Repeticiones realmente hechas", example = "8")
    private Integer repeticiones;

    @Schema(description = "Peso usado en esta serie; nulo si es peso corporal", example = "70.00")
    private BigDecimal peso;

    @Schema(description = "Si el usuario llegó a marcarla como completada", example = "true")
    private Boolean completada;
}
