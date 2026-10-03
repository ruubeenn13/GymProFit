package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// BorrarComidaTest — DELETE /comidas/{id} con sus alimentos (GP-190, lote 1.6.5)
// La comida se borra con sus líneas, en una transacción; la de otra cuenta, 403 y entera.
// ============================================================
class BorrarComidaTest extends AbstractOwnershipTest {

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    @Test
    @DisplayName("la propia, con alimentos: 200, y ni ella ni sus líneas quedan")
    void propia_con_alimentos() throws Exception {
        int comida = comidaCon(owner, 2);
        pedir(owner, "DELETE /comidas/" + comida).andExpect(status().isOk());
        em.flush();
        assertThat(cuenta("comidas", "id", comida)).isZero();
        assertThat(cuenta("alimentos_comida", "comida_id", comida)).isZero();
    }

    @Test
    @DisplayName("la propia, vacía: 200 como siempre")
    void propia_vacia() throws Exception {
        int comida = comidaCon(owner, 0);
        pedir(owner, "DELETE /comidas/" + comida).andExpect(status().isOk());
        em.flush();
        assertThat(cuenta("comidas", "id", comida)).isZero();
    }

    @Test
    @DisplayName("DEC-014: la de otra cuenta, 403, y sigue entera con sus líneas")
    void ajena_403() throws Exception {
        int comida = comidaCon(owner, 2);
        pedir(attacker, "DELETE /comidas/" + comida).andExpect(status().isForbidden());
        em.flush();
        assertThat(cuenta("comidas", "id", comida)).isEqualTo(1);
        assertThat(cuenta("alimentos_comida", "comida_id", comida)).isEqualTo(2);
    }

    private int comidaCon(Usuario quien, int lineas) {
        jdbc.update("INSERT INTO comidas (usuario_id, fecha, tipo_comida, total_calorias) VALUES (?, ?, 'CENA', 0)",
                quien.getId(), Timestamp.valueOf(LocalDate.now().atStartOfDay()));
        int comida = jdbc.queryForObject("SELECT MAX(id) FROM comidas WHERE usuario_id = ?", Integer.class, quien.getId());
        for (int i = 0; i < lineas; i++) {
            Alimento a = crearAlimentoCatalogo();
            a.setNombre("Borrar kzborrar " + i);
            a = alimentoRepository.saveAndFlush(a);
            jdbc.update("INSERT INTO alimentos_comida (comida_id, alimento_id, cantidad_gramos, calorias_totales) VALUES (?, ?, ?, 10)",
                    comida, a.getId(), new BigDecimal("100"));
        }
        return comida;
    }

    private int cuenta(String tabla, String columna, int id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla + " WHERE " + columna + " = ?", Integer.class, id);
    }
}
