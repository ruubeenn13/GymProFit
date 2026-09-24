package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
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
    // Ejercicio del catálogo que está en la rutina del owner (GP-048).
    private Integer ejercicioGp048;
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
            // Siembra el ejercicio del catálogo (CI no lo trae por Flyway).
            Integer ejercicioId = crearEjercicioCatalogo().getId();
            ejercicioGp048 = ejercicioId;

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

    /**
     * Hermano del anterior, y el que faltaba: {@code /ordenados} no hacía NI la
     * comprobación de acceso ni la de existencia, así que devolvía el contenido de la
     * rutina privada de otro en cuanto tenía ejercicios. El 404 por lista vacía tapaba
     * a medias el agujero; al pasar a 200 con [] (GP-069) habría quedado a la vista.
     */
    @Test
    @DisplayName("GET rutinas-ejercicios/rutina/{rutinaAjena}/ordenados → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getEjerciciosOrdenadosDeRutinaAjena_devuelve403() throws Exception {
        mockMvc.perform(get("/rutinas-ejercicios/rutina/" + rutinaIdOwner + "/ordenados"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET rutinas-ejercicios/rutina/{rutinaInexistente}/ordenados → 404")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getEjerciciosOrdenadosDeRutinaInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/rutinas-ejercicios/rutina/999999/ordenados"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET rutina-ejercicio propio → 200 (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getRutinaEjercicioPropio_devuelve200() throws Exception {
        mockMvc.perform(get("/rutinas-ejercicios/" + rutinaEjercicioIdOwner))
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
        return Map.of("re", rutinaEjercicioIdOwner, "rutina", rutinaIdOwner, "ejercicio", ejercicioGp048);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "PATCH /rutinas-ejercicios/{re}",
            "DELETE /rutinas-ejercicios/rutina/{rutina}"
    })
    @DisplayName("GP-048: rutina ajena → 403 al atacante; el dueño no")
    void gp048_idAjeno(String plantilla) throws Exception {
        idAjeno(rellenar(plantilla, idsGp048()));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "GET /rutinas-ejercicios/rutina/{rutina}/ejercicio/{ejercicio}",
            "GET /rutinas-ejercicios/count/rutina/{rutina}",
            "GET /rutinas-ejercicios/exists/rutina/{rutina}/ejercicio/{ejercicio}",
            "DELETE /rutinas-ejercicios/rutina/{rutina}/ejercicio/{ejercicio}"
    })
    @DisplayName("GP-048 (fallo real): hermanos sin comprobación → 403 al atacante; el dueño no")
    void gp048_hermanosSinComprobar(String plantilla) throws Exception {
        idAjeno(rellenar(plantilla, idsGp048()));
    }

    @Test
    @DisplayName("GP-048 (fallo real): el DELETE por rutina y ejercicio no borra de la rutina ajena")
    void gp048_deleteHermano_noBorra() throws Exception {
        pedir(attacker, "DELETE /rutinas-ejercicios/rutina/" + rutinaIdOwner + "/ejercicio/" + ejercicioGp048);
        assertThat(pedir(owner, "GET /rutinas-ejercicios/rutina/" + rutinaIdOwner)
                .andReturn().getResponse().getContentAsString()).contains("\"id\":" + rutinaEjercicioIdOwner);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "GET /rutinas-ejercicios/ejercicio/{ejercicio}",
            "GET /rutinas-ejercicios/count/ejercicio/{ejercicio}"
    })
    @DisplayName("GP-048 (fallo real): por ejercicio del catálogo no salen rutinas privadas ajenas")
    void gp048_porEjercicio_aislado(String plantilla) throws Exception {
        String ruta = rellenar(plantilla, idsGp048());
        String alAtacante = pedir(attacker, ruta).andReturn().getResponse().getContentAsString();
        String alDueno = pedir(owner, ruta).andReturn().getResponse().getContentAsString();
        assertThat(alAtacante).as("atacante en " + ruta).isIn("[]", "{\"count\":0}");
        assertThat(alDueno).as("dueño en " + ruta).isNotIn("[]", "{\"count\":0}");
    }

    @Test
    @DisplayName("GP-048: GET /rutinas-ejercicios (todos) → solo ADMIN")
    void gp048_soloAdmin() throws Exception {
        soloAdmin("GET /rutinas-ejercicios");
    }

    @Test
    @DisplayName("GP-048: PUT /rutinas-ejercicios con el id de uno ajeno en el cuerpo → 403")
    void gp048_putConIdAjeno() throws Exception {
        assertThat(pedir(attacker, "PUT /rutinas-ejercicios", "{\"id\":" + rutinaEjercicioIdOwner + "}")
                .andReturn().getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("GP-048: POST /rutinas-ejercicios en la rutina de otro → 403")
    void gp048_postEnRutinaAjena() throws Exception {
        assertThat(pedir(attacker, "POST /rutinas-ejercicios",
                "{\"rutinaId\":" + rutinaIdOwner + ",\"ejercicioId\":" + ejercicioGp048 + "}")
                .andReturn().getResponse().getStatus()).isEqualTo(403);
    }
}
