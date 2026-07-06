package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.enums.Dificultad;
import com.gymprofit.api.enums.GrupoMuscular;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// CatalogoI18nTest — e2e de la localización del catálogo por Accept-Language.
// Verifica que los mappers (@AfterMapping + LocaleContextHolder) devuelven el
// texto EN cuando el request llega con Accept-Language: en y hay traducción
// en la entidad, y el ES en caso contrario (fallback), sin cambiar los DTOs.
// Reutiliza la infraestructura de AbstractOwnershipTest (contexto completo sin
// mocks + usuarios reales + rollback transaccional tras cada test).
// ============================================================
@DisplayName("i18n catálogo — Accept-Language resuelve ES/EN en los DTOs")
class CatalogoI18nTest extends AbstractOwnershipTest {

    // Nombres ES/EN del ejercicio de catálogo sembrado para el test.
    private static final String NOMBRE_ES = "Ejercicio i18n test";
    private static final String NOMBRE_EN = "i18n test exercise";

    @Autowired
    private IEjercicioRepository ejercicioRepository;

    // Id del ejercicio de catálogo sembrado con traducción EN.
    private Integer ejercicioId;

    // Siembra un ejercicio de catálogo global CON traducción EN (el helper de la
    // base no rellena los campos _en, así que se crea aquí uno propio).
    @BeforeEach
    void seedEjercicioI18n() {
        Ejercicio e = new Ejercicio();
        e.setNombre(NOMBRE_ES);
        e.setNombreEn(NOMBRE_EN);
        e.setGrupoMuscular(GrupoMuscular.values()[0]);
        e.setDificultad(Dificultad.values()[0]);
        e.setCaloriasQuemadas(10);
        e.setActivo(true);
        ejercicioId = ejercicioRepository.save(e).getId();
    }

    // Con Accept-Language: es el DTO mantiene el nombre en español.
    @Test
    @DisplayName("GET /ejercicios/{id} con Accept-Language: es devuelve el nombre ES")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getEjercicio_conAcceptLanguageEs_devuelveNombreEs() throws Exception {
        mockMvc.perform(get("/ejercicios/" + ejercicioId)
                        .header("Accept-Language", "es"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(NOMBRE_ES));
    }

    // Con Accept-Language: en el mapper sobreescribe el nombre con la traducción EN.
    @Test
    @DisplayName("GET /ejercicios/{id} con Accept-Language: en devuelve el nombre EN")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getEjercicio_conAcceptLanguageEn_devuelveNombreEn() throws Exception {
        mockMvc.perform(get("/ejercicios/" + ejercicioId)
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(NOMBRE_EN));
    }
}
