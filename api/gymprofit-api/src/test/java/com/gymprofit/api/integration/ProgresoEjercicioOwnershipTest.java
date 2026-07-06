package com.gymprofit.api.integration;

import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioCreateDTO;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.service.progresoejercicio.IProgresoEjercicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ============================================================
// ProgresoEjercicioOwnershipTest — e2e del 403 IDOR sobre /progreso-ejercicios.
// Ownership DIRECTO: el progreso lleva usuarioId, así que checkOwnership compara
// el usuarioId del recurso con el usuario autenticado. Siembra un progreso del owner
// (usando un ejercicio del catálogo global ya sembrado por Flyway) y verifica que
// el atacante NO puede leerlo/borrarlo/listarlo por usuario (403), pero el dueño sí (200).
// ============================================================
@DisplayName("IDOR /progreso-ejercicios — un usuario no accede al progreso de otro")
class ProgresoEjercicioOwnershipTest extends AbstractOwnershipTest {

    @Autowired
    private IProgresoEjercicioService progresoEjercicioService;

    @Autowired
    private IEjercicioRepository ejercicioRepository;

    // Id de un progreso cuyo dueño es OWNER.
    private Integer progresoIdOwner;

    // Siembra un progreso como el owner. El ejercicioId se toma del catálogo global
    // (findAll -> primero); el service fuerza usuarioId = usuario autenticado.
    @BeforeEach
    void seedProgreso() {
        // Coge un ejercicio ya existente del catálogo (sembrado por Flyway), no lo crea.
        // Siembra el ejercicio del catálogo (CI no lo trae por Flyway).
        Integer ejercicioId = crearEjercicioCatalogo().getId();

        runAs(owner, () -> {
            ProgresoEjercicioCreateDTO dto = new ProgresoEjercicioCreateDTO();
            dto.setUsuarioId(owner.getId());
            dto.setEjercicioId(ejercicioId);
            // Métricas válidas mínimas (todas @PositiveOrZero / @Min(0)).
            dto.setMejorPeso(BigDecimal.valueOf(50));
            dto.setMejorRepeticiones(10);
            dto.setMejorTiempoSegundos(0);
            progresoIdOwner = progresoEjercicioService.save(dto).getId();
        });
    }

    @Test
    @DisplayName("GET progreso ajeno → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getProgresoAjeno_devuelve403() throws Exception {
        mockMvc.perform(get("/progreso-ejercicios/" + progresoIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE progreso ajeno → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void deleteProgresoAjeno_devuelve403() throws Exception {
        mockMvc.perform(delete("/progreso-ejercicios/" + progresoIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET progreso-ejercicios/usuario/{otro} → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getProgresoDeOtroUsuario_devuelve403() throws Exception {
        mockMvc.perform(get("/progreso-ejercicios/usuario/" + owner.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET progreso propio → 200 (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getProgresoPropio_devuelve200() throws Exception {
        mockMvc.perform(get("/progreso-ejercicios/" + progresoIdOwner))
                .andExpect(status().isOk());
    }
}
