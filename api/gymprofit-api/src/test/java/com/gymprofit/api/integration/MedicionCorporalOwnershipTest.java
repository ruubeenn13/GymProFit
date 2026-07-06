package com.gymprofit.api.integration;

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
}
