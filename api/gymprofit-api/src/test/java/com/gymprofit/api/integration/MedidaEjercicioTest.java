package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.enums.MedidaSerie;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
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
// MedidaEjercicioTest — el ejercicio dice si va por tiempo (GP-130)
//
// El ejercicio del catálogo lleva su medida (REPETICIONES o SEGUNDOS) y la API la sirve
// en GET /ejercicios y en la pauta de una rutina (medidaEjercicio). Una build vieja que
// añade un ejercicio a una rutina sin mandar medida se queda con la del ejercicio; la que
// manda la petición, si la manda, gana. Y la migración V202611121801 marca en segundos los
// isométricos de la lista y no los dudosos. Contexto completo, sin servicio simulado.
// ============================================================
@DisplayName("GP-130 — la medida del ejercicio: catálogo, rutina y builds viejas")
class MedidaEjercicioTest extends AbstractOwnershipTest {

    private static final String MIGRACION = "db/migration/V202611121801__Ejercicios_por_tiempo.sql";

    @Autowired
    private IRutinaService rutinaService;

    @Autowired
    private IEjercicioRepository ejercicios;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private Integer rutinaId;
    private Ejercicio plancha;
    private Ejercicio remo;

    @BeforeEach
    void sembrar() {
        runAs(owner, () -> {
            RutinaCreateDTO r = new RutinaCreateDTO();
            r.setUsuarioId(owner.getId());
            r.setNombre("Core");
            r.setNivel("PRINCIPIANTE");
            r.setEsPredefinida(false);
            rutinaId = rutinaService.save(r).getId();
        });
        plancha = crearEjercicioCatalogo();
        plancha.setMedida(MedidaSerie.SEGUNDOS);
        plancha = ejercicios.save(plancha);
        remo = crearEjercicioCatalogo();
    }

    private JsonNode leer(String verboYRuta, String cuerpo) throws Exception {
        String json = (cuerpo == null ? pedir(owner, verboYRuta) : pedir(owner, verboYRuta, cuerpo))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private JsonNode anadir(Ejercicio e, String extra) throws Exception {
        return leer("POST /rutinas-ejercicios", "{\"rutinaId\":" + rutinaId + ",\"ejercicioId\":" + e.getId()
                + ",\"series\":3,\"tiempoDescanso\":60,\"orden\":1" + extra + "}");
    }

    @Test
    @DisplayName("GET /ejercicios/{id} trae la medida; por defecto, repeticiones")
    void el_catalogo_la_sirve() throws Exception {
        assertThat(leer("GET /ejercicios/" + plancha.getId(), null).get("medida").asText()).isEqualTo("SEGUNDOS");
        assertThat(leer("GET /ejercicios/" + remo.getId(), null).get("medida").asText()).isEqualTo("REPETICIONES");
    }

    @Test
    @DisplayName("una build vieja añade la plancha sin medida: queda en segundos, la del ejercicio")
    void build_vieja_copia_la_del_ejercicio() throws Exception {
        JsonNode r = anadir(plancha, ",\"repeticiones\":30");
        assertThat(r.get("medida").asText()).isEqualTo("SEGUNDOS");
        assertThat(r.get("medidaEjercicio").asText()).isEqualTo("SEGUNDOS");

        JsonNode s = anadir(remo, ",\"repeticiones\":10");
        assertThat(s.get("medida").asText()).isEqualTo("REPETICIONES");
    }

    @Test
    @DisplayName("la medida que manda la petición gana a la del ejercicio")
    void la_peticion_manda() throws Exception {
        JsonNode r = anadir(plancha, ",\"repeticionesMin\":8,\"repeticionesMax\":12,\"medida\":\"REPETICIONES\"");
        assertThat(r.get("medida").asText()).isEqualTo("REPETICIONES");
        assertThat(r.get("medidaEjercicio").asText()).isEqualTo("SEGUNDOS");
    }

    /** La fila con ese fed_id en repeticiones: la que hay (en local) o una nueva (en CI). */
    private void filaEnRepeticiones(String fedId) {
        int tocadas = jdbc.update("UPDATE ejercicios SET medida = 'REPETICIONES' WHERE fed_id = ?", fedId);
        if (tocadas == 0) {
            jdbc.update("""
                    INSERT INTO ejercicios (nombre, grupo_muscular, dificultad, activo, fed_id, equipamiento, nombre_revisado)
                    VALUES (?, 'ABDOMEN', 'PRINCIPIANTE', 1, ?, 'PESO_CORPORAL', 1)""", "Prueba " + fedId, fedId);
        }
    }

    private String medidaDe(String fedId) {
        return jdbc.queryForObject("SELECT medida FROM ejercicios WHERE fed_id = ?", String.class, fedId);
    }

    @Test
    @DisplayName("la migración marca en segundos los isométricos de la lista y deja los dudosos en repeticiones")
    void la_migracion_marca_los_isometricos() {
        List<String> isometricos = List.of("Plank", "Side_Bridge", "One_Handed_Hang", "Crucifix", "Plate_Pinch",
                "Stomach_Vacuum", "Isometric_Chest_Squeezes", "Isometric_Neck_Exercise_-_Front_And_Back",
                "Isometric_Neck_Exercise_-_Sides");
        List<String> dudosos = List.of("Isometric_Wipers", "Superman", "Dead_Bug", "Farmers_Walk", "Iron_Cross");
        isometricos.forEach(this::filaEnRepeticiones);
        dudosos.forEach(this::filaEnRepeticiones);

        // La conexión de la transacción del test: Spring la reutiliza y se deshace al acabar.
        ScriptUtils.executeSqlScript(DataSourceUtils.getConnection(dataSource), new ClassPathResource(MIGRACION));

        assertThat(isometricos).allSatisfy(f -> assertThat(medidaDe(f)).as(f).isEqualTo("SEGUNDOS"));
        assertThat(dudosos).allSatisfy(f -> assertThat(medidaDe(f)).as(f).isEqualTo("REPETICIONES"));
    }
}
