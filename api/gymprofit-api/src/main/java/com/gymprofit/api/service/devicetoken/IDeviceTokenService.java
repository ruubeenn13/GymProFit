package com.gymprofit.api.service.devicetoken;

import com.gymprofit.api.dto.entity.devicetoken.DeviceTokenCreateDTO;

// ============================================================
// IDeviceTokenService — contrato del servicio de tokens FCM de dispositivo.
// ============================================================
public interface IDeviceTokenService {

    // Registra (o actualiza) el token FCM del dispositivo para el usuario autenticado.
    void registrar(DeviceTokenCreateDTO deviceTokenCreateDTO);

    // Elimina el token indicado (logout), solo si pertenece al usuario autenticado.
    void eliminar(String token);
}
