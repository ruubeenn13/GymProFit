package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.dto.entity.productooff.ProductoOffImportDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AlimentoRacion;
import com.gymprofit.api.repository.jpa.IAlimentoRacionRepository;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.service.busqueda.IndiceAlimentos;
import com.gymprofit.api.service.externo.LimiteOpenFoodFacts;
import com.gymprofit.api.service.externo.OpenFoodFactsClient;
import com.gymprofit.api.service.productooff.ProductoOffService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// UnidadesEnRespuestasTest — la unidad de la ración llega en singular y en plural (GP-172)
// En la búsqueda, en GET /alimentos/{id}, en GET /alimentos/codigo/{codigo} y en la
// línea de una comida, en el idioma de la petición. nombre y racionNombre no cambian:
// los usan las builds repartidas.
// ============================================================
class UnidadesEnRespuestasTest extends AbstractOwnershipTest {

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @Autowired
    private IAlimentoRacionRepository racionRepository;

    @Autowired
    private ProductoOffService productoOffService;

    @Autowired
    private IndiceAlimentos indice;

    @Autowired
    private LimiteOpenFoodFacts limite;

    @Autowired
    private org.springframework.context.ApplicationEventPublisher eventos;

    @MockitoBean
    private OpenFoodFactsClient openFoodFactsClient;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    private Alimento pan;
    private Integer rebanadaId;

    @BeforeEach
    void sembrar() {
        limite.reiniciar();
        when(openFoodFactsClient.porBarcode(anyString())).thenReturn(Optional.empty());
        pan = crearAlimentoCatalogo();
        pan.setNombre("Pan kzunidad");
        pan.setNombreEn("Bread kzunidad");
        pan.setCategoria("Cereales y pan");
        alimentoRepository.save(pan);
        AlimentoRacion r = new AlimentoRacion();
        r.setAlimento(pan);
        r.setNombre("1 rebanada");
        r.setNombreEn("1 slice");
        r.setGramos(new BigDecimal("28.0"));
        r.setFuente("prueba");
        r.setOrden(1);
        rebanadaId = racionRepository.save(r).getId();
        // Guardado por el repositorio, el índice no se entera solo: sin esto, la búsqueda
        // depende de que ningún test anterior lo haya construido ya.
        eventos.publishEvent(new IndiceAlimentos.CatalogoCambiado());
        em.flush();
        em.clear();
    }

    @AfterEach
    void sinRastro() {
        limite.reiniciar();
    }

    private JsonNode json(MockHttpServletRequestBuilder peticion, String idioma) throws Exception {
        String cuerpo = mockMvc.perform(peticion.header("Authorization", "Bearer " + token(owner))
                        .header("Accept-Language", idioma))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(cuerpo);
    }

    @Test
    @DisplayName("GET /alimentos/{id}: la ración con su unidad, en español y en inglés")
    void por_id() throws Exception {
        JsonNode es = json(get("/alimentos/" + pan.getId()), "es").get("raciones").get(0);
        assertThat(es.get("nombre").asText()).isEqualTo("1 rebanada");
        assertThat(es.get("unidad").asText()).isEqualTo("rebanada");
        assertThat(es.get("unidadPlural").asText()).isEqualTo("rebanadas");
        JsonNode en = json(get("/alimentos/" + pan.getId()), "en").get("raciones").get(0);
        assertThat(en.get("unidad").asText()).isEqualTo("slice");
        assertThat(en.get("unidadPlural").asText()).isEqualTo("slices");
    }

    @Test
    @DisplayName("la búsqueda también, y un producto sin materializar con su envase")
    void busqueda() throws Exception {
        productoOffService.importarLote(List.of(new ProductoOffImportDTO("8400000740012", "Yogur kzunidad",
                "Marca", 60.0, 10.0, 4.0, 0.2, null, null, null, "4 x 125 g", 50, null, null)));
        indice.reconstruirProductos();
        JsonNode content = json(get("/alimentos/buscar").param("q", "kzunidad"), "es").get("content");
        boolean vistoPan = false, vistoYogur = false;
        for (JsonNode a : content) {
            JsonNode r = a.get("raciones").get(0);
            if (a.get("nombre").asText().startsWith("Pan")) {
                assertThat(r.get("unidadPlural").asText()).isEqualTo("rebanadas");
                vistoPan = true;
            } else if (a.get("nombre").asText().startsWith("Yogur")) {
                assertThat(r.get("unidad").asText()).isEqualTo("unidad");
                assertThat(r.get("unidadPlural").asText()).isEqualTo("unidades");
                vistoYogur = true;
            }
        }
        assertThat(vistoPan && vistoYogur).isTrue();
    }

    @Test
    @DisplayName("GET /alimentos/codigo/{codigo}: el envase materializado trae «envase / envases» y «pack / packs»")
    void por_codigo() throws Exception {
        productoOffService.importarLote(List.of(new ProductoOffImportDTO("8400000740029", "Batido kzunidad",
                "Marca", 70.0, 3.0, 10.0, 1.0, null, null, null, "200 ml", 50, null, null)));
        JsonNode es = json(get("/alimentos/codigo/8400000740029"), "es").get("raciones").get(0);
        assertThat(es.get("unidadPlural").asText()).isEqualTo("envases");
        em.flush();
        em.clear();
        JsonNode en = json(get("/alimentos/codigo/8400000740029"), "en").get("raciones").get(0);
        assertThat(en.get("unidadPlural").asText()).isEqualTo("packs");
    }

    @Test
    @DisplayName("la línea de una comida trae la unidad de su ración, en el idioma de la petición")
    void linea_de_comida() throws Exception {
        String anadir = "{\"fecha\":\"2026-10-02\",\"tipoComida\":\"MERIENDA\",\"alimentoId\":" + pan.getId()
                + ",\"racionId\":" + rebanadaId + ",\"raciones\":2}";
        JsonNode es = json(post("/comidas/anadir").contentType("application/json").content(anadir), "es").get("linea");
        assertThat(es.get("racionNombre").asText()).isEqualTo("1 rebanada");
        assertThat(es.get("racionUnidad").asText()).isEqualTo("rebanada");
        assertThat(es.get("racionUnidadPlural").asText()).isEqualTo("rebanadas");
        int comidaId = es.get("comidaId").asInt();
        em.flush();
        em.clear();
        JsonNode en = json(get("/alimentos-comida/comida/" + comidaId), "en").get(0);
        assertThat(en.get("racionUnidad").asText()).isEqualTo("slice");
        assertThat(en.get("racionUnidadPlural").asText()).isEqualTo("slices");
    }
}
