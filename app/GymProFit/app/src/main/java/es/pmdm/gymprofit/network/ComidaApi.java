package es.pmdm.gymprofit.network;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.comida.Comida;
import es.pmdm.gymprofit.model.comida.ResumenDiarioNutricion;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

// ============================================================
// ComidaApi — interfaz Retrofit tipada del dominio "comidas" (etapa 2).
// Cubre la lectura de las comidas de un usuario en una fecha concreta y el alta/
// borrado de comidas usados por las pantallas de nutrición. El listado se
// deserializa a POJOs (Comida) vía Gson, sin UtilJSONParser. Los cuerpos de
// escritura viajan como Map<String,Object> para conservar la semántica de
// creación. El POST devuelve la comida creada con su id generado. Paths relativos
// a BuildConfig.BASE_URL (.../api/); la {fecha} del path va en formato yyyy-MM-dd.
// ============================================================
public interface ComidaApi {

    // Comidas registradas por un usuario en una fecha dada (yyyy-MM-dd).
    @GET("comidas/usuario/{usuarioId}/fecha/{fecha}")
    Call<List<Comida>> getDeUsuarioFecha(@Path("usuarioId") int usuarioId, @Path("fecha") String fecha);

    // Resumen nutricional diario (kcal+macros por día) en un rango [inicio, fin] (yyyy-MM-dd).
    // Un elemento por día con registros, orden ascendente; lista vacía si no hay datos.
    @GET("comidas/usuario/{usuarioId}/resumen")
    Call<List<ResumenDiarioNutricion>> getResumen(@Path("usuarioId") int usuarioId,
                                                  @Query("inicio") String inicio,
                                                  @Query("fin") String fin);

    // Crea una comida nueva. body: usuarioId, tipoComida, fecha (ISO). La respuesta
    // incluye el id generado (necesario para asociarle luego los alimentos).
    @POST("comidas")
    Call<Comida> crear(@Body Map<String, Object> body);

    // Elimina una comida registrada por su id.
    @DELETE("comidas/{id}")
    Call<Void> eliminar(@Path("id") int id);
}
