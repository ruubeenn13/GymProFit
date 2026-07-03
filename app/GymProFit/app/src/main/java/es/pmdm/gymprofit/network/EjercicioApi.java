package es.pmdm.gymprofit.network;

import java.util.List;

import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import retrofit2.Call;
import retrofit2.http.GET;

// ============================================================
// EjercicioApi — interfaz Retrofit tipada del dominio "ejercicios" (etapa 2).
// Solo las lecturas de usuario (no las de admin). Las respuestas se
// deserializan a POJOs (Ejercicio) vía Gson, sin UtilJSONParser.
// Paths relativos a BuildConfig.BASE_URL (.../api/).
// ============================================================
public interface EjercicioApi {

    // Solo los ejercicios activos del catálogo (los que ve el usuario).
    @GET("ejercicios/activos")
    Call<List<Ejercicio>> getActivos();
}
