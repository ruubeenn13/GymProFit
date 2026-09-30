package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.DeviceToken;
import com.gymprofit.api.enums.TipoLogro;
import com.gymprofit.api.repository.jpa.IDeviceTokenRepository;
import com.gymprofit.api.repository.jpa.INotificacionRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.notificacion.RecordatorioNotificacionesTask;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// AvisosPorTipoTest — GP-112, lote 1.5.0
//
// Tres interruptores en usuarios: entrenar (sí), comidas (no) y progreso (sí). Se
// comprueba que salen en UsuarioDTO y se cambian por el PATCH, que los valores de serie
// valen también para una cuenta creada sin ellos (el DEFAULT de la migración), que cada
// tarea respeta el suyo —encendido avisa, apagado no—, que ya no hay recordatorio a las
// 18:00, y que el resumen semanal dice «1 sesión» o «2 sesiones».
// ============================================================
@DisplayName("GP-112 — avisos por tipo")
class AvisosPorTipoTest extends AbstractOwnershipTest {

    @Autowired
    private RecordatorioNotificacionesTask task;

    @Autowired
    private IDeviceTokenRepository deviceTokenRepository;

    @Autowired
    private INotificacionRepository notificacionRepository;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager em;

    @BeforeEach
    void dispositivo() {
        DeviceToken dt = new DeviceToken();
        dt.setToken("test-token-avisos-gp112");
        dt.setUsuario(owner);
        dt.setPlataforma("ANDROID");
        dt.setIdioma("es");
        dt.setFechaRegistro(LocalDateTime.now());
        dt.setFechaActualizacion(LocalDateTime.now());
        deviceTokenRepository.save(dt);
    }

    // --- Los interruptores -----------------------------------------------------

