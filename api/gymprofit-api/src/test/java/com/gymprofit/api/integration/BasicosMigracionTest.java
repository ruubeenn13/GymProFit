package com.gymprofit.api.integration;

import com.gymprofit.api.datos.BasicosCsvTestApoyo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// BasicosMigracionTest — la base tiene los básicos del CSV, ni uno más (GP-127)
// La migración la genera un script desde el CSV: si alguien toca uno sin regenerar
// el otro, aquí se nota. Y la carga es idempotente por (fuente, código): pasarla
// otra vez no duplica nada.
// ============================================================
@SpringBootTest
@Transactional
class BasicosMigracionTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("cada fila del CSV está en la base con sus valores, revisada y en el catálogo")
    void la_base_tiene_el_csv() throws Exception {
        List<Map<String, String>> csv = BasicosCsvTestApoyo.basicos();
        for (Map<String, String> b : csv) {
            Map<String, Object> fila = jdbc.queryForMap("""
                    SELECT nombre, nombre_en, categoria, calorias, revisado, usuario_id, activo
                    FROM alimentos WHERE fuente = ? AND codigo_origen = ?""", b.get("fuente"), b.get("codigo"));
            assertThat(fila.get("nombre")).isEqualTo(b.get("nombre_es"));
            assertThat(fila.get("nombre_en")).isEqualTo(b.get("nombre_en"));
            assertThat(fila.get("categoria")).isEqualTo(b.get("categoria"));
            assertThat(((Number) fila.get("calorias")).intValue()).isEqualTo(Integer.parseInt(b.get("kcal")));
            assertThat(fila.get("usuario_id")).as("un básico es catálogo, no tiene dueño").isNull();
            assertThat(asBoolean(fila.get("revisado"))).isTrue();
            assertThat(asBoolean(fila.get("activo"))).isTrue();
        }
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM alimentos WHERE fuente IN ('CIQUAL','USDA')", Integer.class))
                .isEqualTo(csv.size());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alimento_raciones", Integer.class))
                .isGreaterThanOrEqualTo(BasicosCsvTestApoyo.raciones().size());
    }

    @Test
    @DisplayName("volver a pasar la migración de los básicos no duplica ni alimentos ni raciones")
    void la_carga_es_idempotente() throws Exception {
        int alimentos = jdbc.queryForObject("SELECT COUNT(*) FROM alimentos", Integer.class);
        int raciones = jdbc.queryForObject("SELECT COUNT(*) FROM alimento_raciones", Integer.class);

        Path migracion;
        try (Stream<Path> ficheros = Files.list(Path.of("src/main/resources/db/migration"))) {
            migracion = ficheros.filter(p -> p.getFileName().toString().endsWith("__Alimentos_basicos.sql"))
                    .findFirst().orElseThrow();
        }
        // La conexión de la transacción del test: se suelta, no se cierra.
        var conexion = DataSourceUtils.getConnection(dataSource);
        try {
            ScriptUtils.executeSqlScript(conexion, new FileSystemResource(migracion));
        } finally {
            DataSourceUtils.releaseConnection(conexion, dataSource);
        }

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alimentos", Integer.class)).isEqualTo(alimentos);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alimento_raciones", Integer.class)).isEqualTo(raciones);
    }

    private static boolean asBoolean(Object valor) {
        return valor instanceof Boolean b ? b : ((Number) valor).intValue() != 0;
    }
}
