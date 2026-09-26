package es.pmdm.gymprofit.network;

// ============================================================
// UtilREST — estado de sesión (tokens) compartido en memoria por toda la app.
// Tras la migración a Retrofit tipado (etapa 2), la parte de peticiones/parseo
// desapareció; esta clase se queda SOLO con la gestión del access/refresh token
// que consumen el AuthInterceptor y el TokenAuthenticator de ApiClient:
//  - guarda/lee los tokens en memoria y, si en memoria no están, los carga del
//    almacén cifrado (GP-091): cuando Android mata la app en segundo plano y la
//    vuelve a abrir en la pantalla donde estaba, no pasa por la splash, y antes
//    nadie volvía a rellenar el token, así que la sesión se cerraba sola,
//  - avisa vía OnUnauthorizedListener ante un 401 no recuperable,
//  - recibe los tokens renovados del TokenAuthenticator para actualizarlos.
// ============================================================
public class UtilREST {

    // Callback invocado cuando la API responde 401 y NO se ha podido renovar la sesión
    // (refresh ausente, expirado o revocado): hay que volver a login.
    public interface OnUnauthorizedListener {
        /**
         * @param cuentaDesactivada {@code true} si la API rechazó la sesión porque la
         *                          cuenta está desactivada, no porque haya caducado (GP-083).
         */
        void onTokenExpired(boolean cuentaDesactivada);
    }

    /**
     * Dónde viven los tokens fuera de la memoria del proceso (el almacén cifrado de
     * PreferencesManager). Lo registra la Application al arrancar el proceso, que es lo
     * único que se ejecuta siempre, venga el usuario de la splash o de una pantalla
     * restaurada tras matar el proceso.
     */
    public interface AlmacenTokens {
        String leerToken();
        String leerRefresh();
        void guardar(String token, String refreshToken);
        void borrar();
    }

    // Access token JWT actual (de vida corta), compartido en memoria por toda la app.
    private static volatile String token = null;
    // Refresh token opaco (de vida larga) para renovar el access token sin re-login.
    private static volatile String refreshToken = null;
    private static OnUnauthorizedListener unauthorizedListener = null;
    private static volatile AlmacenTokens almacen = null;

    // Registra el almacén persistente (lo hace GymProFitApp en onCreate).
    public static void setAlmacen(AlmacenTokens a) { almacen = a; }

    public static void setToken(String t) { token = t; }
    // Limpia AMBOS tokens (logout / sesión no recuperable), en memoria y en el almacén:
    // si solo se limpiara la memoria, la siguiente petición los volvería a cargar.
    public static void clearToken() {
        token = null;
        refreshToken = null;
        AlmacenTokens a = almacen;
        if (a != null) a.borrar();
    }
    // El token en memoria o, si falta (proceso recién recreado), el del almacén.
    public static String getToken() {
        String t = token;
        AlmacenTokens a = almacen;
        if (t == null && a != null) {
            t = a.leerToken();
            token = t;
        }
        return t;
    }

    public static void setRefreshToken(String t) { refreshToken = t; }
    public static String getRefreshToken() {
        String r = refreshToken;
        AlmacenTokens a = almacen;
        if (r == null && a != null) {
            r = a.leerRefresh();
            refreshToken = r;
        }
        return r;
    }

    // Solo para tests: olvida la memoria sin tocar el almacén, como al matar el proceso.
    static void olvidarMemoria() { token = null; refreshToken = null; }

    // Registra el listener global que se dispara al recibir un 401 no recuperable.
    public static void setOnUnauthorizedListener(OnUnauthorizedListener l) { unauthorizedListener = l; }

    // Maneja un 401 no recuperable (el TokenAuthenticator ya intentó renovar y no pudo):
    // limpia la sesión y avisa vía OnUnauthorizedListener para volver a login. Lo usa
    // el ApiCallback tipado de la etapa 2.
    static void notifyUnauthorized(boolean cuentaDesactivada) {
        clearToken();
        if (unauthorizedListener != null) unauthorizedListener.onTokenExpired(cuentaDesactivada);
    }

    // Llamado por ApiClient.TokenAuthenticator tras renovar el token: actualiza el estado
    // en memoria y lo persiste para que sobreviva a reinicios. El refresh es de un solo
    // uso (se rota), así que si el nuevo no se guardara, tras matar el proceso se leería
    // el viejo, ya revocado, y la sesión se perdería igual.
    static void onTokensRefreshed(String nuevoToken, String nuevoRefresh) {
        token = nuevoToken;
        refreshToken = nuevoRefresh;
        AlmacenTokens a = almacen;
        if (a != null) a.guardar(nuevoToken, nuevoRefresh);
    }
}
