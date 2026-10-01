package com.gymprofit.api.config.security;

import com.gymprofit.api.exceptions.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// ============================================================
// ClaveImportacion — la cerradura de /importacion/** (GP-164, DEC-041)
//
// La importación semanal de productos no entra con un token de usuario sino con una
// clave propia en la cabecera X-Clave-Importacion. Una cuenta ADMIN guardada en GitHub
// daría a quien la robara todo el panel de administración; esta clave solo deja
// escribir en productos_off. Sin la clave correcta, 403, venga quien venga, ADMIN
// incluido.
//
// Es un interceptor y no una comprobación en el controlador porque corre ANTES de leer
// el cuerpo: sin clave no se llega a parsear ni un byte del JSON.
//
// La clave sale de app.importacion.clave (IMPORTACION_CLAVE en producción, sin valor
// por defecto, DEC-016) y tiene que tener al menos 32 caracteres: si no, la API no
// arranca.
// ============================================================
@Component
public class ClaveImportacion implements HandlerInterceptor {

    public static final String CABECERA = "X-Clave-Importacion";
    static final int LONGITUD_MINIMA = 32;

    private final byte[] clave;

    public ClaveImportacion(@Value("${app.importacion.clave}") String clave) {
        if (clave == null || clave.length() < LONGITUD_MINIMA) {
            throw new IllegalStateException("app.importacion.clave tiene que tener al menos "
                    + LONGITUD_MINIMA + " caracteres");
        }
        this.clave = clave.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String recibida = request.getHeader(CABECERA);
        byte[] bytes = recibida == null ? new byte[0] : recibida.getBytes(StandardCharsets.UTF_8);
        // Comparación en tiempo constante: la latencia no dice cuántos caracteres acierta.
        if (!MessageDigest.isEqual(clave, bytes)) {
            throw new UnauthorizedException("error.acceso.sinPermiso");
        }
        return true;
    }
}
