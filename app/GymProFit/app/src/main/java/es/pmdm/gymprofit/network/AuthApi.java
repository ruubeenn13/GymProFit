package es.pmdm.gymprofit.network;

import java.util.Map;

import es.pmdm.gymprofit.model.auth.TokenResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

// ============================================================
// AuthApi — interfaz Retrofit tipada del dominio "auth" (etapa 2, Fase 7).
// Cubre el arranque de sesión: login con credenciales, registro de nuevos
// usuarios, acceso como invitado y logout (revocación del refresh en servidor).
//
// IMPORTANTE (tokens): estos endpoints /auth/** NO llevan Authorization —
// el AuthInterceptor de ApiClient los excluye por la ruta "/auth/". El
// guardado de los tokens tras un login/guest exitoso (UtilREST.setToken /
// setRefreshToken + PreferencesManager) se hace en la Activity, igual que antes.
//
// login y guest devuelven un TokenResponse (token/refreshToken/username/roles).
// register y logout devuelven en el servidor un objeto {"mensaje": ...} que las
// pantallas ignoran → se tipan Call<Void> (Retrofit consume el cuerpo sin parsear).
// Los cuerpos de escritura viajan como Map<String,Object>.
// Paths relativos a BuildConfig.BASE_URL (.../api/).
// ============================================================
public interface AuthApi {

    // Autentica con username/password. body: {username, password}. Devuelve los tokens emitidos.
    @POST("auth/login")
    Call<TokenResponse> login(@Body Map<String, Object> body);

    // Registra un usuario nuevo (rol USER). body: {username, password, email}.
    // La respuesta ("mensaje") se ignora; el flujo hace login automático a continuación.
    @POST("auth/register")
    Call<Void> register(@Body Map<String, Object> body);

    // Crea una sesión de invitado (rol GUEST) sin credenciales. Devuelve los tokens emitidos.
    @POST("auth/guest")
    Call<TokenResponse> guest();

    // Cierra sesión revocando el refresh token en el servidor (best-effort).
    // body: {refreshToken}. La respuesta ("mensaje") se ignora.
    @POST("auth/logout")
    Call<Void> logout(@Body Map<String, Object> body);
}
