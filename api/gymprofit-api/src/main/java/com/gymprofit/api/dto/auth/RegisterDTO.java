package com.gymprofit.api.dto.auth;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.gymprofit.api.enums.TipoObjetivo;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// RegisterDTO — datos de entrada para el registro de un nuevo usuario
// Contiene las credenciales básicas (username, password, email) y los
// datos físicos/objetivos iniciales usados para dar de alta el perfil
// en el endpoint /auth/register de GymProFit.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDTO implements Serializable {
    // Nombre de usuario único, entre 3 y 50 caracteres
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    // Contraseña en texto plano recibida del cliente (se hashea en el servicio).
    // Política: mínimo 8 caracteres e incluir minúscula, mayúscula, dígito y símbolo.
    @NotBlank
    @Size(min = 8, max = 100)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
            message = "La contraseña debe incluir minúscula, mayúscula, dígito y símbolo")
    private String password;

    // Correo electrónico del usuario, debe tener formato válido
    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    // Peso corporal inicial (kg), opcional pero debe ser positivo si se indica
    @Positive
    private BigDecimal peso;

    // Altura inicial (cm/m), opcional pero debe ser positiva si se indica
    @Positive
    private BigDecimal altura;

    // Edad del usuario, entre 0 y 120 años
    @Min(0) @Max(120)
    private Integer edad;

    private String nivelExperiencia;
    private TipoObjetivo objetivo;
}
