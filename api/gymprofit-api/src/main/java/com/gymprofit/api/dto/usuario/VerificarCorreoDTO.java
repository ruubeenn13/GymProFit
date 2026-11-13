package com.gymprofit.api.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

// ============================================================
// VerificarCorreoDTO — el código que llegó al correo (GP-045)
// Solo el código: la cuenta sale del token, nunca del cuerpo (DEC-013).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerificarCorreoDTO implements Serializable {

    // Los seis dígitos. Mientras vive, el código es una prueba de control del correo:
    // fuera del toString, como la contraseña.
    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "{validacion.codigo.seisDigitos}")
    @ToString.Exclude
    private String codigo;
}
