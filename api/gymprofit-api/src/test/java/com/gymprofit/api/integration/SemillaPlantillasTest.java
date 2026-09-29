package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.service.programa.SembradorPlantillas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// SemillaPlantillasTest — la semilla del catálogo de plantillas v1 (GP-074)
//
// Contra la MariaDB de verdad y dentro de la transacción del test: que Flyway aplicó las
// tres migraciones del lote, y la siembra vuelta a ejecutar con los 62 ejercicios en la
// base, comparada fila a fila con el JSON. Más los dos casos que no deben parar la
// migración (un ejercicio que falta y uno inactivo) y la revisión de los ejercicios.
// ============================================================
@SpringBootTest
@Transactional
@DisplayName("GP-074 — semilla del catálogo de plantillas")
class SemillaPlantillasTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private JsonNode catalogo;

    @BeforeEach
    void preparar() {
        catalogo = CatalogoPlantillasDePrueba.catalogo();
        CatalogoPlantillasDePrueba.asegurarEjercicios(jdbc);
    }

    @Test
    @DisplayName("Flyway ha aplicado el modelo, la retirada de las predefinidas y la semilla")
    void flyway_las_aplica() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history "
                + "WHERE version IN ('202609292000', '202609292001', '202609292002') AND success = 1", Integer.class))
                .isEqualTo(3);
    }

    @Test
    @DisplayName("el JSON es coherente: 13 programas, 31 rutinas, 62 ejercicios y cada día de la semana existe")
    void el_catalogo_es_coherente() {
        assertThat(catalogo.get("programas")).hasSize(13);
        assertThat(catalogo.get("rutinas")).hasSize(31);
        assertThat(catalogo.get("ejercicios")).hasSize(62);
        Set<String> codigos = new HashSet<>();
        catalogo.get("rutinas").forEach(r -> codigos.add(r.get("codigo").asText()));
        catalogo.get("programas").forEach(p -> p.get("semana")
                .forEach(c -> assertThat(codigos).as(p.get("codigo").asText()).contains(c.asText())));
    }

    @Test
    @DisplayName("siembra 13 programas y 31 plantillas, sin usuario, no predefinidas y con cada ejercicio del JSON")
    void siembra_todo_el_catalogo() {
        SembradorPlantillas.Informe informe = CatalogoPlantillasDePrueba.resembrar(jdbc, dataSource);

        int filasJson = 0;
        for (JsonNode r : catalogo.get("rutinas")) filasJson += r.get("ejercicios").size();
        assertThat(informe.programas()).isEqualTo(13);
        assertThat(informe.rutinas()).isEqualTo(31);
        assertThat(informe.ejercicios()).isEqualTo(filasJson);
        assertThat(informe.omitidos()).isEmpty();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM programas", Integer.class)).isEqualTo(13);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM rutinas WHERE es_plantilla = 1 AND usuario_id IS NULL
                AND es_predefinida = 0 AND activa = 1 AND codigo IS NOT NULL""", Integer.class)).isEqualTo(31);

        for (JsonNode r : catalogo.get("rutinas")) {
            String codigo = r.get("codigo").asText();
            Map<String, Object> fila = jdbc.queryForMap(
                    "SELECT nombre, nombre_en, duracion_minutos, nivel, categoria_en FROM rutinas WHERE codigo = ?", codigo);
            assertThat(fila.get("nombre")).as(codigo).isEqualTo(r.get("nombre").asText());
            assertThat(fila.get("nombre_en")).as(codigo).isEqualTo(r.get("nombreEn").asText());
            assertThat(fila.get("duracion_minutos")).as(codigo).isEqualTo(r.get("duracionMinutos").asInt());
            assertThat(fila.get("nivel")).as(codigo).isEqualTo(r.get("nivel").asText());
            assertThat(fila.get("categoria_en")).as(codigo).isEqualTo(r.get("categoriaEn").asText());

            assertThat(ejerciciosEnBase(codigo)).as(codigo).isEqualTo(ejerciciosEnJson(r, Set.of()));
        }

        for (JsonNode p : catalogo.get("programas")) {
            List<String> semana = jdbc.queryForList("""
                    SELECT r.codigo FROM programa_rutina pr JOIN programas p ON p.id = pr.programa_id
                    JOIN rutinas r ON r.id = pr.rutina_id WHERE p.codigo = ? ORDER BY pr.posicion""",
                    String.class, p.get("codigo").asText());
            List<String> esperada = new ArrayList<>();
            p.get("semana").forEach(c -> esperada.add(c.asText()));
            assertThat(semana).as(p.get("codigo").asText()).isEqualTo(esperada);
            assertThat(jdbc.queryForMap("SELECT nivel, equipamiento, dias_min, dias_max FROM programas WHERE codigo = ?",
                    p.get("codigo").asText()))
                    .containsEntry("nivel", p.get("nivel").asText())
                    .containsEntry("equipamiento", p.get("equipamiento").asText())
                    .containsEntry("dias_min", p.get("diasMin").asInt())
                    .containsEntry("dias_max", p.get("diasMax").asInt());
        }
    }

    @Test
    @DisplayName("un ejercicio que falta o está inactivo no para la semilla: se omite y se avisa")
    void omite_lo_que_falta_y_lo_inactivo() {
        jdbc.update("UPDATE ejercicios SET activo = 0 WHERE fed_id = 'Dead_Bug'");
        jdbc.update("UPDATE ejercicios SET fed_id = '__no_existe__' WHERE fed_id = 'Cable_Crossover'");

        SembradorPlantillas.Informe informe = CatalogoPlantillasDePrueba.resembrar(jdbc, dataSource);

        assertThat(informe.programas()).isEqualTo(13);
        assertThat(informe.rutinas()).isEqualTo(31);
        assertThat(informe.omitidos()).containsExactlyInAnyOrder(
                "PC-PIERNA-B · Dead_Bug (inactivo)",
                "GIM-EMPUJE · Cable_Crossover (no está en la base)");

        for (JsonNode r : catalogo.get("rutinas")) {
            String codigo = r.get("codigo").asText();
            assertThat(ejerciciosEnBase(codigo)).as(codigo)
                    .isEqualTo(ejerciciosEnJson(r, Set.of("Dead_Bug", "Cable_Crossover")));
        }
    }

    @Test
    @DisplayName("deja revisados los 62 nombres, Goblet_Squat en mancuernas y la descripción de Side_Bridge si faltaba")
    void revisa_los_ejercicios() {
        jdbc.update("UPDATE ejercicios SET nombre = 'Goblet Squat', equipamiento = 'KETTLEBELL', nombre_revisado = 0 "
                + "WHERE fed_id = 'Goblet_Squat'");
        // La española ya estaba: no se pisa. La inglesa faltaba: se escribe.
        jdbc.update("UPDATE ejercicios SET descripcion = 'Ya tenía una', descripcion_en = NULL WHERE fed_id = 'Side_Bridge'");

        SembradorPlantillas.Informe informe = CatalogoPlantillasDePrueba.resembrar(jdbc, dataSource);

        assertThat(informe.nombresCambiados())
                .contains("Goblet_Squat: Goblet Squat → Sentadilla goblet, con mancuerna o kettlebell");
        for (JsonNode e : catalogo.get("ejercicios")) {
            Map<String, Object> fila = jdbc.queryForMap(
                    "SELECT nombre, nombre_revisado FROM ejercicios WHERE fed_id = ?", e.get("fedId").asText());
            assertThat(fila.get("nombre")).as(e.get("fedId").asText()).isEqualTo(e.get("nombre").asText());
            assertThat(fila.get("nombre_revisado")).as(e.get("fedId").asText()).isEqualTo(true);
        }
        assertThat(jdbc.queryForObject("SELECT equipamiento FROM ejercicios WHERE fed_id = 'Goblet_Squat'", String.class))
                .isEqualTo("MANCUERNAS");

        JsonNode sideBridge = null;
        for (JsonNode e : catalogo.get("ejercicios")) {
            if (e.get("fedId").asText().equals("Side_Bridge")) sideBridge = e;
        }
        Map<String, Object> side = jdbc.queryForMap(
                "SELECT descripcion, descripcion_en FROM ejercicios WHERE fed_id = 'Side_Bridge'");
        assertThat(side.get("descripcion")).isEqualTo("Ya tenía una");
        assertThat(side.get("descripcion_en")).isEqualTo(sideBridge.get("descripcionEnSiVacia").asText());

        jdbc.update("UPDATE ejercicios SET descripcion = '  ' WHERE fed_id = 'Side_Bridge'");
        CatalogoPlantillasDePrueba.resembrar(jdbc, dataSource);
        assertThat(jdbc.queryForObject("SELECT descripcion FROM ejercicios WHERE fed_id = 'Side_Bridge'", String.class))
                .isEqualTo(sideBridge.get("descripcionSiVacia").asText());
    }

    // Una línea por ejercicio de la plantilla en la base, en orden.
    private List<String> ejerciciosEnBase(String codigo) {
        return jdbc.query("""
                SELECT re.orden, e.fed_id, re.tipo, re.series, re.repeticiones, re.repeticiones_min,
                       re.repeticiones_max, re.medida, re.por_lado, re.tiempo_descanso, re.notas, re.notas_en
                FROM rutina_ejercicio re JOIN rutinas r ON r.id = re.rutina_id JOIN ejercicios e ON e.id = re.ejercicio_id
                WHERE r.codigo = ? ORDER BY re.orden""",
                (rs, i) -> String.join(" | ", rs.getString("orden"), rs.getString("fed_id"), rs.getString("tipo"),
                        rs.getString("series"), "rep=" + rs.getString("repeticiones"),
                        rs.getString("repeticiones_min") + "–" + rs.getString("repeticiones_max"),
                        rs.getString("medida"), String.valueOf(rs.getString("por_lado")),
                        rs.getString("tiempo_descanso"), String.valueOf(rs.getString("notas")),
                        String.valueOf(rs.getString("notas_en"))),
                codigo);
    }

    // Lo mismo, sacado del JSON: repeticiones lleva el máximo.
    private static List<String> ejerciciosEnJson(JsonNode rutina, Set<String> omitidos) {
        List<String> lineas = new ArrayList<>();
        for (JsonNode e : rutina.get("ejercicios")) {
            if (omitidos.contains(e.get("fedId").asText())) continue;
            lineas.add(String.join(" | ", e.get("orden").asText(), e.get("fedId").asText(), e.get("tipo").asText(),
                    e.get("series").asText(), "rep=" + e.get("max").asText(),
                    e.get("min").asText() + "–" + e.get("max").asText(), e.get("medida").asText(),
                    e.get("porLado").isNull() ? "null" : e.get("porLado").asText(),
                    e.get("descansoSegundos").asText(),
                    e.get("nota").isNull() ? "null" : e.get("nota").asText(),
                    e.get("notaEn").isNull() ? "null" : e.get("notaEn").asText()));
        }
        return lineas;
    }
}
