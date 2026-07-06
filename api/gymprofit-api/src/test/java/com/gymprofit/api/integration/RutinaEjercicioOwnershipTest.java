package com.gymprofit.api.integration;

import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioCreateDTO;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.service.rutina.IRutinaService;
import com.gymprofit.api.service.rutinaejercicio.IRutinaEjercicioService;
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
// RutinaEjercicioOwnershipTest — e2e del 403 IDOR sobre /rutinas-ejercicios.
// El ownership se resuelve en el service vía la rutina asociada
// (checkRutinaOwnership / checkRutinaReadAccess). MATIZ IMPORTANTE: las rutinas
// PREDEFINIDAS son de lectura pública y NO producen 403 en GET; el IDOR real
// aplica a una rutina PROPIA de OTRO usuario. Por eso se siembra una rutina
// NO predefinida del owner (esPredefinida=false) + una relación rutina-ejercicio
// sobre ella, y se verifica que el atacante no accede (403) y el dueño sí (200).
// ============================================================
@DisplayName("IDOR /rutinas-ejercicios — un usuario no accede a los ejercicios de la rutina propia de otro")
class RutinaEjercicioOwnershipTest extends AbstractOwnershipTest {

    @Autowired
    private IRutinaService rutinaService;

    @Autowired
    private IRutinaEjercicioService rutinaEjercicioService;

    // Catálogo global de ejercicios (sembrado por Flyway); se lee, no se crea.
    @Autowired
    private IEjercicioRepository ejercicioRepository;

    // Id de la rutina NO predefinida cuyo dueño es OWNER.
    private Integer rutinaIdOwner;
    // Id de la relación rutina-ejercicio asociada a esa rutina.
    private Integer rutinaEjercicioIdOwner;

    // Siembra como owner: una rutina propia no predefinida + una relación
    // rutina-ejercicio sobre ella. El service fuerza el propietario = usuario
    // autenticado; el ejercicio se toma del catálogo global ya existente.
    @BeforeEach
    void seedRutinaEjercicio() {
        runAs(owner, () -> {
            // 1) Rutina propia del owner, NO predefinida (para que el IDOR aplique).
            RutinaCreateDTO rutinaDTO = new RutinaCreateDTO();
            rutinaDTO.setUsuarioId(owner.getId());
            rutinaDTO.setNombre("Rutina IDOR test");
            rutinaDTO.setNivel("INTERMEDIO");
            rutinaDTO.setEsPredefinida(false);
            rutinaIdOwner = rutinaService.save(rutinaDTO).getId();

            // 2) Ejercicio del catálogo global (findAll → primer elemento).
            Integer ejercicioId = ejercicioRepository.findAll().get(0).getId();

            // 3) Relación rutina-ejercicio sobre la rutina del owner.
            RutinaEjercicioCreateDTO reDTO = new RutinaEjercicioCreateDTO();
            reDTO.setRutinaId(rutinaIdOwner);
            reDTO.setEjercicioId(ejercicioId);
            reDTO.setSeries(4);
            reDTO.setRepeticiones(10);
            reDTO.setPesoRecomendado(new BigDecimal("20"));
            reDTO.setTiempoDescanso(60);
            reDTO.setOrden(1);
            rutinaEjercicioIdOwner = rutinaEjercicioService.save(reDTO).getId();
        });
    }

    @Test
    @DisplayName("GET rutina-ejercicio de rutina ajena → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getRutinaEjercicioAjeno_devuelve403() throws Exception {
        mockMvc.perform(get("/rutinas-ejercicios/" + rutinaEjercicioIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE rutina-ejercicio de rutina ajena → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void deleteRutinaEjercicioAjeno_devuelve403() throws Exception {
        mockMvc.perform(delete("/rutinas-ejercicios/" + rutinaEjercicioIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET rutinas-ejercicios/rutina/{rutinaAjena} → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getEjerciciosDeRutinaAjena_devuelve403() throws Exception {
        mockMvc.perform(get("/rutinas-ejercicios/rutina/" + rutinaIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET rutina-ejercicio propio → 200 (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getRutinaEjercicioPropio_devuelve200() throws Exception {
        mockMvc.perform(get("/rutinas-ejercicios/" + rutinaEjercicioIdOwner))
                .andExpect(status().isOk());
    }
}
