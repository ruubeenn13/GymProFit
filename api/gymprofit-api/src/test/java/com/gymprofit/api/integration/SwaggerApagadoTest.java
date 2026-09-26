package com.gymprofit.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// SwaggerApagadoTest — GP-075
//
// En producción Swagger está apagado y sus rutas siguen siendo públicas en la cadena
// de seguridad, así que quien las pide llega al manejador global. Respondían 500; lo
// que corresponde es 404, en JSON. Contexto propio con las dos propiedades de
// application-prod.properties que lo apagan.
// ============================================================
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"})
@DisplayName("GP-075 — con Swagger apagado, sus rutas dan 404")
class SwaggerApagadoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("/v3/api-docs → 404 en JSON, sin token")
    void api_docs_404() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("/swagger-ui y /swagger-ui.html → 404, sin token")
    void swagger_ui_404() throws Exception {
        mockMvc.perform(get("/swagger-ui"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isNotFound());
    }
}
