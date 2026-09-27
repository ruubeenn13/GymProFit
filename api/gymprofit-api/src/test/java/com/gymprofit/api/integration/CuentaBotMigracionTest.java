package com.gymprofit.api.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// CuentaBotMigracionTest — GP-106
//
// La cuenta de servicio del bot de Discord sigue a admin y guest al dominio del
// producto. Mismo planteamiento que CuentasSemillaMigracionTest: contra la MariaDB de
// verdad, comprobando que Flyway la aplicó y volviendo a ejecutar el fichero sobre
// filas preparadas, dentro de la transacción del test.
// ============================================================
@SpringBootTest
@Transactional
@DisplayName("GP-106 — migración de la cuenta del bot a gymprofit.app")
class CuentaBotMigracionTest {

    private static final String VERSION = "202609271100";
    private static final String SCRIPT =
            "db/migration/V" + VERSION + "__Cuenta_bot_gymprofit_app.sql";

    private static final String BOT_COM = "bot@gymprofit.com";
    private static final String BOT_APP = "bot@gymprofit.app";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    // Aparta los dos correos en juego, tenga la base local los que tenga.
    @BeforeEach
    void apartarCorreosBot() {
        jdbc.update("UPDATE usuarios SET email = CONCAT('gp106-bot-apartado-', id, '@test.local') "
                + "WHERE email IN (?, ?)", BOT_COM, BOT_APP);
    }

    @Test
    @DisplayName("Flyway la ha aplicado con éxito")
    void flyway_la_aplica() {
        Integer correctas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = ? AND success = 1",
                Integer.class, VERSION);

        assertThat(correctas).isEqualTo(1);
    }

    @Test
    @DisplayName("el correo de .com pasa a .app y el usuario no cambia")
    void correo_com_cambia() {
        int bot = insertar("__gp106_bot__", BOT_COM);

        ejecutarMigracion();

        assertThat(correoDe(bot)).isEqualTo(BOT_APP);
        assertThat(jdbc.queryForObject("SELECT username FROM usuarios WHERE id = ?", String.class, bot))
                .isEqualTo("__gp106_bot__");
    }

    @Test
    @DisplayName("un correo ya cambiado no se toca")
    void correo_cambiado_no_se_toca() {
        int propio = insertar("__gp106_bot__", "bot-propio@ejemplo.test");

        ejecutarMigracion();

        assertThat(correoDe(propio)).isEqualTo("bot-propio@ejemplo.test");
    }

    @Test
    @DisplayName("si el correo de .app ya lo tiene otra cuenta, no falla y no toca nada")
    void destino_ocupado_no_rompe() {
        int ocupante = insertar("__gp106_ocupante__", BOT_APP);
        int bot = insertar("__gp106_bot__", BOT_COM);

        ejecutarMigracion();

        assertThat(correoDe(ocupante)).isEqualTo(BOT_APP);
        assertThat(correoDe(bot)).isEqualTo(BOT_COM);
    }

    // --- Ayudas -------------------------------------------------------------

    private int insertar(String username, String email) {
        jdbc.update("INSERT INTO usuarios (username, password, email, fecha_registro, activo) "
                + "VALUES (?, 'x', ?, NOW(), 1)", username, email);
        return jdbc.queryForObject("SELECT id FROM usuarios WHERE username = ?", Integer.class, username);
    }

    private String correoDe(int id) {
        return jdbc.queryForObject("SELECT email FROM usuarios WHERE id = ?", String.class, id);
    }

    private void ejecutarMigracion() {
        new ResourceDatabasePopulator(new ClassPathResource(SCRIPT)).execute(dataSource);
    }
}
