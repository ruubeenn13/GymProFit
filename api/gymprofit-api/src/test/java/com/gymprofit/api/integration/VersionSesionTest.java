package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.config.security.JwtTokenProvider;
import com.gymprofit.api.entity.PasswordResetCodigo;
import com.gymprofit.api.entity.Role;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IPasswordResetCodigoRepository;
import com.gymprofit.api.repository.jpa.IRefreshTokenRepository;
import com.gymprofit.api.repository.jpa.IRoleRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// ============================================================
// VersionSesionTest — cambiar o recuperar la contraseña cierra todas las sesiones (GP-236)
//
// Cerrar sesiones tiene que alcanzar también a los access tokens ya emitidos, no solo
// a los refresh guardados. Cada caso usa JWT de verdad en la cabecera, porque lo que se
// prueba es el filtro, y el contexto entero levantado.
//
// NO es @Transactional: cada petición tiene su transacción y la confirma, que es como
// pasa en producción y lo único que deja ver la carrera del último caso. El precio es
// limpiar a mano.
// ============================================================
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GP-236 — cambiar la contraseña cierra también los access tokens")
class VersionSesionTest {

    private static final String USUARIO = "__gp236_owner__";
    private static final String PASSWORD = "Test1234";
    private static final String NUEVA = "Nueva-Clave-236x";
    private static final String CODIGO = "482913";

    // Estado de la barrera del caso de la carrera. Estático porque el BeanPostProcessor se
    // crea antes que el test. Solo se para una lectura: la del refresh que se cruza.
    private static final AtomicBoolean PARAR_LECTURA = new AtomicBoolean(false);
    private static volatile CountDownLatch leido;
    private static volatile CountDownLatch seguir;

    @TestConfiguration
    static class BarreraEnElRefresh {

