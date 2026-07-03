package es.pmdm.gymprofit.network;

// ============================================================
// UtilREST — estado de sesión (tokens) compartido en memoria por toda la app.
// Tras la migración a Retrofit tipado (etapa 2), la parte de peticiones/parseo
// desapareció; esta clase se queda SOLO con la gestión del access/refresh token
// que consumen el AuthInterceptor y el TokenAuthenticator de ApiClient:
//  - guarda/lee los tokens en memoria (los persiste PreferencesManager aparte),
//  - avisa vía OnUnauthorizedListener ante un 401 no recuperable,
//  - recibe los tokens renovados del TokenAuthenticator para actualizarlos.
// ============================================================
public class UtilREST {

    // Callback invocado cuando la API responde 401 y NO se ha podido renovar la sesión
    // (refresh ausente, expirado o revocado): hay que volver a login.
    public interface OnUnauthorizedListener {
        void onTokenExpired();
    }

    // Callback para persistir los tokens renovados (p. ej. en PreferencesManager),
    // de modo que la sesión renovada sobreviva a reinicios de la app.
    public interface TokenPersister {
        void persist(String token, String refreshToken);
    }

    // Access token JWT actual (de vida corta), compartido en memoria por toda la app.
    private static volatile String token = null;
    // Refresh token opaco (de vida larga) para renovar el access token sin re-login.
    private static volatile String refreshToken = null;
    private static OnUnauthorizedListener unauthorizedListener = null;
    private static TokenPersister tokenPersister = null;

    public static void setToken(String t) { token = t; }
    // Limpia AMBOS tokens (logout / sesión no recuperable).
    public static void clearToken() { token = null; refreshToken = null; }
    public static String getToken() { return token; }

    public static void setRefreshToken(String t) { refreshToken = t; }
    public static String getRefreshToken() { return refreshToken; }

    // Registra el listener global que se dispara al recibir un 401 no recuperable.
    public static void setOnUnauthorizedListener(OnUnauthorizedListener l) { unauthorizedListener = l; }

    // Registra el persistidor de tokens renovados (se llama tras un refresh exitoso).
    public static void setTokenPersister(TokenPersister p) { tokenPersister = p; }

    // Maneja un 401 no recuperable (el TokenAuthenticator ya intentó renovar y no pudo):
    // limpia la sesión y avisa vía OnUnauthorizedListener para volver a login. Lo usa
    // el ApiCallback tipado de la etapa 2.
    static void notifyUnauthorized() {
        clearToken();
        if (unauthorizedListener != null) unauthorizedListener.onTokenExpired();
    }

    // Llamado por ApiClient.TokenAuthenticator tras renovar el token: actualiza el estado
    // en memoria y lo persiste para que sobreviva a reinicios.
    static void onTokensRefreshed(String nuevoToken, String nuevoRefresh) {
        token = nuevoToken;
        refreshToken = nuevoRefresh;
        if (tokenPersister != null) tokenPersister.persist(nuevoToken, nuevoRefresh);
    }
}
