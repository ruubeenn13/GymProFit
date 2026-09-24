package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
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
    // Ejercicio del catálogo con progreso del owner (GP-048).
    private Integer ejercicioGp048;

    // Siembra un progreso como el owner. El ejercicioId se toma del catálogo global
    // (findAll -> primero); el service fuerza usuarioId = usuario autenticado.
    @BeforeEach
    void seedProgreso() {
        // Coge un ejercicio ya existente del catálogo (sembrado por Flyway), no lo crea.
        // Siembra el ejercicio del catálogo (CI no lo trae por Flyway).
        Integer ejercicioId = crearEjercicioCatalogo().getId();
        ejercicioGp048 = ejercicioId;

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
        return Map.of("progreso", progresoIdOwner, "owner", owner.getId(), "ejercicio", ejercicioGp048);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "PATCH /progreso-ejercicios/{progreso}",
            "GET /progreso-ejercicios/usuario/{owner}/ordenados",
            "GET /progreso-ejercicios/usuario/{owner}/ejercicio/{ejercicio}",
            "GET /progreso-ejercicios/usuario/{owner}/ejercicio/{ejercicio}/historial",
            "GET /progreso-ejercicios/usuario/{owner}/ejercicio/{ejercicio}/ultimo",
            "GET /progreso-ejercicios/usuario/{owner}/record-destacado",
            "GET /progreso-ejercicios/count/usuario/{owner}",
            "GET /progreso-ejercicios/exists/usuario/{owner}/ejercicio/{ejercicio}",
            "DELETE /progreso-ejercicios/usuario/{owner}/ejercicio/{ejercicio}",
            "DELETE /progreso-ejercicios/usuario/{owner}"
    })
    @DisplayName("GP-048: id ajeno → 403 al atacante; el dueño no")
    void gp048_idAjeno(String plantilla) throws Exception {
        idAjeno(rellenar(plantilla, idsGp048()));
    }

    @Test
    @DisplayName("GP-048: GET /progreso-ejercicios (todos) → solo ADMIN")
    void gp048_soloAdmin() throws Exception {
        soloAdmin("GET /progreso-ejercicios");
    }

    @Test
    @DisplayName("GP-048 (fallo real): el progreso por ejercicio del catálogo no enseña el de otros")
    void gp048_porEjercicio_aislado() throws Exception {
        String ruta = "GET /progreso-ejercicios/ejercicio/" + ejercicioGp048;
        String alAtacante = pedir(attacker, ruta).andReturn().getResponse().getContentAsString();
        assertThat(alAtacante).as("atacante en " + ruta).isEqualTo("[]");
        String alDueno = pedir(owner, ruta).andReturn().getResponse().getContentAsString();
        assertThat(alDueno).as("dueño en " + ruta).contains("\"id\":" + progresoIdOwner);
    }

    @Test
    @DisplayName("GP-048 (fallo real): el contador por ejercicio del catálogo no cuenta el de otros")
    void gp048_countPorEjercicio_aislado() throws Exception {
        String ruta = "GET /progreso-ejercicios/count/ejercicio/" + ejercicioGp048;
        assertThat(pedir(attacker, ruta).andReturn().getResponse().getContentAsString()).contains("\"count\":0");
        assertThat(pedir(owner, ruta).andReturn().getResponse().getContentAsString()).contains("\"count\":1");
    }

    @Test
    @DisplayName("GP-048: PUT /progreso-ejercicios con el id de un progreso ajeno en el cuerpo → 403")
    void gp048_putConIdAjeno() throws Exception {
        assertThat(pedir(attacker, "PUT /progreso-ejercicios", "{\"id\":" + progresoIdOwner + "}")
                .andReturn().getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("GP-048: GUEST pidiendo SU propio progreso → 403 (regla de rol)")
    void gp048_guestConSuPropioId() throws Exception {
        assertThat(estado(guest, "GET /progreso-ejercicios/usuario/" + guest.getId())).isEqualTo(403);
    }
}
