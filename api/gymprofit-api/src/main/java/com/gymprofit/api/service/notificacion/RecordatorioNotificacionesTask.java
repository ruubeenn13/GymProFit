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
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
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
// Internacionalización: los textos viven en messages.properties (ES, default) y
// messages_en.properties (EN); cada generador resuelve título/mensaje con el
// MessageSource en el idioma del dispositivo más reciente del usuario (localeDe).
// EDGE CASE anti-spam aceptado: el anti-spam compara el título RESUELTO en el
// idioma actual del usuario; si el usuario cambia de idioma entre dos ejecuciones,
// el título anterior estaba en otro idioma y puede recibir un duplicado puntual.
// Métodos públicos para poder invocarlos directamente desde los tests de integración.
// ============================================================
@Component
@AllArgsConstructor
public class RecordatorioNotificacionesTask {

    // ---- Zona horaria común de todos los cron (prod corre en UTC). ----
    private static final String ZONA = "Europe/Madrid";

    // ---- Idioma por defecto de las notificaciones (usuarios sin idioma resoluble). ----
    private static final String IDIOMA_DEFAULT = "es";

    // ---- Claves i18n de las notificaciones (título = clave del anti-spam una vez resuelto). ----
    // Recordatorios de comidas (uno por franja horaria).
    public static final String KEY_DESAYUNO_TITULO = "notif.desayuno.titulo";
    public static final String KEY_DESAYUNO_MENSAJE = "notif.desayuno.mensaje";
    public static final String KEY_ALMUERZO_TITULO = "notif.almuerzo.titulo";
    public static final String KEY_ALMUERZO_MENSAJE = "notif.almuerzo.mensaje";
    public static final String KEY_COMIDA_TITULO = "notif.comida.titulo";
    public static final String KEY_COMIDA_MENSAJE = "notif.comida.mensaje";
    public static final String KEY_MERIENDA_TITULO = "notif.merienda.titulo";
    public static final String KEY_MERIENDA_MENSAJE = "notif.merienda.mensaje";
    public static final String KEY_CENA_TITULO = "notif.cena.titulo";
    public static final String KEY_CENA_MENSAJE = "notif.cena.mensaje";
    // Entrenamiento y actividad.
    public static final String KEY_ENTRENAR_TITULO = "notif.entrenar.titulo";
    public static final String KEY_ENTRENAR_MENSAJE = "notif.entrenar.mensaje";
    public static final String KEY_INACTIVIDAD_TITULO = "notif.inactividad.titulo";
    public static final String KEY_INACTIVIDAD_MENSAJE = "notif.inactividad.mensaje";
    public static final String KEY_RESUMEN_TITULO = "notif.resumen.titulo";
    public static final String KEY_RESUMEN_MENSAJE = "notif.resumen.mensaje";
    // Logros y progreso.
    public static final String KEY_LOGRO_TITULO = "notif.logro.titulo";
    public static final String KEY_LOGRO_MENSAJE = "notif.logro.mensaje";
    public static final String KEY_MEDICION_TITULO = "notif.medicion.titulo";
    public static final String KEY_MEDICION_MENSAJE = "notif.medicion.mensaje";
    public static final String KEY_OBJETIVO_TITULO = "notif.objetivo.titulo";
    public static final String KEY_OBJETIVO_MENSAJE = "notif.objetivo.mensaje";

    private final IDeviceTokenRepository deviceTokenRepository;
    private final INotificacionRepository notificacionRepository;
    private final IComidaRepository comidaRepository;
    private final ISesionEntrenamientoRepository sesionEntrenamientoRepository;
    private final IMedicionCorporalRepository medicionCorporalRepository;
    private final IObjetivoPersonalRepository objetivoPersonalRepository;
    private final INotificacionService notificacionService;
    private final MessageSource messageSource;
    private final Logger logger = LoggerFactory.getLogger(RecordatorioNotificacionesTask.class);

    // ============================================================
    // Helpers i18n
    // ============================================================

    // Resuelve el Locale actual del usuario: idioma del dispositivo con la fecha de
    // actualización más reciente (el último usado marca el idioma vigente). Si el
    // usuario no tiene tokens (no debería pasar: los generadores iteran usuarios CON
    // token), se cae al idioma por defecto "es".
    private Locale localeDe(Integer usuarioId) {
        return deviceTokenRepository.findTopByUsuarioIdOrderByFechaActualizacionDesc(usuarioId)
                .map(dt -> new Locale(dt.getIdioma()))
                .orElse(new Locale(IDIOMA_DEFAULT));
    }

    // ============================================================
    // 1-5. Recordatorios de comidas (uno por franja horaria)
    // ============================================================

    // 8:00 — recordatorio de desayuno.
    @Scheduled(cron = "0 0 8 * * *", zone = ZONA)
    public void recordatorioDesayuno() {
        recordatorioComida(TipoComida.DESAYUNO, KEY_DESAYUNO_TITULO, KEY_DESAYUNO_MENSAJE);
    }

    // 11:00 — recordatorio de almuerzo.
    @Scheduled(cron = "0 0 11 * * *", zone = ZONA)
    public void recordatorioAlmuerzo() {
        recordatorioComida(TipoComida.ALMUERZO, KEY_ALMUERZO_TITULO, KEY_ALMUERZO_MENSAJE);
    }

    // 14:00 — recordatorio de comida.
    @Scheduled(cron = "0 0 14 * * *", zone = ZONA)
    public void recordatorioComidaPrincipal() {
        recordatorioComida(TipoComida.COMIDA, KEY_COMIDA_TITULO, KEY_COMIDA_MENSAJE);
    }

