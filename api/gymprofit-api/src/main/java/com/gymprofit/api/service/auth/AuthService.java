package com.gymprofit.api.service.auth;

import com.gymprofit.api.config.security.JwtTokenProvider;
import com.gymprofit.api.dto.auth.LoginDTO;
import com.gymprofit.api.dto.auth.RegisterDTO;
import com.gymprofit.api.dto.auth.TokenDTO;
import com.gymprofit.api.entity.RefreshToken;
import com.gymprofit.api.entity.Role;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.NivelExperiencia;
import com.gymprofit.api.exceptions.DuplicateEntityException;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.repository.jpa.IRoleRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.gymprofit.api.enums.RoleType.USER;

// ============================================================
// AuthService — servicio de autenticación de usuarios
// Implementa login (delega en AuthenticationManager de Spring Security),
// registro público (siempre asigna rol USER, nunca lo toma del cliente) y
// acceso como invitado. Genera los tokens JWT usando JwtTokenProvider.
// ============================================================
@Service
@AllArgsConstructor
public class AuthService implements IAuthService {

    private final IUsuarioRepository usuarioRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // Autentica usuario/contraseña con el AuthenticationManager, establece el
    // contexto de seguridad y genera el token JWT con los roles del usuario.
    @Override
    public TokenDTO login(LoginDTO loginDTO) {
        logger.info("Iniciando sesión para usuario: {}", loginDTO.getUsername());

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        String token = jwtTokenProvider.generateToken(auth);

        Usuario usuario = usuarioRepository.findByUsername(loginDTO.getUsername())
                .orElseThrow(() -> new NotFoundEntityException("Usuario no encontrado"));

        List<String> roles = usuario.getRoles().stream()
                .map(role -> role.getNombre().name())
                .collect(Collectors.toList());

        // Emite un refresh token opaco para renovar el access token sin re-login.
        RefreshToken refreshToken = refreshTokenService.crear(usuario);

        logger.info("Login exitoso para usuario: {}", loginDTO.getUsername());

        return new TokenDTO(token, refreshToken.getToken(), usuario.getUsername(), roles);
    }

    // Registra un nuevo usuario público: valida unicidad de username/email,
    // codifica la contraseña, asigna siempre el rol USER y guarda el usuario.
    @Transactional
    @Override
    public void register(RegisterDTO registerDTO) {
        logger.info("Registrando nuevo usuario: {}", registerDTO.getUsername());

        if (usuarioRepository.existsByUsername(registerDTO.getUsername())) {
            throw new DuplicateEntityException("El username '" + registerDTO.getUsername() + "' ya está en uso");
        }

        if (usuarioRepository.existsByEmail(registerDTO.getEmail())) {
            throw new DuplicateEntityException("El email '" + registerDTO.getEmail() + "' ya está en uso");
        }

        // Seguridad: el rol NUNCA se toma del cliente. El registro público crea siempre USER.
        // Los cambios de rol se hacen solo desde el panel admin (PATCH /admin/usuarios/{id}/rol).
        List<Role> roles = roleRepository.findByNombreIn(List.of(USER.getValue()));
        if (roles.isEmpty()) {
            throw new NotFoundEntityException("Rol USER no encontrado; verificar seed de roles");
        }

        NivelExperiencia nivelExperiencia = null;
        if (registerDTO.getNivelExperiencia() != null && !registerDTO.getNivelExperiencia().isEmpty()) {
            try {
                nivelExperiencia = NivelExperiencia.valueOf(registerDTO.getNivelExperiencia().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidDataException("Nivel de experiencia inválido: " + registerDTO.getNivelExperiencia());
            }
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(registerDTO.getUsername());
        usuario.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        usuario.setEmail(registerDTO.getEmail());
        usuario.setPeso(registerDTO.getPeso());
        usuario.setAltura(registerDTO.getAltura());
        usuario.setEdad(registerDTO.getEdad());
        usuario.setNivelExperiencia(nivelExperiencia);
        usuario.setObjetivo(registerDTO.getObjetivo());
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setActivo(true);
        usuario.setRoles(roles);

        usuarioRepository.save(usuario);

        logger.info("Usuario '{}' registrado correctamente con roles: {}", registerDTO.getUsername(),
                roles.stream().map(r -> r.getNombre().name()).collect(Collectors.joining(", ")));
    }

    // Genera un token JWT para el usuario invitado predefinido "guest" sin
    // necesidad de credenciales, usando directamente sus authorities.
    @Override
    public TokenDTO loginAsGuest() {
        Usuario guest = usuarioRepository.findByUsername("guest")
                .orElseThrow(() -> new NotFoundEntityException("Usuario guest no encontrado"));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                guest, null, guest.getAuthorities()
        );

        String token = jwtTokenProvider.generateToken(auth);

        List<String> roles = guest.getRoles().stream()
                .map(role -> role.getNombre().name())
                .collect(Collectors.toList());

        // El invitado también recibe refresh token para renovar su sesión.
        RefreshToken refreshToken = refreshTokenService.crear(guest);

        logger.info("Acceso como invitado concedido");

        return new TokenDTO(token, refreshToken.getToken(), guest.getUsername(), roles);
    }

    // Renueva el access token a partir de un refresh token válido: valida el
    // refresh, rota (revoca el usado y emite uno nuevo) y devuelve un nuevo
    // access token JWT junto con el nuevo refresh token. Transaccional para que
    // el acceso al usuario (LAZY) y la rotación ocurran en la misma sesión.
    @Transactional
    @Override
    public TokenDTO refresh(String refreshTokenValue) {
        RefreshToken actual = refreshTokenService.validar(refreshTokenValue);
        Usuario usuario = actual.getUsuario();

        String nuevoAccessToken = jwtTokenProvider.generateToken(usuario);
        RefreshToken nuevoRefresh = refreshTokenService.rotar(actual);

        List<String> roles = usuario.getRoles().stream()
                .map(role -> role.getNombre().name())
                .collect(Collectors.toList());

        logger.info("Access token renovado para usuario: {}", usuario.getUsername());

        return new TokenDTO(nuevoAccessToken, nuevoRefresh.getToken(), usuario.getUsername(), roles);
    }

    // Cierra sesión revocando el refresh token indicado (idempotente).
    @Transactional
    @Override
    public void logout(String refreshTokenValue) {
        refreshTokenService.revocarPorToken(refreshTokenValue);
        logger.info("Sesión cerrada: refresh token revocado");
    }
}