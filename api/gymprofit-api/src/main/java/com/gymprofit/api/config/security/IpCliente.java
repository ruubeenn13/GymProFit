package com.gymprofit.api.config.security;

import jakarta.servlet.http.HttpServletRequest;

// ============================================================
// IpCliente — la IP del cliente que hizo la petición. Todo lo que necesite la IP pasa
// por aquí.
//
// En Render, el borde (Cloudflare, el de Render) añade al final de X-Forwarded-For la
// IP desde la que le llega la petición, así que la que vale es la ÚLTIMA entrada. Sin
// cabecera (en local, sin proxy delante), la dirección remota de la conexión.
// ============================================================
public final class IpCliente {

    private IpCliente() {
    }

    /**
     * Devuelve la IP del cliente de la petición.
     *
     * @param request la petición HTTP
     * @return la última entrada de X-Forwarded-For o, sin ella, la dirección remota
     */
    public static String de(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada != null) {
            String ultima = reenviada.substring(reenviada.lastIndexOf(',') + 1).trim();
            if (!ultima.isEmpty()) {
                return ultima;
            }
        }
        return request.getRemoteAddr();
    }
}
