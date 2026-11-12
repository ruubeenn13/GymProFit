package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.PasoAlta;
import com.gymprofit.api.enums.RoleType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static com.gymprofit.api.service.admin.AdminResumenService.MADRID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// EmbudoAltaTest — GP-019, POST /alta/pasos y GET /admin/embudo
//
// La ruta es pública y solo suma uno a la fila (día de Madrid, paso). Lo que importa es
// lo que NO hace: guardar algo de quien la llama. Se le manda todo lo que podría
// identificar —token de una cuenta, IP, navegador, correo e id en el cuerpo— y se
// comprueba que la tabla solo tiene día, paso y total, y que no aparece nada de eso.
// ============================================================
@DisplayName("GP-019 — el embudo del alta cuenta sin saber quién")
class EmbudoAltaTest extends AbstractOwnershipTest {

    @PersistenceContext
    private EntityManager em;

    private ResultActions avisar(String cuerpo) throws Exception {
        return mockMvc.perform(post("/alta/pasos").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private long total(PasoAlta paso) {
        List<?> r = em.createNativeQuery("SELECT total FROM alta_pasos WHERE dia = ? AND paso = ?")
                .setParameter(1, LocalDate.now(MADRID)).setParameter(2, paso.name()).getResultList();
        return r.isEmpty() ? 0 : ((Number) r.get(0)).longValue();
    }

    private long filas() {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM alta_pasos").getSingleResult()).longValue();
    }

    @Test
    @DisplayName("sin token → 204 y suma uno al paso de hoy; dos avisos, dos")
    void suma_sin_token() throws Exception {
        long antes = total(PasoAlta.OBJETIVO);
        avisar("{\"paso\":\"OBJETIVO\"}").andExpect(status().isNoContent());
        assertThat(total(PasoAlta.OBJETIVO)).isEqualTo(antes + 1);
        avisar("{\"paso\":\"OBJETIVO\"}").andExpect(status().isNoContent());
        assertThat(total(PasoAlta.OBJETIVO)).isEqualTo(antes + 2);
    }

    @Test
    @DisplayName("un paso que no existe, sin paso o sin cuerpo → 400 y no cuenta nada")
    void rechaza_pasos_que_no_existen() throws Exception {
        long antes = filas();
        long sumaAntes = Arrays.stream(PasoAlta.values()).mapToLong(this::total).sum();
        avisar("{\"paso\":\"PAGO\"}").andExpect(status().isBadRequest());
        avisar("{\"paso\":\"objetivo\"}").andExpect(status().isBadRequest());
        avisar("{\"paso\":null}").andExpect(status().isBadRequest());
        avisar("{}").andExpect(status().isBadRequest());
        avisar("").andExpect(status().isBadRequest());
        assertThat(filas()).isEqualTo(antes);
        assertThat(Arrays.stream(PasoAlta.values()).mapToLong(this::total).sum()).isEqualTo(sumaAntes);
    }

    @Test
    @DisplayName("con token, IP y datos en el cuerpo, la fila solo guarda día, paso y total")
    void no_guarda_nada_identificable() throws Exception {
        Usuario quien = crearUsuario("__gp019_quien__", RoleType.USER);
        long filasAntes = filas();
        long antes = total(PasoAlta.PLAN);

        MockHttpServletRequestBuilder peticion = post("/alta/pasos")
                .header("Authorization", "Bearer " + token(quien))
                .header("X-Forwarded-For", "203.0.113.77")
                .header("User-Agent", "movil-de-prueba-gp019")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"paso\":\"PLAN\",\"email\":\"" + quien.getEmail() + "\",\"usuarioId\":" + quien.getId()
                        + ",\"dispositivo\":\"disp-gp019\",\"hora\":\"2026-11-12T10:15:00\"}");
        mockMvc.perform(peticion).andExpect(status().isNoContent());

        // Las columnas de la tabla son estas tres y ninguna más.
        List<?> columnas = em.createNativeQuery("""
                        SELECT column_name FROM information_schema.columns
                        WHERE table_schema = DATABASE() AND table_name = 'alta_pasos'
                        ORDER BY ordinal_position""").getResultList();
        assertThat(columnas).map(Object::toString).map(String::toLowerCase).containsExactly("dia", "paso", "total");

        // Y en toda la tabla no aparece nada de quien llamó.
        List<?> filas = em.createNativeQuery("SELECT CONCAT(dia, '|', paso, '|', total) FROM alta_pasos").getResultList();
        for (Object f : filas) {
            assertThat(f.toString()).doesNotContain(quien.getEmail(), quien.getUsername(), "203.0.113.77",
                    "movil-de-prueba-gp019", "disp-gp019", "10:15");
        }

        // Solo ha cambiado la fila de hoy y PLAN, en uno.
        assertThat(total(PasoAlta.PLAN)).isEqualTo(antes + 1);
        assertThat(filas()).isIn(filasAntes, filasAntes + 1);
    }

    @Test
    @DisplayName("GET /admin/embudo: USER e invitado → 403; ADMIN → 8 semanas con los 10 pasos")
    void admin_embudo() throws Exception {
        for (Usuario u : List.of(attacker, guest)) {
            pedir(u, "GET /admin/embudo").andExpect(status().isForbidden());
        }
        Usuario admin = crearUsuario("__gp019_admin__", RoleType.ADMIN);
        pedir(admin, "GET /admin/embudo")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoy").value(LocalDate.now(MADRID).toString()))
                .andExpect(jsonPath("$.embudo.length()").value(8))
                .andExpect(jsonPath("$.embudo[7].pasos.length()").value(PasoAlta.values().length))
                .andExpect(jsonPath("$.embudo[7].pasos[0].paso").value("BIENVENIDA"))
                .andExpect(jsonPath("$.nucleo.length()").value(8))
                .andExpect(jsonPath("$.nucleo[7].completa").value(false));
    }
}
