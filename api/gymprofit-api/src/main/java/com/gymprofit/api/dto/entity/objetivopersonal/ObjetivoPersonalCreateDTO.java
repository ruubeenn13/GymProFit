package com.gymprofit.api.dto.entity.objetivopersonal;

import com.gymprofit.api.enums.TipoObjetivo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

// ============================================================
// ObjetivoPersonalCreateDTO — datos necesarios para crear un objetivo personal.
// Recoge el usuario, tipo de objetivo (peso, fuerza, etc.) y los valores
// inicial/meta que el usuario quiere alcanzar en GymProFit.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjetivoPersonalCreateDTO implements Serializable {
    // Usuario propietario del objetivo (obligatorio).
    @NotNull
    private Integer usuarioId;

    // Tipo de objetivo (ej. perder peso, ganar fuerza) (obligatorio).
    @NotNull
    private TipoObjetivo tipoObjetivo;

    private String descripcion;

    // Valor de partida del indicador que se está siguiendo.
    @PositiveOrZero
    private BigDecimal valorActual;

    // Valor que se desea alcanzar.
    @PositiveOrZero
    private BigDecimal valorObjetivo;

    private String unidad;
    private LocalDate fechaLimite;
}