package com.gymprofit.api.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Verifica la ventana fija del AuthRateLimitFilter (2 niveles) sin Spring ni BD:
// instancia el filtro, inyecta la config por reflexión y comprueba que deja
// pasar hasta el máximo del nivel y responde 429 al superarlo.
@DisplayName("AuthRateLimitFilter — ventana fija por IP (estricto + global)")
class AuthRateLimitFilterTest {

    // Construye el filtro con enabled=true; el nivel estricto usa max/ventana dados
    // y el global un cupo muy alto (60s) para que no interfiera en los tests del estricto.
    private AuthRateLimitFilter nuevoFiltro(int max, long ventana) {
        AuthRateLimitFilter f = new AuthRateLimitFilter();
        ReflectionTestUtils.setField(f, "habilitado", true);
        ReflectionTestUtils.setField(f, "maxEstricto", max);
        ReflectionTestUtils.setField(f, "ventanaEstrictaSeg", ventana);
        ReflectionTestUtils.setField(f, "maxGlobal", 1000);
        ReflectionTestUtils.setField(f, "ventanaGlobalSeg", 60L);
        return f;
    }

    // Dispara una petición a una ruta ESTRICTA (/auth/login) desde la misma IP.
    private int dispararUna(AuthRateLimitFilter f) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
        req.setServletPath("/auth/login");
        req.setRemoteAddr("10.0.0.7");
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilterInternal(req, res, new MockFilterChain());
        return res.getStatus();
    }

    // Dispara una petición a una ruta NO estricta (nivel global) desde la IP dada.
    private int dispararGlobal(AuthRateLimitFilter f, String ip) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/rutinas");
        req.setServletPath("/rutinas");
        req.setRemoteAddr(ip);
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
        a.setServletPath("/auth/login");
        a.setRemoteAddr("1.1.1.1");
        MockHttpServletResponse ra = new MockHttpServletResponse();
        f.doFilterInternal(a, ra, new MockFilterChain());
        assertEquals(200, ra.getStatus());

        // Otra IP, su primera petición también pasa (contador propio).
        MockHttpServletRequest b = new MockHttpServletRequest("POST", "/auth/login");
        b.setServletPath("/auth/login");
        b.setRemoteAddr("2.2.2.2");
        MockHttpServletResponse rb = new MockHttpServletResponse();
        f.doFilterInternal(b, rb, new MockFilterChain());
        assertEquals(200, rb.getStatus());

        // Segunda de la primera IP supera su cupo (max=1) -> 429.
        MockHttpServletRequest a2 = new MockHttpServletRequest("POST", "/auth/login");
        a2.setServletPath("/auth/login");
        a2.setRemoteAddr("1.1.1.1");
        MockHttpServletResponse ra2 = new MockHttpServletResponse();
        f.doFilterInternal(a2, ra2, new MockFilterChain());
        assertEquals(429, ra2.getStatus());
    }

    @Test
    @DisplayName("nivel global limita el resto de rutas (backstop anti scraping)")
    void nivel_global_limita_rutas_normales() throws Exception {
        AuthRateLimitFilter f = new AuthRateLimitFilter();
        ReflectionTestUtils.setField(f, "habilitado", true);
        ReflectionTestUtils.setField(f, "maxEstricto", 15);
        ReflectionTestUtils.setField(f, "ventanaEstrictaSeg", 60L);
        ReflectionTestUtils.setField(f, "maxGlobal", 3);
        ReflectionTestUtils.setField(f, "ventanaGlobalSeg", 60L);

        // Las 3 primeras a una ruta normal pasan; la 4ª supera el cupo global -> 429.
        for (int i = 1; i <= 3; i++) {
            assertEquals(200, dispararGlobal(f, "3.3.3.3"), "la petición global " + i + " debería pasar");
        }
        assertEquals(429, dispararGlobal(f, "3.3.3.3"), "la 4ª global debería ser 429");
    }

    @Test
    @DisplayName("los contadores estricto y global son independientes")
    void estricto_y_global_independientes() throws Exception {
        // Estricto agotado (max=1) no debe afectar al cupo global de la misma IP.
        AuthRateLimitFilter f = nuevoFiltro(1, 60);
        assertEquals(200, dispararUna(f), "1ª estricta pasa");
        assertEquals(429, dispararUna(f), "2ª estricta 429 (cupo estricto=1)");
        // La misma IP en una ruta global sigue teniendo su cupo alto intacto.
        assertEquals(200, dispararGlobal(f, "10.0.0.7"), "la global de la misma IP pasa");
    }

    @Test
    @DisplayName("no se limitan preflight OPTIONS ni el health-check de la PaaS")
    void excluye_options_y_actuator() {
        AuthRateLimitFilter f = nuevoFiltro(1, 60);

        MockHttpServletRequest options = new MockHttpServletRequest("OPTIONS", "/rutinas");
        assertTrue(f.shouldNotFilter(options), "el preflight OPTIONS no se filtra");

        MockHttpServletRequest health = new MockHttpServletRequest("GET", "/actuator/health");
        health.setServletPath("/actuator/health");
        assertTrue(f.shouldNotFilter(health), "el health-check no se filtra");

        MockHttpServletRequest normal = new MockHttpServletRequest("GET", "/rutinas");
        normal.setServletPath("/rutinas");
        assertFalse(f.shouldNotFilter(normal), "una ruta normal sí se filtra");
    }
}