    // 17:00 — recordatorio de merienda.
    @Scheduled(cron = "0 0 17 * * *", zone = ZONA)
    public void recordatorioMerienda() {
        recordatorioComida(TipoComida.MERIENDA, KEY_MERIENDA_TITULO, KEY_MERIENDA_MENSAJE);
    }

    // 21:00 — recordatorio de cena.
    @Scheduled(cron = "0 0 21 * * *", zone = ZONA)
    public void recordatorioCena() {
        recordatorioComida(TipoComida.CENA, KEY_CENA_TITULO, KEY_CENA_MENSAJE);
    }

    // Lógica común de los recordatorios de comida: notifica a cada usuario con dispositivo
    // que aún NO ha registrado hoy una comida de ese tipo, con el texto resuelto en su
    // idioma. Sin anti-spam extra: cada cron corre 1 vez/día, así que como mucho se
    // genera una notificación diaria por franja.
    public void recordatorioComida(TipoComida tipo, String claveTitulo, String claveMensaje) {
        try {
            LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
            LocalDateTime finDia = inicioDia.plusDays(1).minusSeconds(1);

            for (Integer usuarioId : deviceTokenRepository.findDistinctUsuarioIds()) {
                // Condición: no existe comida de ese tipo registrada hoy.
                if (!comidaRepository.existsByUsuarioIdAndTipoComidaAndFechaBetween(usuarioId, tipo, inicioDia, finDia)) {
                    // Título y mensaje en el idioma actual del usuario.
                    Locale locale = localeDe(usuarioId);
                    String titulo = messageSource.getMessage(claveTitulo, null, locale);
                    String mensaje = messageSource.getMessage(claveMensaje, null, locale);
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
                    // Textos resueltos en el idioma actual del usuario.
                    Locale locale = localeDe(usuarioId);
                    String titulo = messageSource.getMessage(KEY_ENTRENAR_TITULO, null, locale);
                    String mensaje = messageSource.getMessage(KEY_ENTRENAR_MENSAJE, null, locale);
                    notificacionService.crearSistema(usuarioId, titulo, mensaje, TipoNotificacion.RECORDATORIO);
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
    // El anti-spam compara el título resuelto en el idioma ACTUAL del usuario (ver
    // edge case documentado en la cabecera de la clase).
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

                // Textos resueltos en el idioma actual del usuario.
                Locale locale = localeDe(usuarioId);
                String titulo = messageSource.getMessage(KEY_INACTIVIDAD_TITULO, null, locale);
                String mensaje = messageSource.getMessage(KEY_INACTIVIDAD_MENSAJE, null, locale);

                // Anti-spam: como mucho un aviso de inactividad cada 4 días.
                if (notificacionRepository.existsByUsuarioIdAndTituloAndFechaCreacionAfter(
                        usuarioId, titulo, ahora.minusDays(4))) continue;

                notificacionService.crearSistema(usuarioId, titulo, mensaje, TipoNotificacion.RECORDATORIO);
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

                // Mensaje con placeholders MessageFormat: {0} sesiones, {1} minutos, {2} kcal.
                Locale locale = localeDe(usuarioId);
                String titulo = messageSource.getMessage(KEY_RESUMEN_TITULO, null, locale);
                String mensaje = messageSource.getMessage(KEY_RESUMEN_MENSAJE,
                        new Object[]{sesiones.size(), minutos, kcal}, locale);

                notificacionService.crearSistema(usuarioId, titulo, mensaje, TipoNotificacion.SISTEMA);
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

                // Textos resueltos en el idioma actual del usuario.
                Locale locale = localeDe(usuarioId);
                String titulo = messageSource.getMessage(KEY_LOGRO_TITULO, null, locale);
                String mensaje = messageSource.getMessage(KEY_LOGRO_MENSAJE, null, locale);

                // Anti-spam: máximo un aviso de logro próximo por semana.
                if (notificacionRepository.existsByUsuarioIdAndTituloAndFechaCreacionAfter(
                        usuarioId, titulo, ahora.minusDays(7))) continue;

                notificacionService.crearSistema(usuarioId, titulo, mensaje, TipoNotificacion.LOGRO);
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

                // Textos resueltos en el idioma actual del usuario.
                Locale locale = localeDe(usuarioId);
                String titulo = messageSource.getMessage(KEY_MEDICION_TITULO, null, locale);
                String mensaje = messageSource.getMessage(KEY_MEDICION_MENSAJE, null, locale);

                // Anti-spam: como mucho un aviso cada 30 días.
                if (notificacionRepository.existsByUsuarioIdAndTituloAndFechaCreacionAfter(
                        usuarioId, titulo, ahora.minusDays(30))) continue;

                notificacionService.crearSistema(usuarioId, titulo, mensaje, TipoNotificacion.RECORDATORIO);
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

                // Título fijo en el idioma del usuario (clave del anti-spam).
                Locale locale = localeDe(usuarioId);
                String titulo = messageSource.getMessage(KEY_OBJETIVO_TITULO, null, locale);

                // Anti-spam: máximo un aviso de vencimiento cada 3 días por usuario.
                if (notificacionRepository.existsByUsuarioIdAndTituloAndFechaCreacionAfter(
                        usuarioId, titulo, ahora.minusDays(3))) continue;

                // El "nombre" del objetivo es su descripción (la entidad no tiene campo título);
                // va como {0} del mensaje MessageFormat.
                String mensaje = messageSource.getMessage(KEY_OBJETIVO_MENSAJE,
                        new Object[]{proximos.get(0).getDescripcion()}, locale);

                notificacionService.crearSistema(usuarioId, titulo, mensaje, TipoNotificacion.OBJETIVO);
            }
        } catch (Exception e) {
            logger.warn("Fallo en recordatorio de objetivo próximo a vencer: {}", e.getMessage());
        }
    }
}
