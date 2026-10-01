package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.dto.entity.productooff.ProductoOffImportDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.service.busqueda.IndiceAlimentos;
import com.gymprofit.api.service.externo.OpenFoodFactsClient;
import com.gymprofit.api.service.productooff.ProductoOffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// BusquedaAlimentosTest — GET /alimentos/buscar busca solo en lo nuestro (GP-162)
// Contra la base de verdad: los básicos de la migración, unos productos importados
// por el servicio de la importación y lo que siembra cada test. Ninguna búsqueda sale
// a Open Food Facts (el cliente está simulado y se comprueba que nadie lo llama).
// ============================================================
class BusquedaAlimentosTest extends AbstractOwnershipTest {

    @Autowired
    private ProductoOffService productoOffService;

    @Autowired
    private IndiceAlimentos indice;

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private OpenFoodFactsClient openFoodFactsClient;

    @BeforeEach
    void productos() {
        // Productos que compiten con los básicos: muy escaneados y con las mismas palabras.
        productoOffService.importarLote(List.of(
                producto("8400000900011", "Arroz redondo extra", "Marca Arroz", 360, 7, 79, 1, 5000),
                producto("8400000900028", "Pechuga de pollo asada en lonchas", "Marca Pollo", 110, 22, 1, 2, 4000),
                producto("8400000900035", "Huevos camperos", "Granja", 140, 13, 0.7, 9.7, 3000),
                producto("8400000900042", "Yogur natural cremoso", "Lacteosdelnorte", 70, 4, 5, 3, 100),
                producto("8400000900059", "Leche entera", "Lactitest uno", 64, 3.2, 4.7, 3.6, 900),
                producto("8400000900066", "Leche semidesnatada", "Lactitest dos", 46, 3.2, 4.7, 1.6, 800),
                producto("8400000900073", "Leche desnatada", "Lactitest tres", 35, 3.3, 4.8, 0.2, 700),
                producto("8400000900080", "Leche sin lactosa", "Lactitest cuatro", 46, 3.2, 4.7, 1.6, 600),
                producto("8400000900097", "Leche de cabra", "Lactitest cinco", 65, 3.2, 4.4, 3.8, 500)));
        indice.reconstruirProductos();
    }

    @ParameterizedTest(name = "«{0}» da primero un básico")
    @CsvSource({"arroz, Arroz", "pollo, pollo", "huevo, Huevo"})
    @DisplayName("sin nada tuyo, «arroz», «pollo» y «huevo» dan primero el básico, no un producto")
    void basico_primero(String q, String contiene) throws Exception {
        JsonNode primero = buscar(owner, q).get("content").get(0);
        assertThat(primero.get("grupo").asText()).isEqualTo("BASICO");
        assertThat(primero.get("revisado").asBoolean()).isTrue();
        assertThat(primero.get("nombre").asText()).containsIgnoringCase(contiene);
    }

