package com.gymprofit.api.service.usuario;

import com.gymprofit.api.dto.entity.usuario.UsuarioDTO;
import com.gymprofit.api.dto.usuario.CambiarEmailDTO;

// ============================================================
// ICambioEmailService — contrato del cambio de correo de la cuenta propia (GP-083)
// ============================================================
public interface ICambioEmailService {

    /**
     * Cambia el correo del usuario autenticado, con reautenticación por contraseña.
     *
     * @param dto correo nuevo y contraseña actual.
     * @return el perfil con el correo ya cambiado.
     */
    UsuarioDTO cambiarEmailPropio(CambiarEmailDTO dto);
}
