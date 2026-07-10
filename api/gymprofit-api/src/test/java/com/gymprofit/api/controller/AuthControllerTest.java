package com.gymprofit.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.dto.auth.ChangePasswordDTO;
import com.gymprofit.api.dto.auth.LoginDTO;
import com.gymprofit.api.dto.auth.RegisterDTO;
import com.gymprofit.api.dto.auth.TokenDTO;
import com.gymprofit.api.exceptions.DuplicateEntityException;
import com.gymprofit.api.exceptions.InvalidCredentialsException;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.service.auth.IAuthService;
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
// ============================================================
// AuthControllerTest — tests de integración de los endpoints de autenticación.
// Verifica login/register (éxito, credenciales/registro inválido, sin body)
// simulando el IAuthService con Mockito.
// ============================================================
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

    // Prepara los DTOs de ejemplo usados en los tests.
    @BeforeEach
    void setUp() {
        loginDTO = new LoginDTO("admin", "admin123");

        registerDTO = new RegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setPassword("password123");
        registerDTO.setEmail("newuser@gymprofit.com");

        tokenDTO = new TokenDTO("jwt-token-mock", "refresh-token-mock", "admin", List.of("ADMIN"));
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
                // Verificamos que también devuelve el refresh token
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-mock"))
                // Verificamos que el JSON devuelto contiene el username
                .andExpect(jsonPath("$.username").value("admin"));

        verify(authService).login(any(LoginDTO.class));
    }

    @Test
    @DisplayName("POST /auth/refresh con refresh token válido devuelve 200 y nuevos tokens")
    void refresh_correcto_devuelve_200_y_tokens() throws Exception {
        // El service devuelve un TokenDTO con access nuevo y refresh rotado
        when(authService.refresh(any(String.class))).thenReturn(tokenDTO);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-token-mock\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-mock"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-mock"));

        verify(authService).refresh(any(String.class));
    }

    @Test
    @DisplayName("POST /auth/logout devuelve 200 y revoca el refresh token")
    void logout_devuelve_200() throws Exception {
        doNothing().when(authService).logout(any(String.class));

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-token-mock\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Sesión cerrada correctamente"));

        verify(authService).logout(any(String.class));
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

    // Petición sin body (cuerpo ilegible) debe devolver 400, no 500:
    // el @ExceptionHandler de HttpMessageNotReadableException lo traduce a BAD_REQUEST.
    @Test
    @DisplayName("POST /auth/login sin body devuelve 400")
    void login_sin_body_devuelve_400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // El username se toma del token autenticado (@WithMockUser), nunca del body.
    @Test
    @WithMockUser(username = "admin")
    @DisplayName("POST /auth/change-password autenticado y correcto devuelve 200")
    void changePassword_correcto_devuelve_200() throws Exception {
        doNothing().when(authService).changePassword(any(String.class), any(ChangePasswordDTO.class));

        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Admin1234\",\"newPassword\":\"NuevaPass9\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Contraseña cambiada correctamente"));

        verify(authService).changePassword(any(String.class), any(ChangePasswordDTO.class));
    }

    // Sin token la regla authenticated() de SecurityConfig rechaza con 401 (jwtEntryPoint).
    @Test
    @DisplayName("POST /auth/change-password sin autenticar devuelve 401")
    void changePassword_sin_token_devuelve_401() throws Exception {
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Admin1234\",\"newPassword\":\"NuevaPass9\"}"))
                .andExpect(status().isUnauthorized());

        verify(authService, never()).changePassword(any(String.class), any(ChangePasswordDTO.class));
    }

    // Contraseña actual incorrecta: el service lanza InvalidCredentialsException → 401.
    @Test
    @WithMockUser(username = "admin")
    @DisplayName("POST /auth/change-password con contraseña actual incorrecta devuelve 401")
    void changePassword_actual_incorrecta_devuelve_401() throws Exception {
        doThrow(new InvalidCredentialsException("La contraseña actual no es correcta"))
                .when(authService).changePassword(any(String.class), any(ChangePasswordDTO.class));

        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"malmal1\",\"newPassword\":\"NuevaPass9\"}"))
                .andExpect(status().isUnauthorized());

        verify(authService).changePassword(any(String.class), any(ChangePasswordDTO.class));
    }

    // Nueva contraseña igual a la actual: el service lanza InvalidDataException → 400.
    @Test
    @WithMockUser(username = "admin")
    @DisplayName("POST /auth/change-password con nueva igual a la actual devuelve 400")
    void changePassword_nueva_igual_actual_devuelve_400() throws Exception {
        doThrow(new InvalidDataException("La nueva contraseña debe ser distinta de la actual"))
                .when(authService).changePassword(any(String.class), any(ChangePasswordDTO.class));

        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Admin1234\",\"newPassword\":\"Admin1234\"}"))
                .andExpect(status().isBadRequest());

        verify(authService).changePassword(any(String.class), any(ChangePasswordDTO.class));
    }
}