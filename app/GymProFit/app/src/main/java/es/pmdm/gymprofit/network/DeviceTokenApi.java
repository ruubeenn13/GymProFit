package es.pmdm.gymprofit.network;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.POST;
import retrofit2.http.Query;

// ============================================================
// DeviceTokenApi — interfaz Retrofit del registro de tokens FCM (push).
// Registra/borra el token del dispositivo en el backend; el usuario propietario
// lo determina el servidor a partir del JWT (inyectado por el interceptor).
// ============================================================
public interface DeviceTokenApi {

    // Registra (o actualiza) el token FCM del dispositivo. Body: {token, plataforma?}.
    @POST("notificaciones/token")
    Call<Void> registrar(@Body Map<String, Object> body);

    // Da de baja el token FCM (logout).
    @DELETE("notificaciones/token")
    Call<Void> eliminar(@Query("token") String token);
}
