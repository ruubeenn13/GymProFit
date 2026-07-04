package com.gymprofit.api.service.auth;

import com.gymprofit.api.repository.jpa.IRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
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

    // Se ejecuta cada día a las 04:00 (hora del servidor). Borra los refresh tokens
    // ya revocados o cuya fecha de expiración es anterior a ahora. Loguea el nº borrado.
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void purgarTokensInutiles() {
        int borrados = refreshTokenRepository.borrarRevocadosOExpirados(LocalDateTime.now());
        if (borrados > 0) {
            logger.info("Purga de refresh_tokens: {} tokens revocados/expirados eliminados", borrados);
        }
    }
}
