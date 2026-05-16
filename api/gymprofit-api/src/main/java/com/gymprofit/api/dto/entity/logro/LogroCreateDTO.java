package com.gymprofit.api.dto.entity.logro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogroCreateDTO implements Serializable {
    @NotBlank
    @Size(max = 100)
    private String nombre;

    private String descripcion;

    @NotBlank
    private String tipo;
}
