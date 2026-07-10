package com.gymprofit.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.dto.entity.ejercicio.EjercicioCreateDTO;
import com.gymprofit.api.dto.entity.ejercicio.EjercicioDTO;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.service.ejercicio.IEjercicioService;
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
// EjercicioControllerTest — tests de integración del catálogo de ejercicios.
// Verifica los permisos por rol (GUEST/USER/ADMIN) sobre listar, consultar,
// crear y eliminar ejercicios, simulando el IEjercicioService.
// ============================================================
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tests de integración del EjercicioController")
class EjercicioControllerTest {

    // Simula peticiones HTTP contra los endpoints del controller.
    @Autowired
    private MockMvc mockMvc;

    // Convierte objetos Java a JSON para el body de las peticiones.
    @Autowired
    private ObjectMapper objectMapper;

    // Mock del service para no depender de la BD real.
    @MockitoBean
    private IEjercicioService ejercicioService;

    private EjercicioDTO ejercicioDTO;
    private EjercicioCreateDTO ejercicioCreateDTO;

    // Prepara los DTOs de ejemplo usados en los tests.
    @BeforeEach
    void setup() {
        ejercicioDTO = new EjercicioDTO();
        ejercicioDTO.setId(1);
        ejercicioDTO.setNombre("Press de Banca");
        ejercicioDTO.setGrupoMuscular("PECHO");
        ejercicioDTO.setDificultad("INTERMEDIO");
        ejercicioDTO.setActivo(true);

        ejercicioCreateDTO = new EjercicioCreateDTO();
        ejercicioCreateDTO.setNombre("Press de Banca");
        ejercicioCreateDTO.setGrupoMuscular("PECHO");
        ejercicioCreateDTO.setDificultad("INTERMEDIO");
    }

    // Un GUEST puede ver el catálogo de ejercicios (lectura pública para autenticados).
    @Test
    @DisplayName("GET /api/ejercicios con rol GUEST devuelve 200 y lista")
    @WithMockUser(roles = "GUEST")
    void findAll_con_rol_guest_devuelve_200() throws Exception {
        when(ejercicioService.findAll()).thenReturn(List.of(ejercicioDTO));

        mockMvc.perform(get("/ejercicios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Press de Banca"))
                .andExpect(jsonPath("$[0].grupoMuscular").value("PECHO"));

        verify(ejercicioService).findAll();
    }

    // Sin autenticación el endpoint no debe ser accesible (401).
    @Test
    @DisplayName("GET /api/ejercicios sin autenticación devuelve 401")
    void findAll_sin_autenticacion_devuelve_401() throws Exception {
        mockMvc.perform(get("/ejercicios"))
                .andExpect(status().isUnauthorized());
    }

    // Un USER puede consultar el detalle de un ejercicio existente.
    @Test
    @DisplayName("GET /api/ejercicios/{id} con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void findById_existente_devuelve_200() throws Exception {
        when(ejercicioService.findById(1)).thenReturn(ejercicioDTO);

        mockMvc.perform(get("/ejercicios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Press de Banca"));

        verify(ejercicioService).findById(1);
    }

    // Un id inexistente debe traducirse a 404 vía NotFoundEntityException.
    @Test
    @DisplayName("GET /api/ejercicios/{id} inexistente devuelve 404")
    @WithMockUser(roles = "USER")
    void findById_inexistente_devuelve_404() throws Exception {
        when(ejercicioService.findById(99))
                .thenThrow(new NotFoundEntityException("El ejercicio con id 99 no existe"));

        mockMvc.perform(get("/ejercicios/99"))
                .andExpect(status().isNotFound());
    }

    // Solo ADMIN puede crear ejercicios en el catálogo.
    @Test
    @DisplayName("POST /api/ejercicios con rol ADMIN devuelve 200")
    @WithMockUser(roles = "ADMIN")
    void save_con_rol_admin_devuelve_200() throws Exception {
        when(ejercicioService.save(any(EjercicioCreateDTO.class))).thenReturn(ejercicioDTO);

        mockMvc.perform(post("/ejercicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ejercicioCreateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Press de Banca"));

        verify(ejercicioService).save(any(EjercicioCreateDTO.class));
    }

    // Un USER no tiene permiso para crear ejercicios.
    @Test
    @DisplayName("POST /api/ejercicios con rol USER devuelve 403")
    @WithMockUser(roles = "USER")
    void save_con_rol_user_devuelve_403() throws Exception {
        mockMvc.perform(post("/ejercicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ejercicioCreateDTO)))
                .andExpect(status().isForbidden());

        verify(ejercicioService, never()).save(any());
    }

    // Solo ADMIN puede desactivar (borrar lógicamente) un ejercicio.
    @Test
    @DisplayName("DELETE /api/ejercicios/{id} con rol ADMIN devuelve 200")
    @WithMockUser(roles = "ADMIN")
    void deleteById_con_rol_admin_devuelve_200() throws Exception {
        doNothing().when(ejercicioService).deleteById(1);

        mockMvc.perform(delete("/ejercicios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Ejercicio desactivado con ÉXITO"));

        verify(ejercicioService).deleteById(1);
    }

    // Lista solo los ejercicios activos, accesible también para GUEST.
    @Test
    @DisplayName("GET /api/ejercicios/activos con rol GUEST devuelve 200")
    @WithMockUser(roles = "GUEST")
    void findActivos_devuelve_200() throws Exception {
        when(ejercicioService.findActivos()).thenReturn(List.of(ejercicioDTO));

        mockMvc.perform(get("/ejercicios/activos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activo").value(true));

        verify(ejercicioService).findActivos();
    }
}
