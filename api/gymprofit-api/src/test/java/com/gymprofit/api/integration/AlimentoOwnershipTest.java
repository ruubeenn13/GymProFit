package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
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
// AlimentoOwnershipTest — e2e del 403 IDOR sobre /alimentos/usuario/{usuarioId}.
//
// Mismo patrón que los logros: AlimentoService no comprobaba la propiedad y el GET de
// /alimentos/** entero estaba abierto a GUEST, que se obtiene sin credenciales. Los
// alimentos que crea un usuario no son catálogo —los escribe él y llevan su dieta
// dentro—, así que se separan de la búsqueda pública, que sigue abierta.
// ============================================================
@DisplayName("IDOR /alimentos/usuario/{id} — los alimentos propios no son catálogo")
class AlimentoOwnershipTest extends AbstractOwnershipTest {

    @Autowired
    private IAlimentoRepository alimentoRepository;

    // Siembra un alimento personalizado del owner. Se hace por repositorio y no por el
    // servicio porque aquí lo que importa es que exista una fila con usuario_id = owner,
    // no cómo se creó.
    @BeforeEach
    void seedAlimentoDelOwner() {
        Alimento propio = new Alimento();
        propio.setNombre("Alimento propio IDOR test");
        propio.setCalorias(250);
        propio.setActivo(true);
        propio.setUsuario(owner);
        alimentoRepository.save(propio);
    }

    @Test
    @DisplayName("GET alimentos de otro usuario → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getAlimentosAjenos_devuelve403() throws Exception {
        mockMvc.perform(get("/alimentos/usuario/" + owner.getId()))
                .andExpect(status().isForbidden());
    }

    /**
     * Pide <b>su propio</b> id, no el del owner: con el id ajeno el 403 lo daría ya
     * {@code checkOwnership} y el test pasaría aunque SecurityConfig siguiera abriendo la
     * ruta a GUEST. Pidiendo el suyo, lo único que puede rechazarlo es la regla de rol.
     */
    @Test
    @DisplayName("GET alimentos propios con token GUEST → 403 (la ruta no admite ese rol)")
    @WithUserDetails(value = GUEST, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getAlimentosConTokenGuest_devuelve403() throws Exception {
        mockMvc.perform(get("/alimentos/usuario/" + guest.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET alimentos propios → 200 con el alimento sembrado (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getAlimentosPropios_devuelve200ConDatos() throws Exception {
        mockMvc.perform(get("/alimentos/usuario/" + owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /**
     * El catálogo general sigue abierto al invitado: la app deja mirar alimentos antes de
     * registrarse y esa es la puerta de entrada al producto. Si este test se pusiera en
     * rojo, el arreglo habría cerrado de más.
     */
    @Test
    @DisplayName("GET catálogo de alimentos con token GUEST → 200 (sigue siendo público)")
    @WithUserDetails(value = GUEST, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getCatalogoConTokenGuest_sigue200() throws Exception {
        mockMvc.perform(get("/alimentos/activos"))
                .andExpect(status().isOk());
    }
}
