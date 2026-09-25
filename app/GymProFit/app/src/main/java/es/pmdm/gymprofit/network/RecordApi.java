package es.pmdm.gymprofit.network;

import java.util.List;

import es.pmdm.gymprofit.model.record.PuntoProgresion;
import es.pmdm.gymprofit.model.record.Records;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

// ============================================================
// RecordApi — interfaz Retrofit tipada de /records (GP-088).
//
// Ninguna ruta lleva id de usuario: el usuario sale del token (DEC-013). Sustituye a
// las rutas de /progreso-ejercicios, que la API mantiene solo para las builds ya
// repartidas. Paths relativos a BuildConfig.BASE_URL (.../api/).
// ============================================================
public interface RecordApi {

    // Récords vigentes y, si se pasa desde (AAAA-MM-DD), los batidos desde ese día.
    @GET("records")
    Call<Records> getRecords(@Query("desde") String desde);

    // Mejor serie de cada sesión en ese ejercicio, en orden cronológico.
    @GET("records/ejercicio/{ejercicioId}/progresion")
    Call<List<PuntoProgresion>> getProgresion(@Path("ejercicioId") int ejercicioId);
}
