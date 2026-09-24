package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.config.security.JwtTokenProvider;
import com.gymprofit.api.entity.Role;
import com.gymprofit.api.entity.SesionEntrenamiento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IEjercicioRealizadoRepository;
import com.gymprofit.api.repository.jpa.IRoleRepository;
import com.gymprofit.api.repository.jpa.ISesionEntrenamientoRepository;
import com.gymprofit.api.repository.jpa.IUsuarioLogroRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// ============================================================
// GuardadoSesionCompletaCarreraTest — dos reintentos a la vez con la misma clave (GP-076)
//
// El caso que el guardado idempotente decía cubrir y no cubría: dos peticiones con la
// misma clave pasan las dos la comprobación previa antes de que ninguna inserte. Una
// gana en el índice único; la otra choca. Antes, la perdedora recibía un 500.
//
// Determinista, no a base de suerte: un espía envuelve el repositorio de sesiones y
// hace esperar en una barrera, justo DESPUÉS de la comprobación previa, a las dos
// primeras llamadas de findByUsuarioIdAndIdempotenciaClave. Ninguna sigue hasta que
// las dos han mirado y no han visto nada, que es exactamente la carrera. La tercera
// llamada, la relectura de la perdedora, pasa sin esperar.
//
// NO es @Transactional: cada petición tiene que tener su transacción de verdad, y
// confirmarla, para que exista la carrera. El precio es limpiar a mano.
// ============================================================
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GP-076 — dos guardados simultáneos con la misma clave: los dos 200, una sola sesión")
class GuardadoSesionCompletaCarreraTest {

    private static final String USUARIO = "__gp076_owner__";
    private static final String CLAVE = "clave-carrera-gp076";

    // Estado del espía. Estático porque el BeanPostProcessor se crea antes que el test.
    private static final AtomicInteger COMPROBACIONES = new AtomicInteger();
    private static volatile CyclicBarrier barrera;

    @TestConfiguration
    static class EspiaDelRepositorio {

        /**
         * Envuelve el repositorio de sesiones: todas las llamadas van al real, y las dos
         * primeras comprobaciones por clave esperan a la otra antes de devolver.
         */
        @Bean
        static BeanPostProcessor barreraEnLaComprobacionPrevia() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String nombre) {
                    if (!(bean instanceof ISesionEntrenamientoRepository real)) return bean;
                    return Proxy.newProxyInstance(
                            ISesionEntrenamientoRepository.class.getClassLoader(),
                            new Class<?>[]{ISesionEntrenamientoRepository.class},
                            (proxy, metodo, args) -> {
                                Object resultado;
                                try {
                                    resultado = metodo.invoke(real, args);
                                } catch (InvocationTargetException e) {
                                    throw e.getCause();
                                }
                                CyclicBarrier b = barrera;
                                if (b != null
                                        && "findByUsuarioIdAndIdempotenciaClave".equals(metodo.getName())
                                        && COMPROBACIONES.incrementAndGet() <= 2) {
                                    b.await(20, TimeUnit.SECONDS);
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ISesionEntrenamientoRepository sesionRepository;

    @Autowired
    private IEjercicioRealizadoRepository ejercicioRealizadoRepository;

    @Autowired
    private IUsuarioLogroRepository usuarioLogroRepository;

    private Usuario usuario;

    @BeforeEach
    void sembrar() {
        barrera = null;
        limpiar();

        List<Role> roles = roleRepository.findByNombreIn(List.of(RoleType.USER.getValue()));
        Usuario u = new Usuario();
        u.setUsername(USUARIO);
        u.setPassword(passwordEncoder.encode("Test1234"));
        u.setEmail(USUARIO + "@test.local");
        u.setFechaRegistro(LocalDateTime.now());
        u.setActivo(true);
        u.setRoles(roles);
        usuario = usuarioRepository.save(u);
    }

    @AfterEach
    void limpiarDespues() {
        barrera = null;
        limpiar();
    }

    // Sesiones, logros desbloqueados al completar y el usuario, en orden de dependencia.
    private void limpiar() {
        usuarioRepository.findByUsername(USUARIO).ifPresent(u -> {
            for (SesionEntrenamiento s : sesionRepository.findByUsuarioId(u.getId())) {
                ejercicioRealizadoRepository.deleteAll(ejercicioRealizadoRepository.findBySesionId(s.getId()));
                sesionRepository.delete(s);
            }
            usuarioLogroRepository.deleteAll(usuarioLogroRepository.findByUsuarioId(u.getId()));
            usuarioRepository.delete(u);
        });
    }

    private MockHttpServletResponse guardar(String token) throws Exception {
        return mockMvc.perform(post("/sesiones/completa")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveIdempotencia\":\"" + CLAVE + "\","
                                + "\"duracionMinutos\":30,\"completada\":true,\"ejercicios\":[]}"))
                .andReturn().getResponse();
    }

    @Test
    @DisplayName("las dos peticiones reciben 200 con el mismo id y solo queda una sesión")
    void dosALaVez() throws Exception {
        String token = jwtTokenProvider.generateToken(usuario);
        COMPROBACIONES.set(0);
        barrera = new CyclicBarrier(2);

        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<MockHttpServletResponse> a = hilos.submit(() -> guardar(token));
            Future<MockHttpServletResponse> b = hilos.submit(() -> guardar(token));
            MockHttpServletResponse ra = a.get(60, TimeUnit.SECONDS);
            MockHttpServletResponse rb = b.get(60, TimeUnit.SECONDS);

            assertThat(COMPROBACIONES.get())
                    .as("las dos comprobaciones previas tienen que haber pasado por la barrera")
                    .isGreaterThanOrEqualTo(2);
            assertThat(List.of(ra.getStatus(), rb.getStatus()))
                    .as("la perdedora también recibe 200: cuerpos %s / %s",
                            ra.getContentAsString(), rb.getContentAsString())
                    .containsExactly(200, 200);

            int idA = objectMapper.readTree(ra.getContentAsString()).get("id").asInt();
            int idB = objectMapper.readTree(rb.getContentAsString()).get("id").asInt();
            assertThat(idA).as("las dos devuelven la misma sesión").isEqualTo(idB);
        } finally {
            hilos.shutdownNow();
        }

        assertThat(sesionRepository.findByUsuarioId(usuario.getId()))
                .as("el índice único deja una sola sesión")
                .hasSize(1);
    }
}
