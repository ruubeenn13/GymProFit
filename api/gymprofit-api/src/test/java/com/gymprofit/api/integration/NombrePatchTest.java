package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// NombrePatchTest — GP-116
//
// Un nombre para mostrar, aparte del de usuario: opcional, recortado y de 1 a 40
// caracteres. En blanco lo borra; null no toca nada, como el resto del PATCH.
// ============================================================
@DisplayName("GP-116 — nombre para mostrar")
class NombrePatchTest extends AbstractOwnershipTest {

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @PersistenceContext
    private EntityManager em;

    private String ruta() {
        return "PATCH /usuarios/" + owner.getId();
    }

    private Usuario releer() {
        em.flush();
        em.clear();
        return usuarioRepository.findById(owner.getId()).orElseThrow();
    }

    @Test
    @DisplayName("se guarda recortado y sale en el PATCH y en el GET")
    void se_recorta() throws Exception {
        pedir(owner, ruta(), "{\"nombre\":\"   Ana María  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana María"));

        assertThat(releer().getNombre()).isEqualTo("Ana María");
        pedir(owner, "GET /usuarios/" + owner.getId())
                .andExpect(jsonPath("$.nombre").value("Ana María"));
    }

    @Test
    @DisplayName("40 caracteres valen, contados después de recortar")
    void cuarenta_vale() throws Exception {
        String cuarenta = "a".repeat(40);
        pedir(owner, ruta(), "{\"nombre\":\"  " + cuarenta + "  \"}")
                .andExpect(status().isOk());

        assertThat(releer().getNombre()).isEqualTo(cuarenta);
    }

    @Test
    @DisplayName("41 caracteres dan 400 y no cambian nada")
    void cuarenta_y_uno_400() throws Exception {
        pedir(owner, ruta(), "{\"nombre\":\"Ana\"}").andExpect(status().isOk());

        pedir(owner, ruta(), "{\"nombre\":\"" + "a".repeat(41) + "\",\"edad\":40}")
                .andExpect(status().isBadRequest());

        Usuario leido = releer();
        assertThat(leido.getNombre()).isEqualTo("Ana");
        assertThat(leido.getEdad()).isNull();
    }

    @Test
    @DisplayName("se cuentan caracteres, no unidades de UTF-16: 40 emojis valen")
    void emojis_cuentan_uno() throws Exception {
        String emojis = "💪".repeat(40);
        pedir(owner, ruta(), "{\"nombre\":\"" + emojis + "\"}").andExpect(status().isOk());

        assertThat(releer().getNombre()).isEqualTo(emojis);
    }

    @Test
    @DisplayName("en blanco lo borra")
    void en_blanco_borra() throws Exception {
        pedir(owner, ruta(), "{\"nombre\":\"Ana\"}").andExpect(status().isOk());

        pedir(owner, ruta(), "{\"nombre\":\"   \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").doesNotExist());

        assertThat(releer().getNombre()).isNull();
    }

    @Test
    @DisplayName("null o ausente no lo toca")
    void null_no_toca() throws Exception {
        pedir(owner, ruta(), "{\"nombre\":\"Ana\"}").andExpect(status().isOk());

        pedir(owner, ruta(), "{\"nombre\":null}").andExpect(status().isOk());
        pedir(owner, ruta(), "{\"edad\":30}").andExpect(status().isOk());

        assertThat(releer().getNombre()).isEqualTo("Ana");
    }
}
