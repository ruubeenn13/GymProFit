package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.dto.entity.productooff.ProductoOffImportDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IProductoOffRepository;
import com.gymprofit.api.service.externo.LimiteOpenFoodFacts;
import com.gymprofit.api.service.externo.OpenFoodFactsClient;
import com.gymprofit.api.service.productooff.ProductoOffService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// CodigoBarrasTest — GET /alimentos/codigo/{codigo} (GP-160)
// Tus alimentos primero; después el catálogo, productos_off y, solo si no, una lectura
// a Open Food Facts (simulado) con cupo para toda la API y memoria de lo que no existe.
// Un alimento propio con código no le sale nunca a otro usuario (DEC-027): el código
// es un id de catálogo, así que aquí se comprueba el aislamiento, no un 403.
// ============================================================
class CodigoBarrasTest extends AbstractOwnershipTest {

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @Autowired
    private IProductoOffRepository productoOffRepository;

    @Autowired
    private ProductoOffService productoOffService;

    @Autowired
    private LimiteOpenFoodFacts limite;

    @MockitoBean
    private OpenFoodFactsClient openFoodFactsClient;

    @BeforeEach
    void cupoLimpio() {
        limite.reiniciar();
        when(openFoodFactsClient.porBarcode(anyString())).thenReturn(Optional.empty());
    }

    @AfterEach
    void sinRastro() {
        limite.reiniciar();
    }

