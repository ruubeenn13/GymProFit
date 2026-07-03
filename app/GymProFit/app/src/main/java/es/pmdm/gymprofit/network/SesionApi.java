package es.pmdm.gymprofit.network;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

// ============================================================
// SesionApi — interfaz Retrofit tipada del dominio "sesiones de entrenamiento" (etapa 2).
// Incluye también el POST de "ejercicios-realizados" por ir ligado al registro de
// una sesión. Las respuestas se deserializan a POJOs (SesionEntrenamiento) vía Gson,
// sin UtilJSONParser. Los cuerpos de escritura van como Map<String,Object> para
// mantener la semántica de creación parcial (solo se envían los campos presentes).
// El POST de sesiones devuelve la sesión creada con su id y, si corresponde, la
// lista "nuevosLogros" desbloqueados (campo extra del POJO SesionEntrenamiento).
// Paths relativos a BuildConfig.BASE_URL (.../api/).
// ============================================================
public interface SesionApi {

    // Historial de sesiones de entrenamiento de un usuario.
    @GET("sesiones/usuario/{usuarioId}")
    Call<List<SesionEntrenamiento>> getDeUsuario(@Path("usuarioId") int usuarioId);

    // Detalle de una sesión concreta por su id.
    @GET("sesiones/{id}")
    Call<SesionEntrenamiento> getPorId(@Path("id") int id);

    // Crea una sesión nueva. body: usuarioId, rutinaId, fechaInicio, duración, etc.
    // La respuesta incluye el id generado y la lista de logros nuevos desbloqueados.
    @POST("sesiones")
    Call<SesionEntrenamiento> crear(@Body Map<String, Object> body);

    // Elimina una sesión de entrenamiento por su id (sin cuerpo de respuesta).
    @DELETE("sesiones/{id}")
    Call<Void> eliminar(@Path("id") int id);

    // Registra un ejercicio realizado dentro de una sesión (series, repeticiones,
    // peso usado). body: sesionId + datos del ejercicio; la respuesta se ignora.
    @POST("ejercicios-realizados")
    Call<Void> crearEjercicioRealizado(@Body Map<String, Object> body);
}
