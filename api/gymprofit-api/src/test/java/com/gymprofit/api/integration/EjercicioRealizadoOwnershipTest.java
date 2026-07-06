package com.gymprofit.api.integration;

import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoCreateDTO;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.service.ejerciciorealizado.IEjercicioRealizadoService;
import com.gymprofit.api.service.sesionentrenamiento.ISesionEntrenamientoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ============================================================
// EjercicioRealizadoOwnershipTest — e2e del 403 IDOR sobre /ejercicios-realizados.
// Ownership INDIRECTO: el ejercicio realizado no lleva usuarioId directo, la propiedad
// se resuelve vía sesion.getUsuario(). Siembra como owner una Sesion + un EjercicioRealizado
// (con un ejercicio del catálogo global ya sembrado por Flyway) y verifica que el atacante
// NO puede leerlo/borrarlo/listar por sesión (403), pero el dueño sí (200).
// ============================================================
@DisplayName("IDOR /ejercicios-realizados — un usuario no accede a los de otro (vía sesión)")
class EjercicioRealizadoOwnershipTest extends AbstractOwnershipTest {

    @Autowired
    private IEjercicioRealizadoService ejercicioRealizadoService;

    @Autowired
    private ISesionEntrenamientoService sesionEntrenamientoService;

    @Autowired
    private IEjercicioRepository ejercicioRepository;

    // Id del ejercicio realizado cuyo dueño (vía sesión) es OWNER.
    private Integer ejercicioRealizadoIdOwner;

    // Id de la sesión propiedad del OWNER a la que pertenece el ejercicio realizado.
    private Integer sesionIdOwner;

    // Siembra como owner: primero una Sesion (usuarioId=owner), luego un EjercicioRealizado
    // dentro de esa sesión, usando un ejercicioId del catálogo global (no lo crea).
    @BeforeEach
    void seedEjercicioRealizado() {
        // Coge un ejercicio ya existente del catálogo (sembrado por Flyway), no lo crea.
        Integer ejercicioId = ejercicioRepository.findAll().get(0).getId();

        runAs(owner, () -> {
            // 1) Sesion del owner (solo usuarioId es @NotNull; el resto opcional).
            SesionEntrenamientoCreateDTO sesionDto = new SesionEntrenamientoCreateDTO();
            sesionDto.setUsuarioId(owner.getId());
            sesionDto.setFechaInicio(LocalDateTime.now());
            sesionDto.setDuracionMinutos(60);
            sesionDto.setCompletada(false);
            sesionIdOwner = sesionEntrenamientoService.save(sesionDto).getId();

            // 2) EjercicioRealizado dentro de esa sesión (sesionId + ejercicioId @NotNull).
            EjercicioRealizadoCreateDTO dto = new EjercicioRealizadoCreateDTO();
            dto.setSesionId(sesionIdOwner);
            dto.setEjercicioId(ejercicioId);
            // Métricas válidas mínimas (@Min(0) / @PositiveOrZero).
            dto.setSeriesCompletadas(3);
            dto.setRepeticionesReales(10);
            dto.setPesoUsado(BigDecimal.valueOf(40));
            dto.setTiempoSegundos(0);
            ejercicioRealizadoIdOwner = ejercicioRealizadoService.save(dto).getId();
        });
    }

    @Test
    @DisplayName("GET ejercicio realizado ajeno → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getEjercicioRealizadoAjeno_devuelve403() throws Exception {
        mockMvc.perform(get("/ejercicios-realizados/" + ejercicioRealizadoIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE ejercicio realizado ajeno → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void deleteEjercicioRealizadoAjeno_devuelve403() throws Exception {
        mockMvc.perform(delete("/ejercicios-realizados/" + ejercicioRealizadoIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET ejercicios-realizados/sesion/{sesionAjena} → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getEjerciciosDeSesionAjena_devuelve403() throws Exception {
        mockMvc.perform(get("/ejercicios-realizados/sesion/" + sesionIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET ejercicio realizado propio → 200 (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getEjercicioRealizadoPropio_devuelve200() throws Exception {
        mockMvc.perform(get("/ejercicios-realizados/" + ejercicioRealizadoIdOwner))
                .andExpect(status().isOk());
    }
}
