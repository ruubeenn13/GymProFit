package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
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
        return Map.of("notificacion", notificacionIdOwner, "owner", owner.getId(), "tipo", "RECORDATORIO");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "PATCH /notificaciones/{notificacion}",
            "GET /notificaciones/usuario/{owner}/ordenadas",
            "GET /notificaciones/usuario/{owner}/no-leidas",
            "GET /notificaciones/usuario/{owner}/leidas",
            "GET /notificaciones/usuario/{owner}/tipo/{tipo}",
            "GET /notificaciones/count/usuario/{owner}",
            "GET /notificaciones/count/usuario/{owner}/no-leidas",
            "GET /notificaciones/exists/usuario/{owner}/no-leidas",
            "PUT /notificaciones/usuario/{owner}/leer-todas",
            "DELETE /notificaciones/usuario/{owner}"
    })
    @DisplayName("GP-048: id ajeno → 403 al atacante; el dueño no")
    void gp048_idAjeno(String plantilla) throws Exception {
        idAjeno(rellenar(plantilla, idsGp048()));
    }

    @Test
    @DisplayName("GP-048: GET /notificaciones (todas) → solo ADMIN")
    void gp048_soloAdmin() throws Exception {
        soloAdmin("GET /notificaciones");
    }

    @Test
    @DisplayName("GP-048: GUEST pidiendo SUS propias notificaciones → 403 (regla de rol)")
    void gp048_guestConSuPropioId() throws Exception {
        assertThat(estado(guest, "GET /notificaciones/usuario/" + guest.getId())).isEqualTo(403);
    }
}
