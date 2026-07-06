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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

// ============================================================
// RecordatorioNotificacionesTaskTest — integración de los generadores de recordatorios.
// Extiende AbstractOwnershipTest (siembra owner/attacker reales + rollback por test) y
// invoca los métodos del task DIRECTAMENTE (sin esperar a los cron) para verificar:
//   - condición: sin cena hoy → se crea la notificación de recordatorio de cena;
//   - condición negada: con cena registrada hoy → NO se crea una segunda;
//   - anti-spam: dos ejecuciones seguidas de inactividad → una sola notificación.
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

    // Registra un dispositivo del owner: los generadores solo notifican a usuarios
    // con token FCM (findDistinctUsuarioIds), sin él el owner sería invisible al job.
    @BeforeEach
    void seedDeviceToken() {
        DeviceToken dt = new DeviceToken();
        dt.setToken("test-token-recordatorio");
        dt.setUsuario(owner);
        dt.setPlataforma("ANDROID");
        dt.setFechaRegistro(LocalDateTime.now());
        dt.setFechaActualizacion(LocalDateTime.now());
        deviceTokenRepository.save(dt);
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

        assertEquals(1, notifsOwnerConTitulo(RecordatorioNotificacionesTask.TITULO_CENA));
    }

    @Test
    @DisplayName("Con cena registrada hoy → no se genera una segunda notificación")
    void conCenaHoy_noGeneraSegundaNotificacion() {
        // Primera pasada sin cena: crea el recordatorio.
        task.recordatorioCena();
        assertEquals(1, notifsOwnerConTitulo(RecordatorioNotificacionesTask.TITULO_CENA));

        // El owner registra su cena de hoy (siembra vía service como su dueño real).
        runAs(owner, () -> {
            ComidaCreateDTO dto = new ComidaCreateDTO();
            dto.setUsuarioId(owner.getId());
            dto.setTipoComida("CENA");
            comidaService.save(dto);
        });

        // Segunda pasada: la condición ya no se cumple → sigue habiendo solo 1.
        task.recordatorioCena();
        assertEquals(1, notifsOwnerConTitulo(RecordatorioNotificacionesTask.TITULO_CENA));
    }

    @Test
    @DisplayName("Anti-spam: dos ejecuciones seguidas de inactividad → una sola notificación")
    void inactividadDosVeces_soloUnaNotificacion() {
        // El owner no tiene sesiones (nunca entrenó) → cumple la condición de inactividad.
        task.recordatorioInactividad();
        // Segunda ejecución inmediata: el anti-spam por título (4 días) debe bloquearla.
        task.recordatorioInactividad();

        assertEquals(1, notifsOwnerConTitulo(RecordatorioNotificacionesTask.TITULO_INACTIVIDAD));
    }
}
