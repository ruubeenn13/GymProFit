package es.pmdm.gymprofit.network;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.ejercicio.EjercicioMasHecho;
import es.pmdm.gymprofit.model.ejercicio.EjercicioRealizado;
import es.pmdm.gymprofit.model.envivo.UltimaVez;
import es.pmdm.gymprofit.model.sesion.ResumenSesion;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.model.sesion.VolumenMuscular;
import es.pmdm.gymprofit.model.sesion.VolumenSemanal;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

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

    /**
     * Guarda la sesión ENTERA —sesión, ejercicios y series— en una sola llamada
     * atómica (GP-006). El cuerpo lleva `claveIdempotencia`, obligatoria: si la
     * misma clave llega dos veces, el servidor devuelve la sesión que ya creó en
     * vez de crear otra, que es lo que permite reintentar sin duplicar entrenos.
     *
     * <p>Convive con {@link #crear(Map)}: el camino viejo sigue existiendo para las
     * builds repartidas fuera de Play, que no conocen esta ruta.
     */
    @POST("sesiones/completa")
    Call<SesionEntrenamiento> guardarCompleta(@Body Map<String, Object> body);

    // Elimina una sesión de entrenamiento por su id (sin cuerpo de respuesta).
    @DELETE("sesiones/{id}")
    Call<Void> eliminar(@Path("id") int id);

    // Registra un ejercicio realizado dentro de una sesión (series, repeticiones,
    // peso usado). body: sesionId + datos del ejercicio; la respuesta se ignora.
    @POST("ejercicios-realizados")
    Call<Void> crearEjercicioRealizado(@Body Map<String, Object> body);

    /**
     * Los ejercicios que más hace el usuario del token (GP-216, Biblioteca): el id y en cuántas
     * sesiones terminadas sale, de más a menos. Límite de 1 a 20.
     */
    @GET("ejercicios-realizados/mas-hechos")
    Call<List<EjercicioMasHecho>> masHechos(@Query("limite") int limite);

    /**
     * Las ejecuciones del usuario del token en un ejercicio del catálogo, con sus series
     * (GP-216, ficha del ejercicio: «Tus últimas veces»). Lista vacía si nunca lo ha hecho.
     */
    @GET("ejercicios-realizados/ejercicio/{ejercicioId}")
    Call<List<EjercicioRealizado>> ejecucionesDe(@Path("ejercicioId") int ejercicioId);

    // Series por músculo del usuario en los últimos `dias` días. Alimenta la silueta
    // de Home: solo vienen los músculos tocados, el resto se pintan en gris.
    @GET("sesiones/usuario/{usuarioId}/volumen-muscular")
    Call<List<VolumenMuscular>> getVolumenMuscular(@Path("usuarioId") int usuarioId,
                                                   @Query("dias") int dias);

    // Series por músculo de las sesiones que empiezan desde «desde» (la semana natural
    // de Inicio, GP-105): el lunes a las 00:00 en la hora local, «yyyy-MM-ddTHH:mm:ss».
    @GET("sesiones/usuario/{usuarioId}/volumen-muscular")
    Call<List<VolumenMuscular>> getVolumenMuscularDesde(@Path("usuarioId") int usuarioId,
                                                        @Query("desde") String desde);

    // Kilos movidos por semana (GP-216, «Tu progreso» de Inicio): «semanas» seguidas desde el
    // lunes «desde» (00:00 en la hora local, «yyyy-MM-ddTHH:mm:ss»), una entrada por semana,
    // también las de cero, en orden. De 1 a 12 semanas.
    @GET("sesiones/usuario/{usuarioId}/volumen-semanal")
    Call<List<VolumenSemanal>> getVolumenSemanal(@Path("usuarioId") int usuarioId,
                                                 @Query("desde") String desde,
                                                 @Query("semanas") int semanas);

    // Kilos movidos en una sesión. Respuesta: {"volumenKg": 4250.00}. Es el número
    // grande del resumen tras entrenar.
    @GET("sesiones/{id}/volumen")
    Call<Map<String, Double>> getVolumenSesion(@Path("id") int id);

    /**
     * Última vez de cada ejercicio (GP-014): las series de la última sesión terminada del
     * usuario del token que tuvo cada uno. Hasta 30 ids, separados por comas.
     */
    @GET("sesiones/ultima-vez")
    Call<List<UltimaVez>> ultimaVez(@Query("ejercicios") String ejercicios);

    /**
     * El resumen del entreno (GP-210): kilos y series por ejercicio, lo de la última vez de
     * cada uno y la sesión anterior de la misma rutina. Sirve también sesiones del historial.
     */
    @GET("sesiones/{id}/resumen")
    Call<ResumenSesion> resumen(@Path("id") int id);
}
