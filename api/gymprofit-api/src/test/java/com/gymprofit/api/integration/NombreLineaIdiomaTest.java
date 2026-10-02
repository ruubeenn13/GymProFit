package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// NombreLineaIdiomaTest — el nombre del alimento de una línea, en su idioma (GP-176)
// En inglés se buscaba «Chicken breast, grilled» y dentro de la comida salía «Pechuga de
// pollo, a la plancha». El nombre sale en el idioma de la petición, con el español de
// respaldo. La categoría NO se traduce: es la clave canónica con la que la app elige el
// icono y el nombre (AlimentoController.CATEGORIAS).
// ============================================================
class NombreLineaIdiomaTest extends AbstractOwnershipTest {

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    private Integer comidaId;

    @BeforeEach
    void sembrar() throws Exception {
        Alimento pollo = crearAlimentoCatalogo();
        pollo.setNombre("Pechuga de pollo, a la plancha");
        pollo.setNombreEn("Chicken breast, grilled");
        pollo.setCategoria("Carnes y aves");
        pollo.setCategoriaEn("Meat and poultry");
        alimentoRepository.save(pollo);
        Alimento sinIngles = crearAlimentoCatalogo();
        sinIngles.setNombre("Cocido kznombre");
        sinIngles.setCategoria("Otro");
        alimentoRepository.save(sinIngles);
        for (Alimento a : new Alimento[]{pollo, sinIngles}) {
            String cuerpo = pedir(owner, "POST /comidas/anadir", "{\"fecha\":\"2026-10-02\",\"tipoComida\":\"COMIDA\","
                    + "\"alimentoId\":" + a.getId() + ",\"cantidadGramos\":150}")
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            comidaId = objectMapper.readTree(cuerpo).get("comida").get("id").asInt();
        }
        em.flush();
        em.clear();
    }

    private JsonNode lineas(String idioma) throws Exception {
        String cuerpo = mockMvc.perform(get("/alimentos-comida/comida/" + comidaId)
                        .header("Authorization", "Bearer " + token(owner)).header("Accept-Language", idioma))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(cuerpo);
    }

    private static JsonNode porCategoria(JsonNode lineas, String categoria) {
        for (JsonNode l : lineas) if (categoria.equals(l.get("categoriaAlimento").asText())) return l;
        throw new AssertionError("sin línea de " + categoria);
    }

    @Test
    @DisplayName("en inglés, el nombre en inglés y la categoría canónica")
    void en_ingles() throws Exception {
        JsonNode l = lineas("en");
        assertThat(porCategoria(l, "Carnes y aves").get("nombreAlimento").asText()).isEqualTo("Chicken breast, grilled");
        // Sin nombre en inglés, el español de respaldo.
        assertThat(porCategoria(l, "Otro").get("nombreAlimento").asText()).isEqualTo("Cocido kznombre");
    }

    @Test
    @DisplayName("en español, todo como siempre")
    void en_espanol() throws Exception {
        JsonNode l = lineas("es");
        assertThat(porCategoria(l, "Carnes y aves").get("nombreAlimento").asText()).isEqualTo("Pechuga de pollo, a la plancha");
    }

    @Test
    @DisplayName("la respuesta de añadir, en inglés, también")
    void anadir_en_ingles() throws Exception {
        Alimento huevo = crearAlimentoCatalogo();
        huevo.setNombre("Huevo kznombre");
        huevo.setNombreEn("Egg kznombre");
        huevo.setCategoria("Huevos");
        huevo.setCategoriaEn("Eggs");
        alimentoRepository.save(huevo);
        String cuerpo = mockMvc.perform(post("/comidas/anadir").contentType("application/json")
                        .header("Authorization", "Bearer " + token(owner)).header("Accept-Language", "en")
                        .content("{\"fecha\":\"2026-10-02\",\"tipoComida\":\"CENA\",\"alimentoId\":" + huevo.getId()
                                + ",\"cantidadGramos\":50}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode linea = objectMapper.readTree(cuerpo).get("linea");
        assertThat(linea.get("nombreAlimento").asText()).isEqualTo("Egg kznombre");
        assertThat(linea.get("categoriaAlimento").asText()).isEqualTo("Huevos");
    }
}
