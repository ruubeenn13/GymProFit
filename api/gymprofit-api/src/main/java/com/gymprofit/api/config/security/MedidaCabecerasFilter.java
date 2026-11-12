package com.gymprofit.api.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// ============================================================
// MedidaCabecerasFilter — TEMPORAL. Registra las cabeceras de reenvío con las que
// llega una petición, para medir la cadena de proxies real de producción. Solo se
// activa con la cabecera X-Gp-Medida y no cambia nada de la respuesta. Se borra en
// cuanto quede medido.
// ============================================================
@Component
public class MedidaCabecerasFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MedidaCabecerasFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getHeader("X-Gp-Medida") == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        log.info("medida-cabeceras marca={} xff=[{}] cf-connecting-ip=[{}] true-client-ip=[{}] x-real-ip=[{}]"
                        + " forwarded=[{}] x-forwarded-proto=[{}] cf-ray=[{}] remoteAddr=[{}] host=[{}]",
                request.getHeader("X-Gp-Medida"),
                request.getHeader("X-Forwarded-For"),
                request.getHeader("CF-Connecting-IP"),
                request.getHeader("True-Client-IP"),
                request.getHeader("X-Real-IP"),
                request.getHeader("Forwarded"),
                request.getHeader("X-Forwarded-Proto"),
                request.getHeader("CF-Ray"),
                request.getRemoteAddr(),
                request.getHeader("Host"));
        chain.doFilter(request, response);
    }
}
