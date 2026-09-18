package com.gymprofit.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// ResetPasswordDTO — canje del código de recuperación por una contraseña nueva
// Lleva el identificador de la cuenta, los seis dígitos que llegaron al correo
// y la contraseña nueva, sujeta a la misma política que el registro y el cambio
// de contraseña: si aquí fuera más laxa, recuperar la cuenta sería el atajo para
// saltarse la política.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordDTO implements Serializable {

    // Nombre de usuario o correo, el mismo con el que se pidió el código.
    @NotBlank
    @Size(max = 100)
    private String identificador;

    // Los seis dígitos recibidos por correo.
    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "El código debe tener 6 dígitos")
    private String codigo;

    // Contraseña nueva en texto plano (se hashea en el servicio).
    // Política: mínimo 8 caracteres e incluir minúscula, mayúscula, dígito y símbolo.
    @NotBlank
    @Size(min = 8, max = 100)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
            message = "La contraseña debe incluir minúscula, mayúscula, dígito y símbolo")
    private String newPassword;
}
