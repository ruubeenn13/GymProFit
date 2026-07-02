package com.gymprofit.api.config;

import com.gymprofit.api.entity.Role;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IRoleRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// DataInitializer — inicializador de datos semilla al arrancar la app
// Crea los usuarios base (admin y guest) si no existen todavía en BD.
// Se ejecuta automáticamente al arrancar Spring Boot (CommandLineRunner),
// excepto en el perfil "test" para no interferir con los tests.
// ============================================================
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final IUsuarioRepository usuarioRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    // Punto de entrada ejecutado tras el arranque del contexto Spring: crea usuarios semilla.
    @Override
    @Transactional
    public void run(String... args) {
        crearUsuarioSiNoExiste("admin", "admin@gymprofit.com", "Admin1234", RoleType.ADMIN);
        crearUsuarioSiNoExiste("guest", "guest@gymprofit.com", "guest", RoleType.GUEST);
    }

    // Crea un usuario con el rol indicado si no existe ya un usuario con ese username.
    private void crearUsuarioSiNoExiste(String username, String email, String password, RoleType roleType) {
        if (usuarioRepository.existsByUsername(username)) {
            return;
        }

        List<Role> roles = roleRepository.findByNombreIn(List.of(roleType.getValue()));
        if (roles.isEmpty()) {
            logger.warn("Rol {} no encontrado en BD, no se puede crear el usuario '{}'", roleType.name(), username);
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setEmail(email);
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setActivo(true);
        usuario.setRoles(roles);

        usuarioRepository.save(usuario);
        logger.info("Usuario '{}' creado con rol {}", username, roleType.name());
    }
}
