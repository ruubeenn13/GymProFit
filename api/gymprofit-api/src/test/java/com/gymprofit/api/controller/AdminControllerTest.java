package com.gymprofit.api.controller;

import com.gymprofit.api.dto.admin.AdminEstadisticasDTO;
import com.gymprofit.api.dto.admin.AdminUsuarioDTO;
import com.gymprofit.api.service.usuario.IUsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ============================================================
// AdminControllerTest — tests de integración de los endpoints administrativos.
// Verifica que /admin/usuarios y /admin/estadisticas-globales solo sean accesibles
// con rol ADMIN y comprueba el correcto filtrado/paginación de usuarios.
// ============================================================
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tests de integración del AdminController")
class AdminControllerTest {

    // Simula peticiones HTTP contra los endpoints del controller.
    @Autowired private MockMvc mockMvc;

    // Mock del service para no depender de la BD real.
    @MockitoBean private IUsuarioService usuarioService;

    private AdminUsuarioDTO adminUsuarioDTO;
    private AdminEstadisticasDTO estadisticasDTO;

    // Prepara los DTOs de ejemplo usados en los tests.
    @BeforeEach
    void setUp() {
        adminUsuarioDTO = new AdminUsuarioDTO();
        adminUsuarioDTO.setId(1);
        adminUsuarioDTO.setUsername("admin");
        adminUsuarioDTO.setEmail("admin@gym.com");
        adminUsuarioDTO.setRol("ADMIN");

        estadisticasDTO = new AdminEstadisticasDTO();
        estadisticasDTO.setTotalUsuarios(100L);
        estadisticasDTO.setUsuariosActivos(80L);
        estadisticasDTO.setTotalSesiones(500L);
        estadisticasDTO.setSesionesHoy(10L);
        estadisticasDTO.setTotalEjerciciosRealizados(2000L);
        estadisticasDTO.setTotalObjetivosCompletados(150L);
        estadisticasDTO.setTotalLogrosOtorgados(300L);
    }

    // Con rol ADMIN, el listado de usuarios debe responder 200 con los datos del service.
    @Test
    @DisplayName("GET /admin/usuarios con rol ADMIN devuelve 200")
    @WithMockUser(roles = "ADMIN")
    void getUsuarios_con_admin_devuelve_200() throws Exception {
        when(usuarioService.getUsuariosAdmin(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(adminUsuarioDTO));

        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].rol").value("ADMIN"));

        verify(usuarioService).getUsuariosAdmin(null, null, null, 0, 20);
    }

    // Un usuario sin rol ADMIN no puede acceder al listado administrativo.
    @Test
    @DisplayName("GET /admin/usuarios con rol USER devuelve 403")
    @WithMockUser(roles = "USER")
    void getUsuarios_con_user_devuelve_403() throws Exception {
        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).getUsuariosAdmin(any(), any(), any(), anyInt(), anyInt());
    }

    // Sin autenticación no debe poder acceder al endpoint administrativo.
    @Test
    @DisplayName("GET /admin/usuarios sin autenticación devuelve error")
    void getUsuarios_sin_autenticacion_devuelve_error() throws Exception {
        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().is5xxServerError());
    }

    // Con rol ADMIN, las estadísticas globales deben devolverse correctamente.
    @Test
    @DisplayName("GET /admin/estadisticas-globales con rol ADMIN devuelve 200")
    @WithMockUser(roles = "ADMIN")
    void getEstadisticas_con_admin_devuelve_200() throws Exception {
        when(usuarioService.getEstadisticasGlobales()).thenReturn(estadisticasDTO);

        mockMvc.perform(get("/admin/estadisticas-globales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsuarios").value(100))
                .andExpect(jsonPath("$.usuariosActivos").value(80))
                .andExpect(jsonPath("$.totalSesiones").value(500));

        verify(usuarioService).getEstadisticasGlobales();
    }

    // Un usuario sin rol ADMIN no puede acceder a las estadísticas globales.
    @Test
    @DisplayName("GET /admin/estadisticas-globales con rol USER devuelve 403")
    @WithMockUser(roles = "USER")
    void getEstadisticas_con_user_devuelve_403() throws Exception {
        mockMvc.perform(get("/admin/estadisticas-globales"))
                .andExpect(status().isForbidden());
    }

    // Comprueba que los parámetros de filtro (activo, rol, username, size) se propagan al service.
    @Test
    @DisplayName("GET /admin/usuarios con filtros devuelve resultado filtrado")
    @WithMockUser(roles = "ADMIN")
    void getUsuarios_con_filtros_devuelve_filtrado() throws Exception {
        when(usuarioService.getUsuariosAdmin(true, "USER", "test", 0, 10))
                .thenReturn(List.of(adminUsuarioDTO));

        mockMvc.perform(get("/admin/usuarios")
                        .param("activo", "true")
                        .param("rol", "USER")
                        .param("username", "test")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(usuarioService).getUsuariosAdmin(true, "USER", "test", 0, 10);
    }
}
