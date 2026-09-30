package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// ============================================================
// EdadMinimaTest — la app no es para menores de 14 (lote 1.5.0)
//
// La política de privacidad lo dice; ahora lo dice también la API. El alta y el PATCH
// del perfil rechazan una edad menor con un 400 claro: el mensaje dice el mínimo, en el
// idioma de la petición, y EDAD_MINIMA en "cause" para que la app lo ponga en el campo.
// Sin edad no se comprueba nada: sigue siendo opcional.
// ============================================================
@DisplayName("Edad mínima de 14 en el alta y en el PATCH")
class EdadMinimaTest extends AbstractOwnershipTest {

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Test
    @DisplayName("alta con 13 → 400 EDAD_MINIMA que dice el mínimo, en ES y EN, y no se crea nada")
    void alta_trece() throws Exception {
        MockHttpServletResponse es = alta("trece-gp103@test.local", 13, "es");
        assertThat(es.getStatus()).isEqualTo(400);
        JsonNode cuerpo = objectMapper.readTree(es.getContentAsString());
        assertThat(cuerpo.get("cause").asText()).isEqualTo(InvalidDataException.EDAD_MINIMA);
        assertThat(cuerpo.get("message").asText()).contains("14");
        assertThat(objectMapper.readTree(alta("trece-gp103@test.local", 13, "en").getContentAsString())
                .get("message").asText()).contains("14").containsIgnoringCase("years");
        assertThat(usuarioRepository.findByEmail("trece-gp103@test.local")).isEmpty();
    }

    @Test
    @DisplayName("alta con 0 o negativa → el mismo 400 claro, no el genérico de validación")
    void alta_cero_y_negativa() throws Exception {
        for (int edad : new int[]{0, -1}) {
            MockHttpServletResponse r = alta("cero-gp103@test.local", edad, "es");
            assertThat(r.getStatus()).as("edad " + edad).isEqualTo(400);
            assertThat(objectMapper.readTree(r.getContentAsString()).get("cause").asText())
                    .isEqualTo(InvalidDataException.EDAD_MINIMA);
        }
    }

    @Test
    @DisplayName("alta con 14 o sin edad → 201")
    void alta_catorce_y_sin_edad() throws Exception {
        assertThat(alta("catorce-gp103@test.local", 14, "es").getStatus()).isEqualTo(201);
        assertThat(alta("sinedad-gp103@test.local", null, "es").getStatus()).isEqualTo(201);
    }

    @Test
    @DisplayName("PATCH con 13 → 400 EDAD_MINIMA y la edad no cambia; con 14 → 200")
    void patch() throws Exception {
        owner.setEdad(30);
        usuarioRepository.save(owner);

        MockHttpServletResponse r = pedir(owner, "PATCH /usuarios/" + owner.getId(), "{\"edad\":13}")
                .andReturn().getResponse();
        assertThat(r.getStatus()).isEqualTo(400);
        assertThat(objectMapper.readTree(r.getContentAsString()).get("cause").asText())
                .isEqualTo(InvalidDataException.EDAD_MINIMA);
        assertThat(usuarioRepository.findById(owner.getId()).orElseThrow().getEdad()).isEqualTo(30);

        assertThat(pedir(owner, "PATCH /usuarios/" + owner.getId(), "{\"edad\":14}")
                .andReturn().getResponse().getStatus()).isEqualTo(200);
        // Sin edad en el cuerpo no se comprueba nada.
        assertThat(pedir(owner, "PATCH /usuarios/" + owner.getId(), "{\"nombre\":\"Ana\"}")
                .andReturn().getResponse().getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse alta(String email, Integer edad, String idioma) throws Exception {
        Map<String, Object> cuerpo = new java.util.HashMap<>(Map.of("email", email, "password", "Frase corta de edad"));
        if (edad != null) cuerpo.put("edad", edad);
        return mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", idioma)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andReturn().getResponse();
    }
}
