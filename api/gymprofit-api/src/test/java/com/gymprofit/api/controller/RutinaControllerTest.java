package com.gymprofit.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.dto.entity.rutina.RutinaDTO;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.service.rutina.IRutinaService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ============================================================
// RutinaControllerTest — tests de integración del endpoint /rutinas
// Verifica roles permitidos, filtros (activas/predefinidas/nivel)
// y respuestas HTTP de las operaciones CRUD sobre rutinas.
// ============================================================
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tests de integración del RutinaController")
class RutinaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mock del servicio de rutinas
    @MockitoBean
    private IRutinaService rutinaService;

    private RutinaDTO rutinaDTO;
    private RutinaCreateDTO rutinaCreateDTO;

    // Inicializa los DTOs de prueba usados en los distintos tests
    @BeforeEach
    void setUp() {
        rutinaDTO = new RutinaDTO();
        rutinaDTO.setId(1);
        rutinaDTO.setNombre("Rutina Pecho");
        rutinaDTO.setNivel("INTERMEDIO");
        rutinaDTO.setActiva(true);

        rutinaCreateDTO = new RutinaCreateDTO();
        rutinaCreateDTO.setUsuarioId(1);
        rutinaCreateDTO.setNombre("Rutina Pecho");
        rutinaCreateDTO.setNivel("INTERMEDIO");
        rutinaCreateDTO.setEsPredefinida(false);
    }

    // Comprueba que un GUEST puede listar todas las rutinas
    @Test
    @DisplayName("GET /api/rutinas con rol GUEST devuelve 200 y lista")
    @WithMockUser(roles = "GUEST")
    void findAll_con_rol_guest_devuelve_200() throws Exception {
        when(rutinaService.findAll()).thenReturn(List.of(rutinaDTO));

        mockMvc.perform(get("/rutinas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Rutina Pecho"))
                .andExpect(jsonPath("$[0].nivel").value("INTERMEDIO"));

        verify(rutinaService).findAll();
    }

    // Comprueba que listar rutinas sin autenticación devuelve 401
    @Test
    @DisplayName("GET /api/rutinas sin autenticación devuelve 401")
    void findAll_sin_autenticacion_devuelve_401() throws Exception {
        mockMvc.perform(get("/rutinas"))
                .andExpect(status().isUnauthorized());
    }

    // Comprueba que un USER puede consultar una rutina existente por id
    @Test
    @DisplayName("GET /api/rutinas/{id} con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void findById_existente_devuelve_200() throws Exception {
        when(rutinaService.findById(1)).thenReturn(rutinaDTO);

        mockMvc.perform(get("/rutinas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Rutina Pecho"));

        verify(rutinaService).findById(1);
    }

    // Comprueba que consultar una rutina inexistente devuelve 404
    @Test
    @DisplayName("GET /api/rutinas/{id} inexistente devuelve 404")
    @WithMockUser(roles = "USER")
    void findById_inexistente_devuelve_404() throws Exception {
        when(rutinaService.findById(99))
                .thenThrow(new NotFoundEntityException("La rutina con id 99 no existe"));

        mockMvc.perform(get("/rutinas/99"))
                .andExpect(status().isNotFound());
    }

    // Comprueba que un ADMIN puede crear una nueva rutina
    @Test
    @DisplayName("POST /api/rutinas con rol ADMIN devuelve 200")
    @WithMockUser(roles = "ADMIN")
    void save_con_rol_admin_devuelve_200() throws Exception {
        when(rutinaService.save(any(RutinaCreateDTO.class))).thenReturn(rutinaDTO);

        mockMvc.perform(post("/rutinas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rutinaCreateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Rutina Pecho"));

        verify(rutinaService).save(any(RutinaCreateDTO.class));
    }

    // Comprueba que un USER también puede crear rutinas; la validación de propiedad se hace en el service
    @Test
    @DisplayName("POST /rutinas con rol USER devuelve 200 (validación de propiedad en el service)")
    @WithMockUser(roles = "USER")
    void save_con_rol_user_devuelve_200() throws Exception {
        when(rutinaService.save(any(RutinaCreateDTO.class))).thenReturn(rutinaDTO);

        mockMvc.perform(post("/rutinas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rutinaCreateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Rutina Pecho"));

        verify(rutinaService).save(any(RutinaCreateDTO.class));
    }

    // Comprueba que un ADMIN puede desactivar (eliminar lógicamente) una rutina
    @Test
    @DisplayName("DELETE /api/rutinas/{id} con rol ADMIN devuelve 200")
    @WithMockUser(roles = "ADMIN")
    void deleteById_con_rol_admin_devuelve_200() throws Exception {
        doNothing().when(rutinaService).deleteById(1);

        mockMvc.perform(delete("/rutinas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Rutina desactivada con ÉXITO"));

        verify(rutinaService).deleteById(1);
    }

    // Comprueba que se pueden listar solo las rutinas activas
    @Test
    @DisplayName("GET /api/rutinas/activas con rol GUEST devuelve 200")
    @WithMockUser(roles = "GUEST")
    void findActivas_con_rol_guest_devuelve_200() throws Exception {
        when(rutinaService.findActivas()).thenReturn(List.of(rutinaDTO));

        mockMvc.perform(get("/rutinas/activas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activa").value(true));

        verify(rutinaService).findActivas();
    }

    // Comprueba que se pueden listar las rutinas predefinidas del sistema
    @Test
    @DisplayName("GET /api/rutinas/predefinidas con rol GUEST devuelve 200")
    @WithMockUser(roles = "GUEST")
    void findPredefinidas_con_rol_guest_devuelve_200() throws Exception {
        when(rutinaService.findPredefinidas()).thenReturn(List.of(rutinaDTO));

        mockMvc.perform(get("/rutinas/predefinidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Rutina Pecho"));

        verify(rutinaService).findPredefinidas();
    }

    // Comprueba que se pueden filtrar rutinas por nivel de dificultad
    @Test
    @DisplayName("GET /api/rutinas/nivel/{nivel} con rol GUEST devuelve 200")
    @WithMockUser(roles = "GUEST")
    void findByNivel_devuelve_200() throws Exception {
        when(rutinaService.findByNivel("INTERMEDIO")).thenReturn(List.of(rutinaDTO));

        mockMvc.perform(get("/rutinas/nivel/INTERMEDIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nivel").value("INTERMEDIO"));

        verify(rutinaService).findByNivel("INTERMEDIO");
    }
}