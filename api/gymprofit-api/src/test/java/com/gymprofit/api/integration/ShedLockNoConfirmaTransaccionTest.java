package com.gymprofit.api.integration;

import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.service.auth.RefreshTokenCleanupTask;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// ShedLockNoConfirmaTransaccionTest — coger un bloqueo no confirma la transacción en la
// que ya estaba la tarea (GP-221).
//
// Pasó en el primer intento: sin gestor de transacciones explícito, ShedLock usaba uno de
// JDBC sobre el mismo pool, y al llamar a una tarea bloqueada desde dentro de una
// transacción de JPA esa transacción quedaba confirmada. En los tests eso deja filas en la
// base que el rollback ya no quita; en producción, una tarea @Transactional que fallara a
// medias habría dejado escrita la mitad.
//
// La prueba: dentro de la transacción del test se crea un usuario, se llama a una tarea
// bloqueada y, por una conexión aparte, se mira si el usuario ya es visible fuera. No
// tiene que serlo hasta que la transacción acabe, y como acaba en rollback, nunca.
// ============================================================
@DisplayName("ShedLock — el bloqueo no confirma la transacción en curso")
class ShedLockNoConfirmaTransaccionTest extends AbstractOwnershipTest {

    private static final String MARCA = "__shedlock_tx_marca__";

    @Autowired
    private RefreshTokenCleanupTask purga;

    @Autowired
    private DataSource dataSource;

    private static DataSource fuente;
    private static boolean seEscapo;

    @Test
    @DisplayName("lo escrito antes de la tarea sigue sin confirmar después de ella")
    void el_bloqueo_no_confirma_lo_que_habia_antes() throws SQLException {
        fuente = dataSource;
        crearUsuario(MARCA, RoleType.USER);

        purga.purgarTokensInutiles();

        int fuera = visibleFuera(dataSource);
        seEscapo = fuera > 0;
        assertThat(fuera)
                .as("la transacción del test se ha confirmado al coger el bloqueo")
                .isZero();
    }

    /**
     * Si falla, se confirmó toda la transacción del test: la marca y los tres usuarios de
     * AbstractOwnershipTest. Se quitan, o el resto de la suite chocaría con ellos.
     */
    @AfterAll
    static void limpiarSiSeEscapo() throws SQLException {
        if (fuente == null || !seEscapo) return;
        for (String nombre : new String[]{MARCA, OWNER, ATTACKER, GUEST}) borrar(nombre);
    }

    private static void borrar(String nombre) throws SQLException {
        try (Connection c = fuente.getConnection();
             PreparedStatement roles = c.prepareStatement(
                     "DELETE ur FROM usuario_roles ur JOIN usuarios u ON u.id = ur.usuario_id WHERE u.username = ?");
             PreparedStatement usuario = c.prepareStatement("DELETE FROM usuarios WHERE username = ?")) {
            roles.setString(1, nombre);
            roles.executeUpdate();
            usuario.setString(1, nombre);
            usuario.executeUpdate();
        }
    }

    /** Cuenta el usuario marca desde una conexión que no es la del test. */
    private static int visibleFuera(DataSource ds) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM usuarios WHERE username = ?")) {
            p.setString(1, MARCA);
            try (ResultSet r = p.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        }
    }
}
