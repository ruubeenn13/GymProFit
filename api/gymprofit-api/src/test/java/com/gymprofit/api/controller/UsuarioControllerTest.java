package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.usuario.UsuarioDTO;
import com.gymprofit.api.exceptions.NotFoundEntityException;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ============================================================
// UsuarioControllerTest — tests de integración del endpoint /usuarios
// Verifica que las operaciones de gestión de usuarios (listar,
// buscar, eliminar) respetan las restricciones de rol ADMIN/USER.
// ============================================================
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tests de integración del UsuarioController")
class UsuarioControllerTest {

    @Autowired private MockMvc mockMvc;

    // Mock del servicio de usuarios
    @MockitoBean private IUsuarioService usuarioService;

    private UsuarioDTO usuarioDTO;

    // Inicializa el DTO de usuario de prueba
    @BeforeEach
    void setUp() {
        usuarioDTO = new UsuarioDTO();
        usuarioDTO.setId(1);
        usuarioDTO.setUsername("testuser");
        usuarioDTO.setEmail("test@gymprofit.com");
        usuarioDTO.setActivo(true);
    }

    // Comprueba que un ADMIN puede listar todos los usuarios
    @Test
    @DisplayName("GET /usuarios con rol ADMIN devuelve 200")
    @WithMockUser(roles = "ADMIN")
    void findAll_con_admin_devuelve_200() throws Exception {
        when(usuarioService.findAll()).thenReturn(List.of(usuarioDTO));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("testuser"));

        verify(usuarioService).findAll();
    }

    // Comprueba que un USER no tiene permiso para listar todos los usuarios (solo ADMIN)
    @Test
    @DisplayName("GET /usuarios con rol USER devuelve 403")
    @WithMockUser(roles = "USER")
    void findAll_con_user_devuelve_403() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).findAll();
    }

    // Comprueba que un USER puede consultar un usuario existente por id
    @Test
    @DisplayName("GET /usuarios/{id} con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void findById_con_user_devuelve_200() throws Exception {
        when(usuarioService.findById(1)).thenReturn(usuarioDTO);

        mockMvc.perform(get("/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    // Comprueba que consultar un usuario inexistente devuelve 404
    @Test
    @DisplayName("GET /usuarios/{id} inexistente devuelve 404")
    @WithMockUser(roles = "ADMIN")
    void findById_inexistente_devuelve_404() throws Exception {
        when(usuarioService.findById(99))
                .thenThrow(new NotFoundEntityException("El usuario con id 99 no existe"));

        mockMvc.perform(get("/usuarios/99"))
                .andExpect(status().isNotFound());
    }

    // Comprueba que se puede buscar un usuario por su username
    @Test
    @DisplayName("GET /usuarios/username/{username} con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void findByUsername_con_user_devuelve_200() throws Exception {
        when(usuarioService.findByUsername("testuser")).thenReturn(usuarioDTO);

        mockMvc.perform(get("/usuarios/username/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    // Comprueba que un ADMIN puede desactivar un usuario existente
    @Test
    @DisplayName("DELETE /usuarios/{id} con rol ADMIN devuelve 200")
    @WithMockUser(roles = "ADMIN")
    void deleteById_con_admin_devuelve_200() throws Exception {
        doNothing().when(usuarioService).deleteById(1);

        mockMvc.perform(delete("/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Usuario desactivado con ÉXITO"));
    }

    // Comprueba que un USER no tiene permiso para eliminar usuarios (solo ADMIN)
    @Test
    @DisplayName("DELETE /usuarios/{id} con rol USER devuelve 403")
    @WithMockUser(roles = "USER")
    void deleteById_con_user_devuelve_403() throws Exception {
        mockMvc.perform(delete("/usuarios/1"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).deleteById(any());
    }
}