    @Test
    @DisplayName("tu alimento con ese código sale primero, y a otro usuario no le sale nunca")
    void lo_propio_primero_y_aislado() throws Exception {
        Alimento mio = propio(owner, "Tortilla de mi madre", "8400000700016");

        pedir(owner, "GET /alimentos/codigo/8400000700016")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mio.getId()));

        // Al atacante: ni su id, ni su nombre. No hay producto con ese código: 404.
        String cuerpo = pedir(attacker, "GET /alimentos/codigo/8400000700016")
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(cuerpo).doesNotContain("Tortilla de mi madre");

        // Importar mira solo el catálogo, ni siquiera lo del propio dueño.
        pedir(owner, "POST /alimentos/importar", "{\"barcode\":\"8400000700016\"}").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("el invitado no busca por código: puede materializar catálogo y gasta cupo")
    void invitado_403() throws Exception {
        pedir(guest, "GET /alimentos/codigo/8400000700016").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("dos usuarios pueden tener el mismo código en sus alimentos, y cada uno ve el suyo")
    void mismo_codigo_en_dos_usuarios() throws Exception {
        Alimento delOwner = propio(owner, "Lo de owner", "8400000700023");
        Alimento delAtacante = propio(attacker, "Lo de attacker", "8400000700023");
        pedir(owner, "GET /alimentos/codigo/8400000700023").andExpect(jsonPath("$.id").value(delOwner.getId()));
        pedir(attacker, "GET /alimentos/codigo/8400000700023").andExpect(jsonPath("$.id").value(delAtacante.getId()));
    }

    @Test
    @DisplayName("un producto de productos_off se materializa sin preguntar a Open Food Facts")
    void desde_productos_off() throws Exception {
        productoOffService.importarLote(List.of(galletas("8400000700030")));

        Integer id = idDe(pedir(owner, "GET /alimentos/codigo/8400000700030")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuente").value("OFF"))
                .andExpect(jsonPath("$.usuarioId").doesNotExist())
                // Lote 1.6.1: el envase como primera ración, con su id para elegirla.
                .andExpect(jsonPath("$.raciones[0].nombre").value("1 envase"))
                .andExpect(jsonPath("$.raciones[0].gramos").value(500.0))
                .andExpect(jsonPath("$.raciones[0].id").isNumber())
                .andExpect(jsonPath("$.raciones[1].nombre").value("1 ración"))
                .andExpect(jsonPath("$.raciones[1].gramos").value(30.0))
                .andReturn());
        assertThat(alimentoRepository.findById(id).orElseThrow().getUsuario()).isNull();

        // Ya importado: el mismo, también por POST /alimentos/importar.
        pedir(attacker, "GET /alimentos/codigo/8400000700030").andExpect(jsonPath("$.id").value(id));
        pedir(attacker, "POST /alimentos/importar", "{\"barcode\":\"8400000700030\"}").andExpect(jsonPath("$.id").value(id));
        verify(openFoodFactsClient, never()).porBarcode(anyString());
    }

    @Test
    @DisplayName("si no lo tenemos, una lectura a Open Food Facts que se guarda; la segunda vez ya no sale")
    void lectura_que_se_guarda() throws Exception {
        when(openFoodFactsClient.porBarcode("8400000700047")).thenReturn(Optional.of(galletas("8400000700047")));

        Integer id = idDe(pedir(owner, "GET /alimentos/codigo/8400000700047").andExpect(status().isOk()).andReturn());
        assertThat(productoOffRepository.findByCodigo("8400000700047")).isPresent();

        pedir(attacker, "GET /alimentos/codigo/8400000700047").andExpect(jsonPath("$.id").value(id));
        verify(openFoodFactsClient, times(1)).porBarcode(eq("8400000700047"));
    }

    @Test
    @DisplayName("un código que no existe da 404, y no se vuelve a preguntar en un día")
    void no_existe_y_se_recuerda() throws Exception {
        pedir(owner, "GET /alimentos/codigo/8400000700054").andExpect(status().isNotFound());
        pedir(attacker, "GET /alimentos/codigo/8400000700054").andExpect(status().isNotFound());
        verify(openFoodFactsClient, times(1)).porBarcode(eq("8400000700054"));
    }

    @Test
    @DisplayName("un producto de Open Food Facts sin los cuatro valores es como si no existiera")
    void producto_incompleto_404() throws Exception {
        ProductoOffImportDTO sinGrasas = galletas("8400000700061");
        sinGrasas.setGrasas(null);
        when(openFoodFactsClient.porBarcode("8400000700061")).thenReturn(Optional.of(sinGrasas));
        pedir(owner, "GET /alimentos/codigo/8400000700061").andExpect(status().isNotFound());
        assertThat(productoOffRepository.findByCodigo("8400000700061")).isEmpty();
    }

    @Test
    @DisplayName("10 lecturas por minuto para toda la API; la undécima, 503 con Retry-After")
    void cupo_503() throws Exception {
        // Cuatro cuentas: con 3 por cuenta y minuto (GP-167), dos no llegan a 10.
        Usuario[] quienes = {owner, attacker, crearUsuario("__off_c__", RoleType.USER),
                crearUsuario("__off_d__", RoleType.USER)};
        for (int i = 0; i < 10; i++) {
            pedir(quienes[i % 4],"GET /alimentos/codigo/84000008000" + String.format("%02d", i))
                    .andExpect(status().isNotFound());
        }
        MvcResult saturado = pedir(owner, "GET /alimentos/codigo/8400000800099")
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().exists("Retry-After"))
                .andReturn();
        int segundos = Integer.parseInt(saturado.getResponse().getHeader("Retry-After"));
        assertThat(segundos).isBetween(1, 60);
        verify(openFoodFactsClient, never()).porBarcode(eq("8400000800099"));

        // Lo que ya tenemos sigue saliendo sin cupo.
        productoOffService.importarLote(List.of(galletas("8400000700078")));
        pedir(owner, "GET /alimentos/codigo/8400000700078").andExpect(status().isOk());
    }

    @Test
    @DisplayName("GP-167: 3 lecturas nuevas por minuto y por cuenta; la cuarta, 503, y otra cuenta sigue")
    void cupo_por_cuenta_503() throws Exception {
        for (int i = 0; i < 3; i++) {
            pedir(owner, "GET /alimentos/codigo/84000008100" + i).andExpect(status().isNotFound());
        }
        MvcResult saturado = pedir(owner, "GET /alimentos/codigo/8400000810099")
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().exists("Retry-After"))
                .andReturn();
        assertThat(Integer.parseInt(saturado.getResponse().getHeader("Retry-After"))).isBetween(1, 60);
        verify(openFoodFactsClient, never()).porBarcode(eq("8400000810099"));

        // La otra cuenta no paga lo de la primera.
        pedir(attacker, "GET /alimentos/codigo/8400000810099").andExpect(status().isNotFound());
        verify(openFoodFactsClient).porBarcode(eq("8400000810099"));

        // Y lo que ya tenemos sale sin cupo, también para la cuenta sin él.
        productoOffService.importarLote(List.of(galletas("8400000810088")));
        pedir(owner, "GET /alimentos/codigo/8400000810088").andExpect(status().isOk());
    }

    @Test
    @DisplayName("un código con algo que no sean cifras es un 400, sin preguntar a nadie")
    void codigo_invalido_400() throws Exception {
        pedir(owner, "GET /alimentos/codigo/84ab00").andExpect(status().isBadRequest());
        verify(openFoodFactsClient, never()).porBarcode(anyString());
    }

    // --- Andamiaje ----------------------------------------------------------

    private Alimento propio(Usuario dueno, String nombre, String codigo) {
        Alimento a = new Alimento();
        a.setNombre(nombre);
        a.setCalorias(200);
        a.setActivo(true);
        a.setBarcode(codigo);
        a.setUsuario(dueno);
        return alimentoRepository.saveAndFlush(a);
    }

    private Integer idDe(MvcResult resultado) throws Exception {
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return json.get("id").asInt();
    }

    // 7·4 + 70·4 + 16·9 + 3·2 = 458 kcal.
    private static ProductoOffImportDTO galletas(String codigo) {
        return new ProductoOffImportDTO(codigo, "Galletas de prueba", "Marca", 458.0, 7.0, 70.0, 16.0, 3.0,
                30.0, "30 g", "500 g", 10, null, null);
    }
}
