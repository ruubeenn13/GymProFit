package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import com.gymprofit.api.dto.entity.comida.ComidaCreateDTO;
import com.gymprofit.api.service.comida.IComidaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ============================================================
// ComidaOwnershipTest — e2e del 403 IDOR sobre /comidas.
// Siembra una comida propiedad del owner y verifica que un segundo usuario (atacante)
// NO puede leerla/modificarla/borrarla (403), mientras que el dueño sí (2xx).
// ============================================================
@DisplayName("IDOR /comidas — un usuario no accede a comidas de otro")
class ComidaOwnershipTest extends AbstractOwnershipTest {

    @Autowired
    private IComidaService comidaService;

    // Id de una comida cuyo dueño es OWNER.
    private Integer comidaIdOwner;

    // Crea la comida como el owner (el service fuerza usuarioId = usuario autenticado).
    @BeforeEach
    void seedComida() {
        runAs(owner, () -> {
            ComidaCreateDTO dto = new ComidaCreateDTO();
            dto.setUsuarioId(owner.getId());
            dto.setTipoComida("DESAYUNO");
            comidaIdOwner = comidaService.save(dto).getId();
        });
    }

    @Test
    @DisplayName("GET comida ajena → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getComidaAjena_devuelve403() throws Exception {
        mockMvc.perform(get("/comidas/" + comidaIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE comida ajena → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void deleteComidaAjena_devuelve403() throws Exception {
        mockMvc.perform(delete("/comidas/" + comidaIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET comidas/usuario/{otro} → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getComidasDeOtroUsuario_devuelve403() throws Exception {
        mockMvc.perform(get("/comidas/usuario/" + owner.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET comida propia → 200 (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getComidaPropia_devuelve200() throws Exception {
        mockMvc.perform(get("/comidas/" + comidaIdOwner))
                .andExpect(status().isOk());
    }

    // --- Cobertura de GP-048 ----------------------------------------------------
    // Las rutas que no tenían test. Mismo criterio que SesionOwnershipTest: id ajeno →
    // 403 al atacante y el dueño no lo recibe; lo global, solo ADMIN; y la regla de rol
    // con el propio id del invitado.

    private Usuario adminGp048;

    private Usuario admin() {
        if (adminGp048 == null) adminGp048 = crearUsuario("__idor_admin__", RoleType.ADMIN);
        return adminGp048;
    }

    private void idAjeno(String ruta) throws Exception {
        assertThat(estado(attacker, ruta)).as("atacante en " + ruta).isEqualTo(403);
        assertThat(estado(owner, ruta)).as("dueño en " + ruta).isNotEqualTo(403);
    }

    private void soloAdmin(String ruta) throws Exception {
        assertThat(estado(attacker, ruta)).as("USER en " + ruta).isEqualTo(403);
        assertThat(estado(admin(), ruta)).as("ADMIN en " + ruta).isNotEqualTo(403);
    }

    private Map<String, Object> idsGp048() {
        return Map.of("comida", comidaIdOwner, "owner", owner.getId(), "fecha", java.time.LocalDate.now(),
                "tipo", "DESAYUNO");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "PATCH /comidas/{comida}",
            "GET /comidas/usuario/{owner}/fecha/{fecha}",
            "GET /comidas/usuario/{owner}/resumen?inicio={fecha}&fin={fecha}",
            "GET /comidas/usuario/{owner}/tipo/{tipo}",
            "GET /comidas/count/usuario/{owner}",
            "GET /comidas/count/usuario/{owner}/tipo/{tipo}"
    })
    @DisplayName("GP-048: id ajeno → 403 al atacante; el dueño no")
    void gp048_idAjeno(String plantilla) throws Exception {
        idAjeno(rellenar(plantilla, idsGp048()));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "GET /comidas",
            "GET /comidas/tipo/{tipo}",
            "GET /comidas/fecha/{fecha}",
            "GET /comidas/count/tipo/{tipo}"
    })
    @DisplayName("GP-048: listado global → solo ADMIN")
    void gp048_soloAdmin(String plantilla) throws Exception {
        soloAdmin(rellenar(plantilla, idsGp048()));
    }

    @Test
    @DisplayName("GP-048: PUT /comidas con el id de una comida ajena en el cuerpo → 403")
    void gp048_putConIdAjeno() throws Exception {
        assertThat(pedir(attacker, "PUT /comidas", "{\"id\":" + comidaIdOwner + ",\"tipoComida\":\"CENA\"}")
                .andReturn().getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("GP-048: GUEST pidiendo SUS propias comidas → 403 (regla de rol)")
    void gp048_guestConSuPropioId() throws Exception {
        assertThat(estado(guest, "GET /comidas/usuario/" + guest.getId())).isEqualTo(403);
    }
}
