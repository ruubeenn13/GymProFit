package com.gymprofit.api.integration;

import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoCreateDTO;
import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.Nivel;
import com.gymprofit.api.repository.jpa.IRutinaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// SesionRutinaOwnershipTest — la rutina de una sesión también tiene dueño
//
// POST y PUT /sesiones forzaban el usuario desde el token, pero aceptaban
// después cualquier rutinaId. Un usuario podía enlazar su sesión con la rutina
// de otro: no filtraba datos —el DTO solo devuelve el id— pero dejaba una fila
// cruzada que la aplicación no puede crear, y que al borrar una cuenta obliga a
// tocar los datos de un tercero.
//
// Criterio de DEC-027, el mismo de los alimentos: plantilla del sistema (sin
// dueño) permitida, rutina de otro usuario → 403.
// ============================================================
@DisplayName("IDOR /sesiones — la rutina de la sesión también tiene dueño")
class SesionRutinaOwnershipTest extends AbstractOwnershipTest {

    @Autowired
    private IRutinaRepository rutinaRepository;

    private Integer rutinaDelOwner;
    private Integer plantillaDelSistema;

    @BeforeEach
    void sembrarRutinas() {
        rutinaDelOwner = crearRutina("Rutina privada del owner", owner).getId();
        plantillaDelSistema = crearRutina("Plantilla del sistema", null).getId();
    }

    @Test
    @DisplayName("empezar una sesión con la rutina de otro → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void sesionConRutinaAjena_devuelve403() throws Exception {
        // La sesión es suya —el usuario se fuerza desde el token— y por eso esto pasaba:
        // lo que no es suyo es la rutina.
        mockMvc.perform(post("/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sesionCon(rutinaDelOwner, attacker))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("empezar una sesión con una plantilla del sistema → 200 (el caso normal)")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void sesionConPlantilla_devuelve200() throws Exception {
        // Una rutina sin dueño es de todos: negarla rompería el arranque de cualquier
        // entrenamiento, que es de lo que hay que protegerse al cerrar una IDOR.
        mockMvc.perform(post("/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sesionCon(plantillaDelSistema, attacker))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("empezar una sesión con la rutina propia → 200 (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void sesionConRutinaPropia_devuelve200() throws Exception {
        mockMvc.perform(post("/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sesionCon(rutinaDelOwner, owner))))
                .andExpect(status().isOk());
    }

    // --- Andamiaje ----------------------------------------------------------

    // Rutina con el dueño indicado; null = plantilla del sistema.
    private Rutina crearRutina(String nombre, Usuario dueno) {
        Rutina rutina = new Rutina();
        rutina.setNombre(nombre);
        rutina.setNivel(Nivel.values()[0]);
        rutina.setUsuario(dueno);
        rutina.setActiva(true);
        return rutinaRepository.save(rutina);
    }

    // El usuarioId es @NotNull en el DTO, así que hay que mandarlo aunque el servicio lo
    // sobrescriba con el del token: sin él la petición ni llega, se queda en un 400.
    private SesionEntrenamientoCreateDTO sesionCon(Integer rutinaId, Usuario quienLlama) {
        SesionEntrenamientoCreateDTO dto = new SesionEntrenamientoCreateDTO();
        dto.setUsuarioId(quienLlama.getId());
        dto.setRutinaId(rutinaId);
        dto.setFechaInicio(LocalDateTime.now());
        dto.setFechaFin(LocalDateTime.now());
        return dto;
    }
}
