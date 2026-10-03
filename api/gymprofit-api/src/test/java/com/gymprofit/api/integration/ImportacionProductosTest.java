package com.gymprofit.api.integration;

import com.gymprofit.api.config.security.ClaveImportacion;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IProductoOffRepository;
import com.gymprofit.api.service.externo.OpenFoodFactsClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// ImportacionProductosTest — la ruta de la importación semanal (GP-164, DEC-041)
// Sin la clave propia, 403 para todos: invitado, USER y también ADMIN, porque la
// cerradura no es el rol. Con ella, los lotes se validan, se deduplican y reimportar
// actualiza el producto pero no el alimento que alguien ya eligió.
// ============================================================
class ImportacionProductosTest extends AbstractOwnershipTest {

    private static final String RUTA = "/importacion/productos";

    @Value("${app.importacion.clave}")
    private String clave;

    @Autowired
    private IProductoOffRepository productoOffRepository;

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    // Elegir un producto de la tabla no puede salir a Open Food Facts.
    @MockitoBean
    private OpenFoodFactsClient openFoodFactsClient;

    @Test
    @DisplayName("sin la clave, ni el invitado, ni un USER, ni un ADMIN pueden importar")
    void sin_clave_403_para_todos() throws Exception {
        Usuario admin = crearUsuario("__import_admin__", RoleType.ADMIN);
        for (Usuario quien : List.of(guest, attacker, admin)) {
            pedir(quien, "POST " + RUTA, lote(producto("8400000000011", "Galletas", 450))).andExpect(status().isForbidden());
            pedir(quien, "POST " + RUTA + "/fin").andExpect(status().isForbidden());
        }
        assertThat(productoOffRepository.findByCodigo("8400000000011")).isEmpty();
    }

    @Test
    @DisplayName("con una clave equivocada o sin token ni clave, también 403")
    void clave_equivocada_403() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(lote(producto("8400000000011", "Galletas", 450))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .header(ClaveImportacion.CABECERA, clave + "x")
                        .content(lote(producto("8400000000011", "Galletas", 450))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("con la clave, guarda los válidos, descarta los imposibles y cuenta cada cosa")
    void guarda_validos_y_descarta_imposibles() throws Exception {
        List<Map<String, Object>> productos = List.of(
                producto("8400000000011", "Galletas de avena", 450),
                producto("8400000000028", "Kcal que no cuadran", 50),      // 4·P+4·C+9·G ≈ 450
                producto("ABC", "Código que no es un código", 450),
                Map.of("codigo", "8400000000035", "nombre", "Sin macros", "kcal", 100));

        importar(productos)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recibidos").value(4))
                .andExpect(jsonPath("$.guardados").value(1))
                .andExpect(jsonPath("$.descartados").value(3));

        assertThat(productoOffRepository.findByCodigo("8400000000011")).isPresent();
        assertThat(productoOffRepository.findByCodigo("8400000000028")).isEmpty();
    }

    @Test
    @DisplayName("un lote vacío o de más de 2000 productos es un 400")
    void lote_fuera_de_tamano_400() throws Exception {
        importar(List.of()).andExpect(status().isBadRequest());
        List<Map<String, Object>> enorme = new ArrayList<>();
        for (int i = 0; i < 2001; i++) enorme.add(producto(String.valueOf(8400000100000L + i), "P" + i, 450));
        importar(enorme).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("reimportar actualiza el producto, pero no el alimento que alguien ya eligió")
    void reimportar_no_toca_lo_materializado() throws Exception {
        importar(List.of(producto("8400000000042", "Nombre de la semana 1", 450))).andExpect(status().isOk());

        // Un USER lo elige: se materializa desde la tabla, sin preguntar a Open Food Facts.
        String json = pedir(owner, "POST /alimentos/importar", "{\"barcode\":\"8400000000042\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre de la semana 1"))
                .andExpect(jsonPath("$.fuente").value("OFF"))
                .andExpect(jsonPath("$.revisado").value(false))
                // La ración declarada de 30 g primero, el envase de 500 g después (GP-182).
                .andExpect(jsonPath("$.raciones[0].nombre").value("1 ración"))
                .andExpect(jsonPath("$.raciones[0].gramos").value(30.0))
                .andExpect(jsonPath("$.raciones[1].gramos").value(500.0))
                .andReturn().getResponse().getContentAsString();
        Integer id = objectMapper.readTree(json).get("id").asInt();
        verify(openFoodFactsClient, never()).porBarcode(anyString());

        importar(List.of(producto("8400000000042", "Nombre de la semana 2", 450))).andExpect(status().isOk());

        // La importación escribe por JDBC: se lee de la base, no de la caché de JPA.
        assertThat(jdbc.queryForObject("SELECT nombre FROM productos_off WHERE codigo = '8400000000042'", String.class))
                .isEqualTo("Nombre de la semana 2");
        Alimento alimento = alimentoRepository.findById(id).orElseThrow();
        assertThat(alimento.getNombre()).isEqualTo("Nombre de la semana 1");
        assertThat(alimento.getUsuario()).as("un producto es catálogo, no la comida de quien lo elige").isNull();

        // Elegirlo otra vez devuelve la misma fila.
        pedir(attacker, "POST /alimentos/importar", "{\"barcode\":\"8400000000042\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    // --- Andamiaje ----------------------------------------------------------

    private ResultActions importar(List<Map<String, Object>> productos) throws Exception {
        return mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                .header(ClaveImportacion.CABECERA, clave)
                .content(objectMapper.writeValueAsString(productos)));
    }

    private String lote(Map<String, Object> producto) throws Exception {
        return objectMapper.writeValueAsString(List.of(producto));
    }

    // Unas galletas: 7 g de proteína, 70 de hidratos y 16 de grasa ≈ 452 kcal.
    static Map<String, Object> producto(String codigo, String nombre, double kcal) {
        Map<String, Object> p = new java.util.HashMap<>();
        p.put("codigo", codigo);
        p.put("nombre", nombre);
        p.put("marca", "Marca de prueba");
        p.put("kcal", kcal);
        p.put("proteinas", 7.0);
        p.put("carbohidratos", 70.0);
        p.put("grasas", 16.0);
        p.put("fibra", 3.0);
        p.put("racionGramos", 30.0);
        p.put("racionTexto", "30 g");
        p.put("envase", "500 g");
        p.put("escaneos", 10);
        return p;
    }
}
