package com.gymprofit.api.service.usuario;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.usuario.EstadoCorreoDTO;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.entity.VerificacionCorreoCodigo;
import com.gymprofit.api.exceptions.ConflictEntityException;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.repository.jpa.IVerificacionCorreoCodigoRepository;
import com.gymprofit.api.service.auth.CodigoUnSoloUso;
import com.gymprofit.api.service.email.IEmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// ============================================================
// VerificacionCorreoService — verificar el correo con un código (GP-045, DEC-066)
//
// El mismo mecanismo que la recuperación de contraseña (PasswordResetService), con sus
// mismas reglas en CodigoUnSoloUso: seis dígitos aleatorios, solo el hash en la base,
// 15 minutos, un solo uso, cinco intentos y el código nuevo mata al anterior. Dos
// diferencias, las dos porque aquí hay sesión:
//
//  · La cuenta sale del token, nunca del cuerpo (DEC-013). No hay identificador que
//    buscar ni cuenta inexistente que disimular.
//  · El código está atado a la dirección a la que se mandó. Verificar es decir «quien
//    tiene esta cuenta lee ESTE buzón», así que un código de la dirección vieja no
//    puede verificar la nueva.
//
// Lo que no hace, a propósito: bloquear nada. Una cuenta sin verificar se usa entera
// (DEC-066); quien necesite un correo comprobado mira Usuario.emailVerificado.
// ============================================================
@Service
@RequiredArgsConstructor
public class VerificacionCorreoService implements IVerificacionCorreoService {

    private static final Logger logger = LoggerFactory.getLogger(VerificacionCorreoService.class);

    private final IUsuarioRepository usuarioRepository;
    private final IVerificacionCorreoCodigoRepository codigoRepository;
    private final IEmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;

    /**
     * Emite un código nuevo para el correo actual de la cuenta y lo manda.
     * <p>
     * Mata antes los códigos vivos: si no, cada «Reenviar» dejaría otra llave válida.
     * El envío es asíncrono y además se protege aquí: un fallo al encolarlo se registra
     * y no sube, porque quien emite (el alta, el cambio de correo) ya ha hecho lo suyo y
     * el código siempre se puede pedir otra vez.
     *
     * @param usuario cuenta ya guardada, con el correo al que se manda.
     */
    @Override
    @Transactional
    public void emitirCodigo(Usuario usuario) {
        codigoRepository.invalidarTodosDeUsuario(usuario.getId());

        String codigo = CodigoUnSoloUso.generar();
        LocalDateTime ahora = LocalDateTime.now();

        VerificacionCorreoCodigo registro = new VerificacionCorreoCodigo();
        registro.setUsuario(usuario);
        registro.setEmail(usuario.getEmail());
        registro.setCodigoHash(passwordEncoder.encode(codigo));
        registro.setFechaCreacion(ahora);
        registro.setFechaExpiracion(ahora.plusMinutes(CodigoUnSoloUso.MINUTOS_VALIDEZ));
        registro.setUsado(false);
        registro.setIntentos(0);
        codigoRepository.save(registro);

        try {
            // El código va ya generado: la tarea puede arrancar antes de que esta
            // transacción confirme, y no lo encontraría en la tabla.
            emailService.enviarCodigoVerificacion(usuario, codigo, CodigoUnSoloUso.MINUTOS_VALIDEZ);
        } catch (RuntimeException e) {
            logger.error("No se pudo encolar el código de verificación del correo, usuario id={}",
                    usuario.getId(), e);
            return;
        }
        logger.info("Código de verificación del correo emitido para el usuario id={}", usuario.getId());
    }

    /**
     * Manda otro código a la cuenta del token.
     *
     * @throws ConflictEntityException (→ 409) si el correo ya está verificado: no hay nada
     *                                 que verificar y un correo de más sería ruido.
     */
    @Override
    @Transactional
    public void reenviar() {
        Usuario usuario = cuentaDelToken();
        if (usuario.isEmailVerificado()) {
            throw new ConflictEntityException("error.correo.yaVerificado");
        }
        emitirCodigo(usuario);
    }

    /**
     * Canjea el código de la cuenta del token.
     * <p>
     * Todos los fallos —sin código vivo, caducado, equivocado, de otra dirección o con
     * los intentos agotados— dan el mismo error: distinguirlos diría por dónde seguir.
     * Una cuenta ya verificada devuelve su estado sin gastar nada, para que un doble
     * toque en la app no acabe en error.
     *
     * @throws InvalidDataException (→ 400) si el código no sirve.
     */
    @Override
    @Transactional
    public EstadoCorreoDTO verificar(String codigo) {
        Usuario usuario = cuentaDelToken();
        if (usuario.isEmailVerificado()) {
            return estado(usuario);
        }

        VerificacionCorreoCodigo registro = codigoRepository
                .findFirstByUsuarioIdAndUsadoFalseAndFechaExpiracionAfterOrderByIdDesc(
                        usuario.getId(), LocalDateTime.now())
                .orElseThrow(VerificacionCorreoService::codigoNoValido);

        if (registro.getIntentos() >= CodigoUnSoloUso.MAX_INTENTOS) {
            registro.setUsado(true);
            codigoRepository.save(registro);
            logger.warn("Código de verificación quemado por exceso de intentos, usuario id={}", usuario.getId());
            throw codigoNoValido();
        }

        // Un código de otra dirección muere sin contar como intento: no lo ha tecleado mal
        // nadie, es que el correo de la cuenta ya no es aquel.
        if (!registro.getEmail().equalsIgnoreCase(usuario.getEmail())) {
            registro.setUsado(true);
            codigoRepository.save(registro);
            logger.warn("Código de verificación de una dirección anterior, usuario id={}", usuario.getId());
            throw codigoNoValido();
        }

        if (!passwordEncoder.matches(codigo, registro.getCodigoHash())) {
            registro.setIntentos(registro.getIntentos() + 1);
            codigoRepository.save(registro);
            logger.warn("Código de verificación incorrecto ({} de {}), usuario id={}",
                    registro.getIntentos(), CodigoUnSoloUso.MAX_INTENTOS, usuario.getId());
            throw codigoNoValido();
        }

        usuario.setEmailVerificado(true);
        usuario.setEmailVerificadoEn(LocalDateTime.now());
        usuarioRepository.save(usuario);

        registro.setUsado(true);
        codigoRepository.save(registro);
        codigoRepository.invalidarTodosDeUsuario(usuario.getId());

        logger.info("Correo verificado para el usuario id={}", usuario.getId());
        return estado(usuario);
    }

    private Usuario cuentaDelToken() {
        Integer id = securityUtils.getCurrentUserId();
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("error.usuario.noExiste", id));
    }

    private static EstadoCorreoDTO estado(Usuario usuario) {
        return new EstadoCorreoDTO(usuario.getEmail(), usuario.isEmailVerificado(),
                usuario.getEmailVerificadoEn() == null ? null : usuario.getEmailVerificadoEn().toString());
    }

    // El mismo mensaje que el canje del código de recuperación.
    private static InvalidDataException codigoNoValido() {
        return new InvalidDataException("error.codigo.invalido");
    }
}
