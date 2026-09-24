package com.gymprofit.api.service.sesionentrenamiento;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionCompletaCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

// ============================================================
// GuardadoSesionCompletaService — fachada del guardado completo de una sesión (GP-076)
//
// SIN @Transactional, y es lo que la hace servir. Cuando dos reintentos con la misma
// clave de idempotencia pasan a la vez la comprobación previa, los dos insertan; el
// índice único deja entrar a uno y al otro le lanza DataIntegrityViolationException.
//
// Esa recuperación vivía antes DENTRO de la transacción del guardado, y no podía
// funcionar: la excepción sale de save(), que participa en la transacción, así que
// esta queda marcada rollback-only y al confirmar salta UnexpectedRollbackException;
// y con REPEATABLE READ la relectura usa la instantánea de la comprobación previa y
// no ve la fila de la ganadora. La perdedora recibía un 500.
//
// Aquí la transacción que ha fallado ya se ha revertido entera al llegar al catch, y
// la relectura abre una transacción NUEVA de solo lectura, con instantánea nueva, en
// la que la sesión ganadora ya está confirmada. Lo demuestra
// GuardadoSesionCompletaCarreraTest.
// ============================================================
@Service
@RequiredArgsConstructor
public class GuardadoSesionCompletaService implements IGuardadoSesionCompletaService {

    private static final Logger logger = LoggerFactory.getLogger(GuardadoSesionCompletaService.class);

    private final ISesionEntrenamientoService sesionEntrenamientoService;
    private final SecurityUtils securityUtils;

    /**
     * Guarda la sesión completa y, si pierde la carrera contra otro intento con la misma
     * clave, devuelve la sesión que guardó el otro.
     *
     * @param dto la sesión completa, con su clave de idempotencia.
     * @return la sesión creada, o la que ya existía para esa clave.
     * @throws DataIntegrityViolationException si el choque no es el de la clave: sin
     *         sesión ganadora que devolver, el error sigue su curso.
     */
    @Override
    public SesionEntrenamientoDTO guardar(SesionCompletaCreateDTO dto) {
        try {
            return sesionEntrenamientoService.guardarCompleta(dto);
        } catch (DataIntegrityViolationException e) {
            Integer usuarioId = securityUtils.getCurrentUserId();
            logger.info("Guardado de sesión con clave {} chocó contra otro intento; se relee la ganadora",
                    dto.getClaveIdempotencia());
            return sesionEntrenamientoService
                    .buscarPorClaveIdempotencia(usuarioId, dto.getClaveIdempotencia())
                    .orElseThrow(() -> e);
        }
    }
}
