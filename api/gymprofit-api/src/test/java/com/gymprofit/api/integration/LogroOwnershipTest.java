package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Logro;
import com.gymprofit.api.entity.UsuarioLogro;
import com.gymprofit.api.enums.TipoLogro;
import com.gymprofit.api.repository.jpa.ILogroRepository;
import com.gymprofit.api.repository.jpa.IUsuarioLogroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// LogroOwnershipTest — e2e del 403 IDOR sobre /logros/usuario/{usuarioId}.
//
// La ruta tenía dos agujeros a la vez: LogroService no comprobaba la propiedad, y
// SecurityConfig dejaba pasar TODO el GET /logros/** a GUEST, un rol que se obtiene sin
// credenciales. Con eso, los logros de cualquiera —que dicen cuánto entrena y desde
// cuándo— se leían iterando ids de usuario sin tener cuenta.
//
// El arreglo separa las dos cosas que se llamaban igual: el CATÁLOGO de logros
// (GET /logros) sigue siendo público, porque es el mismo para todo el mundo; los logros
// OBTENIDOS por un usuario, no. Por eso aquí se comprueban las dos caras.
// ============================================================
@DisplayName("IDOR /logros/usuario/{id} — los logros obtenidos no son catálogo")
class LogroOwnershipTest extends AbstractOwnershipTest {

    @Autowired
    private ILogroRepository logroRepository;

    @Autowired
    private IUsuarioLogroRepository usuarioLogroRepository;

    // Siembra un logro del catálogo y se lo concede al owner, para que el control positivo
    // demuestre que además de responder 200 devuelve el dato, y no una lista vacía.
    @BeforeEach
    void seedLogroDelOwner() {
        Logro logro = new Logro();
        logro.setNombre("Logro IDOR test");
        logro.setDescripcion("Sembrado por LogroOwnershipTest");
        logro.setTipo(TipoLogro.values()[0]);
        Logro guardado = logroRepository.save(logro);

        UsuarioLogro concedido = new UsuarioLogro();
        concedido.setUsuario(owner);
        concedido.setLogro(guardado);
        concedido.setFechaObtenido(LocalDateTime.now());
        usuarioLogroRepository.save(concedido);
    }

    @Test
    @DisplayName("GET logros de otro usuario → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getLogrosAjenos_devuelve403() throws Exception {
        mockMvc.perform(get("/logros/usuario/" + owner.getId()))
                .andExpect(status().isForbidden());
    }

    /**
     * El caso que hacía grave el fallo: el invitado no tiene que dar credenciales para
     * existir, así que si pudiera leer esto podría leerlo cualquiera.
     * <p>
     * Pide <b>su propio</b> id a propósito, no el del owner: con el id ajeno el 403 lo
     * daría ya {@code checkOwnership} y el test pasaría aunque SecurityConfig siguiera
     * dejando entrar a GUEST. Pidiendo el suyo, la propiedad se cumple y lo único que
     * puede rechazarlo es la regla de rol, que es justo la que hay que fijar aquí.
     */
    @Test
    @DisplayName("GET logros propios con token GUEST → 403 (la ruta no admite ese rol)")
    @WithUserDetails(value = GUEST, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getLogrosConTokenGuest_devuelve403() throws Exception {
        mockMvc.perform(get("/logros/usuario/" + guest.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET logros propios → 200 con el logro concedido (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getLogrosPropios_devuelve200ConDatos() throws Exception {
        mockMvc.perform(get("/logros/usuario/" + owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /**
     * El catálogo NO se cierra: es el mismo para todo el mundo y la pantalla de logros lo
     * pinta antes de saber cuáles ha desbloqueado el usuario. Si este test se pusiera en
     * rojo, el arreglo se habría pasado de frenada y habría roto al invitado.
     */
    @Test
    @DisplayName("GET catálogo de logros con token GUEST → 200 (sigue siendo público)")
    @WithUserDetails(value = GUEST, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getCatalogoConTokenGuest_sigue200() throws Exception {
        mockMvc.perform(get("/logros"))
                .andExpect(status().isOk());
    }
}
