package com.gymprofit.api.integration;

import com.gymprofit.api.dto.entity.notificacion.NotificacionCreateDTO;
import com.gymprofit.api.enums.TipoNotificacion;
import com.gymprofit.api.service.notificacion.INotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ============================================================
// NotificacionOwnershipTest — e2e del 403 IDOR sobre /notificaciones.
// Ownership DIRECTO: la notificación referencia usuarioId (el service fuerza el
// usuarioId del token al crearla). Siembra una notificación propiedad del owner y
// verifica que el atacante NO puede leerla/borrarla/marcarla como leída ni listar
// las del owner (403), mientras que el dueño sí (200).
// ============================================================
@DisplayName("IDOR /notificaciones — un usuario no accede a notificaciones de otro")
class NotificacionOwnershipTest extends AbstractOwnershipTest {

    @Autowired
    private INotificacionService notificacionService;

    // Id de una notificación cuyo dueño es OWNER.
    private Integer notificacionIdOwner;

    // Crea la notificación como el owner (el service fuerza usuarioId = usuario autenticado).
    // Campos obligatorios del CreateDTO: usuarioId (@NotNull), titulo (@NotBlank),
    // mensaje (@NotBlank) y tipo (@NotBlank, validado contra TipoNotificacion).
    @BeforeEach
    void seedNotificacion() {
        runAs(owner, () -> {
            NotificacionCreateDTO dto = new NotificacionCreateDTO();
            dto.setUsuarioId(owner.getId());
            dto.setTitulo("Recordatorio de prueba");
            dto.setMensaje("Mensaje de prueba");
            dto.setTipo(TipoNotificacion.RECORDATORIO.name());
            notificacionIdOwner = notificacionService.save(dto).getId();
        });
    }

    @Test
    @DisplayName("GET notificación ajena → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getNotificacionAjena_devuelve403() throws Exception {
        mockMvc.perform(get("/notificaciones/" + notificacionIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE notificación ajena → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void deleteNotificacionAjena_devuelve403() throws Exception {
        mockMvc.perform(delete("/notificaciones/" + notificacionIdOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT notificación ajena/leer → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void marcarComoLeidaAjena_devuelve403() throws Exception {
        mockMvc.perform(put("/notificaciones/" + notificacionIdOwner + "/leer"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET notificaciones/usuario/{otro} → 403")
    @WithUserDetails(value = ATTACKER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getNotificacionesDeOtroUsuario_devuelve403() throws Exception {
        mockMvc.perform(get("/notificaciones/usuario/" + owner.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET notificación propia → 200 (control positivo)")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getNotificacionPropia_devuelve200() throws Exception {
        mockMvc.perform(get("/notificaciones/" + notificacionIdOwner))
                .andExpect(status().isOk());
    }
}
