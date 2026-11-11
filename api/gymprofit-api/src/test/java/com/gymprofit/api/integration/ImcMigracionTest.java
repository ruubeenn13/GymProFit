package com.gymprofit.api.integration;

import com.gymprofit.api.service.medicioncorporal.CalculoImc;
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

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// ImcMigracionTest — el IMC guardado a 0,00, recalculado (GP-204)
// Como MacrosLineaMigracionTest: contra la MariaDB de verdad, volviendo a ejecutar
// el fichero de la migración sobre filas preparadas dentro de la transacción, que
// se deshace al acabar. Las filas imitan lo que hay guardado: IMC 0,00 con la altura
// en cm, sin altura, con altura 0 y una altura en metros que no puede ser de cm.
// Cada valor se compara con CalculoImc: la migración y el servicio dicen lo mismo.
// ============================================================
@SpringBootTest
@Transactional
@DisplayName("GP-204 — la migración recalcula el IMC y deja nulo lo que no se puede calcular")
class ImcMigracionTest {

    private static final String SCRIPT = "db/migration/V202611111800__Recalcular_imc_mediciones.sql";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Flyway la ha aplicado")
    void flyway_la_aplica() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version = "
                + "'202611111800' AND success = 1", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("0,00 con peso y altura en cm → el IMC de verdad, igual que CalculoImc")
    void recalcula() {
        int usuario = usuario();
        int a = medicion(usuario, "78.00", "178.00", "0.00");
        int b = medicion(usuario, "75.50", "175.00", "0.00");
        int c = medicion(usuario, "80.00", "180.00", "24.69");

        ejecutar();

        assertThat(imc(a)).isEqualByComparingTo("24.6");
        assertThat(imc(b)).isEqualByComparingTo("24.7");
        assertThat(imc(c)).isEqualByComparingTo("24.7");
        assertThat(imc(a)).isEqualByComparingTo(CalculoImc.calcular(dec("78.00"), dec("178.00")));
        assertThat(imc(b)).isEqualByComparingTo(CalculoImc.calcular(dec("75.50"), dec("175.00")));
    }

    @Test
    @DisplayName("sin altura, con altura 0 o en metros → nulo, nunca 0")
    void sinCalculo_nulo() {
        int usuario = usuario();
        int sinAltura = medicion(usuario, "78.00", null, "0.00");
        int alturaCero = medicion(usuario, "78.00", "0.00", "0.00");
        int enMetros = medicion(usuario, "78.00", "1.78", "24.62");

        ejecutar();

        assertThat(imc(sinAltura)).isNull();
        assertThat(imc(alturaCero)).isNull();
        assertThat(imc(enMetros)).isNull();
    }

    @Test
    @DisplayName("ejecutarla dos veces deja lo mismo")
    void idempotente() {
        int usuario = usuario();
        int a = medicion(usuario, "78.00", "178.00", "0.00");
        int b = medicion(usuario, "78.00", null, "0.00");

        ejecutar();
        ejecutar();

        assertThat(imc(a)).isEqualByComparingTo("24.6");
        assertThat(imc(b)).isNull();
    }

    private int usuario() {
        jdbc.update("INSERT INTO usuarios (username, password, email, fecha_registro, activo) "
                + "VALUES ('__kzmigimc__', 'x', '__kzmigimc__@test.local', NOW(), 1)");
        return jdbc.queryForObject("SELECT id FROM usuarios WHERE username = '__kzmigimc__'", Integer.class);
    }

    private int medicion(int usuario, String peso, String altura, String imc) {
        jdbc.update("INSERT INTO mediciones_corporales (fecha, peso, altura, imc, usuario_id) VALUES (NOW(), ?, ?, ?, ?)",
                dec(peso), dec(altura), dec(imc), usuario);
        return jdbc.queryForObject("SELECT MAX(id) FROM mediciones_corporales WHERE usuario_id = ?", Integer.class, usuario);
    }

    private BigDecimal imc(int id) {
        return jdbc.queryForObject("SELECT imc FROM mediciones_corporales WHERE id = ?", BigDecimal.class, id);
    }

    private static BigDecimal dec(String s) {
        return s == null ? null : new BigDecimal(s);
    }

    private void ejecutar() {
        new ResourceDatabasePopulator(new ClassPathResource(SCRIPT)).execute(dataSource);
    }
}
