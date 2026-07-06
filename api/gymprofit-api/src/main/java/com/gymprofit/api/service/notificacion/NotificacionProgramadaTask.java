package com.gymprofit.api.service.notificacion;

import com.gymprofit.api.entity.Notificacion;
import com.gymprofit.api.repository.jpa.INotificacionRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// NotificacionProgramadaTask — job que envía las notificaciones push programadas.
// Cada minuto busca notificaciones con fecha_programada vencida y push_enviada=false,
// les envía la push (PushNotificationService, tolerante a fallos) y las marca como
// enviadas para no duplicar. Requiere @EnableScheduling (ya activo por la tarea de
// purga de refresh tokens). En Render free el keep-alive mantiene el proceso despierto,
// así que el retraso máximo real es ~1 minuto sobre la hora programada.
// ============================================================
@Component
@AllArgsConstructor
public class NotificacionProgramadaTask {

    private final INotificacionRepository notificacionRepository;
    private final PushNotificationService pushNotificationService;
    private final Logger logger = LoggerFactory.getLogger(NotificacionProgramadaTask.class);

    // Barrido cada 60s (arranca a los 30s del boot para no competir con el arranque).
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    @Transactional
    public void enviarProgramadasVencidas() {
        List<Notificacion> pendientes =
                notificacionRepository.findByPushEnviadaFalseAndFechaProgramadaLessThanEqual(LocalDateTime.now());

        if (pendientes.isEmpty()) return;

        logger.info("Enviando {} notificación(es) programada(s) vencida(s)", pendientes.size());

        for (Notificacion n : pendientes) {
            // enviarA nunca lanza: un fallo de push no bloquea el resto del lote.
            pushNotificationService.enviarA(n.getUsuario().getId(), n.getTitulo(), n.getMensaje());
            // Marcada como enviada aunque el push fallara: evita reintentos infinitos
            // contra tokens rotos; la notificación in-app sigue disponible igualmente.
            n.setPushEnviada(true);
        }
        notificacionRepository.saveAll(pendientes);
    }
}
