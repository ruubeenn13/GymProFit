package com.gymprofit.api.service.auth;

import com.gymprofit.api.dto.auth.ResetPasswordDTO;
import com.gymprofit.api.entity.PasswordResetCodigo;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.repository.jpa.IPasswordResetCodigoRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.email.IEmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

// ============================================================
// PasswordResetService — recuperación de contraseña por código de un solo uso
//
// Dos pasos: se pide un código con el usuario o el correo, llega por email, y se
// canjea junto con la contraseña nueva. Entre medias, tres reglas que no son
// opcionales:
//
//  · El CUERPO de la respuesta al pedir un código es siempre el mismo, exista la
//    cuenta o no. Lo que no es igual es el COSTE: si la cuenta no existe se vuelve
//    pronto y no se paga ni el hash del código ni el encolado del correo, así que
//    el tiempo de respuesta sí distingue los dos casos. No se compensa con trabajo
//    falso, y el motivo está en DEC-028: POST /auth/register ya dice en claro «el
//    username X ya está en uso» y «el email X ya está en uso», de modo que quemar
//    CPU en cada intento fallido aquí compraría una propiedad que el registro
//    regala igualmente.
//  · El código se guarda hasheado y caduca pronto. Mientras vive es, de hecho, una
//    contraseña alternativa esperando en una bandeja de entrada.
//  · Los intentos se cuentan. Seis dígitos sin límite de intentos se agotan a
//    fuerza bruta en un rato.
// ============================================================
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetService implements IPasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    // Vida del código. Corta a propósito: lo suficiente para ir al correo y volver.
    private static final int MINUTOS_VALIDEZ = 15;

    // Intentos fallidos antes de quemar el código y obligar a pedir otro.
    private static final int MAX_INTENTOS = 5;

    // SecureRandom y no Random: de esto depende el acceso a una cuenta.
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final IUsuarioRepository usuarioRepository;
    private final IPasswordResetCodigoRepository codigoRepository;
    private final RefreshTokenService refreshTokenService;
    private final IEmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Emite un código de recuperación y lo manda al correo de la cuenta.
     * <p>
     * No devuelve nada ni lanza nada cuando la cuenta no existe, está desactivada o no
     * tiene correo: el controlador responde lo mismo en los cuatro casos. Ese cuerpo
     * uniforme es deliberado y se mantiene.
     * <p>
     * <strong>Lo que ese cuerpo uniforme NO consigue es ocultar qué cuentas existen.</strong>
     * Las ramas que vuelven pronto se saltan el hash del código y el encolado del correo,
     * y esa diferencia de tiempo se mide desde fuera. Queda así a propósito: la enumeración
     * de cuentas ya está abierta por {@code POST /auth/register}, que responde en texto
     * plano «el username X ya está en uso» y «el email X ya está en uso», y comprar aquí
     * una propiedad que allí se regala solo costaría CPU en cada intento fallido. Ver
     * DEC-028 para las condiciones en las que esto se revisa.
     *
     * @param identificador nombre de usuario o correo tecleado por quien pide el código.
     */
    @Override
    @Transactional
    public void solicitarCodigo(String identificador) {
        Optional<Usuario> encontrado = buscarPorIdentificador(identificador);

        if (encontrado.isEmpty()) {
            logger.info("Recuperación de contraseña pedida para un identificador inexistente");
            return;
        }

        Usuario usuario = encontrado.get();

        // Una cuenta desactivada no se recupera por correo: eso lo decide un administrador.
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            logger.info("Recuperación de contraseña pedida para la cuenta desactivada id={}", usuario.getId());
            return;
        }

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            logger.warn("La cuenta id={} no tiene correo: no hay dónde mandar el código", usuario.getId());
            return;
        }

        // Pedir un código nuevo mata el anterior. Si no, cada solicitud dejaría otra
        // llave viva y el usuario acabaría con varias válidas a la vez.
        codigoRepository.invalidarTodosDeUsuario(usuario.getId());

        String codigo = generarCodigo();

        PasswordResetCodigo registro = new PasswordResetCodigo();
        registro.setUsuario(usuario);
        registro.setCodigoHash(passwordEncoder.encode(codigo));
        registro.setFechaCreacion(LocalDateTime.now());
        registro.setFechaExpiracion(LocalDateTime.now().plusMinutes(MINUTOS_VALIDEZ));
        registro.setUsado(false);
        registro.setIntentos(0);
        codigoRepository.save(registro);

        // Vuelve enseguida: la entrega ocurre en el pool de correo, no en este hilo. El
        // código se le pasa ya generado y NO se relee de la tabla, porque esta transacción
        // puede no haber confirmado todavía cuando la tarea arranque. Si el envío falla,
        // el fallo queda en el log del otro hilo: aquí ya no hay a quién contárselo.
        emailService.enviarCodigoRecuperacion(usuario, codigo, MINUTOS_VALIDEZ);
        logger.info("Código de recuperación emitido para el usuario id={}", usuario.getId());
    }

    /**
     * Canjea el código por una contraseña nueva.
     * <p>
     * Todos los fallos —cuenta inexistente, código caducado, código equivocado, intentos
     * agotados— devuelven el mismo error, por el mismo motivo que arriba: distinguirlos
     * le diría a quien prueba por dónde seguir probando.
     * <p>
     * Al terminar se revocan todas las sesiones abiertas: si alguien había entrado con la
     * contraseña vieja, cambiarla tiene que echarle.
     *
     * @param dto identificador, código y contraseña nueva.
     * @throws InvalidDataException (→ 400) si el código no sirve.
     */
    @Override
    @Transactional
    public void restablecer(ResetPasswordDTO dto) {
        Usuario usuario = buscarPorIdentificador(dto.getIdentificador())
                .orElseThrow(PasswordResetService::codigoNoValido);

        PasswordResetCodigo registro = codigoRepository
                .findFirstByUsuarioIdAndUsadoFalseAndFechaExpiracionAfterOrderByIdDesc(
                        usuario.getId(), LocalDateTime.now())
                .orElseThrow(PasswordResetService::codigoNoValido);

        if (registro.getIntentos() >= MAX_INTENTOS) {
            registro.setUsado(true);
            codigoRepository.save(registro);
            logger.warn("Código de recuperación quemado por exceso de intentos, usuario id={}", usuario.getId());
            throw codigoNoValido();
        }

        if (!passwordEncoder.matches(dto.getCodigo(), registro.getCodigoHash())) {
            registro.setIntentos(registro.getIntentos() + 1);
            codigoRepository.save(registro);
            logger.warn("Código de recuperación incorrecto ({} de {}), usuario id={}",
                    registro.getIntentos(), MAX_INTENTOS, usuario.getId());
            throw codigoNoValido();
        }

        usuario.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        usuarioRepository.save(usuario);

        // El código consumido y cualquier otro que quedara suelto dejan de valer.
        registro.setUsado(true);
        codigoRepository.save(registro);
        codigoRepository.invalidarTodosDeUsuario(usuario.getId());

        refreshTokenService.revocarTodosDeUsuario(usuario);

        logger.info("Contraseña restablecida por código para el usuario id={}", usuario.getId());
    }

    // Admite indistintamente el nombre de usuario o el correo.
    private Optional<Usuario> buscarPorIdentificador(String identificador) {
        if (identificador == null || identificador.isBlank()) return Optional.empty();

        String limpio = identificador.trim();
        Optional<Usuario> porUsername = usuarioRepository.findByUsername(limpio);

        return porUsername.isPresent() ? porUsername : usuarioRepository.findByEmail(limpio);
    }

    // Seis dígitos con ceros a la izquierda incluidos: 000123 es tan válido como 987654.
    private String generarCodigo() {
        return String.format("%06d", ALEATORIO.nextInt(1_000_000));
    }

    // Un único mensaje para todos los fallos de canje, a propósito.
    private static InvalidDataException codigoNoValido() {
        return new InvalidDataException("El código no es válido o ha caducado");
    }
}
