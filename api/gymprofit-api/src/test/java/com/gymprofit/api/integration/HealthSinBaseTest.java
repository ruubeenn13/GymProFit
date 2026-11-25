package com.gymprofit.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// HealthSinBaseTest — el health que se consulta sin parar no toca la base (GP-283).
//
// Render pregunta cada pocos segundos, UptimeRobot cada pocos minutos: con el health
// completo, cada pregunta era un ping a la base de producción, que cobra en RU. Liveness
// no la incluye; readiness y el health completo sí, como antes.
// ============================================================
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Health — liveness sin base, readiness y el completo con ella")
class HealthSinBaseTest {

    @Autowired private HealthEndpointGroups grupos;
    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("liveness no incluye la base")
    void livenessSinBase() {
        assertThat(grupos.get("liveness")).isNotNull();
        assertThat(grupos.get("liveness").isMember("db")).isFalse();
    }

    @Test
    @DisplayName("readiness y el health completo siguen incluyendo la base")
    void readinessYCompletoConBase() {
        assertThat(grupos.get("readiness").isMember("db")).isTrue();
        assertThat(grupos.getPrimary().isMember("db")).isTrue();
    }

    @Test
    @DisplayName("las tres rutas responden UP sin sesión")
    void respondenSinSesion() throws Exception {
        for (String ruta : new String[]{"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"}) {
            mockMvc.perform(get(ruta)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        }
    }
}
