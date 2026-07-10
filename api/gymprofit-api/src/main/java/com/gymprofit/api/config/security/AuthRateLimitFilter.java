package com.gymprofit.api.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// ============================================================
// AuthRateLimitFilter — limitador de peticiones por IP en DOS niveles.
//  - Nivel ESTRICTO: rutas de autenticación sin login (login/register/guest/
//    refresh/change-password), objetivo de fuerza bruta → cupo bajo.
//  - Nivel GLOBAL (backstop): TODAS las demás rutas, con un cupo alto que un
//    usuario real nunca alcanza pero que frena scraping/DoS (vaciar la BD).
// Ventana fija por IP y nivel; al superar el cupo responde 429 + Retry-After.
// Se ejecuta antes del filtro JWT. Es en memoria (válido con una sola instancia,
// como el free tier de Render; si se escala a varias haría falta un store compartido).
// Configurable/deshabilitable por properties (se desactiva en dev y en tests).
// ============================================================
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    // Rutas sensibles sin autenticación con el cupo ESTRICTO (anti fuerza bruta).
    private static final Set<String> RUTAS_ESTRICTAS = Set.of(
            "/auth/login", "/auth/register", "/auth/guest", "/auth/refresh", "/auth/change-password");

    // Master de activación (existente): desactiva TODO el filtro (dev/tests).
    @Value("${app.auth.rate-limit.enabled:true}")
    private boolean habilitado;

    // Nivel estricto: nº máximo de peticiones por IP en la ventana.
    @Value("${app.auth.rate-limit.max-requests:15}")
    private int maxEstricto;

    // Duración de la ventana del nivel estricto (segundos).
    @Value("${app.auth.rate-limit.window-seconds:60}")
    private long ventanaEstrictaSeg;

    // Nivel global (backstop): cupo alto por IP para el resto de rutas.
    @Value("${app.rate-limit.global.max-requests:200}")
    private int maxGlobal;

    // Duración de la ventana del nivel global (segundos).
    @Value("${app.rate-limit.global.window-seconds:60}")
    private long ventanaGlobalSeg;

    // Contadores separados por nivel (una entrada por IP en cada uno).
    private final ConcurrentHashMap<String, Contador> contadoresEstrictos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Contador> contadoresGlobales = new ConcurrentHashMap<>();

    // Contador mutable de una IP: momento de inicio de la ventana y nº de peticiones.
    private static final class Contador {
        long inicioVentanaMs;
        int peticiones;
        Contador(long inicioVentanaMs, int peticiones) {
            this.inicioVentanaMs = inicioVentanaMs;
            this.peticiones = peticiones;
        }
    }

    // No se limitan: el preflight CORS (OPTIONS, no lo lanza un usuario) ni el
    // health-check que Render sondea con frecuencia (no debe gastar cupo global).
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        return path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Deshabilitado por config: no aplica ningún límite.
        if (!habilitado) {
            chain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);
        boolean estricta = RUTAS_ESTRICTAS.contains(request.getServletPath());

        // Cada petición cuenta SOLO en el nivel que le corresponde (estricto o global).
        boolean permitido = estricta
                ? permitido(contadoresEstrictos, ip, maxEstricto, ventanaEstrictaSeg)
                : permitido(contadoresGlobales, ip, maxGlobal, ventanaGlobalSeg);

        if (permitido) {
            chain.doFilter(request, response);
            return;
        }

        // Cupo superado: 429 con cuerpo JSON homogéneo (mismo shape que Response) y Retry-After.
        long retryAfter = estricta ? ventanaEstrictaSeg : ventanaGlobalSeg;
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.getWriter().write(
                "{\"code\":429,\"message\":\"Demasiadas peticiones. Inténtalo de nuevo en unos segundos.\"}");
    }

    // Aplica la ventana fija de forma atómica: reinicia si la ventana caducó, si no incrementa.
    // Permitido mientras el conteo no supere el máximo del nivel.
    private boolean permitido(ConcurrentHashMap<String, Contador> contadores, String ip, int max, long ventanaSeg) {
        long ahora = System.currentTimeMillis();
        long ventanaMs = ventanaSeg * 1000L;
        Contador c = contadores.compute(ip, (k, actual) -> {
            if (actual == null || ahora - actual.inicioVentanaMs >= ventanaMs) {
                return new Contador(ahora, 1);
            }
            actual.peticiones++;
            return actual;
        });
        return c.peticiones <= max;
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
