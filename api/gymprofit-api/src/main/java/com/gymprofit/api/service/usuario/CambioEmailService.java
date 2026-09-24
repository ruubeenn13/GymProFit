package com.gymprofit.api.service.usuario;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.usuario.UsuarioDTO;
import com.gymprofit.api.dto.usuario.CambiarEmailDTO;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.ConflictEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.UnauthorizedException;
import com.gymprofit.api.mappers.UsuarioMapper;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ============================================================
// CambioEmailService — cambio del correo de la cuenta propia (GP-083)
//
// Antes el correo se cambiaba por PATCH /usuarios/{id}, sin validar el formato, sin
// mirar si lo usaba otra cuenta (lo paraba la restricción única, como un error
// genérico) y sin pedir la contraseña. Ahora es una ruta propia, sin id: el usuario
// sale del token y tiene que reautenticarse, como en el borrado de cuenta.
//
// Servicio aparte de UsuarioService por la misma razón que BorradoCuentaService:
// UsuarioService es el UserDetailsService de SecurityConfig, que define el
// PasswordEncoder, y pedírselo desde ahí cerraría un ciclo de dependencias.
//
// PENDIENTE (GP-045): el correo nuevo no se verifica. Se guarda tal cual; confirmar
// que quien lo pide lo controla es trabajo de GP-045 y no se hace aquí.
// ============================================================
@Service
@RequiredArgsConstructor
public class CambioEmailService implements ICambioEmailService {

    private static final Logger logger = LoggerFactory.getLogger(CambioEmailService.class);

    private final IUsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cambia el correo del usuario autenticado.
     * <p>
     * La contraseña se comprueba ANTES que la disponibilidad del correo: así, quien no
     * la sabe no puede usar esta ruta para averiguar qué correos están registrados.
     * Pedir el correo que ya se tiene no es un error: se devuelve el perfil sin tocarlo.
     *
     * @throws UnauthorizedException (→ 403) si la contraseña no es la de la cuenta.
     * @throws ConflictEntityException (→ 409) si otra cuenta ya usa ese correo.
     */
    @Override
    @Transactional
    public UsuarioDTO cambiarEmailPropio(CambiarEmailDTO dto) {
        Integer usuarioId = securityUtils.getCurrentUserId();

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + usuarioId + " no existe"));

        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPassword())) {
            logger.warn("Intento de cambio de correo con contraseña incorrecta, usuario id={}", usuarioId);
            throw new UnauthorizedException("La contraseña no es correcta");
        }

        String nuevo = dto.getEmail().trim();
        if (nuevo.equalsIgnoreCase(usuario.getEmail())) {
            return usuarioMapper.toDTO(usuario);
        }

        if (usuarioRepository.existsByEmailAndIdNot(nuevo, usuarioId)) {
            throw new ConflictEntityException("Ese correo ya lo usa otra cuenta");
        }

        usuario.setEmail(nuevo);
        try {
            // saveAndFlush para que el choque con la restricción única, si otra petición se
            // lleva el correo entre la comprobación y aquí, salte dentro de este método y
            // no al confirmar. La excepción no se atrapa para seguir en la transacción:
            // se traduce y se relanza, así que la transacción se revierte entera.
            // No hay test concurrente de esa carrera. Lo que sí se vio al validar GP-083:
            // quitando la comprobación de arriba, el 409 lo sigue dando este catch.
            usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictEntityException("Ese correo ya lo usa otra cuenta");
        }

        logger.info("Correo cambiado para el usuario id={}", usuarioId);
        return usuarioMapper.toDTO(usuario);
    }
}
