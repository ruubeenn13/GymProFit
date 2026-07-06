package com.gymprofit.api.service.notificacion;

import com.gymprofit.api.entity.MedicionCorporal;
import com.gymprofit.api.entity.ObjetivoPersonal;
import com.gymprofit.api.entity.SesionEntrenamiento;
import com.gymprofit.api.enums.TipoComida;
import com.gymprofit.api.enums.TipoNotificacion;
import com.gymprofit.api.repository.jpa.IComidaRepository;
import com.gymprofit.api.repository.jpa.IDeviceTokenRepository;
import com.gymprofit.api.repository.jpa.IMedicionCorporalRepository;
import com.gymprofit.api.repository.jpa.INotificacionRepository;
import com.gymprofit.api.repository.jpa.IObjetivoPersonalRepository;
import com.gymprofit.api.repository.jpa.ISesionEntrenamientoRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ============================================================
// RecordatorioNotificacionesTask — generadores de notificaciones push programadas.
// Jobs @Scheduled que evalúan la actividad de cada usuario (comidas, sesiones,
// mediciones, objetivos, logros) y generan notificaciones motivacionales vía
// NotificacionService.crearSistema (sin SecurityContext, push inmediata).
// Reglas comunes de todos los generadores:
//   - Solo se notifica a usuarios con dispositivo registrado (findDistinctUsuarioIds):
//     evita engordar la tabla con notificaciones para cuentas muertas.
//   - Anti-spam por título exacto con existsByUsuarioIdAndTituloAndFechaCreacionAfter
//     en los recordatorios que podrían repetirse varios días seguidos.
//   - Todos los cron van con zone Europe/Madrid (el servidor de prod corre en UTC).
//   - Cada job envuelve su cuerpo en try/catch: un fallo no debe matar el scheduler.
// Métodos públicos para poder invocarlos directamente desde los tests de integración.
// ============================================================
@Component
@AllArgsConstructor
public class RecordatorioNotificacionesTask {

    // ---- Zona horaria común de todos los cron (prod corre en UTC). ----
    private static final String ZONA = "Europe/Madrid";

    // ---- Textos de las notificaciones (título = clave del anti-spam, mensaje corto y motivacional). ----
    // Recordatorios de comidas (uno por franja horaria).
    public static final String TITULO_DESAYUNO = "¿Ya has desayunado?";
    static final String MSG_DESAYUNO = "Empieza el día con energía: registra tu desayuno en GymProFit.";
    public static final String TITULO_ALMUERZO = "Hora del almuerzo";
    static final String MSG_ALMUERZO = "Un pequeño empujón a media mañana. ¡Registra tu almuerzo!";
    public static final String TITULO_COMIDA = "¿Qué hay para comer?";
    static final String MSG_COMIDA = "No olvides registrar tu comida para llevar tus macros al día.";
    public static final String TITULO_MERIENDA = "Hora de la merienda";
    static final String MSG_MERIENDA = "Una merienda a tiempo mantiene tus objetivos en marcha. ¡Regístrala!";
    public static final String TITULO_CENA = "No olvides registrar tu cena";
    static final String MSG_CENA = "Cierra el día completando tu registro de comidas. ¡Buen trabajo hoy!";
    // Entrenamiento y actividad.
    public static final String TITULO_ENTRENAR = "¡Hora de entrenar!";
    static final String MSG_ENTRENAR = "Hoy todavía no has entrenado. ¡Dale caña y mantén la racha!";
    public static final String TITULO_INACTIVIDAD = "Te echamos de menos";
    static final String MSG_INACTIVIDAD = "Hace días que no entrenas. Una sesión corta hoy es mejor que ninguna. ¡Vuelve!";
    public static final String TITULO_RESUMEN = "Tu resumen semanal";
    // Logros y progreso.
    public static final String TITULO_LOGRO_PROXIMO = "¡Estás a una sesión de un logro!";
    static final String MSG_LOGRO_PROXIMO = "Solo te falta una sesión para desbloquear un nuevo logro. ¡A por ella!";
    public static final String TITULO_MEDICION = "¿Cómo va tu progreso?";
    static final String MSG_MEDICION = "Hace más de un mes de tu última medición. Registra tu peso y comprueba tu evolución.";
    public static final String TITULO_OBJETIVO = "Tu objetivo está cerca de vencer";

