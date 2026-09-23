package com.gymprofit.api.integration;

import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.service.rutina.IRutinaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// ListaVaciaContratoTest — e2e de la convención de DEC-033.
//
// Una colección que existe y está vacía responde 200 con [], y el 404 queda para
// que el recurso PADRE de la ruta no exista. Hasta GP-069 la API respondía 404 a
// las dos cosas —58 sitios en 13 controladores— y la app no podía distinguir «no
// tienes datos» de «ha fallado algo»: por eso cada pantalla llevaba su apaño.
//
// Hace falta el contexto completo (como AbstractOwnershipTest): los tests de
// controlador con el service simulado nunca ejecutan ni la comprobación de
// existencia ni la de propiedad, así que no verían ninguna de las dos respuestas.
// ============================================================
@DisplayName("DEC-033 — lista vacía es 200 con [], y el 404 es del recurso padre")
class ListaVaciaContratoTest extends AbstractOwnershipTest {

    // Admin propio del test: hace falta para llegar a los endpoints que exigen ADMIN
    // (/sesiones/rutina/{id}) y para pedir el id de OTRO usuario sin comerse el 403,
    // que es donde se ve si el 404 por usuario inexistente existe de verdad.
    private static final String ADMIN = "__contrato_admin__";

    // Id que no existe en ninguna tabla. Se usa como recurso padre inexistente.
    private static final int ID_INEXISTENTE = 999999;

    @Autowired
    private IRutinaService rutinaService;

    // Rutina propia del owner, sin ninguna sesión asociada: es el caso que separa
    // «la rutina no existe» de «la rutina existe y no tiene sesiones».
    private Integer rutinaIdSinSesiones;

    @BeforeEach
    void seedContrato() {
        crearUsuario(ADMIN, RoleType.ADMIN);

        runAs(owner, () -> {
            RutinaCreateDTO dto = new RutinaCreateDTO();
            dto.setUsuarioId(owner.getId());
            dto.setNombre("Rutina sin sesiones");
            dto.setNivel("INTERMEDIO");
            dto.setEsPredefinida(false);
            rutinaIdSinSesiones = rutinaService.save(dto).getId();
        });
    }

    // ── Colección vacía → 200 con [] ──────────────────────────────────────────
    //
    // El owner recién creado no tiene NADA: ni sesiones, ni mediciones, ni
    // notificaciones, ni objetivos, ni progresos, ni comidas. Antes las seis rutas
    // respondían 404 y la app lo leía como error de red.

    @Test
    @DisplayName("GET sesiones/usuario/{propio} sin sesiones → 200 con []")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void sesionesDeUsuarioSinDatos_devuelve200Vacio() throws Exception {
        mockMvc.perform(get("/sesiones/usuario/" + owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET mediciones-corporales/usuario/{propio}/ordenadas sin datos → 200 con []")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void medicionesDeUsuarioSinDatos_devuelve200Vacio() throws Exception {
        mockMvc.perform(get("/mediciones-corporales/usuario/" + owner.getId() + "/ordenadas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET notificaciones/usuario/{propio} sin datos → 200 con []")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void notificacionesDeUsuarioSinDatos_devuelve200Vacio() throws Exception {
        mockMvc.perform(get("/notificaciones/usuario/" + owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET objetivos-personales/usuario/{propio} sin datos → 200 con []")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void objetivosDeUsuarioSinDatos_devuelve200Vacio() throws Exception {
        mockMvc.perform(get("/objetivos-personales/usuario/" + owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET progreso-ejercicios/usuario/{propio} sin datos → 200 con []")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void progresosDeUsuarioSinDatos_devuelve200Vacio() throws Exception {
        mockMvc.perform(get("/progreso-ejercicios/usuario/" + owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET comidas/usuario/{propio} sin datos → 200 con []")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void comidasDeUsuarioSinDatos_devuelve200Vacio() throws Exception {
        mockMvc.perform(get("/comidas/usuario/" + owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── Las dos respuestas del ejemplo del encargo ────────────────────────────
    //
    // /sesiones/rutina/{id} daba 404 igual si la rutina no existía que si existía
    // sin sesiones. Son dos cosas distintas y ahora se distinguen.

    @Test
    @DisplayName("GET sesiones/rutina/{existente} sin sesiones → 200 con []")
    @WithUserDetails(value = ADMIN, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void sesionesDeRutinaExistenteSinSesiones_devuelve200Vacio() throws Exception {
        mockMvc.perform(get("/sesiones/rutina/" + rutinaIdSinSesiones))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET sesiones/rutina/{inexistente} → 404")
    @WithUserDetails(value = ADMIN, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void sesionesDeRutinaInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/sesiones/rutina/" + ID_INEXISTENTE))
                .andExpect(status().isNotFound());
    }

    // ── Recurso padre inexistente → 404, comprobado explícitamente ────────────

    @Test
    @DisplayName("GET sesiones/usuario/{inexistente} → 404")
    @WithUserDetails(value = ADMIN, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void sesionesDeUsuarioInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/sesiones/usuario/" + ID_INEXISTENTE))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET mediciones-corporales/usuario/{inexistente}/ordenadas → 404")
    @WithUserDetails(value = ADMIN, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void medicionesDeUsuarioInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/mediciones-corporales/usuario/" + ID_INEXISTENTE + "/ordenadas"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET rutinas-ejercicios/ejercicio/{inexistente} → 404")
    @WithUserDetails(value = ADMIN, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void rutinasDeEjercicioInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/rutinas-ejercicios/ejercicio/" + ID_INEXISTENTE))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET ejercicios-realizados/ejercicio/{inexistente} → 404")
    @WithUserDetails(value = ADMIN, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void ejerciciosRealizadosDeEjercicioInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/ejercicios-realizados/ejercicio/" + ID_INEXISTENTE))
                .andExpect(status().isNotFound());
    }

    // ── El 403 sigue ganando al 404 ───────────────────────────────────────────
    //
    // A quien no es dueño no se le dice si el id existe: la comprobación de
    // propiedad va antes que la de existencia (DEC-027 no se toca).

    @Test
    @DisplayName("GET sesiones/usuario/{ajeno} sigue siendo 403, no 404")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void sesionesDeUsuarioAjeno_sigue403() throws Exception {
        mockMvc.perform(get("/sesiones/usuario/" + owner.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET sesiones/usuario/{inexistente} desde un USER es 403, no 404")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void sesionesDeUsuarioInexistenteDesdeUser_es403() throws Exception {
        mockMvc.perform(get("/sesiones/usuario/" + ID_INEXISTENTE))
                .andExpect(status().isForbidden());
    }
}
