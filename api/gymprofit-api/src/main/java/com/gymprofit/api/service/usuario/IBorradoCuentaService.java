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

    /**
     * Borra de forma definitiva la cuenta indicada y todo lo suyo, sin más comprobaciones.
     * <p>
     * Es el mismo borrado que {@link #eliminarCuentaPropia}; quien llama responde de
     * haber comprobado antes que se puede (la contraseña del titular, o la confirmación
     * del administrador en el borrado a petición, GP-085).
     *
     * @param usuarioId cuenta que se borra.
     */
    void borrarCuenta(Integer usuarioId);
}