    @Test
    @DisplayName("salen en UsuarioDTO con sus valores de serie y se cambian por el PATCH")
    void dto_y_patch() throws Exception {
        JsonNode u = json(pedir(owner, "GET /usuarios/" + owner.getId()).andReturn().getResponse().getContentAsString());
        assertThat(u.get("avisosEntrenar").asBoolean()).isTrue();
        assertThat(u.get("avisosComidas").asBoolean()).isFalse();
        assertThat(u.get("avisosProgreso").asBoolean()).isTrue();

        JsonNode p = json(pedir(owner, "PATCH /usuarios/" + owner.getId(),
                "{\"avisosEntrenar\":false,\"avisosComidas\":true}").andReturn().getResponse().getContentAsString());
        assertThat(p.get("avisosEntrenar").asBoolean()).isFalse();
        assertThat(p.get("avisosComidas").asBoolean()).isTrue();
        assertThat(p.get("avisosProgreso").asBoolean()).isTrue();

        // Sin los campos, el PATCH no los toca.
        p = json(pedir(owner, "PATCH /usuarios/" + owner.getId(), "{\"nombre\":\"Ana\"}")
                .andReturn().getResponse().getContentAsString());
        assertThat(p.get("avisosComidas").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("una cuenta creada sin ellos (como las que ya existían) recibe los de serie de la base")
    void valores_de_serie_en_la_base() {
        jdbc.update("INSERT INTO usuarios (username, password, email, activo) VALUES ('vieja-gp112', 'x', "
                + "'vieja-gp112@test.local', 1)");
        // Entrenar, comidas y progreso, en ese orden.
        assertThat(jdbc.queryForObject("SELECT CONCAT(avisos_entrenar, avisos_comidas, avisos_progreso) "
                + "FROM usuarios WHERE username = 'vieja-gp112'", String.class)).isEqualTo("101");
    }

    @Test
    @DisplayName("el alta crea la cuenta con los de serie")
    void alta_con_los_de_serie() throws Exception {
        String cuerpo = "{\"email\":\"alta-gp112@test.local\",\"password\":\"Frase corta de avisos\"}";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/register")
                .contentType("application/json").content(cuerpo));
        var u = usuarioRepository.findByEmail("alta-gp112@test.local").orElseThrow();
        assertThat(u.getAvisosEntrenar()).isTrue();
        assertThat(u.getAvisosComidas()).isFalse();
        assertThat(u.getAvisosProgreso()).isTrue();
    }

    // --- Cada tarea respeta su interruptor --------------------------------------

    @Test
    @DisplayName("comidas: las cinco solo con comidas encendido")
    void comidas() {
        List<Runnable> cinco = List.of(task::recordatorioDesayuno, task::recordatorioAlmuerzo,
                task::recordatorioComidaPrincipal, task::recordatorioMerienda, task::recordatorioCena);
        avisos(false, false, false);
        cinco.forEach(Runnable::run);
        assertThat(notificaciones()).isZero();

        avisos(false, true, false);
        cinco.forEach(Runnable::run);
        assertThat(notificaciones()).isEqualTo(5);
    }

    @Test
    @DisplayName("inactividad: solo con entrenar encendido")
    void inactividad() {
        comprobar(u -> {}, task::recordatorioInactividad, true, false, false);
    }

    @Test
    @DisplayName("resumen semanal: solo con progreso encendido")
    void resumen() {
        comprobar(u -> sesion(LocalDateTime.now(), true), task::resumenSemanal, false, false, true);
    }

    @Test
    @DisplayName("logro próximo: solo con progreso encendido")
    void logro_proximo() {
        comprobar(u -> {
            for (int i = 0; i < TipoLogro.CONSTANCIA.getUmbral() - 1; i++) {
                sesion(LocalDateTime.now().minusDays(20 + i), true);
            }
        }, task::logroProximo, false, false, true);
    }

    @Test
    @DisplayName("medición mensual: solo con progreso encendido")
    void medicion() {
        comprobar(u -> jdbc.update("INSERT INTO mediciones_corporales (usuario_id, peso, fecha) VALUES (?, 80, ?)",
                owner.getId(), LocalDateTime.now().minusDays(40)), task::recordatorioMedicionMensual, false, false, true);
    }

    @Test
    @DisplayName("objetivo por vencer: solo con progreso encendido")
    void objetivo() {
        comprobar(u -> jdbc.update("INSERT INTO objetivos_personales (usuario_id, descripcion, tipo_objetivo, "
                        + "valor_objetivo, fecha_inicio, fecha_limite, completado) VALUES (?, 'Llegar a 80', "
                        + "'PERDER_PESO', 80, ?, ?, 0)", owner.getId(), LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(1)), task::recordatorioObjetivoProximo, false, false, true);
    }

    @Test
    @DisplayName("ya no hay recordatorio a las 18:00: saltaba también los días de descanso")
    void sin_las_18() {
        List<String> crones = Arrays.stream(RecordatorioNotificacionesTask.class.getMethods())
                .map(m -> m.getAnnotation(Scheduled.class)).filter(a -> a != null).map(Scheduled::cron).toList();
        assertThat(crones).isNotEmpty().doesNotContain("0 0 18 * * *");
        assertThat(Arrays.stream(RecordatorioNotificacionesTask.class.getMethods()).map(Method::getName))
                .doesNotContain("recordatorioEntrenar");
    }

    // --- El resumen, en singular y en plural --------------------------------------

    @Test
    @DisplayName("resumen: «1 sesión» y «2 sesiones», en ES y EN, sin «(es)»")
    void resumen_plural() {
        LocalDateTime estaSemana = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay().plusMinutes(1);
        sesion(estaSemana, true);
        task.resumenSemanal();
        assertThat(ultimoMensaje()).contains("1 sesión ").doesNotContain("(es)");

        sesion(estaSemana.plusMinutes(1), true);
        task.resumenSemanal();
        assertThat(ultimoMensaje()).contains("2 sesiones").doesNotContain("(es)");

        DeviceToken dt = deviceTokenRepository.findByToken("test-token-avisos-gp112").orElseThrow();
        dt.setIdioma("en");
        dt.setFechaActualizacion(LocalDateTime.now());
        deviceTokenRepository.save(dt);
        task.resumenSemanal();
        assertThat(ultimoMensaje()).contains("2 sessions").doesNotContain("(s)");
        jdbc.update("DELETE FROM sesiones_entrenamiento WHERE usuario_id = ? AND fecha_inicio > ?", owner.getId(),
                estaSemana);
        task.resumenSemanal();
        assertThat(ultimoMensaje()).contains("1 session ").doesNotContain("(s)");
    }

    // --- Ayudas ----------------------------------------------------------------

    // Con la condición de la tarea cumplida: apagado su interruptor (y los demás
    // encendidos) no avisa; encendido solo el suyo, sí.
    private void comprobar(Consumer<Void> preparar, Runnable tarea, boolean entrenar, boolean comidas,
                           boolean progreso) {
        preparar.accept(null);
        avisos(!entrenar, !comidas, !progreso);
        tarea.run();
        assertThat(notificaciones()).as("apagado").isZero();

        avisos(entrenar, comidas, progreso);
        tarea.run();
        assertThat(notificaciones()).as("encendido").isEqualTo(1);
    }

    private void avisos(boolean entrenar, boolean comidas, boolean progreso) {
        owner.setAvisosEntrenar(entrenar);
        owner.setAvisosComidas(comidas);
        owner.setAvisosProgreso(progreso);
        usuarioRepository.saveAndFlush(owner);
    }

    private void sesion(LocalDateTime inicio, boolean completada) {
        jdbc.update("INSERT INTO sesiones_entrenamiento (usuario_id, fecha_inicio, fecha_fin, duracion_minutos, "
                + "completada) VALUES (?, ?, ?, 30, ?)", owner.getId(), inicio, inicio.plusMinutes(30), completada);
    }

    private long notificaciones() {
        em.flush();
        return notificacionRepository.findByUsuarioId(owner.getId()).size();
    }

    private String ultimoMensaje() {
        return notificacionRepository.findByUsuarioId(owner.getId()).stream()
                .max((a, b) -> a.getId().compareTo(b.getId())).orElseThrow().getMensaje();
    }

    private JsonNode json(String cuerpo) throws Exception {
        return objectMapper.readTree(cuerpo);
    }
}
