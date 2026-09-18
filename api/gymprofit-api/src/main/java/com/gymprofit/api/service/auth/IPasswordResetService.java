package com.gymprofit.api.service.auth;

import com.gymprofit.api.dto.auth.ResetPasswordDTO;

// ============================================================
// IPasswordResetService — contrato de la recuperación de contraseña por código
// ============================================================
public interface IPasswordResetService {

    /**
     * Emite un código de un solo uso y lo envía al correo de la cuenta.
     * No falla ni informa cuando la cuenta no existe: la respuesta es siempre la misma.
     *
     * @param identificador nombre de usuario o correo.
     */
    void solicitarCodigo(String identificador);

    /**
     * Canjea el código por una contraseña nueva y cierra todas las sesiones abiertas.
     *
     * @param dto identificador, código de seis dígitos y contraseña nueva.
     */
    void restablecer(ResetPasswordDTO dto);
}
