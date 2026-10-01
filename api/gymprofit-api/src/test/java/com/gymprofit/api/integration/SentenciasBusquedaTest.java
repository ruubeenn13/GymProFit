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
        // (2), lo tuyo (2), los productos de la página, sus alimentos y sus raciones en un lote.
        assertThat(contar("pollo", 30)).isEqualTo(7);
        // Con 3, todos básicos: una menos, la de productos. Nunca una por resultado.
        assertThat(contar("pollo", 3)).isEqualTo(6);
        // Vacía: los 31 habituales con sus raciones, sin productos.
        assertThat(contar("", 31)).isEqualTo(6);
        // Solo productos sin materializar: ni alimentos ni raciones.
        assertThat(contar("kzsentencias", 30)).isEqualTo(5);
        assertThat(contar("aceite", 30)).isLessThanOrEqualTo(7);
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
