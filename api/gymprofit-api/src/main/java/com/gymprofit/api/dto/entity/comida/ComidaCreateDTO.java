package com.gymprofit.api.dto.entity.comida;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComidaCreateDTO implements Serializable {
    @NotNull
    private Integer usuarioId;

    private LocalDateTime fecha;

    @NotBlank
    private String tipoComida;

    private String notas;
}
