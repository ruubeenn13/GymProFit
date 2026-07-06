package com.gymprofit.api.integration;

import com.gymprofit.api.dto.entity.comida.ComidaCreateDTO;
import com.gymprofit.api.entity.DeviceToken;
import com.gymprofit.api.repository.jpa.IDeviceTokenRepository;
import com.gymprofit.api.repository.jpa.INotificacionRepository;
import com.gymprofit.api.service.comida.IComidaService;
import com.gymprofit.api.service.notificacion.RecordatorioNotificacionesTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import java.time.LocalDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

// ============================================================
// RecordatorioNotificacionesTaskTest — integración de los generadores de recordatorios.
// Extiende AbstractOwnershipTest (siembra owner/attacker reales + rollback por test) y
// invoca los métodos del task DIRECTAMENTE (sin esperar a los cron) para verificar:
//   - condición: sin cena hoy → se crea la notificación de recordatorio de cena;
//   - condición negada: con cena registrada hoy → NO se crea una segunda;
//   - anti-spam: dos ejecuciones seguidas de inactividad → una sola notificación;
//   - i18n: dispositivo con idioma "en" → la notificación se crea con el título en inglés.
// Los títulos esperados se resuelven con el MessageSource (mismas claves KEY_* del task)
// en el idioma sembrado en el device token del owner.
// Firebase no se inicializa en tests, así que el envío push es no-op y no falla.
// ============================================================
@DisplayName("RecordatorioNotificacionesTask — generadores de notificaciones programadas")
class RecordatorioNotificacionesTaskTest extends AbstractOwnershipTest {

    @Autowired
    private RecordatorioNotificacionesTask task;

    @Autowired
    private IDeviceTokenRepository deviceTokenRepository;

    @Autowired
    private INotificacionRepository notificacionRepository;

    @Autowired
    private IComidaService comidaService;

    @Autowired
    private MessageSource messageSource;

    // Registra un dispositivo del owner: los generadores solo notifican a usuarios
    // con token FCM (findDistinctUsuarioIds), sin él el owner sería invisible al job.
    // Idioma "es" por defecto (campo NOT NULL); el test de i18n lo cambia a "en".
    @BeforeEach
    void seedDeviceToken() {
        DeviceToken dt = new DeviceToken();
        dt.setToken("test-token-recordatorio");
        dt.setUsuario(owner);
        dt.setPlataforma("ANDROID");
        dt.setIdioma("es");
        dt.setFechaRegistro(LocalDateTime.now());
        dt.setFechaActualizacion(LocalDateTime.now());
        deviceTokenRepository.save(dt);
    }

    // Resuelve una clave i18n del task en el idioma indicado (igual que hace el task).
    private String texto(String clave, String idioma) {
        return messageSource.getMessage(clave, null, new Locale(idioma));
    }

    // Cuenta las notificaciones del owner con un título concreto.
    private long notifsOwnerConTitulo(String titulo) {
        return notificacionRepository.findByUsuarioId(owner.getId()).stream()
                .filter(n -> titulo.equals(n.getTitulo()))
                .count();
    }

    @Test
    @DisplayName("Sin cena hoy → se genera el recordatorio de cena")
    void sinCenaHoy_generaRecordatorio() {
        // El owner no tiene ninguna comida CENA hoy → la condición del generador se cumple.
        task.recordatorioCena();

        assertEquals(1, notifsOwnerConTitulo(texto(RecordatorioNotificacionesTask.KEY_CENA_TITULO, "es")));
    }

    @Test
    @DisplayName("Con cena registrada hoy → no se genera una segunda notificación")
    void conCenaHoy_noGeneraSegundaNotificacion() {
        String tituloCena = texto(RecordatorioNotificacionesTask.KEY_CENA_TITULO, "es");

        // Primera pasada sin cena: crea el recordatorio.
        task.recordatorioCena();
        assertEquals(1, notifsOwnerConTitulo(tituloCena));

        // El owner registra su cena de hoy (siembra vía service como su dueño real).
        runAs(owner, () -> {
            ComidaCreateDTO dto = new ComidaCreateDTO();
            dto.setUsuarioId(owner.getId());
            dto.setTipoComida("CENA");
            comidaService.save(dto);
        });

        // Segunda pasada: la condición ya no se cumple → sigue habiendo solo 1.
        task.recordatorioCena();
        assertEquals(1, notifsOwnerConTitulo(tituloCena));
    }

    @Test
    @DisplayName("Anti-spam: dos ejecuciones seguidas de inactividad → una sola notificación")
    void inactividadDosVeces_soloUnaNotificacion() {
        // El owner no tiene sesiones (nunca entrenó) → cumple la condición de inactividad.
        task.recordatorioInactividad();
        // Segunda ejecución inmediata: el anti-spam por título (4 días) debe bloquearla.
        task.recordatorioInactividad();

        assertEquals(1, notifsOwnerConTitulo(texto(RecordatorioNotificacionesTask.KEY_INACTIVIDAD_TITULO, "es")));
    }

    @Test
    @DisplayName("i18n: dispositivo con idioma \"en\" → la notificación se crea en inglés")
    void deviceTokenEnIngles_notificacionEnIngles() {
        // Cambia el idioma del dispositivo sembrado a inglés (dispositivo más reciente del owner).
        DeviceToken dt = deviceTokenRepository.findByToken("test-token-recordatorio").orElseThrow();
        dt.setIdioma("en");
        dt.setFechaActualizacion(LocalDateTime.now());
        deviceTokenRepository.save(dt);

        // Sin cena hoy → se genera el recordatorio, pero resuelto con Locale("en").
        task.recordatorioCena();

        // El título debe ser el inglés (y NO debe existir el español).
        assertEquals(1, notifsOwnerConTitulo(texto(RecordatorioNotificacionesTask.KEY_CENA_TITULO, "en")));
        assertEquals(0, notifsOwnerConTitulo(texto(RecordatorioNotificacionesTask.KEY_CENA_TITULO, "es")));
    }
}
