package com.gymprofit.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// ForgotPasswordDTO — petición de código para recuperar la contraseña
// Un único campo identificador, que admite tanto el nombre de usuario como el
// correo: quien ha olvidado su contraseña no tiene por qué recordar con cuál de
// los dos se registró, y obligarle a acertar es una barrera gratuita.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ForgotPasswordDTO implements Serializable {

    // Nombre de usuario o correo de la cuenta cuya contraseña se quiere recuperar.
    @NotBlank
    @Size(max = 100)
    private String identificador;
}
