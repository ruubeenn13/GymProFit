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
        deviceToken.setFechaActualizacion(ahora);

        deviceTokenRepository.save(deviceToken);
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
