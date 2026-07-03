package es.pmdm.gymprofit.model.auth;

import java.util.List;

// ============================================================
// TokenResponse — POJO de la respuesta de autenticación (etapa 2, Fase 7).
// Deserializa vía Gson el TokenDTO que devuelve la API en login/guest:
// access token JWT (vida corta), refresh token opaco (vida larga), username
// y la lista de roles. Sustituye al parseo manual de UtilJSONParser
// (parseToken/parseRefreshToken/parseTokenUsername/parseTokenRol) en las
// pantallas de login. El método rolPrincipal() replica EXACTAMENTE la lógica
// de UtilJSONParser.parseTokenRol (primer rol normalizado con prefijo "ROLE_",
// o "ROLE_USER" por defecto) para no alterar cómo se guarda el rol en prefs.
// ============================================================
public class TokenResponse {

    // Access token JWT de vida corta que autoriza las peticiones.
    private String token;
    // Refresh token opaco de larga duración para renovar el access token.
    private String refreshToken;
    // Nombre de usuario autenticado.
    private String username;
    // Roles asignados al usuario (p. ej. ["ROLE_USER"] o ["ROLE_ADMIN"]).
    private List<String> roles;

    // Constructor vacío requerido para deserialización JSON (Gson/Retrofit).
    public TokenResponse() {}

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    // Devuelve el rol principal normalizado con prefijo "ROLE_", replicando
    // exactamente UtilJSONParser.parseTokenRol: toma el primer rol de la lista
    // (o "USER" si viene vacío), le antepone "ROLE_" si no lo lleva, y devuelve
    // "ROLE_USER" cuando no hay roles. Así el rol guardado en prefs es idéntico.
    public String rolPrincipal() {
        if (roles != null && !roles.isEmpty()) {
            String r = roles.get(0);
            if (r == null || r.isEmpty()) r = "USER";
            return r.startsWith("ROLE_") ? r : "ROLE_" + r;
        }
        return "ROLE_USER";
    }
}