    private final IDeviceTokenRepository deviceTokenRepository;
    private final INotificacionRepository notificacionRepository;
    private final IComidaRepository comidaRepository;
    private final ISesionEntrenamientoRepository sesionEntrenamientoRepository;
    private final IMedicionCorporalRepository medicionCorporalRepository;
    private final IObjetivoPersonalRepository objetivoPersonalRepository;
    private final INotificacionService notificacionService;
    private final Logger logger = LoggerFactory.getLogger(RecordatorioNotificacionesTask.class);

    // ============================================================
    // 1-5. Recordatorios de comidas (uno por franja horaria)
    // ============================================================

    // 8:00 — recordatorio de desayuno.
    @Scheduled(cron = "0 0 8 * * *", zone = ZONA)
    public void recordatorioDesayuno() {
        recordatorioComida(TipoComida.DESAYUNO, TITULO_DESAYUNO, MSG_DESAYUNO);
    }

    // 11:00 — recordatorio de almuerzo.
    @Scheduled(cron = "0 0 11 * * *", zone = ZONA)
    public void recordatorioAlmuerzo() {
        recordatorioComida(TipoComida.ALMUERZO, TITULO_ALMUERZO, MSG_ALMUERZO);
    }

    // 14:00 — recordatorio de comida.
    @Scheduled(cron = "0 0 14 * * *", zone = ZONA)
    public void recordatorioComidaPrincipal() {
        recordatorioComida(TipoComida.COMIDA, TITULO_COMIDA, MSG_COMIDA);
    }

    // 17:00 — recordatorio de merienda.
    @Scheduled(cron = "0 0 17 * * *", zone = ZONA)
    public void recordatorioMerienda() {
        recordatorioComida(TipoComida.MERIENDA, TITULO_MERIENDA, MSG_MERIENDA);
    }

    // 21:00 — recordatorio de cena.
    @Scheduled(cron = "0 0 21 * * *", zone = ZONA)
    public void recordatorioCena() {
        recordatorioComida(TipoComida.CENA, TITULO_CENA, MSG_CENA);
    }

    // Lógica común de los recordatorios de comida: notifica a cada usuario con dispositivo
    // que aún NO ha registrado hoy una comida de ese tipo. Sin anti-spam extra: cada cron
    // corre 1 vez/día, así que como mucho se genera una notificación diaria por franja.
    public void recordatorioComida(TipoComida tipo, String titulo, String mensaje) {
        try {
            LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
            LocalDateTime finDia = inicioDia.plusDays(1).minusSeconds(1);

            for (Integer usuarioId : deviceTokenRepository.findDistinctUsuarioIds()) {
                // Condición: no existe comida de ese tipo registrada hoy.
                if (!comidaRepository.existsByUsuarioIdAndTipoComidaAndFechaBetween(usuarioId, tipo, inicioDia, finDia)) {
                    notificacionService.crearSistema(usuarioId, titulo, mensaje, TipoNotificacion.RECORDATORIO);
                }
            }
        } catch (Exception e) {
            logger.warn("Fallo en recordatorio de comida {}: {}", tipo, e.getMessage());
        }
    }

    // ============================================================
    // 6. Recordatorio de entrenamiento (usuarios activos)
    // ============================================================

    // 18:00 — recuerda entrenar a quien aún no lo ha hecho hoy pero SÍ ha entrenado en los
    // últimos 3 días (usuario activo). Los inactivos los cubre recordatorioInactividad, así
    // no se duplican avisos el mismo día. Sin anti-spam extra: el cron es 1 vez/día.
    @Scheduled(cron = "0 0 18 * * *", zone = ZONA)
    public void recordatorioEntrenar() {
        try {
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();

            for (Integer usuarioId : deviceTokenRepository.findDistinctUsuarioIds()) {
                // ¿Entrenó hoy? Si ya entrenó, no molestar.
                boolean entrenoHoy = sesionEntrenamientoRepository
                        .existsByUsuarioIdAndFechaInicioBetween(usuarioId, inicioHoy, ahora);
                if (entrenoHoy) continue;

                // ¿Activo? Alguna sesión en los últimos 3 días (si no, es territorio del job de inactividad).
                boolean activo = sesionEntrenamientoRepository
                        .existsByUsuarioIdAndFechaInicioBetween(usuarioId, ahora.minusDays(3), ahora);
                if (activo) {
                    notificacionService.crearSistema(usuarioId, TITULO_ENTRENAR, MSG_ENTRENAR, TipoNotificacion.RECORDATORIO);
                }
            }
        } catch (Exception e) {
            logger.warn("Fallo en recordatorio de entrenamiento: {}", e.getMessage());
        }
    }

