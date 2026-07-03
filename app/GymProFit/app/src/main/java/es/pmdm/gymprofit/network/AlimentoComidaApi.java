package es.pmdm.gymprofit.network;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.comida.AlimentoComida;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

// ============================================================
// AlimentoComidaApi — interfaz Retrofit tipada de la relación "alimentos-comida"
// (etapa 2). Cubre el listado de alimentos añadidos a una comida (con sus totales
// nutricionales calculados) y el CRUD de esa relación usado por las pantallas de
// nutrición. El listado se deserializa a POJOs (AlimentoComida) vía Gson, sin
// UtilJSONParser. Los cuerpos de escritura viajan como Map<String,Object>
// (BigDecimal en decimales) para conservar la semántica de alta/edición parcial.
// Paths relativos a BuildConfig.BASE_URL (.../api/).
// ============================================================
public interface AlimentoComidaApi {

    // Alimentos (con cantidad y totales) asociados a una comida concreta.
    @GET("alimentos-comida/comida/{comidaId}")
    Call<List<AlimentoComida>> getDeComida(@Path("comidaId") int comidaId);

    // Añade un alimento a una comida. body: comidaId, alimentoId, cantidadGramos.
    // La respuesta se ignora en las pantallas actuales.
    @POST("alimentos-comida")
    Call<Void> anadir(@Body Map<String, Object> body);

    // Actualiza parcialmente la relación (p. ej. cantidadGramos). Respuesta ignorada.
    @PATCH("alimentos-comida/{id}")
    Call<Void> patch(@Path("id") int id, @Body Map<String, Object> body);

    // Elimina un alimento de una comida (quita el registro de la relación).
    @DELETE("alimentos-comida/{id}")
    Call<Void> eliminar(@Path("id") int id);
}
