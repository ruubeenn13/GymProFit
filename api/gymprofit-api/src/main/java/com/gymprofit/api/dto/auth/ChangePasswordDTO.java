package com.gymprofit.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    // Nueva contraseña en texto plano (se hashea en el servicio). Mismo mínimo que el registro.
    @NotBlank
    @Size(min = 6, max = 100)
    private String newPassword;
}
