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

    @NotNull(message = "{validacion.serie.numeroObligatorio}")
    @Min(value = 1, message = "{validacion.serie.primera}")
    @Max(value = 20, message = "{validacion.serie.maximo}")
    @Schema(description = "Orden dentro del ejercicio, empezando en 1", example = "3")
    private Integer numero;

    @NotNull(message = "{validacion.repeticiones.obligatorias}")
    @Min(value = 0, message = "{validacion.repeticiones.negativas}")
    @Max(value = 100, message = "{validacion.repeticiones.maximo}")
    @Schema(description = "Repeticiones realmente hechas", example = "8")
    private Integer repeticiones;

    @DecimalMin(value = "0.0", message = "{validacion.peso.negativo}")
    @DecimalMax(value = "500.0", message = "{validacion.peso.maximo}")
    @Schema(description = "Peso usado; nulo o cero si es peso corporal", example = "70.00")
    private BigDecimal peso;

    @Schema(description = "Si se marcó como completada; por defecto sí", example = "true")
    private Boolean completada;
}
