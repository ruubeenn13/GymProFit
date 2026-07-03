package es.pmdm.gymprofit.network;

import es.pmdm.gymprofit.model.usuario.Usuario;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

// ============================================================
// UsuarioApi — interfaz Retrofit tipada del dominio "usuarios" (etapa 2, Fase 7).
// Por ahora solo expone la búsqueda por username usada en el flujo de login/
// registro (obtener id y rol tras autenticarse); el resto del CRUD de usuario
// (perfil, patch, estadísticas, foto) permanece en la capa antigua (API.java)
// hasta su fase. La respuesta se deserializa a Usuario vía Gson. Retrofit
// codifica el segmento {username} en la URL, sustituyendo al encode() manual.
// Path relativo a BuildConfig.BASE_URL (.../api/).
// ============================================================
public interface UsuarioApi {

    // Busca un usuario por su username. Devuelve el perfil completo (id, rol, nivel...).
    @GET("usuarios/username/{username}")
    Call<Usuario> getPorUsername(@Path("username") String username);
}