        /**
         * Envuelve el repositorio de refresh tokens: todo va al real, y la primera lectura
         * por valor después de armar la barrera avisa y espera a que el test la suelte.
         * Así el refresh ya ha leído su token vigente cuando la contraseña cambia, y sigue
         * después: exactamente el cruce.
         */
        @Bean
        static BeanPostProcessor barreraTrasLeerElRefresh() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String nombre) {
                    if (!(bean instanceof IRefreshTokenRepository real)) return bean;
                    return Proxy.newProxyInstance(
                            IRefreshTokenRepository.class.getClassLoader(),
                            new Class<?>[]{IRefreshTokenRepository.class},
                            (proxy, metodo, args) -> {
                                Object resultado;
                                try {
                                    resultado = metodo.invoke(real, args);
                                } catch (InvocationTargetException e) {
                                    throw e.getCause();
                                }
                                if ("findByToken".equals(metodo.getName())
                                        && PARAR_LECTURA.compareAndSet(true, false)) {
                                    leido.countDown();
                                    if (!seguir.await(30, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException("la barrera no se soltó");
                                    }
                                }
                                return resultado;
                            });
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IPasswordResetCodigoRepository codigoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbc;

    @Value("${jwt.secret}")
    private String secreto;

    @Value("${jwt.expiration}")
    private long caducidad;

    private Usuario usuario;

    @BeforeEach
    void sembrar() {
        PARAR_LECTURA.set(false);
        limpiar();

        List<Role> roles = roleRepository.findByNombreIn(List.of(RoleType.USER.getValue()));
        Usuario u = new Usuario();
        u.setUsername(USUARIO);
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setEmail(USUARIO + "@test.local");
        u.setFechaRegistro(LocalDateTime.now());
        u.setActivo(true);
        u.setRoles(roles);
        usuario = usuarioRepository.save(u);
    }

    @AfterEach
    void limpiarDespues() {
        PARAR_LECTURA.set(false);
        limpiar();
    }

    // Refresh y códigos van por SQL porque sus repositorios no buscan por usuario.
    private void limpiar() {
        usuarioRepository.findByUsername(USUARIO).ifPresent(u -> {
            jdbc.update("DELETE FROM refresh_tokens WHERE usuario_id = ?", u.getId());
            jdbc.update("DELETE FROM password_reset_codigos WHERE usuario_id = ?", u.getId());
            usuarioRepository.delete(u);
        });
    }

    // ── Peticiones, como las haría la app ───────────────────────────────────

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder peticion, Object cuerpo)
            throws Exception {
        return peticion.accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cuerpo));
    }

    private MockHttpServletResponse perform(MockHttpServletRequestBuilder peticion) throws Exception {
        return mockMvc.perform(peticion).andReturn().getResponse();
    }

    private int protegida(String token) throws Exception {
        return perform(get("/usuarios/{id}", usuario.getId())
                .header("Authorization", "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON)).getStatus();
    }

    private int publica(String token) throws Exception {
        return perform(get("/actuator/health")
                .header("Authorization", "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON)).getStatus();
    }

    private JsonNode entrar(String password) throws Exception {
        MockHttpServletResponse r = perform(json(post("/auth/login"),
                Map.of("username", USUARIO, "password", password)));
        assertThat(r.getStatus()).as("login: %s", r.getContentAsString()).isEqualTo(200);
        return objectMapper.readTree(r.getContentAsString());
    }

    private MockHttpServletResponse renovar(String refresh) throws Exception {
        return perform(json(post("/auth/refresh"), Map.of("refreshToken", refresh)));
    }

    private void cambiarContrasena(String token) throws Exception {
        MockHttpServletResponse r = perform(json(post("/auth/change-password"),
                Map.of("currentPassword", PASSWORD, "newPassword", NUEVA))
                .header("Authorization", "Bearer " + token));
        assertThat(r.getStatus()).as("cambio: %s", r.getContentAsString()).isBetween(200, 299);
    }

    private void restablecerPorCodigo() throws Exception {
        PasswordResetCodigo c = new PasswordResetCodigo();
        c.setUsuario(usuario);
        c.setCodigoHash(passwordEncoder.encode(CODIGO));
        c.setFechaCreacion(LocalDateTime.now());
        c.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));
        c.setUsado(false);
        c.setIntentos(0);
        codigoRepository.save(c);

        MockHttpServletResponse r = perform(json(post("/auth/reset-password"),
                Map.of("identificador", USUARIO, "codigo", CODIGO, "newPassword", NUEVA)));
        assertThat(r.getStatus()).as("restablecer: %s", r.getContentAsString()).isBetween(200, 299);
    }

    // Un token como los que emitía la API antes de GP-236: mismo secreto, sin versión.
    private String tokenSinVersion() {
        return Jwts.builder()
                .subject(USUARIO)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + caducidad))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto)))
                .compact();
    }

    // ── Casos ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("tras cambiar la contraseña, un access token de antes recibe 401 en una ruta protegida y en una pública")
    void cambio_cierra_access_de_antes() throws Exception {
        String sesionA = entrar(PASSWORD).get("token").asText();
        String sesionB = entrar(PASSWORD).get("token").asText();
        assertThat(protegida(sesionB)).as("control: antes del cambio, B entra").isEqualTo(200);

        cambiarContrasena(sesionA);

        assertThat(protegida(sesionB)).as("B, ruta protegida").isEqualTo(401);
        assertThat(publica(sesionB)).as("B, ruta pública").isEqualTo(401);
        assertThat(protegida(sesionA)).as("A, la que cambió, también vuelve a entrar").isEqualTo(401);
    }

    @Test
    @DisplayName("entrar justo después de cambiar la contraseña da un token que vale")
    void entrar_tras_cambio_200() throws Exception {
        String sesionA = entrar(PASSWORD).get("token").asText();

        cambiarContrasena(sesionA);
        JsonNode nueva = entrar(NUEVA);

        assertThat(protegida(nueva.get("token").asText())).isEqualTo(200);
        assertThat(renovar(nueva.get("refreshToken").asText()).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("restablecer por código cierra los access de antes, y entrar después vale")
    void restablecer_cierra_access_de_antes() throws Exception {
        String antes = entrar(PASSWORD).get("token").asText();

        restablecerPorCodigo();

        assertThat(protegida(antes)).as("ruta protegida").isEqualTo(401);
        assertThat(publica(antes)).as("ruta pública").isEqualTo(401);
        assertThat(protegida(entrar(NUEVA).get("token").asText())).as("entrar después").isEqualTo(200);
    }

    @Test
    @DisplayName("un token sin versión vale como versión 0 y deja de valer tras un cambio")
    void token_sin_version() throws Exception {
        String viejo = tokenSinVersion();
        assertThat(protegida(viejo)).as("desplegar no cierra la sesión a nadie").isEqualTo(200);

        cambiarContrasena(entrar(PASSWORD).get("token").asText());

        assertThat(protegida(viejo)).isEqualTo(401);
    }

    @Test
    @DisplayName("un refresh sellado con una versión menor no renueva, aunque nadie lo haya revocado")
    void refresh_de_version_menor_401() throws Exception {
        String refresh = entrar(PASSWORD).get("refreshToken").asText();

        // Solo la versión, sin revocar nada: lo único que puede pararlo es la comparación.
        jdbc.update("UPDATE usuarios SET version_sesion = version_sesion + 1 WHERE id = ?", usuario.getId());

        assertThat(renovar(refresh).getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("un refresh que se cruza con el cambio no deja vivo ni el access ni el refresh")
    void refresh_cruzado_con_el_cambio() throws Exception {
        JsonNode b = entrar(PASSWORD);
        String accessA = entrar(PASSWORD).get("token").asText();

        leido = new CountDownLatch(1);
        seguir = new CountDownLatch(1);
        PARAR_LECTURA.set(true);

        ExecutorService hilo = Executors.newSingleThreadExecutor();
        try {
            // B renueva y se queda parado justo después de leer su refresh, aún vigente.
            Future<MockHttpServletResponse> refreshB = hilo.submit(() -> renovar(b.get("refreshToken").asText()));
            assertThat(leido.await(30, TimeUnit.SECONDS)).as("el refresh tiene que llegar a la barrera").isTrue();

            // Mientras, A cambia la contraseña y su transacción se confirma entera.
            cambiarContrasena(accessA);
            seguir.countDown();

            MockHttpServletResponse r = refreshB.get(60, TimeUnit.SECONDS);
            if (r.getStatus() == 200) {
                JsonNode emitido = objectMapper.readTree(r.getContentAsString());
                assertThat(protegida(emitido.get("token").asText()))
                        .as("el access que emitió el refresh cruzado").isEqualTo(401);
                assertThat(renovar(emitido.get("refreshToken").asText()).getStatus())
                        .as("el refresh que emitió el refresh cruzado").isEqualTo(401);
            } else {
                assertThat(r.getStatus()).as("refresh cruzado: %s", r.getContentAsString()).isEqualTo(401);
            }
        } finally {
            seguir.countDown();
            hilo.shutdownNow();
        }

        assertThat(protegida(b.get("token").asText())).as("el access de B de antes").isEqualTo(401);
    }
}
