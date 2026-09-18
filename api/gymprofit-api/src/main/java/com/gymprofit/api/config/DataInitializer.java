package com.gymprofit.api.config;

import com.gymprofit.api.entity.Role;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IRoleRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

// ============================================================
// DataInitializer — inicializador de datos semilla al arrancar la app
// Crea los usuarios base (admin y guest) si no existen todavía en BD.
// Se ejecuta automáticamente al arrancar Spring Boot (CommandLineRunner),
// excepto en el perfil "test" para no interferir con los tests.
//
// La contraseña del administrador NO está en el código: sale de la propiedad
// app.seed.admin.password, que en dev/ci trae un valor local de usar y tirar y
// en prod se mapea a la variable de entorno ADMIN_PASSWORD sin valor por defecto.
// Si esa propiedad viene vacía no se crea NINGUNA cuenta con rol ADMIN. Antes el
// seed llevaba la contraseña del admin escrita aquí, y como Render arranca con el
// perfil prod, esa cuenta de administración existía en producción con una
// contraseña publicada en un repositorio público.
// ============================================================
@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_EMAIL = "admin@gymprofit.com";
    private static final String GUEST_USERNAME = "guest";
    private static final String GUEST_EMAIL = "guest@gymprofit.com";

    // Generador de la contraseña inutilizable del invitado (ver crearInvitado).
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IUsuarioRepository usuarioRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // Contraseña del administrador semilla. Vacía = no se crea el administrador.
    private final String adminPassword;

    public DataInitializer(IUsuarioRepository usuarioRepository,
                           IRoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.seed.admin.password:}") String adminPassword) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
    }

    // Punto de entrada ejecutado tras el arranque del contexto Spring: crea usuarios semilla.
    @Override
    @Transactional
    public void run(String... args) {
        crearAdministrador();
        crearInvitado();
    }

    /**
     * Crea el administrador semilla, pero solo si hay una contraseña configurada.
     * <p>
     * Sin contraseña la API arranca igual, simplemente sin cuenta de administración:
     * una API sin panel de administración sigue sirviendo a sus usuarios, mientras que
     * un ADMIN con contraseña conocida es acceso total al sistema. Por eso este caso
     * es un aviso y no un fallo de arranque, al revés que el correo (ver EmailService).
     */
    private void crearAdministrador() {
        if (!StringUtils.hasText(adminPassword)) {
            logger.warn("No se crea el usuario '{}': la propiedad app.seed.admin.password está vacía. "
                    + "En el perfil prod esa propiedad viene de la variable de entorno ADMIN_PASSWORD, "
                    + "que no tiene valor por defecto a propósito. Mientras no esté definida, la API "
                    + "arranca SIN ninguna cuenta con rol ADMIN.", ADMIN_USERNAME);
            return;
        }
        crearUsuarioSiNoExiste(ADMIN_USERNAME, ADMIN_EMAIL, adminPassword, RoleType.ADMIN);
    }

    /**
     * Crea la cuenta de invitado con una contraseña aleatoria que se descarta al instante.
     * <p>
     * El invitado es una cuenta COMPARTIDA y sin dueño: existe para que POST /auth/guest
     * pueda emitir un token de solo lectura sin obligar a registrarse. Ese endpoint no
     * comprueba credenciales —firma el token directamente a partir de la cuenta—, así que
     * la contraseña del invitado no la necesita nadie: lo único que permitiría es entrar
     * por POST /auth/login con unas credenciales que conocería todo el mundo.
     * <p>
     * De ahí la decisión: la cuenta se sigue creando en todos los perfiles, porque sin ella
     * el acceso de invitado responde 404, pero con una contraseña aleatoria de 32 bytes que
     * no se guarda ni se registra en ningún sitio. Queda solo su hash, que no abre nada.
     */
    private void crearInvitado() {
        crearUsuarioSiNoExiste(GUEST_USERNAME, GUEST_EMAIL, passwordInutilizable(), RoleType.GUEST);
    }

    // Contraseña aleatoria de 32 bytes: nadie la conoce y nadie la necesita.
    private String passwordInutilizable() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
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
