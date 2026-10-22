package com.gymprofit.api.integration;

import com.gymprofit.api.service.ejercicio.AplicadorCatalogoEs;
import com.gymprofit.api.service.ejercicio.CatalogoEs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.StringReader;
import java.sql.Connection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// CatalogoEsMigracionTest — el español del catálogo llega a la base (GP-217)
//
// Contra la MariaDB de verdad y dentro de la transacción del test: que Flyway aplicó la
// migración, y que aplicar el CSV a unas filas con el español de antes lo cambia sin tocar
// el inglés ni nada más, no repite lo que ya está hecho, no borra una descripción con un
// CSV vacío y no falla si el CSV trae ejercicios que la base no tiene.
// ============================================================
@SpringBootTest
@Transactional
@DisplayName("GP-217 — migración del catálogo en español")
class CatalogoEsMigracionTest {

    private static final String CSV = "fed_id,nombre_es,descripcion_es\n"
            + "Prueba_217_a,Press de banca revisado,\"1. Túmbate en el banco.\n2. Empuja la barra.\"\n"
            + "Prueba_217_b,Plancha revisada,\n"
            + "Prueba_217_c,Ya estaba,1. Tal cual.\n"
            + "Prueba_217_no_esta,Sin fila,1. Nada.\n";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private void fila(String fedId, String nombre, String descripcion) {
        jdbc.update("""
                INSERT INTO ejercicios (nombre, nombre_en, descripcion, descripcion_en, instrucciones_en,
                                        equipo_necesario_en, musculo_primario_en, grupo_muscular, dificultad,
                                        activo, fed_id, equipamiento, nombre_revisado, imagen_url)
                VALUES (?, ?, ?, ?, 'Instructions EN', 'Barbell', 'Chest', 'PECHO', 'INTERMEDIO', 1, ?, 'BARRA', 0,
                        'https://ejemplo/a.jpg')""",
                nombre, "English name " + fedId, descripcion, "1. English description " + fedId, fedId);
    }

    private Map<String, Object> leer(String fedId) {
        return jdbc.queryForMap("SELECT * FROM ejercicios WHERE fed_id = ?", fedId);
    }

    private AplicadorCatalogoEs.Resultado aplicar() throws Exception {
        // La conexión de la transacción del test: Spring la reutiliza, y se deshace al acabar.
        Connection c = DataSourceUtils.getConnection(dataSource);
        return AplicadorCatalogoEs.aplicar(c, CatalogoEs.de(new StringReader(CSV)));
    }

    @Test
    @DisplayName("Flyway ha aplicado la migración del catálogo en español")
    void flyway_la_aplica() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history "
                + "WHERE version = '202610221810' AND success = 1", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("cambia nombre y descripción, y no toca el inglés ni la imagen ni el equipamiento")
    void cambia_el_espanol_y_nada_mas() throws Exception {
        fila("Prueba_217_a", "Bench press", "1. Start by lying on the bench.");
        Map<String, Object> antes = leer("Prueba_217_a");

        aplicar();

        Map<String, Object> despues = leer("Prueba_217_a");
        assertThat(despues.get("nombre")).isEqualTo("Press de banca revisado");
        assertThat(despues.get("descripcion")).isEqualTo("1. Túmbate en el banco.\n2. Empuja la barra.");
        for (String columna : new String[]{"nombre_en", "descripcion_en", "instrucciones_en", "equipo_necesario_en",
                "musculo_primario_en", "grupo_muscular", "dificultad", "activo", "equipamiento", "imagen_url",
                "nombre_revisado", "fed_id"}) {
            assertThat(despues.get(columna)).as(columna).isEqualTo(antes.get(columna));
        }
    }

    @Test
    @DisplayName("es idempotente: volver a aplicar no cambia nada")
    void idempotente() throws Exception {
        fila("Prueba_217_a", "Bench press", "1. Start.");
        fila("Prueba_217_c", "Ya estaba", "1. Tal cual.");

        AplicadorCatalogoEs.Resultado primera = aplicar();
        AplicadorCatalogoEs.Resultado segunda = aplicar();

        assertThat(primera.actualizados()).isEqualTo(1);
        assertThat(primera.sinCambios()).isEqualTo(1);
        assertThat(segunda.actualizados()).isZero();
        assertThat(segunda.sinCambios()).isEqualTo(2);
    }

    @Test
    @DisplayName("un CSV sin descripción no borra la que hay")
    void no_borra_una_descripcion() throws Exception {
        fila("Prueba_217_b", "Plank", "1. Hold the plank.");

        aplicar();

        assertThat(leer("Prueba_217_b").get("nombre")).isEqualTo("Plancha revisada");
        assertThat(leer("Prueba_217_b").get("descripcion")).isEqualTo("1. Hold the plank.");
    }

    @Test
    @DisplayName("los ejercicios del CSV que la base no tiene se cuentan, no paran la migración")
    void ejercicios_que_faltan() throws Exception {
        fila("Prueba_217_a", "Bench press", "1. Start.");

        AplicadorCatalogoEs.Resultado r = aplicar();

        assertThat(r.enElCsv()).isEqualTo(4);
        assertThat(r.sinFila()).containsExactlyInAnyOrder("Prueba_217_b", "Prueba_217_c", "Prueba_217_no_esta");
    }
}
