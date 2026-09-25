package com.gymprofit.api.integration;

import com.gymprofit.api.exceptions.DuplicateEntityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// RegistroDuplicadoTest — GP-095
//
// El registro rechaza un usuario o un correo que ya tienen cuenta con un 400, y la
// app no tenía forma de saber cuál de los dos era: enseñaba «Error al crear la
// cuenta» para todo. Ahora el 400 lleva en "cause" un código estable. El estado y el
// mensaje no cambian, para que las builds ya repartidas sigan igual.
//
// Contexto entero levantado y servicio real: lo que se prueba es que el código llega
// de AuthService a la respuesta a través del manejador global. Los usuarios sembrados
// son los de AbstractOwnershipTest y todo se revierte al acabar cada test.
// ============================================================
@DisplayName("GP-095 — el registro dice si el usuario o el correo están en uso")
class RegistroDuplicadoTest extends AbstractOwnershipTest {

    private static final String PASSWORD_VALIDA = "Passw0rd!";

    private ResultActions registrar(String username, String email) throws Exception {
        String cuerpo = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "email", email,
                "password", PASSWORD_VALIDA));
        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    @Test
    @DisplayName("usuario en uso → 400 con USERNAME_EN_USO y el mensaje de siempre")
    void usuario_en_uso() throws Exception {
        registrar(owner.getUsername(), "libre-gp095@test.local")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cause").value(DuplicateEntityException.USERNAME_EN_USO))
                .andExpect(jsonPath("$.message", containsString("ya está en uso")));
    }

    @Test
    @DisplayName("correo en uso → 400 con EMAIL_EN_USO y el mensaje de siempre")
    void correo_en_uso() throws Exception {
        registrar("libre-gp095", owner.getEmail())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cause").value(DuplicateEntityException.EMAIL_EN_USO))
                .andExpect(jsonPath("$.message", containsString("ya está en uso")));
    }

    @Test
    @DisplayName("usuario y correo en uso → manda el usuario, que es lo primero que se comprueba")
    void ambos_en_uso() throws Exception {
        registrar(owner.getUsername(), owner.getEmail())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cause").value(DuplicateEntityException.USERNAME_EN_USO));
    }

    @Test
    @DisplayName("contraseña sin símbolo → 400 de validación, sin código de duplicado")
    void password_debil_sin_codigo() throws Exception {
        String cuerpo = objectMapper.writeValueAsString(Map.of(
                "username", "libre-gp095",
                "email", "libre-gp095@test.local",
                "password", "Gymprofit1"));
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cause").value(nullValue()));
    }

    @Test
    @DisplayName("alta libre → 201 (control del test)")
    void alta_libre_201() throws Exception {
        registrar("libre-gp095", "libre-gp095@test.local")
                .andExpect(status().isCreated());
    }
}
