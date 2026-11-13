package com.gymprofit.api.dto.usuario;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// EstadoCorreoDTO — cómo está el correo de la cuenta propia (GP-045)
// Lo devuelve POST /usuarios/me/email/verificar. El mismo dato va en el perfil
// (UsuarioDTO.emailVerificado); aquí se añade cuándo.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EstadoCorreoDTO implements Serializable {

    // El correo de la cuenta.
    private String email;

    // Si está verificado.
    private boolean verificado;

    // Cuándo se verificó (ISO, reloj del servidor); null si no lo está.
    private String verificadoEn;
}
