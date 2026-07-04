package com.gymprofit.api.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Verifica la ventana fija del AuthRateLimitFilter sin Spring ni BD:
// instancia el filtro, inyecta la config por reflexión y comprueba que
// deja pasar hasta el máximo y responde 429 al superarlo.
@DisplayName("AuthRateLimitFilter — ventana fija por IP")
class AuthRateLimitFilterTest {

    // Construye el filtro con enabled=true, max peticiones y ventana dados.
    private AuthRateLimitFilter nuevoFiltro(int max, long ventana) {
        AuthRateLimitFilter f = new AuthRateLimitFilter();
        ReflectionTestUtils.setField(f, "habilitado", true);
        ReflectionTestUtils.setField(f, "maxPeticiones", max);
        ReflectionTestUtils.setField(f, "ventanaSegundos", ventana);
        return f;
    }

    // Dispara una petición desde la misma IP y devuelve el status de la respuesta.
    private int dispararUna(AuthRateLimitFilter f) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
        req.setRemoteAddr("10.0.0.7");
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilterInternal(req, res, new MockFilterChain());
        return res.getStatus();
    }

    @Test
    @DisplayName("permite hasta el máximo y bloquea (429) al superarlo")
    void bloquea_al_superar_el_maximo() throws Exception {
        AuthRateLimitFilter f = nuevoFiltro(15, 60);

        // Las 15 primeras pasan (la MockFilterChain no toca el status -> 200 por defecto).
        for (int i = 1; i <= 15; i++) {
            assertEquals(200, dispararUna(f), "la petición " + i + " debería pasar");
        }
        // La 16ª supera el cupo de la ventana -> 429.
        assertEquals(429, dispararUna(f), "la 16ª debería ser 429");
    }

    @Test
    @DisplayName("enabled=false deja pasar siempre")
    void deshabilitado_no_limita() throws Exception {
        AuthRateLimitFilter f = nuevoFiltro(2, 60);
        ReflectionTestUtils.setField(f, "habilitado", false);
        for (int i = 1; i <= 5; i++) {
            assertEquals(200, dispararUna(f), "sin límite, todas pasan");
        }
    }

    @Test
    @DisplayName("IPs distintas tienen contadores independientes")
    void ips_independientes() throws Exception {
        AuthRateLimitFilter f = nuevoFiltro(1, 60);

        MockHttpServletRequest a = new MockHttpServletRequest("POST", "/auth/login");
        a.setRemoteAddr("1.1.1.1");
        MockHttpServletResponse ra = new MockHttpServletResponse();
        f.doFilterInternal(a, ra, new MockFilterChain());
        assertEquals(200, ra.getStatus());

        // Otra IP, su primera petición también pasa (contador propio).
        MockHttpServletRequest b = new MockHttpServletRequest("POST", "/auth/login");
        b.setRemoteAddr("2.2.2.2");
        MockHttpServletResponse rb = new MockHttpServletResponse();
        f.doFilterInternal(b, rb, new MockFilterChain());
        assertEquals(200, rb.getStatus());

        // Segunda de la primera IP supera su cupo (max=1) -> 429.
        MockHttpServletRequest a2 = new MockHttpServletRequest("POST", "/auth/login");
        a2.setRemoteAddr("1.1.1.1");
        MockHttpServletResponse ra2 = new MockHttpServletResponse();
        f.doFilterInternal(a2, ra2, new MockFilterChain());
        assertEquals(429, ra2.getStatus());
    }
}
