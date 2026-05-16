package com.gymprofit.api.dto.entity.usuario;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.gymprofit.api.enums.TipoObjetivo;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioUpdateDTO implements Serializable {
    @NotNull
    private Integer id;

    @Email
    @Size(max = 100)
    private String email;

    @Positive
    private Double peso;

    @Positive
    private Double altura;

    @Min(0) @Max(120)
    private Integer edad;

    private String nivelExperiencia;
    private TipoObjetivo objetivo;
    private Boolean activo;
}
