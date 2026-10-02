package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.dto.entity.productooff.ProductoOffImportDTO;
import com.gymprofit.api.pruebas.ContadorSentencias;
import com.gymprofit.api.service.busqueda.IndiceAlimentos;
import com.gymprofit.api.service.productooff.ProductoOffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// SentenciasBusquedaTest — cuántas sentencias SQL cuesta una búsqueda (GP-168)
// En Render cada sentencia es un viaje a Aiven. El número no puede crecer con los
// resultados: una página de 30 cuesta lo mismo que una de 1.
// ============================================================
@Import(ContadorSentencias.class)
class SentenciasBusquedaTest extends AbstractOwnershipTest {

    @Autowired
    private ProductoOffService productoOffService;

    @Autowired
    private IndiceAlimentos indice;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private javax.sql.DataSource dataSource;

    @BeforeEach
    void productos() {
        List<ProductoOffImportDTO> lote = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            lote.add(new ProductoOffImportDTO(String.valueOf(8400000980000L + i), "Pollo kzsentencias " + i,
                    "Marca", 458.0, 7.0, 70.0, 16.0, null, 30.0, "30 g", null, 100 - i, null, null));
        }
        productoOffService.importarLote(lote);
        indice.reconstruirProductos();
    }

    @Test
    @DisplayName("las sentencias de una búsqueda no crecen con los resultados: 30 cuestan lo mismo que 3")
    void no_crecen_con_los_resultados() throws Exception {
        buscar("pollo", 30); // la primera petición del contexto carga cosas que no son de la búsqueda

        // «pollo» trae básicos (con raciones) y productos: el usuario del token y sus roles
        // (2), lo tuyo en una (1), los productos de la página (1) y sus alimentos con sus
        // raciones en otra (1). Eran 7 hasta la 1.6.1: lo tuyo eran dos y las raciones, aparte.
        assertThat(contar("pollo", 30)).isEqualTo(5);
        // Con 3, todos básicos: una menos, la de productos. Nunca una por resultado.
        assertThat(contar("pollo", 3)).isEqualTo(4);
        // Vacía: los 31 habituales con sus raciones, sin productos.
        assertThat(contar("", 31)).isEqualTo(4);
        // Solo productos sin materializar: ni alimentos ni raciones.
        assertThat(contar("kzsentencias", 30)).isEqualTo(4);
        assertThat(contar("aceite", 30)).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("lote 1.6.3: con lo tuyo apuntado, por raciones, y con favoritos, las mismas sentencias")
    void ultima_y_favorito_sin_viajes_nuevos() throws Exception {
        buscar("pollo", 30);
        org.springframework.jdbc.core.JdbcTemplate jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        // Un alimento propio con su ración, apuntado; y dos básicos de pollo apuntados y favoritos.
        jdbc.update("INSERT INTO alimentos (nombre, calorias, usuario_id, activo, categoria) "
                + "VALUES ('Pollo kzsentencias de casa', 150, ?, 1, 'Otro')", owner.getId());
        Integer propio = jdbc.queryForObject("SELECT MAX(id) FROM alimentos WHERE usuario_id = ?", Integer.class, owner.getId());
        jdbc.update("INSERT INTO alimento_raciones (alimento_id, nombre, nombre_en, gramos, fuente, orden) "
                + "VALUES (?, '1 ración', '1 serving', 120, 'prueba', 1)", propio);
        Integer racion = jdbc.queryForObject("SELECT MAX(id) FROM alimento_raciones WHERE alimento_id = ?", Integer.class, propio);
        jdbc.update("INSERT INTO comidas (usuario_id, fecha, tipo_comida) VALUES (?, NOW(), 'COMIDA')", owner.getId());
        Integer comida = jdbc.queryForObject("SELECT MAX(id) FROM comidas WHERE usuario_id = ?", Integer.class, owner.getId());
        jdbc.update("INSERT INTO alimentos_comida (comida_id, alimento_id, cantidad_gramos, racion_id, raciones) "
                + "VALUES (?, ?, 120, ?, 1)", comida, propio, racion);
        List<Integer> basicos = jdbc.queryForList("SELECT id FROM alimentos WHERE usuario_id IS NULL AND fuente = 'CIQUAL' "
                + "AND nombre LIKE '%pollo%' ORDER BY id LIMIT 2", Integer.class);
        for (Integer b : basicos) {
            jdbc.update("INSERT INTO alimentos_comida (comida_id, alimento_id, cantidad_gramos) VALUES (?, ?, 100)", comida, b);
            jdbc.update("INSERT INTO favoritos (usuario_id, alimento_id, creado) VALUES (?, ?, NOW())", owner.getId(), b);
        }

        // Las mismas que sin nada tuyo: lo tuyo, sus favoritos y sus últimas líneas van en la misma consulta.
        assertThat(contar("pollo", 30)).isEqualTo(5);
        assertThat(contar("", 31)).isEqualTo(4);
    }

    // Cada búsqueda con la sesión de JPA limpia, como una petición de verdad: si no, lo
    // que cargó la anterior dentro de la transacción del test no se vuelve a pedir.
    private int contar(String q, int size) throws Exception {
        entityManager.flush();
        entityManager.clear();
        ContadorSentencias.empezar();
        int n = buscar(q, size);
        List<String> vistas = ContadorSentencias.sentencias();
        System.out.printf("BUSQUEDA «%s», size %d: %d resultados, %d sentencias%n", q, size, n, vistas.size());
        vistas.forEach(s -> System.out.println("SQL| " + s.replaceAll("\\s+", " ").substring(0, Math.min(140, s.length()))));
        return vistas.size();
    }

    private int buscar(String q, int size) throws Exception {
        String cuerpo = mockMvc.perform(get("/alimentos/buscar").param("q", q).param("size", String.valueOf(size))
                        .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode content = objectMapper.readTree(cuerpo).get("content");
        return content.size();
    }
}
