package es.pmdm.gymprofit.network;

import java.util.List;

import es.pmdm.gymprofit.model.logro.LogroProgreso;
import retrofit2.Call;
import retrofit2.http.GET;

// ============================================================
// LogroApi — interfaz Retrofit tipada del dominio "logros".
// Paths relativos a BuildConfig.BASE_URL (.../api/).
//
// Desde GP-079 la app usa solo GET /logros/progreso. GET /logros y
// GET /logros/usuario/{id} siguen en la API, porque hay builds repartidas
// fuera de Play que los usan, pero aquí ya no se llaman.
// ============================================================
public interface LogroApi {

    // Catálogo con el estado y el progreso del usuario del token. Sin id: el
    // usuario sale del token, así que no hay id ajeno que pedir.
    @GET("logros/progreso")
    Call<List<LogroProgreso>> getProgreso();
}
