package com.gymprofit.api.service.notificacion;

import com.gymprofit.api.config.ShedLockConfig;
import com.gymprofit.api.entity.Notificacion;
import com.gymprofit.api.repository.jpa.INotificacionRepository;
import lombok.AllArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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
    //
    // Bloqueada con ShedLock (GP-221): con dos instancias, un solo barrido por minuto; sin el
    // bloqueo las dos leerían las mismas pendientes y cada push saldría dos veces.
    // lockAtMostFor = 5 min. Sin push, 300 vencidas de golpe tardan 149 ms en local (informe
    // de GP-221); lo que pesa es el envío a FCM, síncrono, de 100 a 300 ms por notificación.
    // 5 min dan para unas mil vencidas en el mismo minuto, y un minuto normal tiene muy pocas.
    // Es también lo más que se retrasarían las programadas si una instancia muere con el
    // bloqueo cogido: por eso no es más largo.
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    @SchedulerLock(name = "notificaciones-programadas", lockAtMostFor = "PT5M",
            lockAtLeastFor = ShedLockConfig.BLOQUEO_MINIMO_PROPIEDAD)
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
