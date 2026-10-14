package es.pmdm.gymprofit.network;

import java.util.Map;

import es.pmdm.gymprofit.model.plato.LoMio;
import es.pmdm.gymprofit.model.plato.Plato;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

// ============================================================
// PlatoApi — los platos propios y la pestaña «Mis platos» (lote 1.6.6)
// Cuerpo de crear y cambiar: nombre, raciones, pesoGramos (opcional) e ingredientes, cada
// uno con alimentoId (o barcode) y su cantidad como al añadir: cantidadGramos, o racionId
// (o racionIndice) y raciones. El dueño es el del token.
// ============================================================
public interface PlatoApi {

    @POST("platos")
    Call<Plato> crear(@Body Map<String, Object> body);

    @GET("platos/{id}")
    Call<Plato> porId(@Path("id") int id);

    @PUT("platos/{id}")
    Call<Plato> cambiar(@Path("id") int id, @Body Map<String, Object> body);

    // Lo desactiva: lo ya apuntado no cambia. Repetible.
    @DELETE("platos/{id}")
    Call<Void> borrar(@Path("id") int id);

    // Los platos de la cuenta y los alimentos que ha creado.
    @GET("alimentos/mios")
    Call<LoMio> loMio();
}
