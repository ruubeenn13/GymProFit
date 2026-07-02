package com.gymprofit.api.dto.entity.comida;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

// ============================================================
// ComidaCreateDTO — datos necesarios para registrar una nueva comida
// Se usa en la petición de alta de comida (nutrición). Referencia al usuario
// propietario, fecha/hora, tipo (desayuno, comida, cena, snack...) y notas libres.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComidaCreateDTO implements Serializable {
    // Id del usuario al que pertenece la comida (obligatorio)
    @NotNull
    private Integer usuarioId;

    // Fecha y hora en la que se registra la comida
    private LocalDateTime fecha;

    // Tipo de comida (ej: desayuno, almuerzo, cena)
    @NotBlank
    private String tipoComida;

    // Notas u observaciones opcionales
    private String notas;
}
