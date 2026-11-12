package es.pmdm.gymprofit.network;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

// ============================================================
// EmbudoApi — el aviso de que se ha llegado a un paso del alta (GP-019, DEC-064).
//
// Va por ApiClient.anonimo: sin token aunque haya sesión y sin Accept-Language. Un 429
// o un fallo de red se reintentan una vez, no más (ReintentoEmbudo, GP-264). El cuerpo es {"paso": "<NOMBRE>"} y nada más (EmbudoAlta.cuerpo).
// ============================================================
public interface EmbudoApi {

    @POST("alta/pasos")
    Call<Void> avisar(@Body Map<String, String> cuerpo);
}
