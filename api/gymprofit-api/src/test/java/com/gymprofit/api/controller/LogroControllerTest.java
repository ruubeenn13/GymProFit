package com.gymprofit.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.dto.entity.logro.LogroCreateDTO;
import com.gymprofit.api.dto.entity.logro.LogroDTO;
import com.gymprofit.api.enums.TipoLogro;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.service.logro.ILogroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tests de integración del LogroController")
class LogroControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ILogroService logroService;

    private LogroDTO logroDTO;
    private LogroCreateDTO logroCreateDTO;

    @BeforeEach
    void setUp() {
        logroDTO = new LogroDTO();
        logroDTO.setId(1);
        logroDTO.setNombre("Primera Sesión");
        logroDTO.setTipo(TipoLogro.PRIMERA_SESION);

        logroCreateDTO = new LogroCreateDTO();
        logroCreateDTO.setNombre("Primera Sesión");
        logroCreateDTO.setDescripcion("Completa tu primera sesión");
        logroCreateDTO.setTipo("PRIMERA_SESION");
    }

    @Test
    @DisplayName("GET /logros con rol GUEST devuelve 200")
    @WithMockUser(roles = "GUEST")
    void findAll_con_guest_devuelve_200() throws Exception {
        when(logroService.findAll()).thenReturn(List.of(logroDTO));

        mockMvc.perform(get("/logros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Primera Sesión"));

        verify(logroService).findAll();
    }

    @Test
    @DisplayName("GET /logros/usuario/{id} con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void findByUsuarioId_con_user_devuelve_200() throws Exception {
        when(logroService.findByUsuarioId(1)).thenReturn(List.of());

        mockMvc.perform(get("/logros/usuario/1"))
                .andExpect(status().isOk());

        verify(logroService).findByUsuarioId(1);
    }

    @Test
    @DisplayName("GET /logros/usuario/{id} inexistente devuelve 404")
    @WithMockUser(roles = "USER")
    void findByUsuarioId_inexistente_devuelve_404() throws Exception {
        when(logroService.findByUsuarioId(99))
                .thenThrow(new NotFoundEntityException("Usuario con id 99 no encontrado"));

        mockMvc.perform(get("/logros/usuario/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /logros con rol ADMIN devuelve 201")
    @WithMockUser(roles = "ADMIN")
    void save_con_admin_devuelve_201() throws Exception {
        when(logroService.save(any(LogroCreateDTO.class))).thenReturn(logroDTO);

        mockMvc.perform(post("/logros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logroCreateDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Primera Sesión"));

        verify(logroService).save(any(LogroCreateDTO.class));
    }

    @Test
    @DisplayName("POST /logros con rol USER devuelve 403")
    @WithMockUser(roles = "USER")
    void save_con_user_devuelve_403() throws Exception {
        mockMvc.perform(post("/logros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logroCreateDTO)))
                .andExpect(status().isForbidden());

        verify(logroService, never()).save(any());
    }

    @Test
    @DisplayName("PUT /logros/{id} con rol ADMIN devuelve 200")
    @WithMockUser(roles = "ADMIN")
    void update_con_admin_devuelve_200() throws Exception {
        when(logroService.update(eq(1), any(LogroCreateDTO.class))).thenReturn(logroDTO);

        mockMvc.perform(put("/logros/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logroCreateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Primera Sesión"));

        verify(logroService).update(eq(1), any(LogroCreateDTO.class));
    }

    @Test
    @DisplayName("PUT /logros/{id} inexistente devuelve 404")
    @WithMockUser(roles = "ADMIN")
    void update_inexistente_devuelve_404() throws Exception {
        when(logroService.update(eq(99), any(LogroCreateDTO.class)))
                .thenThrow(new NotFoundEntityException("Logro con id 99 no encontrado"));

        mockMvc.perform(put("/logros/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logroCreateDTO)))
                .andExpect(status().isNotFound());
    }
}
