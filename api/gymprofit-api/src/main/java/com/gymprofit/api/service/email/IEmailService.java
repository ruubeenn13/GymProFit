package com.gymprofit.api.service.email;

import com.gymprofit.api.entity.Usuario;

// ============================================================
// IEmailService — contrato de los correos transaccionales de GymProFit
// ============================================================
public interface IEmailService {

    /**
     * Envía al usuario el código de un solo uso para restablecer su contraseña.
     *
     * @param usuario        destinatario.
     * @param codigo         los seis dígitos en claro.
     * @param minutosValidez minutos que el código seguirá sirviendo.
     */
    void enviarCodigoRecuperacion(Usuario usuario, String codigo, int minutosValidez);
}