    @ParameterizedTest(name = "«{0}» da «{1}»")
    @CsvSource({"platano, Plátano", "PLÁTANO, Plátano", "huevos, Huevo", "melocoton, Melocotón", "MELOCOTÓN, Melocotón"})
    @DisplayName("da igual tildes, mayúsculas, singular y plural")
    void tildes_mayusculas_plurales(String q, String esperado) throws Exception {
        assertThat(buscar(owner, q).get("content").get(0).get("nombre").asText()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("«pechga» da una pechuga: una errata en una palabra de 5 letras o más")
    void errata() throws Exception {
        assertThat(buscar(owner, "pechga").get("content").get(0).get("nombre").asText()).contains("Pechuga");
    }

    @Test
    @DisplayName("«pollo pechuga» da «Pechuga de pollo»: da igual el orden y el «de»")
    void orden_de_las_palabras() throws Exception {
        assertThat(buscar(owner, "pollo pechuga").get("content").get(0).get("nombre").asText())
                .startsWith("Pechuga de pollo");
    }

    @Test
    @DisplayName("un producto se encuentra por su marca, sin id y con su código para importarlo")
    void por_marca() throws Exception {
        JsonNode primero = buscar(owner, "lacteosdelnorte").get("content").get(0);
        assertThat(primero.get("grupo").asText()).isEqualTo("PRODUCTO");
        assertThat(primero.get("id").isNull()).isTrue();
        assertThat(primero.get("barcode").asText()).isEqualTo("8400000900042");
        assertThat(primero.get("nombre").asText()).isEqualTo("Yogur natural cremoso");
    }

    @Test
    @DisplayName("lo propio de otro usuario nunca sale; lo tuyo sale primero (DEC-027)")
    void lo_de_otro_no_sale() throws Exception {
        Alimento ajeno = alimentoDe(attacker, "Arroz zxqwvk de la abuela");
        Alimento mio = alimentoDe(owner, "Arroz con verduras de casa");

        List<Integer> ids = new ArrayList<>();
        JsonNode pagina = buscar(owner, "arroz");
        pagina.get("content").forEach(n -> ids.add(n.get("id").isNull() ? -1 : n.get("id").asInt()));
        assertThat(ids).doesNotContain(ajeno.getId());
        assertThat(pagina.get("content").get(0).get("id").asInt()).isEqualTo(mio.getId());
        assertThat(pagina.get("content").get(0).get("grupo").asText()).isEqualTo("TUYO");

        assertThat(buscar(owner, "zxqwvk").get("content")).isEmpty();
        assertThat(buscar(guest, "zxqwvk").get("content")).isEmpty();
        // Su dueño sí lo encuentra.
        assertThat(buscar(attacker, "zxqwvk").get("content").get(0).get("id").asInt()).isEqualTo(ajeno.getId());
    }

    @Test
    @DisplayName("lo apuntado en los últimos 60 días es TUYO y sale antes que los básicos")
    void apuntado_reciente() throws Exception {
        Integer platano = jdbc.queryForObject(
                "SELECT id FROM alimentos WHERE fuente = 'CIQUAL' AND codigo_origen = '13005'", Integer.class);
        apuntar(owner, platano, LocalDateTime.now().minusDays(2));

        JsonNode primero = buscar(owner, "pla").get("content").get(0);
        assertThat(primero.get("grupo").asText()).isEqualTo("TUYO");
        assertThat(primero.get("id").asInt()).isEqualTo(platano);
        // A quien no lo ha apuntado le sale como básico.
        assertThat(buscar(attacker, "platano").get("content").get(0).get("grupo").asText()).isEqualTo("BASICO");
    }

    @Test
    @DisplayName("lo apuntado hace más de 60 días ya no es TUYO")
    void apuntado_antiguo() throws Exception {
        Integer platano = jdbc.queryForObject(
                "SELECT id FROM alimentos WHERE fuente = 'CIQUAL' AND codigo_origen = '13005'", Integer.class);
        apuntar(owner, platano, LocalDateTime.now().minusDays(61));
        assertThat(buscar(owner, "platano").get("content").get(0).get("grupo").asText()).isEqualTo("BASICO");
    }

    @Test
    @DisplayName("sin texto: lo tuyo por uso reciente y después los básicos habituales, sin productos")
    void sin_texto() throws Exception {
        JsonNode vacia = buscar(owner, "");
        assertThat(vacia.get("content").get(0).get("nombre").asText()).isEqualTo("Huevo");
        vacia.get("content").forEach(n -> assertThat(n.get("grupo").asText()).isEqualTo("BASICO"));

        Alimento viejo = alimentoDe(owner, "Bizcocho de casa");
        Alimento usado = alimentoDe(owner, "Gazpacho de casa");
        apuntar(owner, usado.getId(), LocalDateTime.now().minusDays(1));
        JsonNode content = buscar(owner, null).get("content");
        assertThat(content.get(0).get("id").asInt()).isEqualTo(usado.getId());
        assertThat(content.get(1).get("id").asInt()).isEqualTo(viejo.getId());
        assertThat(content.get(2).get("grupo").asText()).isEqualTo("BASICO");
    }

    @Test
    @DisplayName("sin repetidos entre páginas, y el total cuadra con lo recorrido")
    void paginas_sin_repetidos() throws Exception {
        Set<String> vistos = new HashSet<>();
        int total = -1;
        int recorridos = 0;
        for (int pagina = 0; pagina < 50; pagina++) {
            JsonNode respuesta = json(mockMvc.perform(get("/alimentos/buscar")
                    .param("q", "lactitest").param("page", String.valueOf(pagina)).param("size", "2")
                    .header("Authorization", bearer(owner))));
            total = respuesta.get("totalElements").asInt();
            for (JsonNode n : respuesta.get("content")) {
                String clave = n.get("id").isNull() ? "P" + n.get("barcode").asText() : "A" + n.get("id").asInt();
                assertThat(vistos.add(clave)).as("repetido: " + clave).isTrue();
                recorridos++;
            }
            if (respuesta.get("last").asBoolean()) break;
        }
        assertThat(recorridos).isEqualTo(total).isEqualTo(5);
    }

    @Test
    @DisplayName("con más productos que una página, cada página sigue el orden y el total cuenta todos")
    void muchas_paginas_en_orden() throws Exception {
        List<ProductoOffImportDTO> muchos = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            muchos.add(producto(String.valueOf(8400000950000L + i), "Galleta pqzmuchas " + i, "Marca",
                    458, 7, 70, 16, 1000 - i * 10));
        }
        productoOffService.importarLote(muchos);
        indice.reconstruirProductos();

        List<String> recorridos = new ArrayList<>();
        for (int pagina = 0; pagina < 10; pagina++) {
            JsonNode respuesta = json(mockMvc.perform(get("/alimentos/buscar")
                    .param("q", "pqzmuchas").param("page", String.valueOf(pagina)).param("size", "7")
                    .header("Authorization", bearer(owner))));
            assertThat(respuesta.get("totalElements").asInt()).isEqualTo(30);
            respuesta.get("content").forEach(n -> recorridos.add(n.get("barcode").asText()));
            if (respuesta.get("last").asBoolean()) break;
        }
        List<String> esperados = new ArrayList<>();
        for (int i = 0; i < 30; i++) esperados.add(String.valueOf(8400000950000L + i)); // más escaneados primero
        assertThat(recorridos).isEqualTo(esperados);
    }

    @Test
    @DisplayName("la categoría sigue filtrando, y con ella no salen productos")
    void categoria() throws Exception {
        JsonNode content = json(mockMvc.perform(get("/alimentos/buscar")
                .param("q", "pollo").param("categoria", "Carnes y aves")
                .header("Authorization", bearer(owner)))).get("content");
        assertThat(content).isNotEmpty();
        content.forEach(n -> {
            assertThat(n.get("categoria").asText()).isEqualTo("Carnes y aves");
            assertThat(n.get("grupo").asText()).isNotEqualTo("PRODUCTO");
        });
    }

    @Test
    @DisplayName("en inglés, busca y nombra en inglés")
    void ingles() throws Exception {
        JsonNode primero = json(mockMvc.perform(get("/alimentos/buscar").param("q", "banana")
                .header("Accept-Language", "en").header("Authorization", bearer(owner)))).get("content").get(0);
        assertThat(primero.get("nombre").asText()).isEqualTo("Banana");
        assertThat(primero.get("raciones").get(0).get("nombre").asText()).isEqualTo("1 medium banana");
    }

    @Test
    @DisplayName("ninguna búsqueda sale a Open Food Facts")
    void nunca_sale_a_open_food_facts() throws Exception {
        buscar(owner, "cocacola");
        buscar(owner, "");
        buscar(owner, "producto que no existe en ninguna parte");
        verify(openFoodFactsClient, never()).porBarcode(anyString());
    }

    // --- Andamiaje ----------------------------------------------------------

    private JsonNode buscar(Usuario quien, String q) throws Exception {
        var peticion = get("/alimentos/buscar").header("Authorization", bearer(quien));
        if (q != null) peticion.param("q", q);
        return json(mockMvc.perform(peticion));
    }

    private JsonNode json(org.springframework.test.web.servlet.ResultActions acciones) throws Exception {
        String cuerpo = acciones.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(cuerpo);
    }

    private String bearer(Usuario quien) {
        return "Bearer " + token(quien);
    }

    private Alimento alimentoDe(Usuario dueno, String nombre) {
        Alimento a = new Alimento();
        a.setNombre(nombre);
        a.setCalorias(150);
        a.setCategoria("Otro");
        a.setActivo(true);
        a.setUsuario(dueno);
        return alimentoRepository.saveAndFlush(a);
    }

    private void apuntar(Usuario quien, Integer alimentoId, LocalDateTime cuando) {
        jdbc.update("INSERT INTO comidas (usuario_id, fecha, tipo_comida) VALUES (?, ?, 'COMIDA')",
                quien.getId(), Timestamp.valueOf(cuando));
        Integer comida = jdbc.queryForObject("SELECT MAX(id) FROM comidas WHERE usuario_id = ?", Integer.class, quien.getId());
        jdbc.update("INSERT INTO alimentos_comida (comida_id, alimento_id, cantidad_gramos, calorias_totales) VALUES (?, ?, 100, 90)",
                comida, alimentoId);
    }

    private static ProductoOffImportDTO producto(String codigo, String nombre, String marca, double kcal,
                                                 double p, double c, double g, int escaneos) {
        return new ProductoOffImportDTO(codigo, nombre, marca, kcal, p, c, g, null, null, null, null, escaneos, null, null);
    }
}
