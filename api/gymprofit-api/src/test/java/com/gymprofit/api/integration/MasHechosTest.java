package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// MasHechosTest — GET /ejercicios-realizados/mas-hechos (GP-216, lote 2)
//
// «Los que más haces» de la Biblioteca: los ejercicios con más sesiones terminadas, con el
// recuento. Como /diario/mas-apuntados, el usuario sale del token (DEC-013) y no hay id en la
// ruta: lo único que se defiende es el ALCANCE —ni los datos de otra cuenta, ni los de un
// ADMIN, entran en el recuento— y el invitado, que no entra. Con el contexto levantado y JWT
// de verdad (DEC-014).
// ============================================================
@DisplayName("GET /ejercicios-realizados/mas-hechos — los ejercicios que más haces")
class MasHechosTest extends AbstractOwnershipTest {

    @Autowired
    private JdbcTemplate jdbc;

    private Integer press;
    private Integer sentadilla;
    private Integer remo;

    @BeforeEach
    void catalogo() {
        press = crearEjercicioCatalogo().getId();
        sentadilla = crearEjercicioCatalogo().getId();
        remo = crearEjercicioCatalogo().getId();
    }

    private JsonNode masHechos(Usuario quien, String query) throws Exception {
        String json = pedir(quien, "GET /ejercicios-realizados/mas-hechos" + query).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    @Test
    @DisplayName("ordena por sesiones, de más a menos, y cuenta una vez por sesión aunque repita el ejercicio")
    void orden_y_recuento() throws Exception {
        guardar(owner, "2026-09-14T18:00:00", press, sentadilla);
        guardar(owner, "2026-09-16T18:00:00", press);
        guardar(owner, "2026-09-18T18:00:00", press, press, remo);   // el press dos veces en la misma sesión: una
        guardar(owner, "2026-09-20T18:00:00", sentadilla);

        JsonNode r = masHechos(owner, "");

        assertThat(r).hasSize(3);
        assertThat(r.get(0).get("ejercicioId").asInt()).isEqualTo(press);
        assertThat(r.get(0).get("veces").asInt()).isEqualTo(3);
        assertThat(r.get(1).get("ejercicioId").asInt()).isEqualTo(sentadilla);
        assertThat(r.get(1).get("veces").asInt()).isEqualTo(2);
        assertThat(r.get(2).get("ejercicioId").asInt()).isEqualTo(remo);
        assertThat(r.get(2).get("veces").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("un empate se resuelve por id y el límite recorta")
    void empate_y_limite() throws Exception {
        guardar(owner, "2026-09-14T18:00:00", press, sentadilla, remo);

        JsonNode todos = masHechos(owner, "");
        assertThat(todos).hasSize(3);
        assertThat(todos.get(0).get("ejercicioId").asInt()).isLessThan(todos.get(1).get("ejercicioId").asInt());
        assertThat(todos.get(1).get("ejercicioId").asInt()).isLessThan(todos.get(2).get("ejercicioId").asInt());
        assertThat(masHechos(owner, "?limite=2")).hasSize(2);
        assertThat(masHechos(owner, "?limite=1")).hasSize(1);
    }

    @Test
    @DisplayName("las sesiones sin terminar no cuentan; sin nada, [] y no 404")
    void sin_terminar_y_vacio() throws Exception {
        assertThat(masHechos(owner, "")).isEmpty();
        int s = guardar(owner, "2026-09-14T18:00:00", press);
        jdbc.update("UPDATE sesiones_entrenamiento SET completada = FALSE WHERE id = ?", s);

        assertThat(masHechos(owner, "")).isEmpty();
    }

    @Test
    @DisplayName("aislamiento: ni otra cuenta ni un ADMIN cuentan en lo mío, y el invitado no entra")
    void aislamiento() throws Exception {
        Usuario admin = crearUsuario("__mashechos_admin__", RoleType.ADMIN);
        for (int i = 0; i < 3; i++) guardar(attacker, "2026-09-1" + (4 + i) + "T18:00:00", remo);
        guardar(admin, "2026-09-14T18:00:00", remo);
        guardar(owner, "2026-09-15T18:00:00", press);

        JsonNode mio = masHechos(owner, "");
        assertThat(mio).hasSize(1);
        assertThat(mio.get(0).get("ejercicioId").asInt()).isEqualTo(press);
        assertThat(mio.get(0).get("veces").asInt()).isEqualTo(1);
        // Un ADMIN ve lo suyo, no lo de todos.
        JsonNode delAdmin = masHechos(admin, "");
        assertThat(delAdmin).hasSize(1);
        assertThat(delAdmin.get(0).get("veces").asInt()).isEqualTo(1);
        pedir(guest, "GET /ejercicios-realizados/mas-hechos").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("el límite va de 1 a 20")
    void limite() throws Exception {
        pedir(owner, "GET /ejercicios-realizados/mas-hechos?limite=0").andExpect(status().isBadRequest());
        pedir(owner, "GET /ejercicios-realizados/mas-hechos?limite=21").andExpect(status().isBadRequest());
        pedir(owner, "GET /ejercicios-realizados/mas-hechos?limite=20").andExpect(status().isOk());
    }

    // --- Siembra ------------------------------------------------------------

    private int guardar(Usuario quien, String inicio, Integer... ejercicios) throws Exception {
        StringBuilder lista = new StringBuilder();
        for (int i = 0; i < ejercicios.length; i++) {
            if (i > 0) lista.append(',');
            lista.append("{\"ejercicioId\":%d,\"series\":[{\"numero\":1,\"repeticiones\":8,\"peso\":50,\"completada\":true}]}"
                    .formatted(ejercicios[i]));
        }
        String cuerpo = """
                {"claveIdempotencia":"%s","fechaInicio":"%s","duracionMinutos":50,"completada":true,
                 "ejercicios":[%s]}
                """.formatted(UUID.randomUUID(), inicio, lista);
        String json = pedir(quien, "POST /sesiones/completa", cuerpo).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asInt();
    }
}
