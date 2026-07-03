package es.pmdm.gymprofit.network;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

// ============================================================
// RutinaApi — interfaz Retrofit tipada del dominio "rutinas" y "rutinas-ejercicios"
// (etapa 2). Cubre las lecturas y el CRUD usados por las pantallas de rutinas de
// usuario. Las respuestas de listado/detalle se deserializan a POJOs (Rutina /
// RutinaEjercicio) vía Gson, sin UtilJSONParser. Los cuerpos de escritura viajan
// como Map<String,Object> para mantener la semántica de creación/edición parcial.
// El POST de rutinas devuelve la rutina creada con su id generado. Paths relativos
// a BuildConfig.BASE_URL (.../api/).
// ============================================================
public interface RutinaApi {

    // ── RUTINAS ───────────────────────────────────────────────

    // Catálogo de rutinas predefinidas del sistema.
    @GET("rutinas/predefinidas")
    Call<List<Rutina>> getPredefinidas();

    // Rutinas activas creadas o asignadas a un usuario concreto.
    @GET("rutinas/usuario/{usuarioId}/activas")
    Call<List<Rutina>> getDeUsuarioActivas(@Path("usuarioId") int usuarioId);

    // Crea una rutina nueva. body: nombre, descripcion, nivel, duracionMinutos,
    // usuarioId, esPredefinida... La respuesta incluye el id generado.
    @POST("rutinas")
    Call<Rutina> crear(@Body Map<String, Object> body);

    // Actualiza parcialmente una rutina existente (nombre, descripción, nivel, etc.).
    // La respuesta (rutina actualizada) se ignora en las pantallas actuales.
    @PATCH("rutinas/{id}")
    Call<Void> patch(@Path("id") int id, @Body Map<String, Object> body);

    // Elimina (borrado lógico) una rutina por su id.
    @DELETE("rutinas/{id}")
    Call<Void> eliminar(@Path("id") int id);

    // Reactiva una rutina previamente desactivada.
    @PUT("rutinas/{id}/activar")
    Call<Void> activar(@Path("id") int id);

    // ── RUTINAS-EJERCICIOS ────────────────────────────────────

    // Relaciones ejercicio↔rutina (con series/repeticiones/orden) de una rutina.
    @GET("rutinas-ejercicios/rutina/{rutinaId}")
    Call<List<RutinaEjercicio>> getEjerciciosDeRutina(@Path("rutinaId") int rutinaId);

    // Añade un ejercicio a una rutina. body: rutinaId, ejercicioId, series,
    // repeticiones, orden. La respuesta se ignora.
    @POST("rutinas-ejercicios")
    Call<Void> addEjercicio(@Body Map<String, Object> body);

    // Elimina un ejercicio concreto de una rutina.
    @DELETE("rutinas-ejercicios/rutina/{rutinaId}/ejercicio/{ejercicioId}")
    Call<Void> eliminarEjercicio(@Path("rutinaId") int rutinaId, @Path("ejercicioId") int ejercicioId);
}
