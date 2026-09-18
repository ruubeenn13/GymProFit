package com.gymprofit.api.dto.entity.serierealizada;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
// SerieRealizadaCreateDTO — una serie tal y como la manda el cliente.
//
// Los rangos son los mismos que valida la app en utils/Numeros, para que un
// cliente que se los salte reciba un 400 en vez de meter basura en la base.
// ============================================================
@Schema(description = "Datos para registrar una serie de un ejercicio")
public class SerieRealizadaCreateDTO implements Serializable {

    @NotNull(message = "El número de serie es obligatorio")
    @Min(value = 1, message = "La primera serie es la 1")
    @Max(value = 20, message = "Más de 20 series en un ejercicio no es creíble")
    @Schema(description = "Orden dentro del ejercicio, empezando en 1", example = "3")
    private Integer numero;

    @NotNull(message = "Las repeticiones son obligatorias")
    @Min(value = 0, message = "Las repeticiones no pueden ser negativas")
    @Max(value = 100, message = "Más de 100 repeticiones en una serie no es creíble")
    @Schema(description = "Repeticiones realmente hechas", example = "8")
    private Integer repeticiones;

    @DecimalMin(value = "0.0", message = "El peso no puede ser negativo")
    @DecimalMax(value = "500.0", message = "Más de 500 kg no es creíble")
    @Schema(description = "Peso usado; nulo o cero si es peso corporal", example = "70.00")
    private BigDecimal peso;

    @Schema(description = "Si se marcó como completada; por defecto sí", example = "true")
    private Boolean completada;
}
