package es.pmdm.gymprofit.network;

import java.util.Map;

import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.model.usuario.UsuarioEstadisticas;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
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
}
