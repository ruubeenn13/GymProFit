package com.gymprofit.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// IdiomaErroresTest — GP-109
//
// Los errores salen en español si la petición no dice otra cosa, y en inglés con
// Accept-Language: en. Antes, sin cabecera, mandaba el idioma del servidor —inglés
// en Render— y los de validación llegaban en inglés a la app en español.
//
// MockMvc sirve de testigo: una petición sin Accept-Language tiene Locale.ENGLISH,
// igual que Tomcat en Render, así que sin el arreglo estos tests salen en inglés.
//
// Cubre las cuatro fuentes de texto: la validación de los DTO (la del proveedor y
// las claves propias), una excepción de dominio, el manejador global y el punto de
// entrada de seguridad, que corre antes de Spring MVC.
// ============================================================
@DisplayName("GP-109 — errores en español por defecto y en inglés con Accept-Language: en")
class IdiomaErroresTest extends AbstractOwnershipTest {

    private MockHttpServletRequestBuilder registro(String cuerpo) {
        return post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    @Test
    @DisplayName("validación del proveedor sin cabecera → español")
    void validacion_proveedor_espanol() throws Exception {
        mockMvc.perform(registro("{\"password\":\"una frase larga\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("username = no debe estar vacío")));
    }

    @Test
    @DisplayName("validación del proveedor con Accept-Language: en → inglés")
    void validacion_proveedor_ingles() throws Exception {
        mockMvc.perform(registro("{\"password\":\"una frase larga\"}").header("Accept-Language", "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("username = must not be blank")));
    }

    @Test
    @DisplayName("validación propia (contraseña corta) en los dos idiomas")
    void validacion_propia() throws Exception {
        String cuerpo = "{\"username\":\"gp109\",\"password\":\"corta\"}";
        mockMvc.perform(registro(cuerpo))
                .andExpect(jsonPath("$.message", containsString("al menos 8 caracteres")));
        mockMvc.perform(registro(cuerpo).header("Accept-Language", "en-GB,en;q=0.9"))
                .andExpect(jsonPath("$.message", containsString("at least 8 characters")));
    }

    @Test
    @DisplayName("excepción de dominio (403 a un usuario ajeno) en los dos idiomas")
    void dominio() throws Exception {
        String ruta = "/usuarios/" + owner.getId();
        mockMvc.perform(get(ruta).header("Authorization", "Bearer " + token(attacker)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", equalTo("No tienes permiso para acceder a este recurso")));
        mockMvc.perform(get(ruta).header("Authorization", "Bearer " + token(attacker))
                        .header("Accept-Language", "en"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", equalTo("You do not have permission to access this resource")));
    }

    @Test
    @DisplayName("ruta inexistente: el texto del manejador global en los dos idiomas")
    void manejador_global() throws Exception {
        mockMvc.perform(get("/no-existe-gp109").header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", equalTo("Recurso no encontrado")));
        mockMvc.perform(get("/no-existe-gp109").header("Authorization", "Bearer " + token(owner))
                        .header("Accept-Language", "en"))
                .andExpect(jsonPath("$.message", equalTo("Resource not found")));
    }

    @Test
    @DisplayName("401 sin token, que sale antes de Spring MVC, en los dos idiomas")
    void punto_de_entrada() throws Exception {
        mockMvc.perform(get("/sesiones/usuario/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", equalTo("No autenticado")));
        mockMvc.perform(get("/sesiones/usuario/1").header("Accept-Language", "en"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", equalTo("Not authenticated")));
    }

    @Test
    @DisplayName("un idioma que no hay (francés) → español, no inglés")
    void idioma_sin_traduccion() throws Exception {
        mockMvc.perform(get("/no-existe-gp109").header("Authorization", "Bearer " + token(owner))
                        .header("Accept-Language", "fr"))
                .andExpect(jsonPath("$.message", equalTo("Recurso no encontrado")))
                .andExpect(jsonPath("$.message", not(containsString("Resource"))));
    }
}
