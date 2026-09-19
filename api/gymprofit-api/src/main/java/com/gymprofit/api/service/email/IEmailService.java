package com.gymprofit.api.service.email;

import com.gymprofit.api.entity.Usuario;

// ============================================================
// IEmailService — contrato de los correos transaccionales de GymProFit
// ============================================================
public interface IEmailService {

    /**
     * Envía al usuario el código de un solo uso para restablecer su contraseña.
     * <p>
     * <strong>Vuelve antes de haber enviado nada.</strong> La entrega ocurre en otro hilo
     * (ver la implementación), así que quien llama no sabe si salió y no debe intentar
     * averiguarlo: un fallo se registra allí y no llega hasta aquí. Todo lo que el envío
     * necesita viaja en los parámetros, porque la tarea puede arrancar antes de que
     * confirme la transacción de quien llama.
     *
     * @param usuario        destinatario, ya cargado.
     * @param codigo         los seis dígitos en claro.
     * @param minutosValidez minutos que el código seguirá sirviendo.
     */
    void enviarCodigoRecuperacion(Usuario usuario, String codigo, int minutosValidez);
}
