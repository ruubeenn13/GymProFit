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

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tests de integración del UsuarioController")
class UsuarioControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private IUsuarioService usuarioService;

    private UsuarioDTO usuarioDTO;

    @BeforeEach
    void setUp() {
        usuarioDTO = new UsuarioDTO();
        usuarioDTO.setId(1);
        usuarioDTO.setUsername("testuser");
        usuarioDTO.setEmail("test@gymprofit.com");
        usuarioDTO.setActivo(true);
    }

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

    @Test
    @DisplayName("GET /usuarios con rol USER devuelve 403")
    @WithMockUser(roles = "USER")
    void findAll_con_user_devuelve_403() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).findAll();
    }

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

    @Test
    @DisplayName("GET /usuarios/{id} inexistente devuelve 404")
    @WithMockUser(roles = "ADMIN")
    void findById_inexistente_devuelve_404() throws Exception {
        when(usuarioService.findById(99))
                .thenThrow(new NotFoundEntityException("El usuario con id 99 no existe"));

        mockMvc.perform(get("/usuarios/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /usuarios/username/{username} con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void findByUsername_con_user_devuelve_200() throws Exception {
        when(usuarioService.findByUsername("testuser")).thenReturn(usuarioDTO);

        mockMvc.perform(get("/usuarios/username/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("DELETE /usuarios/{id} con rol ADMIN devuelve 200")
    @WithMockUser(roles = "ADMIN")
    void deleteById_con_admin_devuelve_200() throws Exception {
        doNothing().when(usuarioService).deleteById(1);

        mockMvc.perform(delete("/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Usuario desactivado con ÉXITO"));
    }

    @Test
    @DisplayName("DELETE /usuarios/{id} con rol USER devuelve 403")
    @WithMockUser(roles = "USER")
    void deleteById_con_user_devuelve_403() throws Exception {
        mockMvc.perform(delete("/usuarios/1"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).deleteById(any());
    }
}
