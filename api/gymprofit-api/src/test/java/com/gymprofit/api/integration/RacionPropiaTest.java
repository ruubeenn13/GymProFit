package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// RacionPropiaTest — un alimento propio con marca y con su ración (lote 1.6.2)
// La ración se elige de una lista cerrada (UNIDAD, RACION, ENVASE, REBANADA), con sus
// gramos (más de 0 y hasta 2000), y solo una por ahora. Solo en alimentos con dueño: las
// del catálogo están revisadas. En el PATCH: sin el campo, nada; vacía, se quita; con una,
// se cambia en su misma fila, para que las líneas de comidas que la usan no la pierdan.
// ============================================================
class RacionPropiaTest extends AbstractOwnershipTest {

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    private static String crear(String raciones) {
        return "{\"nombre\":\"Galletas de casa\",\"marca\":\"Abuela\",\"calorias\":450,\"proteinas\":7,"
                + "\"carbohidratos\":70,\"grasas\":16,\"porcionGramos\":100" + (raciones == null ? "" : ",\"raciones\":" + raciones) + "}";
    }

    private int crearPropio(String raciones) throws Exception {
        String json = pedir(owner, "POST /alimentos", crear(raciones)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        em.flush();
        em.clear();
        return objectMapper.readTree(json).get("id").asInt();
    }

    private JsonNode leer(int id) throws Exception {
        em.flush();
        em.clear();
        return objectMapper.readTree(pedir(owner, "GET /alimentos/" + id).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("crear con marca y ración, y leerla con su clave y su unidad")
    void crear_y_leer() throws Exception {
        int id = crearPropio("[{\"unidad\":\"REBANADA\",\"gramos\":28}]");
        JsonNode a = leer(id);
        assertThat(a.get("marca").asText()).isEqualTo("Abuela");
        JsonNode r = a.get("raciones").get(0);
        assertThat(r.get("nombre").asText()).isEqualTo("1 rebanada");
        assertThat(r.get("gramos").asDouble()).isEqualTo(28.0);
        assertThat(r.get("clave").asText()).isEqualTo("REBANADA");
        assertThat(r.get("unidadPlural").asText()).isEqualTo("rebanadas");
    }

    @Test
    @DisplayName("PATCH: cambiar los gramos conserva el id y la línea que la usaba sigue con ella")
    void cambiar_conserva_id() throws Exception {
        int id = crearPropio("[{\"unidad\":\"UNIDAD\",\"gramos\":30}]");
        int racionId = leer(id).get("raciones").get(0).get("id").asInt();
        pedir(owner, "POST /comidas/anadir", "{\"fecha\":\"2026-10-02\",\"tipoComida\":\"MERIENDA\",\"alimentoId\":" + id
                + ",\"racionId\":" + racionId + ",\"raciones\":2}").andExpect(status().isOk());

        pedir(owner, "PATCH /alimentos/" + id, "{\"raciones\":[{\"unidad\":\"ENVASE\",\"gramos\":45}]}")
                .andExpect(status().isOk());
        JsonNode r = leer(id).get("raciones").get(0);
        assertThat(r.get("id").asInt()).isEqualTo(racionId);
        assertThat(r.get("clave").asText()).isEqualTo("ENVASE");
        assertThat(r.get("gramos").asDouble()).isEqualTo(45.0);
        Integer usada = jdbc.queryForObject("SELECT COUNT(*) FROM alimentos_comida WHERE racion_id = ?", Integer.class, racionId);
        assertThat(usada).isEqualTo(1);
    }

    @Test
    @DisplayName("PATCH sin el campo no toca la ración; con la lista vacía se quita y la línea queda en gramos")
    void quitar() throws Exception {
        int id = crearPropio("[{\"unidad\":\"RACION\",\"gramos\":60}]");
        int racionId = leer(id).get("raciones").get(0).get("id").asInt();
        pedir(owner, "POST /comidas/anadir", "{\"fecha\":\"2026-10-02\",\"tipoComida\":\"MERIENDA\",\"alimentoId\":" + id
                + ",\"racionId\":" + racionId + ",\"raciones\":1}").andExpect(status().isOk());

        pedir(owner, "PATCH /alimentos/" + id, "{\"nombre\":\"Galletas de la abuela\"}").andExpect(status().isOk());
        assertThat(leer(id).get("raciones")).hasSize(1);

        pedir(owner, "PATCH /alimentos/" + id, "{\"raciones\":[]}").andExpect(status().isOk());
        assertThat(leer(id).get("raciones")).isEmpty();
        Integer enGramos = jdbc.queryForObject("SELECT COUNT(*) FROM alimentos_comida ac JOIN comidas c ON c.id = ac.comida_id "
                + "WHERE ac.alimento_id = ? AND ac.racion_id IS NULL AND ac.cantidad_gramos = 60", Integer.class, id);
        assertThat(enGramos).isEqualTo(1);
    }

    @Test
    @DisplayName("PATCH con una ración sobre un alimento sin ración la crea")
    void anadir_por_patch() throws Exception {
        int id = crearPropio(null);
        pedir(owner, "PATCH /alimentos/" + id, "{\"raciones\":[{\"unidad\":\"UNIDAD\",\"gramos\":120}]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raciones[0].clave").value("UNIDAD"));
    }

    @Test
    @DisplayName("400: unidad desconocida, más de una, gramos fuera de rango")
    void datos_400() throws Exception {
        pedir(owner, "POST /alimentos", crear("[{\"unidad\":\"CUCHARON\",\"gramos\":30}]")).andExpect(status().isBadRequest());
        pedir(owner, "POST /alimentos", crear("[{\"unidad\":\"UNIDAD\",\"gramos\":30},{\"unidad\":\"ENVASE\",\"gramos\":60}]"))
                .andExpect(status().isBadRequest());
        pedir(owner, "POST /alimentos", crear("[{\"unidad\":\"UNIDAD\",\"gramos\":0}]")).andExpect(status().isBadRequest());
        pedir(owner, "POST /alimentos", crear("[{\"unidad\":\"UNIDAD\",\"gramos\":2001}]")).andExpect(status().isBadRequest());
        pedir(owner, "POST /alimentos", crear("[{\"unidad\":\"UNIDAD\"}]")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DEC-014: el PATCH con raciones sobre el alimento de otro usuario, 403; el invitado, 403")
    void ajeno() throws Exception {
        int id = crearPropio("[{\"unidad\":\"UNIDAD\",\"gramos\":30}]");
        pedir(attacker, "PATCH /alimentos/" + id, "{\"raciones\":[{\"unidad\":\"UNIDAD\",\"gramos\":90}]}")
                .andExpect(status().isForbidden());
        pedir(guest, "PATCH /alimentos/" + id, "{\"raciones\":[]}").andExpect(status().isForbidden());
        assertThat(leer(id).get("raciones").get(0).get("gramos").asDouble()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("en un alimento del catálogo, 400: sus raciones están revisadas")
    void catalogo_400() throws Exception {
        Alimento catalogo = crearAlimentoCatalogo();
        pedir(crearUsuario("__racion_admin__", com.gymprofit.api.enums.RoleType.ADMIN), "PATCH /alimentos/" + catalogo.getId(),
                "{\"raciones\":[{\"unidad\":\"UNIDAD\",\"gramos\":90}]}").andExpect(status().isBadRequest());
        assertThat(alimentoRepository.findById(catalogo.getId())).isPresent();
    }
}
