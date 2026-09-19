package com.gymprofit.api.service.usuario;

import com.gymprofit.api.dto.usuario.EliminarCuentaDTO;

// ============================================================
// IBorradoCuentaService — contrato del borrado de cuenta
// ============================================================
public interface IBorradoCuentaService {

    /**
     * Borra de forma definitiva la cuenta del usuario autenticado y todo lo suyo.
     * <p>
     * Exige la contraseña actual. No hay periodo de gracia ni copia anonimizada: lo que
     * se promete en gymprofit.app/eliminar-cuenta es que es irreversible.
     *
     * @param dto contraseña actual, para reautenticar.
     */
    void eliminarCuentaPropia(EliminarCuentaDTO dto);
}
