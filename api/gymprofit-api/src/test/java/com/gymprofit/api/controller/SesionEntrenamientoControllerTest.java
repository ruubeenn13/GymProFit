package com.gymprofit.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoDTO;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.service.sesionentrenamiento.ISesionEntrenamientoService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ============================================================
// SesionEntrenamientoControllerTest — tests de integración del endpoint /sesiones
// Comprueba autenticación, CRUD y el flujo de completar una
// sesión de entrenamiento (calorías quemadas y notas).
// ============================================================
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tests de integración del SesionEntrenamientoController")
class SesionEntrenamientoControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Mock del servicio de sesiones de entrenamiento
    @MockitoBean private ISesionEntrenamientoService sesionService;

    private SesionEntrenamientoDTO sesionDTO;
    private SesionEntrenamientoCreateDTO createDTO;

    // Inicializa los DTOs de prueba usados en los distintos tests
    @BeforeEach
    void setUp() {
        sesionDTO = new SesionEntrenamientoDTO();
        sesionDTO.setId(1);
        sesionDTO.setUsuarioId(1);
        sesionDTO.setRutinaId(1);
        sesionDTO.setCompletada(false);

        createDTO = new SesionEntrenamientoCreateDTO();
        createDTO.setUsuarioId(1);
        createDTO.setFechaInicio(LocalDateTime.now());
    }

    // Comprueba que un USER puede listar todas las sesiones de entrenamiento
    @Test
    @DisplayName("GET /sesiones con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void findAll_con_user_devuelve_200() throws Exception {
        when(sesionService.findAll()).thenReturn(List.of(sesionDTO));

        mockMvc.perform(get("/sesiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(sesionService).findAll();
    }

    // Comprueba que listar sesiones sin autenticación devuelve un error 5xx
    @Test
    @DisplayName("GET /sesiones sin autenticación devuelve error")
    void findAll_sin_autenticacion_devuelve_error() throws Exception {
        mockMvc.perform(get("/sesiones"))
                .andExpect(status().is5xxServerError());
    }

    // Comprueba que un USER puede consultar una sesión existente por id
    @Test
    @DisplayName("GET /sesiones/{id} con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void findById_existente_devuelve_200() throws Exception {
        when(sesionService.findById(1)).thenReturn(sesionDTO);

        mockMvc.perform(get("/sesiones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // Comprueba que consultar una sesión inexistente devuelve 404
    @Test
    @DisplayName("GET /sesiones/{id} inexistente devuelve 404")
    @WithMockUser(roles = "USER")
    void findById_inexistente_devuelve_404() throws Exception {
        when(sesionService.findById(99))
                .thenThrow(new NotFoundEntityException("Sesión con id 99 no existe"));

        mockMvc.perform(get("/sesiones/99"))
                .andExpect(status().isNotFound());
    }

    // Comprueba que un USER puede crear una nueva sesión de entrenamiento
    @Test
    @DisplayName("POST /sesiones con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void save_con_user_devuelve_200() throws Exception {
        when(sesionService.save(any(SesionEntrenamientoCreateDTO.class))).thenReturn(sesionDTO);

        mockMvc.perform(post("/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(1));

        verify(sesionService).save(any(SesionEntrenamientoCreateDTO.class));
    }

    // Comprueba que un USER puede eliminar una sesión de entrenamiento existente
    @Test
    @DisplayName("DELETE /sesiones/{id} con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void delete_con_user_devuelve_200() throws Exception {
        doNothing().when(sesionService).deleteById(1);

        mockMvc.perform(delete("/sesiones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Sesión de entrenamiento eliminada con ÉXITO"));
    }

    // Comprueba que se pueden listar las sesiones de un usuario concreto
    @Test
    @DisplayName("GET /sesiones/usuario/{id} con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void findByUsuario_devuelve_200() throws Exception {
        when(sesionService.findByUsuarioId(1)).thenReturn(List.of(sesionDTO));

        mockMvc.perform(get("/sesiones/usuario/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(1));
    }

    // Comprueba que se puede completar una sesión indicando calorías quemadas y notas
    @Test
    @DisplayName("PUT /sesiones/{id}/completar con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void completar_sesion_devuelve_200() throws Exception {
        sesionDTO.setCompletada(true);
        when(sesionService.completarSesion(1, 400, "Buena sesión")).thenReturn(sesionDTO);

        mockMvc.perform(put("/sesiones/1/completar")
                        .param("caloriasQuemadas", "400")
                        .param("notas", "Buena sesión"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completada").value(true));
    }
}
