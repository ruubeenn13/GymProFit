package com.gymprofit.api.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// ============================================================
// AuthRateLimitFilter — limitador de peticiones en /auth/** (anti brute-force/spam)
// Los endpoints de login, registro, invitado y refresh no requieren autenticación,
// así que son objetivo de fuerza bruta y spam. Este filtro aplica una ventana fija
// por IP: como máximo N peticiones cada X segundos; al superarlo responde 429.
// Se ejecuta antes del filtro JWT. Es en memoria (una sola instancia en free tier).
// Configurable/deshabilitable por properties (se desactiva en el perfil de tests).
// ============================================================
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    // Rutas sensibles sin autenticación que se protegen con el límite.
    private static final Set<String> RUTAS_LIMITADAS = Set.of(
            "/auth/login", "/auth/register", "/auth/guest", "/auth/refresh");

    // Si está deshabilitado (p.ej. en tests), el filtro no hace nada.
    @Value("${app.auth.rate-limit.enabled:true}")
    private boolean habilitado;

    // Nº máximo de peticiones permitidas por IP dentro de la ventana.
    @Value("${app.auth.rate-limit.max-requests:15}")
    private int maxPeticiones;

    // Duración de la ventana en segundos.
    @Value("${app.auth.rate-limit.window-seconds:60}")
    private long ventanaSegundos;

    // Contadores por IP. Cada entrada guarda el inicio de su ventana y el conteo.
    private final ConcurrentHashMap<String, Contador> contadores = new ConcurrentHashMap<>();

    // Contador mutable de una IP: momento de inicio de la ventana y nº de peticiones.
    private static final class Contador {
        long inicioVentanaMs;
        int peticiones;
        Contador(long inicioVentanaMs, int peticiones) {
            this.inicioVentanaMs = inicioVentanaMs;
            this.peticiones = peticiones;
        }
    }

    // Solo se filtran las rutas limitadas; el resto pasa sin coste.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !RUTAS_LIMITADAS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Deshabilitado por config: no aplica límite.
        if (!habilitado) {
            chain.doFilter(request, response);
            return;
        }
        // Si la IP no ha superado su cupo en la ventana, deja pasar la petición.
        if (permitido(clientIp(request))) {
            chain.doFilter(request, response);
            return;
        }
        // Cupo superado: 429 con cuerpo JSON homogéneo (mismo shape que Response) y Retry-After.
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(ventanaSegundos));
        response.getWriter().write(
                "{\"code\":429,\"message\":\"Demasiadas peticiones. Inténtalo de nuevo en unos segundos.\"}");
    }

    // Aplica la ventana fija de forma atómica: reinicia si la ventana caducó, si no incrementa.
    // Permitido mientras el conteo no supere el máximo configurado.
    private boolean permitido(String ip) {
        long ahora = System.currentTimeMillis();
        long ventanaMs = ventanaSegundos * 1000L;
        Contador c = contadores.compute(ip, (k, actual) -> {
            if (actual == null || ahora - actual.inicioVentanaMs >= ventanaMs) {
                return new Contador(ahora, 1);
            }
            actual.peticiones++;
            return actual;
        });
        return c.peticiones <= maxPeticiones;
    }

    // Obtiene la IP real del cliente. Tras el proxy de Render la IP va en X-Forwarded-For
    // (primer salto); si no está, usa la IP remota directa.
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
