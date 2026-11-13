package com.gymprofit.api.service.auth;

import com.gymprofit.api.config.ShedLockConfig;
import com.gymprofit.api.repository.jpa.IPasswordResetCodigoRepository;
import com.gymprofit.api.repository.jpa.IVerificacionCorreoCodigoRepository;
import com.gymprofit.api.repository.jpa.IRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// ============================================================
// RefreshTokenCleanupTask — purga periódica de refresh tokens inútiles
// La tabla refresh_tokens crece sin límite: cada login/rotación revoca el
// token usado (revocado=true) pero nunca lo borra, y los caducados quedan
// muertos. Esta tarea programada borra los revocados o expirados a diario
// para que la tabla no crezca indefinidamente. Requiere @EnableScheduling.
// ============================================================
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenCleanupTask.class);

    private final IRefreshTokenRepository refreshTokenRepository;
    // Los códigos de recuperación tienen el mismo problema: se marcan como usados o
    // caducan, pero nadie los borraba. Se purgan en la misma pasada.
    private final IPasswordResetCodigoRepository passwordResetCodigoRepository;
    // Y los de verificación del correo (GP-045), por lo mismo.
    private final IVerificacionCorreoCodigoRepository verificacionCorreoCodigoRepository;

    // Se ejecuta cada día a las 04:00 (hora del servidor). Borra los refresh tokens
    // ya revocados o cuya fecha de expiración es anterior a ahora. Loguea el nº borrado.
    //
    // Bloqueada con ShedLock (GP-221): con dos instancias, una sola purga al día.
    // lockAtMostFor = 10 min. Son dos DELETE por fecha: con 300 tokens caducados tardan 26 ms
    // en local (informe de GP-221); aunque las tablas crecieran mil veces seguirían lejos del
    // tope. Si la instancia muere con el bloqueo cogido, la purga la retoma al día siguiente.
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(name = "purga-refresh-tokens", lockAtMostFor = "PT10M",
            lockAtLeastFor = ShedLockConfig.BLOQUEO_MINIMO_PROPIEDAD)
    @Transactional
    public void purgarTokensInutiles() {
        LocalDateTime ahora = LocalDateTime.now();

        int borrados = refreshTokenRepository.borrarRevocadosOExpirados(ahora);
        if (borrados > 0) {
            logger.info("Purga de refresh_tokens: {} tokens revocados/expirados eliminados", borrados);
        }

        int codigos = passwordResetCodigoRepository.borrarUsadosOExpirados(ahora);
        if (codigos > 0) {
            logger.info("Purga de password_reset_codigos: {} códigos usados/expirados eliminados", codigos);
        }

        int verificaciones = verificacionCorreoCodigoRepository.borrarUsadosOExpirados(ahora);
        if (verificaciones > 0) {
            logger.info("Purga de verificacion_correo_codigos: {} códigos usados/expirados eliminados", verificaciones);
        }
    }
}
