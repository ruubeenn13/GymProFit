package com.gymprofit.api.integration;

import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoCreateDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// ValoracionSesionTest — la valoración de una sesión es un DATO (GP-070).
//
// Antes viajaba dentro de `notas`, formateada con un recurso de idioma y puesta
// delante de lo que hubiera escrito el usuario. Aquí se fija lo que eso pasa a
// ser: un campo propio, opcional, y acotado a 1..5 por el servidor.
//
// El rango se comprueba de verdad y no solo «está la anotación»: hasta este
// commit el PATCH de sesiones no llevaba @Valid, así que las restricciones del
// DTO no se evaluaban nunca. Un test que solo mirase el POST no lo habría visto.
// ============================================================
@DisplayName("GP-070 — la valoración de la sesión es un campo, no una línea de las notas")
class ValoracionSesionTest extends AbstractOwnershipTest {

    private SesionEntrenamientoCreateDTO nuevaSesion(Integer valoracion, String notas) {
        SesionEntrenamientoCreateDTO dto = new SesionEntrenamientoCreateDTO();
        dto.setUsuarioId(owner.getId());
        dto.setDuracionMinutos(45);
        dto.setValoracion(valoracion);
        dto.setNotas(notas);
        dto.setCompletada(true);
        return dto;
    }

    @Test
    @DisplayName("POST con valoración 4 la guarda como campo y deja las notas intactas")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void crear_conValoracion_guardaElCampo() throws Exception {
        mockMvc.perform(post("/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevaSesion(4, "Buenas sensaciones"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valoracion").value(4))
                .andExpect(jsonPath("$.notas").value("Buenas sensaciones"));
    }

    @Test
    @DisplayName("POST sin valoración es válido: no valorar es un caso normal")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void crear_sinValoracion_esValido() throws Exception {
        mockMvc.perform(post("/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevaSesion(null, "Sin valorar"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valoracion").doesNotExist())
                .andExpect(jsonPath("$.notas").value("Sin valorar"));
    }

    @Test
    @DisplayName("POST con valoración 0 → 400: cero estrellas no es una valoración")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void crear_conCero_rechaza() throws Exception {
        mockMvc.perform(post("/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevaSesion(0, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con valoración 6 → 400")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void crear_fueraDeRango_rechaza() throws Exception {
        mockMvc.perform(post("/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevaSesion(6, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH cambia la valoración sin tocar las notas")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void patch_cambiaLaValoracion() throws Exception {
        String creada = mockMvc.perform(post("/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevaSesion(2, "Notas del usuario"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(creada).get("id").asInt();

        mockMvc.perform(patch("/sesiones/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valoracion\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valoracion").value(5))
                .andExpect(jsonPath("$.notas").value("Notas del usuario"));
    }

    @Test
    @DisplayName("PATCH con valoración 9 → 400 (el PATCH también valida)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void patch_fueraDeRango_rechaza() throws Exception {
        String creada = mockMvc.perform(post("/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevaSesion(3, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(creada).get("id").asInt();

        mockMvc.perform(patch("/sesiones/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valoracion\":9}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------
    // DEC-068: la base no guarda el rango. TiDB descarta el CHECK
    // chk_sesion_valoracion en silencio, así que la API es la ÚNICA guarda.
    // Por eso se cubren las tres rutas que escriben la valoración, con los dos
    // bordes de fuera (0 y 6), y se exige que el 400 sea por `valoracion` y no
    // por otro campo del cuerpo.
    // ------------------------------------------------------------

    // Cuerpo mínimo válido de /sesiones/completa: sin ejercicios (entrenamiento libre).
    private static String cuerpoCompleta(int valoracion) {
        return "{\"claveIdempotencia\":\"dec068-" + valoracion + "-" + System.nanoTime() + "\","
                + "\"duracionMinutos\":30,\"valoracion\":" + valoracion + ","
                + "\"completada\":true,\"ejercicios\":[]}";
    }

    @Test
    @DisplayName("POST /sesiones/completa con valoración 5 → 200 (el cuerpo de los rechazos es válido)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void completa_enRango_guarda() throws Exception {
        mockMvc.perform(post("/sesiones/completa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCompleta(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valoracion").value(5));
    }

    @ParameterizedTest(name = "POST /sesiones/completa con valoración {0} → 400")
    @ValueSource(ints = {0, 6})
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void completa_fueraDeRango_rechaza(int valoracion) throws Exception {
        mockMvc.perform(post("/sesiones/completa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCompleta(valoracion)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("valoracion")));
    }

    @ParameterizedTest(name = "POST /sesiones con valoración {0} → 400 por la valoración")
    @ValueSource(ints = {0, 6})
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void crear_fueraDeRango_rechazaPorLaValoracion(int valoracion) throws Exception {
        mockMvc.perform(post("/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevaSesion(valoracion, null))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("valoracion")));
    }

    @ParameterizedTest(name = "PATCH con valoración {0} → 400")
    @ValueSource(ints = {0, 6})
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void patch_bordesFuera_rechaza(int valoracion) throws Exception {
        String creada = mockMvc.perform(post("/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevaSesion(3, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(creada).get("id").asInt();

        mockMvc.perform(patch("/sesiones/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valoracion\":" + valoracion + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("valoracion")));
    }
}
