package com.gymprofit.api.config.security;

import com.gymprofit.api.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

// ============================================================
// JwtTokenProvider — generación y validación de tokens JWT
// Encapsula la creación de tokens firmados con clave secreta HMAC, la
// extracción de claims (username, expiración) y la validación de tokens
// recibidos en las peticiones. Usado por el filtro de autenticación JWT.
// ============================================================
@Service
public class JwtTokenProvider {

    // Clave secreta (Base64) usada para firmar/verificar los tokens, inyectada desde properties.
    @Value("${jwt.secret}")
    private String secretKey;

    // Tiempo de expiración del token en milisegundos, inyectado desde properties.
    @Value("${jwt.expiration}")
    private long expiration;

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    // Claim con la versión de sesión con la que se emitió el token (GP-236).
    public static final String CLAIM_VERSION_SESION = "ver";

    // Genera un JWT firmado a partir de la autenticación actual (subject = username, con fecha de emisión y expiración).
    public String generateToken(Authentication authentication) {
        return generateToken((UserDetails) authentication.getPrincipal());
    }

    // Genera un JWT firmado directamente desde un UserDetails, sellado con la versión de
    // sesión actual del usuario (GP-236).
    public String generateToken(UserDetails userDetails) {
        int version = userDetails instanceof Usuario usuario ? usuario.getVersionSesion() : 0;
        return generateToken(userDetails, version);
    }

    /**
     * Genera un JWT firmado y sellado con la versión de sesión indicada (GP-236). Al renovar
     * en /auth/refresh se pasa la del refresh usado, no la que tenga el usuario en ese
     * momento: así lo que renueve un refresh de antes de cerrar las sesiones nace caducado.
     */
    public String generateToken(UserDetails userDetails, int versionSesion) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim(CLAIM_VERSION_SESION, versionSesion)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    // Extrae el username (subject) contenido en el token.
    public String extractUsername(String token) {
        return getClaim(token, Claims::getSubject);
    }

    // Valida que el token pertenece al usuario indicado y no ha expirado.
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * La versión de sesión sellada en el token (GP-236). Un token emitido antes de que
     * existiera no la lleva y cuenta como 0, la versión con la que empieza todo usuario.
     */
    public int extractVersionSesion(String token) {
        Integer version = getClaim(token, claims -> claims.get(CLAIM_VERSION_SESION, Integer.class));
        return version != null ? version : 0;
    }

    // Comprueba si la fecha de expiración del token ya ha pasado.
    public boolean isTokenExpired(String token) {
        return getClaim(token, Claims::getExpiration).before(new Date());
    }

    // Extrae un claim concreto del token aplicando la función resolutora dada.
    public <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    // Parsea y verifica la firma del token, devolviendo todos sus claims.
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Construye la clave secreta HMAC a partir del secreto Base64 configurado.
    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}