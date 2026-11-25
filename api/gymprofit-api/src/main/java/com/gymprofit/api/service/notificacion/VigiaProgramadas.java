package com.gymprofit.api.service.notificacion;

import com.gymprofit.api.repository.jpa.INotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

// ============================================================
// VigiaProgramadas — decide cada minuto si toca barrer las notificaciones programadas
// (GP-283).
//
// Antes, NotificacionProgramadaTask iba a la base cada 60 s pasara lo que pasara: bloqueo
// de ShedLock (dos UPDATE), su consulta y dos transacciones. Con la API en reposo era la
// fuente de sentencias más constante después del health check. Ahora:
//
//   · Cada 60 s, como antes, se mira la hora de la próxima en memoria (ProximaProgramada).
//     Si ha vencido, se llama al barrido de siempre, con su bloqueo y su transacción. Si
//     no, no se toca la base.
//   · Al arrancar, tras cada barrido y cada SINCRONIZAR se relee de la base la más
//     temprana pendiente: una sola sentencia, sin bloqueo (solo lee).
//
// Cuándo le llega la push al usuario no cambia para lo que entra por la API: crear o
// reprogramar avisa a ProximaProgramada al confirmar, y el barrido sale en el minuto en que
// vence, igual que antes. Lo que se escribiera por otro camino (un UPDATE a mano, otra
// instancia que la cree) se ve como mucho SINCRONIZAR más tarde. Con dos instancias, la
// que crea la notificación la conoce al momento, y el bloqueo de ShedLock sigue haciendo
// que la mande una sola.
// ============================================================
@Component
public class VigiaProgramadas {

    /** Cada cuánto se relee de la base aunque nadie haya avisado. */
    static final Duration SINCRONIZAR = Duration.ofMinutes(15);

    private static final Logger logger = LoggerFactory.getLogger(VigiaProgramadas.class);

    private final NotificacionProgramadaTask barrido;
    private final INotificacionRepository repositorio;
    private final ProximaProgramada proxima;
    private final Clock reloj;
    private Instant ultimaLectura;

    @Autowired
    public VigiaProgramadas(NotificacionProgramadaTask barrido, INotificacionRepository repositorio,
                            ProximaProgramada proxima) {
        this(barrido, repositorio, proxima, Clock.systemDefaultZone());
    }

    VigiaProgramadas(NotificacionProgramadaTask barrido, INotificacionRepository repositorio,
                     ProximaProgramada proxima, Clock reloj) {
        this.barrido = barrido;
        this.repositorio = repositorio;
        this.proxima = proxima;
        this.reloj = reloj;
    }

    /**
     * Cada 60 s, arrancando a los 30 s del boot, igual que el barrido de antes. Sin
     * ShedLock aquí: decidir no escribe nada, y el barrido lleva su propio bloqueo.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void comprobar() {
        Instant ahora = reloj.instant();
        if (ultimaLectura == null || !ahora.isBefore(ultimaLectura.plus(SINCRONIZAR))) {
            leerDeLaBase(ahora);
        }
        if (!proxima.vence(LocalDateTime.now(reloj))) return;

        barrido.enviarProgramadasVencidas();
        leerDeLaBase(reloj.instant());
    }

    private void leerDeLaBase(Instant ahora) {
        long marca = proxima.empezarLectura();
        LocalDateTime desdeBase = repositorio.proximaProgramadaPendiente();
        proxima.fijar(desdeBase, marca);
        ultimaLectura = ahora;
        logger.debug("Próxima notificación programada pendiente: {}", desdeBase);
    }
}
