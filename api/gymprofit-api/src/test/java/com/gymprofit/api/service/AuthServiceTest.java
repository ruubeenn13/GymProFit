package com.gymprofit.api.service;

import com.gymprofit.api.config.security.JwtTokenProvider;
import com.gymprofit.api.dto.auth.LoginDTO;
import com.gymprofit.api.dto.auth.RegisterDTO;
import com.gymprofit.api.dto.auth.TokenDTO;
import com.gymprofit.api.entity.Role;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.exceptions.DuplicateEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.repository.jpa.IRoleRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// ============================================================
// AuthServiceTest — tests unitarios del AuthService
// Comprueba login (generación de token JWT) y registro de
// usuarios, incluyendo los casos de duplicado y no encontrado.
// ============================================================
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del AuthService")
class AuthServiceTest {

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private IRoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario;
    private Role roleUser;
    private LoginDTO loginDTO;
    private RegisterDTO registerDTO;

    // Inicializa usuario, rol y DTOs de login/registro de prueba
    @BeforeEach
    void setUp() {
        roleUser = new Role(1, RoleType.USER);

        usuario = new Usuario();
        usuario.setId(1);
        usuario.setUsername("testuser");
        usuario.setPassword("encodedPassword");
        usuario.setEmail("test@gymprofit.com");
        usuario.setActivo(true);
        usuario.setRoles(List.of(roleUser));

        loginDTO = new LoginDTO("testuser", "password123");

        registerDTO = new RegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setPassword("password123");
        registerDTO.setEmail("newuser@gymprofit.com");
    }

    // Comprueba que un login válido genera un TokenDTO con el JWT y los roles del usuario
    @Test
    @DisplayName("Login correcto devuelve TokenDTO con token y roles")
    void login_correcto_devuelve_token() {
        Authentication auth = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtTokenProvider.generateToken(auth)).thenReturn("jwt-token-mock");
        when(usuarioRepository.findByUsername("testuser")).thenReturn(Optional.of(usuario));

        TokenDTO result = authService.login(loginDTO);

        assertNotNull(result);
        assertEquals("jwt-token-mock", result.getToken());
        assertEquals("testuser", result.getUsername());
        assertEquals(List.of("USER"), result.getRoles());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).generateToken(auth);
    }

    // Comprueba que hacer login con un usuario no persistido lanza NotFoundEntityException
    @Test
    @DisplayName("Login con usuario inexistente lanza NotFoundEntityException")
    void login_usuario_inexistente_lanza_excepcion() {
        Authentication auth = mock(Authentication.class);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateToken(auth)).thenReturn("token");
        when(usuarioRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> authService.login(loginDTO));
    }

    // Comprueba que el registro correcto persiste el usuario con el rol USER por defecto
    @Test
    @DisplayName("Register correcto guarda el usuario con rol USER por defecto")
    void register_correcto_guarda_usuario() {
        when(usuarioRepository.existsByUsername("newuser")).thenReturn(false);
        when(usuarioRepository.existsByEmail("newuser@gymprofit.com")).thenReturn(false);
        when(roleRepository.findByNombreIn(List.of(RoleType.USER.getValue()))).thenReturn(List.of(roleUser));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        assertDoesNotThrow(() -> authService.register(registerDTO));

        verify(usuarioRepository).save(any(Usuario.class));
    }

    // Comprueba que registrar con un username ya existente lanza DuplicateEntityException
    @Test
    @DisplayName("Register con username duplicado lanza DuplicateEntityException")
    void register_username_duplicado_lanza_excepcion() {
        when(usuarioRepository.existsByUsername("newuser")).thenReturn(true);

        assertThrows(DuplicateEntityException.class, () -> authService.register(registerDTO));

        verify(usuarioRepository, never()).save(any());
    }

    // Comprueba que registrar con un email ya existente lanza DuplicateEntityException
    @Test
    @DisplayName("Register con email duplicado lanza DuplicateEntityException")
    void register_email_duplicado_lanza_excepcion() {
        when(usuarioRepository.existsByUsername("newuser")).thenReturn(false);
        when(usuarioRepository.existsByEmail("newuser@gymprofit.com")).thenReturn(true);

        assertThrows(DuplicateEntityException.class, () -> authService.register(registerDTO));

        verify(usuarioRepository, never()).save(any());
    }
}