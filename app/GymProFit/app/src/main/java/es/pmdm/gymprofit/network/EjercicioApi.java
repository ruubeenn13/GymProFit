package es.pmdm.gymprofit.network;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.PUT;
import retrofit2.http.Path;

// ============================================================
// EjercicioApi — interfaz Retrofit tipada del dominio "ejercicios" (etapa 2).
// Cubre la lectura de usuario (catálogo activo) y el CRUD de administración
// (editar/activar/desactivar) que reutiliza los endpoints de dominio ejercicios/*.
// Las respuestas se deserializan a POJOs (Ejercicio) vía Gson, sin UtilJSONParser;
// los cuerpos de escritura viajan como Map<String,Object> (semántica de edición
// parcial). Paths relativos a BuildConfig.BASE_URL (.../api/).
// ============================================================
public interface EjercicioApi {

    // Solo los ejercicios activos del catálogo (los que ve el usuario).
    @GET("ejercicios/activos")
    Call<List<Ejercicio>> getActivos();

    // Edita parcialmente un ejercicio (panel admin). body: nombre, descripcion,
    // grupoMuscular, dificultad, caloriasQuemadas, equipoNecesario, instrucciones...
    @PATCH("ejercicios/{id}")
    Call<Void> patch(@Path("id") int id, @Body Map<String, Object> body);

    // Reactiva un ejercicio previamente desactivado (panel admin).
    @PUT("ejercicios/{id}/activar")
    Call<Void> activar(@Path("id") int id);

    // Elimina (borrado lógico) un ejercicio por su id (panel admin).
    @DELETE("ejercicios/{id}")
    Call<Void> eliminar(@Path("id") int id);
}
