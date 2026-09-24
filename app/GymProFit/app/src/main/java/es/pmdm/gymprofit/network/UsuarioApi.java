package es.pmdm.gymprofit.network;

import java.util.Map;

import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.model.usuario.UsuarioEstadisticas;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.PUT;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

// ============================================================
// UsuarioApi — interfaz Retrofit tipada del dominio "usuarios" (etapa 2).
// Cubre el perfil de usuario: búsqueda por username/id, estadísticas de
// entrenamiento agregadas, edición parcial (PATCH) y subida de la foto de perfil.
// Las respuestas de lectura se deserializan a POJO (Usuario/UsuarioEstadisticas)
// vía Gson; las de escritura (PATCH/foto) devuelven un cuerpo que las pantallas
// ignoran → Call<Void>. Retrofit codifica los segmentos {..} de la URL,
// sustituyendo al encode() manual. Paths relativos a BuildConfig.BASE_URL (.../api/).
//
// NOTA: el PUT completo (usuarios) de la capa antigua (API.actualizarUsuario) no
// se migra porque ninguna pantalla lo usa; solo se exponen los verbos en uso.
// ============================================================
public interface UsuarioApi {

    // Busca un usuario por su username. Devuelve el perfil completo (id, rol, nivel...).
    @GET("usuarios/username/{username}")
    Call<Usuario> getPorUsername(@Path("username") String username);

    // Obtiene el perfil de un usuario por su id.
    @GET("usuarios/{id}")
    Call<Usuario> getPorId(@Path("id") int id);

    // Obtiene las estadísticas de entrenamiento agregadas de un usuario.
    @GET("usuarios/{id}/estadisticas")
    Call<UsuarioEstadisticas> getEstadisticas(@Path("id") int id);

    // Actualiza parcialmente los datos de un usuario (onboarding y edición de perfil).
    // body: Map con los campos a cambiar (null en un campo = borrarlo, vía serializeNulls).
    @PATCH("usuarios/{id}")
    Call<Void> patch(@Path("id") int id, @Body Map<String, Object> body);

    // Sube la foto de perfil del usuario como multipart/form-data (campo "foto").
    // La Part se construye igual que en UtilREST.uploadMultipart (bytes del Uri, image/jpeg).
    @Multipart
    @POST("usuarios/{id}/foto")
    Call<Void> subirFoto(@Path("id") int id, @Part MultipartBody.Part foto);

    // Descarga la foto de perfil como bytes crudos (image/jpeg). Sin @Streaming:
    // Retrofit bufferiza el cuerpo en memoria, así que decodificar el Bitmap en el
    // callback (hilo principal) no bloquea con I/O de red. Sustituye la última ruta
    // con AsyncTask + HttpURLConnection que quedaba fuera de Retrofit.
    @GET("usuarios/{id}/foto")
    Call<ResponseBody> descargarFoto(@Path("id") int id);

    /**
     * Cambia el correo del usuario autenticado (GP-083).
     *
     * <p>Sin id en la ruta: el usuario sale del token. El cuerpo lleva {@code email} y
     * {@code password}, la contraseña actual. La API responde 403 si la contraseña no
     * es la de la cuenta, 409 si otra cuenta ya usa el correo y 400 si no tiene formato.
     */
    @PUT("usuarios/me/email")
    Call<Void> cambiarEmail(@Body Map<String, Object> body);

    /**
     * Borra definitivamente la cuenta del usuario autenticado (GP-008).
     *
     * <p>No lleva id en la ruta a propósito: el usuario que se borra sale del token
     * (DEC-013), así que ni el cuerpo ni la URL pueden decir a quién se borra. El
     * cuerpo solo lleva {@code password}, la contraseña actual, porque el borrado es
     * irreversible y un token robado no debe bastar para vaciar una cuenta.
     *
     * <p>Se declara con {@code @HTTP} y no con {@code @DELETE} porque el DELETE de
     * Retrofit no admite cuerpo; {@code hasBody = true} es lo que lo permite.
     *
     * <p>La API responde 200 con {@code {"mensaje": ...}}, que aquí se descarta
     * ({@code Call<Void>}), y <b>403</b> si la contraseña no es la de la cuenta.
     */
    @HTTP(method = "DELETE", path = "usuarios/me", hasBody = true)
    Call<Void> eliminarCuentaPropia(@Body Map<String, Object> body);
}