    // ============================================================
    // 7. Recordatorio de inactividad
    // ============================================================

    // 12:00 — anima a volver a quien lleva ≥3 días sin entrenar (o nunca ha entrenado).
    // Anti-spam de 4 días: no se re-envía si ya se mandó este mismo aviso recientemente.
    @Scheduled(cron = "0 0 12 * * *", zone = ZONA)
    public void recordatorioInactividad() {
        try {
            LocalDateTime ahora = LocalDateTime.now();

            for (Integer usuarioId : deviceTokenRepository.findDistinctUsuarioIds()) {
                // Última sesión del usuario: inactivo si no existe o si es de hace 3 días o más.
                Optional<SesionEntrenamiento> ultima =
                        sesionEntrenamientoRepository.findTopByUsuarioIdOrderByFechaInicioDesc(usuarioId);
                boolean inactivo = ultima.isEmpty()
                        || ultima.get().getFechaInicio().isBefore(ahora.minusDays(3));
                if (!inactivo) continue;

                // Anti-spam: como mucho un aviso de inactividad cada 4 días.
                if (notificacionRepository.existsByUsuarioIdAndTituloAndFechaCreacionAfter(
                        usuarioId, TITULO_INACTIVIDAD, ahora.minusDays(4))) continue;

                notificacionService.crearSistema(usuarioId, TITULO_INACTIVIDAD, MSG_INACTIVIDAD, TipoNotificacion.RECORDATORIO);
            }
        } catch (Exception e) {
            logger.warn("Fallo en recordatorio de inactividad: {}", e.getMessage());
        }
    }

    // ============================================================
    // 8. Resumen semanal
    // ============================================================

    // Domingo 20:00 — resumen de la semana (lunes 00:00 → ahora) con nº de sesiones,
    // minutos y kcal totales. Si no hubo ninguna sesión, no se envía nada.
    // Sin anti-spam: el cron solo dispara una vez por semana.
    @Scheduled(cron = "0 0 20 * * SUN", zone = ZONA)
    public void resumenSemanal() {
        try {
            LocalDateTime lunes = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
            LocalDateTime ahora = LocalDateTime.now();

            for (Integer usuarioId : deviceTokenRepository.findDistinctUsuarioIds()) {
                List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository
                        .findByUsuarioIdAndFechaInicioBetween(usuarioId, lunes, ahora);
                if (sesiones.isEmpty()) continue;

                // Totales de la semana (los campos pueden venir a null en sesiones sin cerrar).
                int minutos = sesiones.stream()
                        .mapToInt(s -> s.getDuracionMinutos() == null ? 0 : s.getDuracionMinutos()).sum();
                int kcal = sesiones.stream()
                        .mapToInt(s -> s.getCaloriasQuemadas() == null ? 0 : s.getCaloriasQuemadas()).sum();

                String mensaje = "Esta semana: " + sesiones.size() + " sesión(es), "
                        + minutos + " min de entrenamiento y " + kcal + " kcal quemadas. ¡Sigue así!";

                notificacionService.crearSistema(usuarioId, TITULO_RESUMEN, mensaje, TipoNotificacion.SISTEMA);
            }
        } catch (Exception e) {
            logger.warn("Fallo en resumen semanal: {}", e.getMessage());
        }
    }

    // ============================================================
    // 9. Logro próximo
    // ============================================================

