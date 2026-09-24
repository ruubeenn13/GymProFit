package com.gymprofit.api.integration;

import com.gymprofit.api.entity.SesionEntrenamiento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.repository.jpa.ISesionEntrenamientoRepository;
import com.gymprofit.api.service.logro.ILogroService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// LogroProgresoTest — GP-079: GET /logros/progreso, con el contexto entero.
//
// Lo que tiene que fijar es el BORDE: con 6 sesiones, «Constancia» (umbral 7)
// va 6 de 7 y sin conceder; con la 7.ª, 7 de 7 y concedido. El progreso y la
// concesión leen el umbral del mismo sitio (TipoLogro), y este test es el que
// se pondría rojo si alguien volviera a escribir un 7 a mano en uno de los dos.
//
// La ruta no lleva id, así que no hay id ajeno que rechazar: lo que se prueba es
// que cada token ve lo suyo (DEC-013) y que un invitado no entra.
// ============================================================
@DisplayName("GET /logros/progreso — progreso y concesión coinciden en el borde")
class LogroProgresoTest extends AbstractOwnershipTest {

    private static final String CONSTANCIA = "$[?(@.tipo == 'CONSTANCIA')]";
    private static final String PRIMERA = "$[?(@.tipo == 'PRIMERA_SESION')]";

    @Autowired
    private ISesionEntrenamientoRepository sesionRepository;

    @Autowired
    private ILogroService logroService;

    // Registra n sesiones completadas del usuario y pasa la evaluación, como hace
    // el guardado real de una sesión.
    private void sesionesCompletadas(Usuario usuario, int n) {
        for (int i = 0; i < n; i++) {
            SesionEntrenamiento s = new SesionEntrenamiento();
            s.setUsuario(usuario);
            s.setFechaInicio(LocalDateTime.now().minusDays(n - i).minusHours(1));
            s.setFechaFin(LocalDateTime.now().minusDays(n - i));
            s.setDuracionMinutos(60);
            s.setCompletada(true);
            sesionRepository.save(s);
        }
        logroService.evaluarLogros(usuario.getId());
    }

    @Test
    @DisplayName("Cuenta nueva → todo sin conseguir, Constancia 0 de 7")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void cuentaNueva_todoPendiente() throws Exception {
        mockMvc.perform(get("/logros/progreso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].conseguido", everyItem(org.hamcrest.Matchers.is(false))))
                .andExpect(jsonPath(CONSTANCIA + ".progreso", contains(0)))
                .andExpect(jsonPath(CONSTANCIA + ".umbral", contains(7)))
                .andExpect(jsonPath(CONSTANCIA + ".metrica", contains("SESIONES_COMPLETADAS")))
                .andExpect(jsonPath(CONSTANCIA + ".fechaObtenido", contains(nullValue())));
    }

    @Test
    @DisplayName("6 sesiones → Constancia 6 de 7 y SIN conceder")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void seisSesiones_seisDeSieteSinConceder() throws Exception {
        sesionesCompletadas(owner, 6);

        mockMvc.perform(get("/logros/progreso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CONSTANCIA + ".progreso", contains(6)))
                .andExpect(jsonPath(CONSTANCIA + ".conseguido", contains(false)))
                // La primera sesión sí está concedida y trae su fecha.
                .andExpect(jsonPath(PRIMERA + ".conseguido", contains(true)))
                .andExpect(jsonPath(PRIMERA + ".fechaObtenido", contains(notNullValue())));
    }

    @Test
    @DisplayName("7 sesiones → Constancia 7 de 7 y concedido")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void sieteSesiones_sieteDeSieteConcedido() throws Exception {
        sesionesCompletadas(owner, 6);
        sesionesCompletadas(owner, 1);

        mockMvc.perform(get("/logros/progreso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CONSTANCIA + ".progreso", contains(7)))
                .andExpect(jsonPath(CONSTANCIA + ".conseguido", contains(true)))
                .andExpect(jsonPath(CONSTANCIA + ".fechaObtenido", contains(notNullValue())));
    }

    @Test
    @DisplayName("El progreso no pasa del umbral: 8 sesiones siguen siendo 7 de 7")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void progresoRecortadoAlUmbral() throws Exception {
        sesionesCompletadas(owner, 8);

        mockMvc.perform(get("/logros/progreso"))
                .andExpect(jsonPath(CONSTANCIA + ".progreso", contains(7)))
                .andExpect(jsonPath(PRIMERA + ".progreso", contains(1)));
    }

    /**
     * Aislamiento: el dueño tiene seis sesiones y el atacante ninguna. Si la ruta
     * leyera de otro sitio que no fuera el token, aquí saldría un 6.
     */
    @Test
    @DisplayName("Cada token ve SU progreso, no el de otro usuario")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void cadaTokenVeLoSuyo() throws Exception {
        sesionesCompletadas(owner, 6);

        mockMvc.perform(get("/logros/progreso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CONSTANCIA + ".progreso", contains(0)))
                .andExpect(jsonPath(PRIMERA + ".conseguido", contains(false)));
    }

    /**
     * El invitado se consigue sin credenciales y no tiene progreso: la regla va antes
     * del GET general de /logros/**, que sí le deja ver el catálogo.
     */
    @Test
    @DisplayName("Token GUEST → 403")
    @WithUserDetails(value = GUEST, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void invitado_403() throws Exception {
        mockMvc.perform(get("/logros/progreso"))
                .andExpect(status().isForbidden());
    }
}
