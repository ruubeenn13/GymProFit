package com.gymprofit.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalCreateDTO;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalDTO;
import com.gymprofit.api.enums.TipoObjetivo;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.service.objetivopersonal.IObjetivoPersonalService;
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

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tests de integración del ObjetivoPersonalController")
class ObjetivoPersonalControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private IObjetivoPersonalService objetivoService;

    private ObjetivoPersonalDTO objetivoDTO;
    private ObjetivoPersonalCreateDTO createDTO;

    @BeforeEach
    void setUp() {
        objetivoDTO = new ObjetivoPersonalDTO();
        objetivoDTO.setId(1);
        objetivoDTO.setUsuarioId(1);
        objetivoDTO.setTipoObjetivo(TipoObjetivo.PERDER_PESO);
        objetivoDTO.setCompletado(false);

        createDTO = new ObjetivoPersonalCreateDTO();
        createDTO.setUsuarioId(1);
        createDTO.setTipoObjetivo(TipoObjetivo.PERDER_PESO);
    }

    @Test
    @DisplayName("GET /objetivos-personales con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void findAll_con_user_devuelve_200() throws Exception {
        when(objetivoService.findAll()).thenReturn(List.of(objetivoDTO));

        mockMvc.perform(get("/objetivos-personales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(objetivoService).findAll();
    }

    @Test
    @DisplayName("GET /objetivos-personales/{id} con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void findById_existente_devuelve_200() throws Exception {
        when(objetivoService.findById(1)).thenReturn(objetivoDTO);

        mockMvc.perform(get("/objetivos-personales/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /objetivos-personales/{id} inexistente devuelve 404")
    @WithMockUser(roles = "USER")
    void findById_inexistente_devuelve_404() throws Exception {
        when(objetivoService.findById(99))
                .thenThrow(new NotFoundEntityException("El objetivo personal con id 99 no existe"));

        mockMvc.perform(get("/objetivos-personales/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /objetivos-personales con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void save_con_user_devuelve_200() throws Exception {
        when(objetivoService.save(any(ObjetivoPersonalCreateDTO.class))).thenReturn(objetivoDTO);

        mockMvc.perform(post("/objetivos-personales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(1));

        verify(objetivoService).save(any(ObjetivoPersonalCreateDTO.class));
    }

    @Test
    @DisplayName("POST /objetivos-personales sin autenticación devuelve error")
    void save_sin_autenticacion_devuelve_error() throws Exception {
        mockMvc.perform(post("/objetivos-personales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("DELETE /objetivos-personales/{id} con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void delete_con_user_devuelve_200() throws Exception {
        doNothing().when(objetivoService).deleteById(1);

        mockMvc.perform(delete("/objetivos-personales/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Objetivo personal eliminado con ÉXITO"));
    }

    @Test
    @DisplayName("PUT /objetivos-personales/{id}/completar con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void completar_devuelve_200() throws Exception {
        objetivoDTO.setCompletado(true);
        when(objetivoService.completar(1)).thenReturn(objetivoDTO);

        mockMvc.perform(put("/objetivos-personales/1/completar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completado").value(true));
    }

    @Test
    @DisplayName("GET /objetivos-personales/usuario/{id} con rol USER devuelve 200")
    @WithMockUser(roles = "USER")
    void findByUsuario_devuelve_200() throws Exception {
        when(objetivoService.findByUsuarioId(1)).thenReturn(List.of(objetivoDTO));

        mockMvc.perform(get("/objetivos-personales/usuario/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(1));
    }
}