    // 20:30 — avisa cuando el usuario está a UNA sesión de un logro de constancia.
    // DECISIÓN: los umbrales de logros NO son legibles de BD (la entidad Logro no tiene
    // columna de umbral; están hardcodeados en el switch de LogroService.evaluarLogros:
    // CONSTANCIA = 7 y DEDICADO = 30 sesiones completadas). Por eso aquí se hardcodean
    // los valores 6 y 29 (a una sesión de cada logro), usando el mismo contador que
    // LogroService (countByUsuarioIdAndCompletadaTrue) para mantener la coherencia.
    // Anti-spam de 7 días por si el usuario se queda parado justo en 6/29 sesiones.
    @Scheduled(cron = "0 30 20 * * *", zone = ZONA)
    public void logroProximo() {
        try {
            LocalDateTime ahora = LocalDateTime.now();

            for (Integer usuarioId : deviceTokenRepository.findDistinctUsuarioIds()) {
                long completadas = sesionEntrenamientoRepository.countByUsuarioIdAndCompletadaTrue(usuarioId);
                // A una sesión de CONSTANCIA (7) o de DEDICADO (30).
                if (completadas != 6 && completadas != 29) continue;

                // Anti-spam: máximo un aviso de logro próximo por semana.
                if (notificacionRepository.existsByUsuarioIdAndTituloAndFechaCreacionAfter(
                        usuarioId, TITULO_LOGRO_PROXIMO, ahora.minusDays(7))) continue;

                notificacionService.crearSistema(usuarioId, TITULO_LOGRO_PROXIMO, MSG_LOGRO_PROXIMO, TipoNotificacion.LOGRO);
            }
        } catch (Exception e) {
            logger.warn("Fallo en aviso de logro próximo: {}", e.getMessage());
        }
    }

    // ============================================================
    // 10. Medición mensual
    // ============================================================

    // 10:00 — invita a registrar el peso a quien tiene mediciones pero la última es de
    // hace ≥30 días. Anti-spam de 30 días (un aviso por ciclo mensual como máximo).
    @Scheduled(cron = "0 0 10 * * *", zone = ZONA)
    public void recordatorioMedicionMensual() {
        try {
            LocalDateTime ahora = LocalDateTime.now();

            for (Integer usuarioId : deviceTokenRepository.findDistinctUsuarioIds()) {
                // Condición: tiene al menos una medición y la última es antigua (≥30 días).
                Optional<MedicionCorporal> ultima =
                        medicionCorporalRepository.findFirstByUsuarioIdOrderByFechaDesc(usuarioId);
                if (ultima.isEmpty() || !ultima.get().getFecha().isBefore(ahora.minusDays(30))) continue;

                // Anti-spam: como mucho un aviso cada 30 días.
                if (notificacionRepository.existsByUsuarioIdAndTituloAndFechaCreacionAfter(
                        usuarioId, TITULO_MEDICION, ahora.minusDays(30))) continue;

                notificacionService.crearSistema(usuarioId, TITULO_MEDICION, MSG_MEDICION, TipoNotificacion.RECORDATORIO);
            }
        } catch (Exception e) {
            logger.warn("Fallo en recordatorio de medición mensual: {}", e.getMessage());
        }
    }

    // ============================================================
    // 11. Objetivo próximo a vencer
    // ============================================================

    // 12:30 — avisa de objetivos NO completados cuya fecha límite cae entre hoy y hoy+3 días.
    // El anti-spam (3 días) es por título exacto, por eso el título es FIJO y el nombre
    // del objetivo va solo en el mensaje: así un mismo objetivo no genera un aviso diario.
    @Scheduled(cron = "0 30 12 * * *", zone = ZONA)
    public void recordatorioObjetivoProximo() {
        try {
            LocalDateTime ahora = LocalDateTime.now();
            LocalDate hoy = LocalDate.now();

            for (Integer usuarioId : deviceTokenRepository.findDistinctUsuarioIds()) {
                List<ObjetivoPersonal> proximos = objetivoPersonalRepository
                        .findByUsuarioIdAndCompletadoFalseAndFechaLimiteBetween(usuarioId, hoy, hoy.plusDays(3));
                if (proximos.isEmpty()) continue;

                // Anti-spam: máximo un aviso de vencimiento cada 3 días por usuario.
                if (notificacionRepository.existsByUsuarioIdAndTituloAndFechaCreacionAfter(
                        usuarioId, TITULO_OBJETIVO, ahora.minusDays(3))) continue;

                // El "nombre" del objetivo es su descripción (la entidad no tiene campo título).
                String mensaje = "\"" + proximos.get(0).getDescripcion()
                        + "\" vence en pocos días. ¡Último empujón, tú puedes!";

                notificacionService.crearSistema(usuarioId, TITULO_OBJETIVO, mensaje, TipoNotificacion.OBJETIVO);
            }
        } catch (Exception e) {
            logger.warn("Fallo en recordatorio de objetivo próximo a vencer: {}", e.getMessage());
        }
    }
}
