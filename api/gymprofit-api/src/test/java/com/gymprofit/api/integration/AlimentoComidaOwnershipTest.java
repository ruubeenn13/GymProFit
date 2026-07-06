package com.gymprofit.api.integration;

import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaCreateDTO;
import com.gymprofit.api.dto.entity.comida.ComidaCreateDTO;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.service.alimentocomida.IAlimentoComidaService;
import com.gymprofit.api.service.comida.IComidaService;
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
// AlimentoComidaOwnershipTest — e2e del 403 IDOR sobre /alimentos-comida.
// El ownership aquí es INDIRECTO: una relación alimento-comida no tiene dueño
// propio, sino que pertenece al usuario dueño de la comida asociada
// (comida.getUsuario()). Se siembra una comida del owner + una relación
// alimento-comida sobre ella, y se verifica que el atacante no puede
// leerla/borrarla ni listar los alimentos de esa comida ajena (403),
// mientras que el dueño sí (200).
// ============================================================
@DisplayName("IDOR /alimentos-comida — un usuario no accede a los alimentos de la comida de otro")
class AlimentoComidaOwnershipTest extends AbstractOwnershipTest {

    @Autowired
    private IComidaService comidaService;

    @Autowired
    private IAlimentoComidaService alimentoComidaService;

    // Catálogo global de alimentos (sembrado por Flyway); se lee, no se crea.
    @Autowired
    private IAlimentoRepository alimentoRepository;

    // Id de la comida cuyo dueño es OWNER.
    private Integer comidaIdOwner;
    // Id de la relación alimento-comida asociada a esa comida.
    private Integer alimentoComidaIdOwner;

    // Siembra como owner: una comida propia + una relación alimento-comida sobre ella.
    // El service de comida fuerza usuarioId = usuario autenticado; el alimento se toma
    // del catálogo global ya existente.
    @BeforeEach
    void seedAlimentoComida() {
        runAs(owner, () -> {
            // 1) Comida del owner (mismo patrón que ComidaOwnershipTest).
            ComidaCreateDTO comidaDTO = new ComidaCreateDTO();
            comidaDTO.setUsuarioId(owner.getId());
            comidaDTO.setTipoComida("DESAYUNO");
            comidaIdOwner = comidaService.save(comidaDTO).getId();

            // 2) Alimento del catálogo global (findAll → primer elemento).
            Integer alimentoId = alimentoRepository.findAll().get(0).getId();

            // 3) Relación alimento-comida sobre la comida del owner.
            AlimentoComidaCreateDTO acDTO = new AlimentoComidaCreateDTO();
            acDTO.setComidaId(comidaIdOwner);
            acDTO.setAlimentoId(alimentoId);
            acDTO.setCantidadGramos(new BigDecimal("100"));
            alimentoComidaIdOwner = alimentoComidaService.save(acDTO).getId();
        });
    }

    @Test
    @DisplayName("GET alimento-comida ajeno → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getAlimentoComidaAjeno_devuelve403() throws Exception {
        mockMvc.perform(get("/alimentos-comida/" + alimentoComidaIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE alimento-comida ajeno → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void deleteAlimentoComidaAjeno_devuelve403() throws Exception {
        mockMvc.perform(delete("/alimentos-comida/" + alimentoComidaIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET alimentos-comida/comida/{comidaAjena} → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getAlimentosDeComidaAjena_devuelve403() throws Exception {
        mockMvc.perform(get("/alimentos-comida/comida/" + comidaIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET alimento-comida propio → 200 (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getAlimentoComidaPropio_devuelve200() throws Exception {
        mockMvc.perform(get("/alimentos-comida/" + alimentoComidaIdOwner))
                .andExpect(status().isOk());
    }
}
