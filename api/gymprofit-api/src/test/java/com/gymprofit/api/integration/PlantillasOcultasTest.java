package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// PlantillasOcultasTest — las builds 1.1.x no ven las plantillas (GP-074)
//
// Las 31 rutinas de los programas son plantillas: sin usuario y NO predefinidas.
// /rutinas/predefinidas alimenta el carrusel de Entrenar y el selector de Registrar
// sesión de las builds repartidas, y ahí no pueden salir. Tampoco en los demás listados
// de /rutinas, ni para ADMIN, y un usuario no puede leerlas por su id: se sirven por
// /programas. Más la retirada de las seis predefinidas del TFG.
// ============================================================
@DisplayName("GP-074 — las plantillas no salen por las rutas de rutinas")
class PlantillasOcultasTest extends AbstractOwnershipTest {

    private static final String RETIRADA = "db/migration/V202609292001__Retirar_predefinidas_tfg.sql";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private Usuario admin;
    private Set<Integer> plantillas;

    @BeforeEach
    void sembrar() {
        CatalogoPlantillasDePrueba.sembrarCompleto(jdbc, dataSource);
        admin = crearUsuario("__admin_plantillas__", RoleType.ADMIN);
        plantillas = new HashSet<>(jdbc.queryForList("SELECT id FROM rutinas WHERE es_plantilla = 1", Integer.class));
        assertThat(plantillas).hasSize(31);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "GET /rutinas/predefinidas",
            "GET /rutinas/predefinidas/nivel/PRINCIPIANTE",
            "GET /rutinas/predefinidas/nivel/INTERMEDIO",
            "GET /rutinas/activas",
            "GET /rutinas/nivel/PRINCIPIANTE",
            "GET /rutinas/nivel/INTERMEDIO",
            "GET /rutinas/nombre/Torso",
            "GET /rutinas/nombre/empezar"})
    @DisplayName("ningún listado de /rutinas enseña una plantilla, ni a un usuario, ni al invitado, ni a ADMIN")
    void ningun_listado_las_ensena(String ruta) throws Exception {
        for (Usuario quien : List.of(owner, guest, admin)) {
            assertThat(idsDe(pedir(quien, ruta).andReturn().getResponse()))
                    .as("%s como %s", ruta, quien.getUsername())
                    .doesNotContainAnyElementsOf(plantillas);
        }
    }

    @Test
    @DisplayName("las rutinas de un usuario no incluyen plantillas")
    void las_de_un_usuario_tampoco() throws Exception {
        String ruta = "GET /rutinas/usuario/" + owner.getId() + "/activas";
        assertThat(idsDe(pedir(owner, ruta).andReturn().getResponse())).doesNotContainAnyElementsOf(plantillas);
    }

    @Test
    @DisplayName("un usuario o el invitado no leen una plantilla por su id: 403")
    void por_id_es_403() throws Exception {
        Integer plantilla = plantillas.iterator().next();
        for (Usuario quien : List.of(owner, guest)) {
            assertThat(estado(quien, "GET /rutinas/" + plantilla)).as(quien.getUsername()).isEqualTo(403);
        }
        assertThat(estado(owner, "GET /rutinas-ejercicios/rutina/" + plantilla)).isEqualTo(403);
    }

    @Test
    @DisplayName("la retirada borra las predefinidas sin sesiones y desactiva las que tienen historial")
    void retirada_de_las_predefinidas() throws Exception {
        Integer ejercicio = crearEjercicioCatalogo().getId();
        jdbc.update("INSERT INTO rutinas (nombre, nivel, es_predefinida, activa) VALUES ('Full Body', 'INTERMEDIO', 1, 1)");
        Integer sinSesiones = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        jdbc.update("INSERT INTO rutina_ejercicio (rutina_id, ejercicio_id) VALUES (?, ?)", sinSesiones, ejercicio);
        jdbc.update("INSERT INTO rutinas (nombre, nivel, es_predefinida, activa) VALUES ('HIIT Avanzado', 'AVANZADO', 1, 1)");
        Integer conSesiones = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        jdbc.update("INSERT INTO sesiones_entrenamiento (usuario_id, rutina_id, fecha_inicio, fecha_fin) VALUES (?, ?, NOW(), NOW())",
                owner.getId(), conSesiones);

        ScriptUtils.executeSqlScript(DataSourceUtils.getConnection(dataSource), new ClassPathResource(RETIRADA));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rutinas WHERE id = ?", Integer.class, sinSesiones)).isZero();
        assertThat(jdbc.queryForMap("SELECT es_predefinida, activa FROM rutinas WHERE id = ?", conSesiones))
                .containsEntry("es_predefinida", false).containsEntry("activa", false);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rutinas WHERE es_predefinida = 1", Integer.class)).isZero();
        assertThat(pedir(owner, "GET /rutinas/predefinidas").andReturn().getResponse().getContentAsString())
                .isEqualTo("[]");
        // La que se queda por su historial no la ve nadie más que ADMIN.
        assertThat(estado(owner, "GET /rutinas/" + conSesiones)).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rutinas WHERE es_plantilla = 1", Integer.class))
                .as("las plantillas no se tocan").isEqualTo(31);
    }

    private Set<Integer> idsDe(org.springframework.mock.web.MockHttpServletResponse respuesta) throws Exception {
        assertThat(respuesta.getStatus()).isEqualTo(200);
        Set<Integer> ids = new HashSet<>();
        for (JsonNode r : objectMapper.readTree(respuesta.getContentAsString())) ids.add(r.get("id").asInt());
        return ids;
    }
}
