package com.gymprofit.api.integration;

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
}
