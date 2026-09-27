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
// CuentasSemillaMigracionTest — GP-106
//
// La migración que lleva las cuentas semilla a gymprofit.app se prueba contra la
// MariaDB de verdad, no contra un H2: lo que importa es que el UPDATE funcione en el
// motor en el que va a correr, con su restricción UNIQUE sobre el correo incluida.
//
// Flyway ya la ha aplicado al levantar el contexto, así que primero se comprueba eso
// y después se vuelve a ejecutar el mismo fichero sobre filas preparadas a propósito.
// Todo va dentro de la transacción del test y se revierte al acabar.
// ============================================================
@SpringBootTest
@Transactional
@DisplayName("GP-106 — migración de las cuentas semilla a gymprofit.app")
class CuentasSemillaMigracionTest {

    private static final String VERSION = "202609271000";
    private static final String SCRIPT =
            "db/migration/V" + VERSION + "__Cuentas_semilla_gymprofit_app.sql";

    private static final String ADMIN_COM = "admin@gymprofit.com";
    private static final String ADMIN_APP = "admin@gymprofit.app";
    private static final String GUEST_COM = "guest@gymprofit.com";
    private static final String GUEST_APP = "guest@gymprofit.app";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    // Aparta los cuatro correos en juego para que el test decida qué filas los llevan,
    // tenga la base local los que tenga.
    @BeforeEach
    void apartarCorreosSemilla() {
        jdbc.update("UPDATE usuarios SET email = CONCAT('gp106-apartado-', id, '@test.local') "
                + "WHERE email IN (?, ?, ?, ?)", ADMIN_COM, ADMIN_APP, GUEST_COM, GUEST_APP);
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
    @DisplayName("un correo que sigue siendo el de .com pasa a .app")
    void correo_com_cambia() {
        int admin = insertar("__gp106_admin__", ADMIN_COM);
        int guest = insertar("__gp106_guest__", GUEST_COM);

        ejecutarMigracion();

        assertThat(correoDe(admin)).isEqualTo(ADMIN_APP);
        assertThat(correoDe(guest)).isEqualTo(GUEST_APP);
    }

    @Test
    @DisplayName("un correo ya cambiado no se toca")
    void correo_cambiado_no_se_toca() {
        int yaEnApp = insertar("__gp106_admin__", ADMIN_APP);
        int propio = insertar("__gp106_guest__", "propietario@ejemplo.test");

        ejecutarMigracion();

        assertThat(correoDe(yaEnApp)).isEqualTo(ADMIN_APP);
        assertThat(correoDe(propio)).isEqualTo("propietario@ejemplo.test");
    }

    // Si la dirección de destino ya la lleva otra fila, la migración no puede reventar
    // por la UNIQUE: tumbaría el arranque de producción. Deja esa fila como estaba.
    @Test
    @DisplayName("si el correo de .app ya lo tiene otra cuenta, no falla y no toca nada")
    void destino_ocupado_no_rompe() {
        int ocupante = insertar("__gp106_ocupante__", ADMIN_APP);
        int semilla = insertar("__gp106_admin__", ADMIN_COM);

        ejecutarMigracion();

        assertThat(correoDe(ocupante)).isEqualTo(ADMIN_APP);
        assertThat(correoDe(semilla)).isEqualTo(ADMIN_COM);
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

    // El populator toma la conexión de la transacción en curso: se revierte con el test.
    private void ejecutarMigracion() {
        new ResourceDatabasePopulator(new ClassPathResource(SCRIPT)).execute(dataSource);
    }
}
