package com.gymprofit.api.integration;

import com.gymprofit.api.entity.ProductoOff;
import com.gymprofit.api.service.productooff.RacionesProducto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// RacionAntesQueEnvaseMigracionTest — GP-182 (lote 1.6.4)
//
// Un producto materializado antes de la 1.6.4 tiene «1 envase» delante de «1 ración».
// La migración lo deja como uno nuevo (RacionesProducto) sin cambiar los ids de sus
// raciones, que usan las líneas de las comidas. Como CuentaBotMigracionTest: contra la
// MariaDB de verdad, volviendo a ejecutar el fichero sobre filas preparadas dentro de
// la transacción del test.
// ============================================================
@SpringBootTest
@Transactional
@DisplayName("GP-182 — la ración delante del envase en los productos ya materializados")
class RacionAntesQueEnvaseMigracionTest {

    private static final String VERSION = "202610031000";
    private static final String SCRIPT = "db/migration/V" + VERSION + "__Racion_antes_que_envase.sql";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Flyway la ha aplicado con éxito")
    void flyway_la_aplica() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version = ? AND success = 1",
                Integer.class, VERSION)).isEqualTo(1);
    }

    @Test
    @DisplayName("unas galletas de 400 g con ración de 30: la ración pasa delante, con los mismos ids")
    void galletas() {
        int alimento = producto("OFF");
        int envase = racion(alimento, "1 envase", "400", "Open Food Facts: envase 400 g", 1);
        int racion = racion(alimento, "1 ración", "30", "Open Food Facts: 30 g", 2);

        ejecutarMigracion();

        assertThat(orden(alimento)).containsExactly(racion, envase);
        // Lo mismo que tendría uno materializado hoy, con los mismos datos.
        ProductoOff nuevo = new ProductoOff();
        nuevo.setEnvase("400 g");
        nuevo.setRacionGramos(new BigDecimal("30"));
        nuevo.setRacionTexto("30 g");
        assertThat(nombres(alimento)).containsExactlyElementsOf(
                RacionesProducto.de(nuevo).stream().map(RacionesProducto.Racion::nombre).toList());
    }

    @Test
    @DisplayName("un yogur de 125 g con ración de 150, un multipack y uno sin ración no cambian")
    void lo_demas_no_cambia() {
        int yogur = producto("OFF");
        int yogurEnvase = racion(yogur, "1 envase", "125", "Open Food Facts: envase 125 g", 1);
        int yogurRacion = racion(yogur, "1 ración", "150", "Open Food Facts: 150 g", 2);
        int pack = producto("OFF");
        int packUnidad = racion(pack, "1 unidad", "125", "Open Food Facts: envase 4 x 125 g", 1);
        int packRacion = racion(pack, "1 ración", "100", "Open Food Facts: 100 g", 2);
        int solo = producto("OFF");
        int soloEnvase = racion(solo, "1 envase", "200", "Open Food Facts: envase 200 g", 1);

        ejecutarMigracion();

        assertThat(orden(yogur)).containsExactly(yogurEnvase, yogurRacion);
        assertThat(orden(pack)).containsExactly(packUnidad, packRacion);
        assertThat(orden(solo)).containsExactly(soloEnvase);
    }

    @Test
    @DisplayName("un alimento que no es de Open Food Facts no se toca")
    void otro_origen() {
        int casero = producto(null);
        int envase = racion(casero, "1 envase", "400", "a mano", 1);
        int racion = racion(casero, "1 ración", "30", "a mano", 2);

        ejecutarMigracion();

        assertThat(orden(casero)).containsExactly(envase, racion);
    }

    @Test
    @DisplayName("ejecutarla dos veces deja lo mismo")
    void idempotente() {
        int alimento = producto("OFF");
        int envase = racion(alimento, "1 envase", "400", "Open Food Facts: envase 400 g", 1);
        int racion = racion(alimento, "1 ración", "30", "Open Food Facts: 30 g", 2);

        ejecutarMigracion();
        ejecutarMigracion();

        assertThat(orden(alimento)).containsExactly(racion, envase);
    }

    // --- Ayudas -------------------------------------------------------------

    private int producto(String fuente) {
        jdbc.update("INSERT INTO alimentos (nombre, calorias, activo, categoria, fuente) "
                + "VALUES ('Producto kzmigracion', 450, 1, 'Snacks', ?)", fuente);
        return jdbc.queryForObject("SELECT MAX(id) FROM alimentos", Integer.class);
    }

    private int racion(int alimento, String nombre, String gramos, String fuente, int orden) {
        jdbc.update("INSERT INTO alimento_raciones (alimento_id, nombre, nombre_en, gramos, fuente, orden) "
                + "VALUES (?, ?, ?, ?, ?, ?)", alimento, nombre, nombre, new BigDecimal(gramos), fuente, orden);
        return jdbc.queryForObject("SELECT MAX(id) FROM alimento_raciones WHERE alimento_id = ?", Integer.class, alimento);
    }

    // Los ids de las raciones del alimento, por su orden.
    private List<Integer> orden(int alimento) {
        return jdbc.queryForList("SELECT id FROM alimento_raciones WHERE alimento_id = ? ORDER BY orden",
                Integer.class, alimento);
    }

    private List<String> nombres(int alimento) {
        return jdbc.queryForList("SELECT nombre FROM alimento_raciones WHERE alimento_id = ? ORDER BY orden",
                String.class, alimento);
    }

    private void ejecutarMigracion() {
        new ResourceDatabasePopulator(new ClassPathResource(SCRIPT)).execute(dataSource);
    }
}
