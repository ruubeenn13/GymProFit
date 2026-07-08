package es.pmdm.gymprofit.network;

import java.util.List;

import es.pmdm.gymprofit.model.progreso.ProgresoEjercicio;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

// ============================================================
// ProgresoEjercicioApi — interfaz Retrofit tipada del dominio "progreso-ejercicios".
// Lee el histórico de progreso de un usuario para un ejercicio concreto (ordenado
// por fecha), que alimenta la gráfica de progresión del detalle de ejercicio.
// Paths relativos a BuildConfig.BASE_URL (.../api/).
// ============================================================
public interface ProgresoEjercicioApi {

    // Histórico de progreso (ordenado por fecha) de un usuario para un ejercicio.
    @GET("progreso-ejercicios/usuario/{usuarioId}/ejercicio/{ejercicioId}/historial")
    Call<List<ProgresoEjercicio>> getHistorial(@Path("usuarioId") int usuarioId,
                                               @Path("ejercicioId") int ejercicioId);
}
