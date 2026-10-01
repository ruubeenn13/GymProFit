package es.pmdm.gymprofit.network;

import retrofit2.Call;
import retrofit2.http.GET;

// ============================================================
// SaludApi — la llamada más ligera que hay: GET /actuator/health (GP-103, lote 1.5.1).
//
// Render gratis duerme la API cuando nadie la usa y tarda en despertar. La bienvenida la
// llama al abrirse, sin esperar respuesta, para que cuando «Tu plan» pida el programa,
// un minuto después, la API ya esté en pie. Es pública: no lleva token ni lee nada.
// ============================================================
public interface SaludApi {

    @GET("actuator/health")
    Call<Void> despertar();
}
