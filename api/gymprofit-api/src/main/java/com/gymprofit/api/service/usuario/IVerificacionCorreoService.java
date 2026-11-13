package com.gymprofit.api.service.usuario;

import com.gymprofit.api.dto.usuario.EstadoCorreoDTO;
import com.gymprofit.api.entity.Usuario;

// ============================================================
// IVerificacionCorreoService — verificar el correo de la cuenta (GP-045, DEC-066)
// ============================================================
public interface IVerificacionCorreoService {

    /**
     * Emite un código nuevo para el correo actual de la cuenta y lo manda por correo.
     * Mata los códigos vivos anteriores. Un fallo del envío se registra y no se propaga.
     *
     * @param usuario cuenta ya guardada, con el correo al que se manda el código.
     */
    void emitirCodigo(Usuario usuario);

    /**
     * Manda otro código a la cuenta del token.
     *
     * @throws com.gymprofit.api.exceptions.ConflictEntityException (→ 409) si ya está verificada.
     */
    void reenviar();

    /**
     * Canjea el código de la cuenta del token. Si ya estaba verificada, devuelve su
     * estado sin mirar el código.
     *
     * @param codigo los seis dígitos.
     * @return el estado del correo tras canjearlo.
     * @throws com.gymprofit.api.exceptions.InvalidDataException (→ 400) si el código no sirve.
     */
    EstadoCorreoDTO verificar(String codigo);
}
