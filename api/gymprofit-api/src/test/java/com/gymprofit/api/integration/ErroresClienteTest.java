package com.gymprofit.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// ErroresClienteTest — GP-075
//
// Un 500 dice «el servidor ha fallado», y lo que sigue no es un fallo del servidor:
// una ruta que no existe (404), un cuerpo al que le falta un campo obligatorio (400)
// y la documentación de Swagger pedida donde está apagada, que es producción (404,
// en SwaggerApagadoTest, que necesita su propio contexto).
// Además, un error sale en JSON aunque la petición no mande Accept: salía en XML,
// que ningún cliente de la API sabe leer.
//
// Contexto entero y JWT real: el 404 de una ruta inexistente solo aparece después de
// pasar el filtro de seguridad, y lo que se prueba es el manejador global de verdad.
// ============================================================
@DisplayName("GP-075 — la API no responde 500 a lo que no es un fallo del servidor")
class ErroresClienteTest extends AbstractOwnershipTest {

    @Test
    @DisplayName("ruta inexistente con token → 404 en JSON")
    void ruta_inexistente_404() throws Exception {
        pedir(owner, "GET /no-existe-gp075")
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("ruta inexistente sin Accept → el error sale en JSON, no en XML")
    void sin_accept_json() throws Exception {
        mockMvc.perform(get("/no-existe-gp075")
                        .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("400 de validación sin Accept → también en JSON")
    void validacion_sin_accept_json() throws Exception {
        mockMvc.perform(put("/comidas")
                        .header("Authorization", "Bearer " + token(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("PUT /comidas sin tipoComida → 400 que nombra el campo")
    void put_comida_sin_tipo_400() throws Exception {
        pedir(owner, "PUT /comidas", "{\"id\":1,\"usuarioId\":" + owner.getId() + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("tipoComida")));
    }
}
