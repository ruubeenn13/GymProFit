package com.gymprofit.api.dto.entity.logro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// LogroCreateDTO — DTO para la creación de un logro (achievement)
// Recoge los datos necesarios para dar de alta un nuevo logro
// desbloqueable por los usuarios de GymProFit.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogroCreateDTO implements Serializable {
    // Nombre del logro, obligatorio y máximo 100 caracteres
    @NotBlank
    @Size(max = 100)
    private String nombre;

    // Descripción del logro (opcional)
    private String descripcion;

    // Tipo de logro (categoría), obligatorio
    @NotBlank
    private String tipo;
}
