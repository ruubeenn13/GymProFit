package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.auth.NombreUsuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// ============================================================
// AltaSimultaneaTest — dos altas a la vez con la misma base (GP-146, lote 1.5.1)
//
// Las dos miran si «zqcarrera» está libre antes de que ninguna lo guarde, y las dos
// lo ven libre. La restricción única deja entrar a una; la otra chocaba al guardar y
// recibía un 500. Ahora prueba el número siguiente.
//
// Determinista, como GuardadoSesionCompletaCarreraTest: un espía hace esperar en una
// barrera a las dos primeras propuestas, justo DESPUÉS de mirar, hasta que las dos
// han mirado. La tercera, la de la que perdió, pasa sin esperar.
//
// NO es @Transactional: cada alta tiene que confirmar su transacción para que exista
// la carrera. Se limpia a mano.
// ============================================================
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GP-146 — dos altas simultáneas con la misma base: las dos 201, con usuarios distintos")
class AltaSimultaneaTest {

    private static final String BASE = "zqcarrera";
    private static final List<String> CORREOS = List.of("zqcarrera-a@gp146.test", "zqcarrera-b@gp146.test");

    private final AtomicInteger propuestas = new AtomicInteger();
    private volatile CyclicBarrier barrera;

    @MockitoSpyBean
    private NombreUsuario nombreUsuario;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @BeforeEach
    void preparar() {
        limpiar();
        propuestas.set(0);
        barrera = new CyclicBarrier(2);
        doAnswer(inv -> {
            Object propuesto = inv.callRealMethod();
            CyclicBarrier b = barrera;
            if (b != null && propuestas.incrementAndGet() <= 2) {
                b.await(20, TimeUnit.SECONDS);
            }
            return propuesto;
        }).when(nombreUsuario).proponer(any(), anyString());
    }

    @AfterEach
    void limpiar() {
        barrera = null;
        for (String correo : CORREOS) {
            usuarioRepository.findByEmail(correo).ifPresent(usuarioRepository::delete);
        }
    }

    private MockHttpServletResponse alta(String correo) throws Exception {
        return mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", correo, "nombre", "Zq Carrera", "password", "Frase corta de alta"))))
                .andReturn().getResponse();
    }

    @Test
    @DisplayName("la que choca al guardar prueba el número siguiente en vez de dar 500")
    void dosALaVez() throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        MockHttpServletResponse ra;
        MockHttpServletResponse rb;
        try {
            Future<MockHttpServletResponse> a = hilos.submit(() -> alta(CORREOS.get(0)));
            Future<MockHttpServletResponse> b = hilos.submit(() -> alta(CORREOS.get(1)));
            ra = a.get(60, TimeUnit.SECONDS);
            rb = b.get(60, TimeUnit.SECONDS);
        } finally {
            hilos.shutdownNow();
        }

        assertThat(propuestas.get())
                .as("las dos primeras propuestas tienen que haber pasado por la barrera")
                .isGreaterThanOrEqualTo(2);
        assertThat(List.of(ra.getStatus(), rb.getStatus()))
                .as("cuerpos %s / %s", ra.getContentAsString(), rb.getContentAsString())
                .containsExactly(201, 201);

        String ua = objectMapper.readTree(ra.getContentAsString()).get("username").asText();
        String ub = objectMapper.readTree(rb.getContentAsString()).get("username").asText();
        assertThat(List.of(ua, ub)).containsExactlyInAnyOrder(BASE, BASE + "2");
        assertThat(usuarioRepository.findByUsername(ua).orElseThrow().getEmail()).isIn(CORREOS);
        assertThat(usuarioRepository.findByUsername(ub).orElseThrow().getEmail()).isIn(CORREOS);
    }
}
