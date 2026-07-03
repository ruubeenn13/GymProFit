package es.pmdm.gymprofit.network;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.medicion.MedicionCorporal;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

// ============================================================
// MedicionApi — interfaz Retrofit tipada del dominio "mediciones corporales" (etapa 2).
// Las respuestas se deserializan a POJOs (MedicionCorporal) vía Gson, sin UtilJSONParser.
// Los cuerpos de escritura van como Map<String,Object> para mantener la semántica
// PARCIAL (solo se envían los campos presentes; un valor null borra el campo en PATCH).
// Paths relativos a BuildConfig.BASE_URL (.../api/).
// ============================================================
public interface MedicionApi {

    // Mediciones del usuario ordenadas de más reciente a más antigua.
    @GET("mediciones-corporales/usuario/{usuarioId}/ordenadas")
    Call<List<MedicionCorporal>> getOrdenadas(@Path("usuarioId") int usuarioId);

    // Crea una medición nueva. body: usuarioId + campos rellenados (peso, altura, ...).
    @POST("mediciones-corporales")
    Call<MedicionCorporal> crear(@Body Map<String, Object> body);

    // Actualiza parcialmente una medición existente. body: solo los campos a cambiar.
    @PATCH("mediciones-corporales/{id}")
    Call<MedicionCorporal> patch(@Path("id") int id, @Body Map<String, Object> body);
}
