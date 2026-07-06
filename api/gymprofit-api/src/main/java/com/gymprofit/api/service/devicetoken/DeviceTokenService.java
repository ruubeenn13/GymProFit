package com.gymprofit.api.service.devicetoken;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.devicetoken.DeviceTokenCreateDTO;
import com.gymprofit.api.entity.DeviceToken;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.repository.jpa.IDeviceTokenRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// ============================================================
// DeviceTokenService — registro/borrado de tokens FCM de dispositivo.
// El usuario propietario SIEMPRE se toma del JWT (SecurityUtils), nunca del body.
// El registro es un upsert idempotente por token: si el token ya existe (Firebase
// lo reasigna entre usuarios/reinstalaciones), se reasigna al usuario actual y se
// refresca la fecha; si no, se crea. Así el mismo dispositivo no duplica filas.
// ============================================================
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class DeviceTokenService implements IDeviceTokenService {

    private final IDeviceTokenRepository deviceTokenRepository;
    private final IUsuarioRepository usuarioRepository;
    private final SecurityUtils securityUtils;
    private final Logger logger = LoggerFactory.getLogger(DeviceTokenService.class);

    // Upsert del token FCM para el usuario autenticado.
    @Transactional
    @Override
    public void registrar(DeviceTokenCreateDTO dto) {
        Integer usuarioId = securityUtils.getCurrentUserId();
        logger.info("Registrando token FCM para usuario id: {}", usuarioId);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + usuarioId + " no existe"));

        // Plataforma por defecto ANDROID si no se indica.
        String plataforma = (dto.getPlataforma() == null || dto.getPlataforma().isBlank())
                ? "ANDROID" : dto.getPlataforma().toUpperCase();

        // Idioma normalizado: por defecto "es"; si viene una etiqueta tipo "en-US"
        // se conserva solo el código de idioma de 2 letras en minúsculas ("en").
        String idioma = normalizarIdioma(dto.getIdioma());

        LocalDateTime ahora = LocalDateTime.now();

        // Reutiliza la fila si el token ya existe (reasigna dueño); si no, crea una nueva.
        DeviceToken deviceToken = deviceTokenRepository.findByToken(dto.getToken())
                .orElseGet(() -> {
                    DeviceToken nuevo = new DeviceToken();
                    nuevo.setToken(dto.getToken());
                    nuevo.setFechaRegistro(ahora);
                    return nuevo;
                });
        deviceToken.setUsuario(usuario);
        deviceToken.setPlataforma(plataforma);
        // El idioma se refresca también al reasignar un token existente (cambio de
        // usuario o de idioma en el mismo dispositivo).
        deviceToken.setIdioma(idioma);
        deviceToken.setFechaActualizacion(ahora);

        deviceTokenRepository.save(deviceToken);
    }

    // Normaliza el idioma recibido del cliente: null/vacío → "es" (default);
    // en otro caso, minúsculas y solo el código de 2 letras (p.ej. "en-US" → "en").
    private String normalizarIdioma(String idioma) {
        if (idioma == null || idioma.isBlank()) return "es";
        String normalizado = idioma.trim().toLowerCase();
        // Recorta etiquetas BCP-47 tipo "en-us" o "en_us" al código base de 2 letras.
        if (normalizado.length() > 2) normalizado = normalizado.substring(0, 2);
        return normalizado;
    }

    // Borra el token en el logout, solo si es del usuario autenticado (evita borrar ajenos).
    @Transactional
    @Override
    public void eliminar(String token) {
        deviceTokenRepository.findByToken(token).ifPresent(deviceToken -> {
            securityUtils.checkOwnership(deviceToken.getUsuario().getId());
            deviceTokenRepository.deleteByToken(token);
            logger.info("Token FCM eliminado (logout) del usuario id: {}", deviceToken.getUsuario().getId());
        });
    }
}
