package com.gymprofit.api.integration;

import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioCreateDTO;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.service.rutina.IRutinaService;
import com.gymprofit.api.service.rutinaejercicio.IRutinaEjercicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// SinCaloriasEntrenamientoTest — la API no devuelve calorías de ENTRENAMIENTO.
//
// DEC-004 lo prohíbe desde el principio, y aun así el número seguía saliendo por
// cinco sitios: el @Formula de Rutina, los DTO de rutina, el catálogo de
// ejercicios, la sesión y las estadísticas del usuario. Los comentarios no lo
// impiden; un test sí, porque falla en cuanto alguien vuelve a añadir el campo.
//
// Se afirma sobre el JSON y no sobre la clase Java a propósito: lo que DEC-004
// prohíbe es ENSEÑAR el número, y lo que se enseña es lo que sale por el cable.
//
// La otra mitad del test es igual de importante: las calorías de NUTRICIÓN son
// reales —vienen del alimento, no de una estimación— y tienen que seguir ahí.
// ============================================================
@DisplayName("DEC-004 — la API no devuelve calorías de entrenamiento (y sí las de nutrición)")
class SinCaloriasEntrenamientoTest extends AbstractOwnershipTest {

    @Autowired
    private IRutinaService rutinaService;

    @Autowired
    private IRutinaEjercicioService rutinaEjercicioService;

    private Integer rutinaId;
    private Integer ejercicioId;

    @BeforeEach
    void seedRutinaConEjercicio() {
        runAs(owner, () -> {
            RutinaCreateDTO dto = new RutinaCreateDTO();
            dto.setUsuarioId(owner.getId());
            dto.setNombre("Rutina sin kcal");
            dto.setNivel("INTERMEDIO");
            dto.setEsPredefinida(false);
            rutinaId = rutinaService.save(dto).getId();

            Ejercicio ejercicio = crearEjercicioCatalogo();
            ejercicioId = ejercicio.getId();

            RutinaEjercicioCreateDTO re = new RutinaEjercicioCreateDTO();
            re.setRutinaId(rutinaId);
            re.setEjercicioId(ejercicioId);
            re.setSeries(4);
            re.setRepeticiones(10);
            re.setPesoRecomendado(new BigDecimal("20"));
            re.setTiempoDescanso(60);
            re.setOrden(1);
            rutinaEjercicioService.save(re);
        });
    }

    // ── Entrenamiento: el número no sale por ningún sitio ─────────────────────

    @Test
    @DisplayName("GET rutinas/{id} no trae caloriasAproximadas")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void rutina_sinCalorias() throws Exception {
        mockMvc.perform(get("/rutinas/" + rutinaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caloriasAproximadas").doesNotExist());
    }

    @Test
    @DisplayName("GET rutinas/usuario/{id} no trae caloriasAproximadas en ninguna fila")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void listaDeRutinas_sinCalorias() throws Exception {
        mockMvc.perform(get("/rutinas/usuario/" + owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].caloriasAproximadas").doesNotExist());
    }

    @Test
    @DisplayName("GET ejercicios/{id} no trae caloriasQuemadas")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void ejercicio_sinCalorias() throws Exception {
        mockMvc.perform(get("/ejercicios/" + ejercicioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caloriasQuemadas").doesNotExist());
    }

    @Test
    @DisplayName("GET rutinas-ejercicios/rutina/{id} no trae caloriasEjercicio")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void rutinaEjercicio_sinCalorias() throws Exception {
        mockMvc.perform(get("/rutinas-ejercicios/rutina/" + rutinaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].caloriasEjercicio").doesNotExist());
    }

    @Test
    @DisplayName("GET usuarios/{id}/estadisticas no trae totalCaloriasQuemadas")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void estadisticas_sinCalorias() throws Exception {
        mockMvc.perform(get("/usuarios/" + owner.getId() + "/estadisticas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCaloriasQuemadas").doesNotExist())
                .andExpect(jsonPath("$.totalMinutosEntrenados").exists());
    }

    // ── Nutrición: estas SÍ son reales y tienen que seguir ────────────────────

    @Test
    @DisplayName("GET alimentos/{id} SIGUE trayendo calorias: las de nutrición son reales")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void alimento_conserva_calorias() throws Exception {
        final Integer[] alimentoId = new Integer[1];
        runAs(owner, () -> alimentoId[0] = crearAlimentoCatalogo().getId());

        mockMvc.perform(get("/alimentos/" + alimentoId[0]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calorias").value(100));
    }
}
