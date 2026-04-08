package com.gymprofit.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.dto.auth.LoginDTO;
import com.gymprofit.api.dto.auth.RegisterDTO;
import com.gymprofit.api.dto.auth.TokenDTO;
import com.gymprofit.api.exceptions.DuplicateEntityException;
import com.gymprofit.api.service.auth.IAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración del AuthController.
 *
 * A diferencia de los tests unitarios, aquí usamos @SpringBootTest que levanta
 * todo el contexto de Spring (filtros, seguridad, controladores...) igual que
 * si arrancáramos la app de verdad.
 *
 * @AutoConfigureMockMvc configura MockMvc para simular peticiones HTTP sin
 * necesitar un servidor real.
 *
 * @MockitoBean reemplaza el IAuthService real con un mock para no tocar la BD.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tests de integración del AuthController")
class AuthControllerTest {

    /**
     * MockMvc nos permite simular peticiones HTTP (POST, GET, etc.)
     * y verificar la respuesta (código de estado, JSON devuelto...).
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * ObjectMapper convierte objetos Java a JSON y viceversa,
     * necesario para enviar el body de las peticiones.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * @MockitoBean reemplaza el bean real del contexto de Spring con un mock.
     * Así controlamos qué devuelve el service sin tocar la BD.
     */
    @MockitoBean
    private IAuthService authService;

    private LoginDTO loginDTO;
    private RegisterDTO registerDTO;
    private TokenDTO tokenDTO;

    @BeforeEach
    void setUp() {
        loginDTO = new LoginDTO("admin", "admin123");

        registerDTO = new RegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setPassword("password123");
        registerDTO.setEmail("newuser@gymprofit.com");

        tokenDTO = new TokenDTO("jwt-token-mock", "admin", List.of("ADMIN"));
    }

    @Test
    @DisplayName("POST /auth/login con credenciales correctas devuelve 200 y token")
    void login_correcto_devuelve_200_y_token() throws Exception {
        // Simulamos que el service devuelve un TokenDTO al hacer login
        when(authService.login(any(LoginDTO.class))).thenReturn(tokenDTO);

        // Realizamos la petición POST a /auth/login con el body en JSON
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                // Verificamos que el código de respuesta es 200
                .andExpect(status().isOk())
                // Verificamos que el JSON devuelto contiene el token
                .andExpect(jsonPath("$.token").value("jwt-token-mock"))
                // Verificamos que el JSON devuelto contiene el username
                .andExpect(jsonPath("$.username").value("admin"));

        verify(authService).login(any(LoginDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register correcto devuelve 201 y mensaje")
    void register_correcto_devuelve_201() throws Exception {
        // Simulamos que el service registra correctamente (no devuelve nada)
        doNothing().when(authService).register(any(RegisterDTO.class));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                // Verificamos que el código de respuesta es 201 (Created)
                .andExpect(status().isCreated())
                // Verificamos que el JSON contiene el mensaje de éxito
                .andExpect(jsonPath("$.mensaje").value("Usuario registrado correctamente"));

        verify(authService).register(any(RegisterDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register con username duplicado devuelve 400")
    void register_username_duplicado_devuelve_400() throws Exception {
        // Simulamos que el service lanza DuplicateEntityException
        doThrow(new DuplicateEntityException("El username 'newuser' ya está en uso"))
                .when(authService).register(any(RegisterDTO.class));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                // Verificamos que el código de respuesta es 400 (Bad Request)
                .andExpect(status().isBadRequest());

        verify(authService).register(any(RegisterDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login sin body devuelve 500")
    void login_sin_body_devuelve_500() throws Exception {
        // Enviamos una petición sin body para verificar que la API responde con error
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }
}