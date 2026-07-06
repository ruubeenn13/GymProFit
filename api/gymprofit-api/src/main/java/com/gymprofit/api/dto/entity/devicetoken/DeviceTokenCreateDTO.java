package com.gymprofit.api.dto.entity.devicetoken;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// DeviceTokenCreateDTO — datos para registrar el token FCM de un dispositivo.
// El usuario propietario NO viene en el body: se toma del JWT en el service
// (evita IDOR — un usuario no puede registrar tokens a nombre de otro).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceTokenCreateDTO implements Serializable {
    // Token FCM del dispositivo (obligatorio).
    @NotBlank
    private String token;

    // Plataforma del dispositivo (opcional; por defecto ANDROID).
    private String plataforma;

    // Idioma del usuario en el dispositivo (opcional; p.ej. "es", "en" o "en-US").
    // Si no se envía, el service asume español ("es").
    private String idioma;
}
