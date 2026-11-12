package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.service.rutina.IRutinaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// EstiramientosPorTiempoTest — los estiramientos estáticos y el rodillo van en segundos (GP-262)
//
// La migración V202611122130 marca en SEGUNDOS, por fed_id, los estiramientos que se
// mantienen y la liberación con rodillo; los que se hacen moviéndose (dinámicos, cruz de
// hierro, alrededor del mundo...) y Superman, Bird Dog y Dead bug siguen en repeticiones.
// Y lo que importa en la app: añadido a una rutina por una build que no manda la medida,
// el estático queda en segundos y el dinámico no. Contexto completo, sin servicio simulado.
// ============================================================
@DisplayName("GP-262 — estiramientos estáticos y rodillo en segundos")
class EstiramientosPorTiempoTest extends AbstractOwnershipTest {

    private static final String MIGRACION = "db/migration/V202611122130__Estiramientos_por_tiempo.sql";

    private static final List<String> ESTATICOS = List.of("Quad_Stretch", "Cat_Stretch", "Worlds_Greatest_Stretch",
            "Standing_Gastrocnemius_Calf_Stretch", "Calves-SMR", "Iliotibial_Tract-SMR");
    private static final List<String> SIGUEN = List.of("Dynamic_Chest_Stretch", "Dynamic_Back_Stretch",
            "Iron_Crosses_stretch", "Round_The_World_Shoulder_Stretch", "Spinal_Stretch", "Superman", "Bird_Dog",
            "Dead_Bug", "Arm_Circles");

    @Autowired
    private IRutinaService rutinaService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private Integer rutinaId;

    @BeforeEach
    void sembrar() {
        runAs(owner, () -> {
            RutinaCreateDTO r = new RutinaCreateDTO();
            r.setUsuarioId(owner.getId());
            r.setNombre("Movilidad");
            r.setNivel("PRINCIPIANTE");
            r.setEsPredefinida(false);
            rutinaId = rutinaService.save(r).getId();
        });
        ESTATICOS.forEach(this::filaEnRepeticiones);
        SIGUEN.forEach(this::filaEnRepeticiones);
    }

    /** La fila con ese fed_id en repeticiones: la que hay (en local) o una nueva (en CI). */
    private void filaEnRepeticiones(String fedId) {
        int tocadas = jdbc.update("UPDATE ejercicios SET medida = 'REPETICIONES', activo = 1 WHERE fed_id = ?", fedId);
        if (tocadas == 0) {
            jdbc.update("""
                    INSERT INTO ejercicios (nombre, grupo_muscular, dificultad, activo, fed_id, equipamiento, nombre_revisado)
                    VALUES (?, 'PIERNAS', 'PRINCIPIANTE', 1, ?, 'PESO_CORPORAL', 1)""", "Prueba " + fedId, fedId);
        }
    }

    private String medidaDe(String fedId) {
        return jdbc.queryForObject("SELECT medida FROM ejercicios WHERE fed_id = ?", String.class, fedId);
    }

    private Integer idDe(String fedId) {
        return jdbc.queryForObject("SELECT id FROM ejercicios WHERE fed_id = ?", Integer.class, fedId);
    }

    // La conexión de la transacción del test: Spring la reutiliza y se deshace al acabar.
    private void aplicarMigracion() {
        ScriptUtils.executeSqlScript(DataSourceUtils.getConnection(dataSource), new ClassPathResource(MIGRACION));
    }

    // Como una build que no manda la medida: la pauta se queda con la del ejercicio.
    private JsonNode anadir(String fedId) throws Exception {
        String json = pedir(owner, "POST /rutinas-ejercicios", "{\"rutinaId\":" + rutinaId + ",\"ejercicioId\":"
                + idDe(fedId) + ",\"series\":2,\"repeticiones\":30,\"tiempoDescanso\":30,\"orden\":1}")
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    @Test
    @DisplayName("la migración marca los estáticos y el rodillo, y deja los dinámicos; aplicarla dos veces da lo mismo")
    void la_migracion_marca_los_estaticos() {
        aplicarMigracion();
        aplicarMigracion();
        assertThat(ESTATICOS).allSatisfy(f -> assertThat(medidaDe(f)).as(f).isEqualTo("SEGUNDOS"));
        assertThat(SIGUEN).allSatisfy(f -> assertThat(medidaDe(f)).as(f).isEqualTo("REPETICIONES"));
    }

    @Test
    @DisplayName("añadido a una rutina, un estiramiento estático queda en segundos y uno dinámico no")
    void en_una_rutina() throws Exception {
        aplicarMigracion();
        assertThat(anadir("Quad_Stretch").get("medida").asText()).isEqualTo("SEGUNDOS");
        assertThat(anadir("Dynamic_Chest_Stretch").get("medida").asText()).isEqualTo("REPETICIONES");
    }
}
