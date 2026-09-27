package com.gymprofit.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// CorsProduccionTest — GP-085
//
// El preflight que hará el navegador desde admin.gymprofit.app, con el valor de CORS
// que lleva de verdad application-prod.properties: se lee de ese fichero y se inyecta,
// porque el perfil prod entero no arranca sin sus secretos. Si la línea desaparece del
// fichero, el contexto cae al valor por defecto (localhost) y el primer test falla.
// ============================================================
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GP-085 — CORS de producción: la web de administración sí, cualquier otro origen no")
class CorsProduccionTest {

    private static final String WEB_ADMIN = "https://admin.gymprofit.app";

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void corsDeProduccion(DynamicPropertyRegistry registro) throws IOException {
        Properties prod = new Properties();
        try (InputStream in = new ClassPathResource("application-prod.properties").getInputStream()) {
            prod.load(in);
        }
        String origenes = prod.getProperty("app.cors.allowed-origins");
        if (origenes != null) {
            registro.add("app.cors.allowed-origins", () -> origenes);
        }
    }

    @Test
    @DisplayName("preflight desde la web de administración → 200 con su origen")
    void origen_permitido() throws Exception {
        mockMvc.perform(options("/admin/resumen")
                        .header("Origin", WEB_ADMIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", WEB_ADMIN));
    }

    @Test
    @DisplayName("preflight desde otro origen → 403 y sin cabecera de permiso")
    void otro_origen() throws Exception {
        mockMvc.perform(options("/admin/resumen")
                        .header("Origin", "https://gymprofit.app.evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("el localhost del desarrollo no vale en producción")
    void localhost_no() throws Exception {
        mockMvc.perform(options("/admin/resumen")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
