package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalCreateDTO;
import com.gymprofit.api.service.medicioncorporal.IMedicionCorporalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ============================================================
// MedicionCorporalOwnershipTest — e2e del 403 IDOR sobre /mediciones-corporales.
// Ownership DIRECTO: la medición referencia usuarioId. Siembra una medición
// propiedad del owner y verifica que el atacante NO puede leerla/borrarla ni
// listar las del owner (403), mientras que el dueño sí (200).
// ============================================================
@DisplayName("IDOR /mediciones-corporales — un usuario no accede a mediciones de otro")
class MedicionCorporalOwnershipTest extends AbstractOwnershipTest {

    @Autowired
    private IMedicionCorporalService medicionCorporalService;

    // Id de una medición cuyo dueño es OWNER.
    private Integer medicionIdOwner;

    // Crea la medición como el owner (el service fuerza usuarioId = usuario autenticado).
    // Campos obligatorios del CreateDTO: usuarioId (@NotNull) y peso (@NotNull @Positive).
    @BeforeEach
    void seedMedicion() {
        runAs(owner, () -> {
            MedicionCorporalCreateDTO dto = new MedicionCorporalCreateDTO();
            dto.setUsuarioId(owner.getId());
            dto.setPeso(new BigDecimal("75.5"));
            medicionIdOwner = medicionCorporalService.save(dto).getId();
        });
    }

    @Test
    @DisplayName("GET medición ajena → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getMedicionAjena_devuelve403() throws Exception {
        mockMvc.perform(get("/mediciones-corporales/" + medicionIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE medición ajena → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void deleteMedicionAjena_devuelve403() throws Exception {
        mockMvc.perform(delete("/mediciones-corporales/" + medicionIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET mediciones-corporales/usuario/{otro} → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getMedicionesDeOtroUsuario_devuelve403() throws Exception {
        mockMvc.perform(get("/mediciones-corporales/usuario/" + owner.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET medición propia → 200 (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getMedicionPropia_devuelve200() throws Exception {
        mockMvc.perform(get("/mediciones-corporales/" + medicionIdOwner))
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
        return Map.of("medicion", medicionIdOwner, "owner", owner.getId());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "PATCH /mediciones-corporales/{medicion}",
            "GET /mediciones-corporales/usuario/{owner}/ordenadas",
            "GET /mediciones-corporales/usuario/{owner}/ultimas",
            "GET /mediciones-corporales/usuario/{owner}/rango?inicio=2020-01-01T00:00:00&fin=2030-01-01T00:00:00"
    })
    @DisplayName("GP-048: id ajeno → 403 al atacante; el dueño no")
    void gp048_idAjeno(String plantilla) throws Exception {
        idAjeno(rellenar(plantilla, idsGp048()));
    }

    @Test
    @DisplayName("GP-048: GET /mediciones-corporales (todas) → solo ADMIN")
    void gp048_soloAdmin() throws Exception {
        soloAdmin("GET /mediciones-corporales");
    }

    @Test
    @DisplayName("GP-048: PUT /mediciones-corporales con el id de una medición ajena en el cuerpo → 403")
    void gp048_putConIdAjeno() throws Exception {
        assertThat(pedir(attacker, "PUT /mediciones-corporales", "{\"id\":" + medicionIdOwner + "}")
                .andReturn().getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("GP-048: GUEST pidiendo SUS propias mediciones → 403 (regla de rol)")
    void gp048_guestConSuPropioId() throws Exception {
        assertThat(estado(guest, "GET /mediciones-corporales/usuario/" + guest.getId())).isEqualTo(403);
    }
}
