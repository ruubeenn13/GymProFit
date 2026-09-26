package com.gymprofit.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// ChangePasswordDTO — datos de entrada para el cambio de contraseña
// Requiere la contraseña actual (se verifica contra el hash almacenado)
// y la nueva contraseña. Usado por el endpoint autenticado
// /auth/change-password. Base para futuros flujos de seguridad
// (2FA, confirmación por código de email, etc.).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordDTO implements Serializable {
    // Contraseña actual en texto plano; se valida contra el hash guardado antes de permitir el cambio.
    @NotBlank
    private String currentPassword;

    // Contraseña en texto plano (se hashea en el servicio). Política de GP-101 (DEC-034):
    // mínimo 8 caracteres, máximo 72 bytes, sin reglas de composición. La lista de
    // bloqueo y el nombre los comprueba PoliticaContrasena en el servicio.
    @NotBlank
    @ContrasenaNueva
    private String newPassword;
}
