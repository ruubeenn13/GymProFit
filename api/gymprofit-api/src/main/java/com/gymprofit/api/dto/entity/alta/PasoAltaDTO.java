package com.gymprofit.api.dto.entity.alta;

import com.gymprofit.api.enums.PasoAlta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * El aviso de que alguien ha llegado a un paso del alta (GP-019). Es todo lo que se
 * manda: el nombre del paso. Cualquier otro campo del cuerpo se ignora y no se guarda.
 *
 * @param paso la pantalla a la que se ha llegado
 */
@Schema(description = "Paso del alta al que se ha llegado. No lleva nada más.")
public record PasoAltaDTO(@NotNull PasoAlta paso) {
}
