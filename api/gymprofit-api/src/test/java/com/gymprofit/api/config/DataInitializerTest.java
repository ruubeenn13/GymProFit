package com.gymprofit.api.config;

import com.gymprofit.api.entity.Role;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IRoleRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ============================================================
// DataInitializerTest — la semilla de usuarios no puede abrir una cuenta ADMIN
// con credenciales conocidas en producción.
//
// DataInitializer lleva @Profile("!test") a propósito, para que la semilla no
// interfiera con los @SpringBootTest. La anotación no se toca: este test no levanta
// contexto, instancia la clase a mano con los repositorios simulados y ejecuta run().
// Así se pueden probar los dos caminos —con y sin contraseña de administrador— sin
// depender de qué perfil esté activo ni de que haya base de datos.
// ============================================================
@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    private static final String SIN_PASSWORD_DE_ADMIN = "";
    private static final String PASSWORD_DE_ADMIN = "Admin1234";

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private IRoleRepository roleRepository;

    @Captor
    private ArgumentCaptor<Usuario> usuarioCaptor;

    // Codificador de mentira: deja la contraseña legible dentro del "hash" para poder
    // comprobar QUÉ se ha sembrado, y de paso evita pagar un BCrypt en cada test.
    private final PasswordEncoder passwordEncoder = new PasswordEncoder() {
        @Override
        public String encode(CharSequence raw) {
            return "enc:" + raw;
        }

        @Override
        public boolean matches(CharSequence raw, String encoded) {
            return encoded.equals("enc:" + raw);
        }
    };

    /**
     * El caso que motiva el cambio: con el perfil prod y sin ADMIN_PASSWORD definida,
     * la propiedad llega vacía y no debe crearse NINGUNA cuenta con rol ADMIN.
     */
    @Test
    void sinContrasenaDeAdmin_noSeCreaNingunUsuarioConRolAdmin() {
        prepararBdVacia();

        ejecutarSemilla(SIN_PASSWORD_DE_ADMIN);

        List<Usuario> creados = usuariosCreados(1);
        assertThat(creados).noneMatch(usuario -> tieneRol(usuario, RoleType.ADMIN));
        assertThat(creados).extracting(Usuario::getUsername).containsExactly("guest");
    }

    /**
     * Y el caso contrario: en dev y ci la propiedad trae valor, así que la semilla
     * sigue creando el administrador exactamente como antes.
     */
    @Test
    void conContrasenaDeAdmin_seSiembraElAdminConEsaContrasena() {
        prepararBdVacia();

        ejecutarSemilla(PASSWORD_DE_ADMIN);

        Usuario admin = buscar(usuariosCreados(2), "admin");
        assertThat(tieneRol(admin, RoleType.ADMIN)).isTrue();
        assertThat(admin.getPassword()).isEqualTo("enc:" + PASSWORD_DE_ADMIN);
        assertThat(admin.getEmail()).isEqualTo("admin@gymprofit.com");
        assertThat(admin.getActivo()).isTrue();
    }

    /**
     * El invitado sigue existiendo —POST /auth/guest devolvería 404 sin él— pero su
     * contraseña ya no es "guest": es aleatoria y se descarta, de modo que la cuenta
     * compartida no sirve para entrar por POST /auth/login.
     */
    @Test
    void elInvitadoSeCreaConUnaContrasenaQueNoSirveParaLogin() {
        prepararBdVacia();

        ejecutarSemilla(PASSWORD_DE_ADMIN);

        Usuario guest = buscar(usuariosCreados(2), "guest");
        assertThat(tieneRol(guest, RoleType.GUEST)).isTrue();
        assertThat(passwordEncoder.matches("guest", guest.getPassword())).isFalse();
        assertThat(passwordEncoder.matches("", guest.getPassword())).isFalse();
    }

    // Dos arranques no pueden compartir la contraseña del invitado: si fuese determinista
    // volvería a ser una credencial conocida, solo que más larga.
    @Test
    void laContrasenaDelInvitadoCambiaEnCadaArranque() {
        prepararBdVacia();

        ejecutarSemilla(SIN_PASSWORD_DE_ADMIN);
        ejecutarSemilla(SIN_PASSWORD_DE_ADMIN);

        List<Usuario> creados = usuariosCreados(2);
        assertThat(creados.get(0).getPassword()).isNotEqualTo(creados.get(1).getPassword());
    }

    // Comportamiento de siempre: si los usuarios ya están en BD, la semilla no toca nada.
    @Test
    void noRecreaLosUsuariosQueYaExisten() {
        when(usuarioRepository.existsByUsername(anyString())).thenReturn(true);

        ejecutarSemilla(PASSWORD_DE_ADMIN);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // --- Ayudas -------------------------------------------------------------

    // BD sin usuarios y con los tres roles del catálogo ya sembrados por Flyway.
    private void prepararBdVacia() {
        when(usuarioRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleRepository.findByNombreIn(anyList())).thenAnswer(invocacion -> {
            List<Integer> ids = invocacion.getArgument(0);
            return ids.stream().map(this::rolConId).toList();
        });
    }

    private void ejecutarSemilla(String contrasenaDeAdmin) {
        new DataInitializer(usuarioRepository, roleRepository, passwordEncoder, contrasenaDeAdmin).run();
    }

    private List<Usuario> usuariosCreados(int esperados) {
        verify(usuarioRepository, times(esperados)).save(usuarioCaptor.capture());
        return usuarioCaptor.getAllValues();
    }

    private Usuario buscar(List<Usuario> usuarios, String username) {
        return usuarios.stream()
                .filter(usuario -> username.equals(usuario.getUsername()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se ha creado el usuario '" + username + "'"));
    }

    private boolean tieneRol(Usuario usuario, RoleType tipo) {
        return usuario.getRoles().stream().anyMatch(rol -> rol.getNombre() == tipo);
    }

    private Role rolConId(Integer id) {
        RoleType tipo = Arrays.stream(RoleType.values())
                .filter(valor -> valor.getValue() == id)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Rol desconocido: " + id));
        return new Role(id, tipo);
    }
}
