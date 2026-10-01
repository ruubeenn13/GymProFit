package com.gymprofit.api.service.busqueda;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.dto.entity.productooff.ProductoOffImportDTO;
import com.gymprofit.api.integration.AbstractOwnershipTest;
import com.gymprofit.api.service.externo.LimiteOpenFoodFacts;
import com.gymprofit.api.service.externo.OpenFoodFactsClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// LecturaCodigoIndiceTest — leer un código no reconstruye el índice de productos (GP-160)
// El producto que llega de Open Food Facts se materializa en el acto y entra por el
// índice del catálogo, que es pequeño. Reconstruir el de los ~200 000 productos por
// cada lectura no añadía nada y en Render (0,1 de CPU) se notaba.
// ============================================================
class LecturaCodigoIndiceTest extends AbstractOwnershipTest {

    @Autowired
    private IndiceAlimentos indice;

    @Autowired
    private LimiteOpenFoodFacts limite;

    @MockitoBean
    private OpenFoodFactsClient openFoodFactsClient;

    @BeforeEach
    @AfterEach
    void cupoLimpio() {
        limite.reiniciar();
    }

    @Test
    @DisplayName("leer un código nuevo no programa la reconstrucción, y el producto sale al buscarlo justo después")
    void leer_un_codigo_no_reconstruye_productos() throws Exception {
        // 7·4 + 70·4 + 16·9 + 3·2 = 458 kcal.
        when(openFoodFactsClient.porBarcode("8400000600019")).thenReturn(Optional.of(new ProductoOffImportDTO(
                "8400000600019", "Galletas qwzlecturas", "Marca", 458.0, 7.0, 70.0, 16.0, 3.0,
                null, null, null, 1, null, null)));
        long antes = indice.reconstruccionesProgramadas();

        pedir(owner, "GET /alimentos/codigo/8400000600019").andExpect(status().isOk());

        assertThat(indice.reconstruccionesProgramadas()).isEqualTo(antes);
        String cuerpo = mockMvc.perform(get("/alimentos/buscar").param("q", "qwzlecturas")
                        .header("Authorization", "Bearer " + token(attacker)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode primero = objectMapper.readTree(cuerpo).get("content").get(0);
        assertThat(primero.get("nombre").asText()).isEqualTo("Galletas qwzlecturas");
        assertThat(primero.get("grupo").asText()).isEqualTo("PRODUCTO");
        assertThat(primero.get("id").isNull()).as("ya materializado: lleva id").isFalse();
    }
}
