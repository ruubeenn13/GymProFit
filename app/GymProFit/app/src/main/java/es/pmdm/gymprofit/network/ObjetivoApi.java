package es.pmdm.gymprofit.network;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.objetivo.ObjetivoPersonal;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

// ============================================================
// ObjetivoApi — interfaz Retrofit tipada del dominio "objetivos personales" (etapa 2).
// Las respuestas se deserializan a POJOs (ObjetivoPersonal) vía Gson, sin
// UtilJSONParser. Los cuerpos de escritura van como Map<String,Object> para
// mantener la semántica PARCIAL (solo se envían los campos presentes; un valor
// null borra el campo en PATCH). Paths relativos a BuildConfig.BASE_URL (.../api/).
// ============================================================
public interface ObjetivoApi {

    // Objetivos personales de un usuario.
    @GET("objetivos/usuario/{usuarioId}")
    Call<List<ObjetivoPersonal>> getDeUsuario(@Path("usuarioId") int usuarioId);

    // Crea un objetivo nuevo. body: usuarioId + tipo, valorObjetivo, fechas, etc.
    @POST("objetivos")
    Call<ObjetivoPersonal> crear(@Body Map<String, Object> body);

    // Actualiza parcialmente un objetivo (p. ej. progreso o estado completado).
    @PATCH("objetivos/{id}")
    Call<ObjetivoPersonal> patch(@Path("id") int id, @Body Map<String, Object> body);

    // Elimina un objetivo personal por su id (sin cuerpo de respuesta).
    @DELETE("objetivos/{id}")
    Call<Void> eliminar(@Path("id") int id);
}
