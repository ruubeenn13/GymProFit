package com.gymprofit.api.integration;

import io.sentry.Sentry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// LoginSinBaseTest — si la base no responde durante el login, la respuesta es 503 y el
// fallo llega a Sentry; no un 401 que culpa a la contraseña (GP-260).
//
// El pool se agota de verdad: el test coge todas las conexiones de Hikari y las retiene
// mientras pide el login, con un tiempo de espera de 250 ms para no alargar la prueba.
// Sin @Transactional a propósito: un test transaccional ya tiene su conexión, y el login
// la reutilizaría sin tocar el pool.
// ============================================================
@DisplayName("Login sin base — 503 y aviso a Sentry, no 401")
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LoginSinBaseTest {

    private static final int TAMANO_POOL = 2;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void poolPequenoYSentry(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.hikari.maximum-pool-size", () -> TAMANO_POOL);
        registro.add("spring.datasource.hikari.minimum-idle", () -> 1);
        registro.add("spring.datasource.hikari.connection-timeout", () -> 250);
        registro.add("sentry.dsn", ServidorSentryFalso::dsn);
    }

    @AfterAll
    static void apagarSentry() {
        Sentry.close();
    }

    @BeforeEach
    void vaciar() {
        Sentry.flush(5000);
        ServidorSentryFalso.vaciar();
    }

    @Test
    @DisplayName("con el pool agotado, el login da 503 y llega a Sentry")
    void pool_agotado_da_503() throws Exception {
        List<Connection> retenidas = new ArrayList<>();
        try {
            // Todas las del pool, aunque tarde: en la suite completa, una tarea programada
            // del contexto puede tener una un momento, y si el login la recogiera al
            // soltarla el pool no estaría agotado.
            long limite = System.currentTimeMillis() + 30_000;
            while (retenidas.size() < TAMANO_POOL) {
                try {
                    retenidas.add(dataSource.getConnection());
                } catch (SQLTransientConnectionException ocupada) {
                    assertThat(System.currentTimeMillis()).as("no se pudo retener el pool entero")
                            .isLessThan(limite);
                }
            }
            mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"__sin_base__\",\"password\":\"Test1234\"}"))
                    .andExpect(status().isServiceUnavailable());
        } finally {
            for (Connection c : retenidas) {
                c.close();
            }
        }
        Sentry.flush(5000);

        assertThat(ServidorSentryFalso.recibidos()).as("el fallo de la base tenía que llegar a Sentry")
                .isNotEmpty();
        assertThat(String.join("\n", ServidorSentryFalso.recibidos()))
                .contains("/auth/login")
                .doesNotContain("Test1234");
    }

    @Test
    @DisplayName("con la base bien, una clave mala sigue siendo 401 y no llega a Sentry")
    void clave_mala_sigue_401() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"__no_existe__\",\"password\":\"Mala1234\"}"))
                .andExpect(status().isUnauthorized());
        Sentry.flush(5000);

        assertThat(ServidorSentryFalso.recibidos()).isEmpty();
    }
}
