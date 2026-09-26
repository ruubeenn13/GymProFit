package com.gymprofit.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// CommitDesplegadoTest — GP-100
//
// Qué commit corre en producción tiene que poder verse sin iniciar sesión: el health
// check sigue en verde aunque un despliegue falle y la instancia vieja siga sirviendo,
// así que «responde UP» no dice qué versión responde. /actuator/info devuelve el commit
// que Render deja en RENDER_GIT_COMMIT y NADA más: ni variables de entorno ni
// configuración, porque la ruta es pública.
//
// Contexto entero: lo que se prueba es la exposición del endpoint y la regla de
// seguridad, no la clase que aporta el dato.
// ============================================================
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GP-100 — /actuator/info dice qué commit corre")
class CommitDesplegadoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("sin RENDER_GIT_COMMIT → 200 público con commit «local» y nada más")
    void local_sin_token() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commit").value("local"))
                .andExpect(jsonPath("$", aMapWithSize(1)));
    }

    @Nested
    @TestPropertySource(properties = "RENDER_GIT_COMMIT=0123456789abcdef0123456789abcdef01234567")
    @DisplayName("con RENDER_GIT_COMMIT")
    class EnRender {

        // Propio: el del test exterior sale del contexto sin la propiedad.
        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("→ devuelve ese commit, sin token y sin nada más")
        void commit_de_render() throws Exception {
            mockMvc.perform(get("/actuator/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.commit").value("0123456789abcdef0123456789abcdef01234567"))
                    .andExpect(jsonPath("$", aMapWithSize(1)));
        }
    }
}
