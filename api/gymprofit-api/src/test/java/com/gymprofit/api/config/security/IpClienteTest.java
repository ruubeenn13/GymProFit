package com.gymprofit.api.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Verifica de dónde sale la IP del cliente: la última entrada de X-Forwarded-For, o la
// dirección remota si no hay cabecera.
@DisplayName("IpCliente — la IP del cliente tras el proxy de Render")
class IpClienteTest {

    private static MockHttpServletRequest peticion(String xff) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("162.158.110.48");
        if (xff != null) {
            req.addHeader("X-Forwarded-For", xff);
        }
        return req;
    }

    @Test
    @DisplayName("con varias entradas, la última")
    void varias_entradas_la_ultima() {
        assertEquals("193.228.204.206",
                IpCliente.de(peticion("203.0.113.9,198.51.100.7,193.228.204.206")));
    }

    @Test
    @DisplayName("quita los espacios de alrededor")
    void quita_espacios() {
        assertEquals("193.228.204.206", IpCliente.de(peticion("203.0.113.9 ,  193.228.204.206 ")));
    }

    @Test
    @DisplayName("con una sola entrada, esa")
    void una_entrada() {
        assertEquals("193.228.204.206", IpCliente.de(peticion("193.228.204.206")));
    }

    @Test
    @DisplayName("IPv6 tal cual")
    void ipv6() {
        assertEquals("2001:db8::1", IpCliente.de(peticion("203.0.113.9, 2001:db8::1")));
    }

    @Test
    @DisplayName("sin cabecera, la dirección remota")
    void sin_cabecera() {
        assertEquals("162.158.110.48", IpCliente.de(peticion(null)));
    }

    @Test
    @DisplayName("cabecera en blanco o con la última entrada vacía, la dirección remota")
    void cabecera_vacia() {
        assertEquals("162.158.110.48", IpCliente.de(peticion("   ")));
        assertEquals("162.158.110.48", IpCliente.de(peticion("203.0.113.9, ")));
    }
}
