package com.gymprofit.api.integration;

import com.gymprofit.api.config.security.ClaveImportacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// ImportacionFinTest — el cierre de la importación dice cuánto hay y cuánto ocupa (GP-164)
// Va SIN @Transactional a propósito: ANALYZE TABLE confirma la transacción en curso, y
// dentro de una de test se llevaría por delante el rollback de lo sembrado. Aquí no se
// siembra nada: solo se lee.
// ============================================================
@SpringBootTest
@AutoConfigureMockMvc
class ImportacionFinTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.importacion.clave}")
    private String clave;

    @Test
    @DisplayName("con la clave, /fin devuelve el número de productos y los bytes de datos e índices")
    void fin_devuelve_tamano() throws Exception {
        mockMvc.perform(post("/importacion/productos/fin").header(ClaveImportacion.CABECERA, clave))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productos").isNumber())
                .andExpect(jsonPath("$.bytesDatos").isNumber())
                .andExpect(jsonPath("$.bytesIndices").isNumber());
    }
}
