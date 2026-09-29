package es.pmdm.gymprofit.network;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.programa.Programa;
import es.pmdm.gymprofit.model.programa.ProgramaDetalle;
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.programa.Recomendado;
import es.pmdm.gymprofit.model.programa.VistaPrevia;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

// ============================================================
// ProgramaApi — programas del catálogo y el que sigue el usuario (GP-074, lote 1.2.1).
// Todo lo que depende del nivel, el objetivo o el tiempo lo calcula la API: la app no
// aplica ninguna regla de los programas.
// ============================================================
public interface ProgramaApi {

    // Programas del catálogo; con equipamiento, solo los de ese.
    @GET("programas")
    Call<List<Programa>> listar(@Query("equipamiento") String equipamiento);

    // El recomendado para el perfil, dónde entrena y cuántos días (2 a 6).
    @GET("programas/recomendado")
    Call<Recomendado> recomendado(@Query("equipamiento") String equipamiento, @Query("dias") int dias);

    // La semana y cada rutina con sus ejercicios.
    @GET("programas/{codigo}")
    Call<ProgramaDetalle> detalle(@Path("codigo") String codigo);

    // Cómo quedaría al seguirlo con esos minutos; no guarda nada.
    @GET("programas/{codigo}/vista-previa")
    Call<VistaPrevia> vistaPrevia(@Path("codigo") String codigo, @Query("minutos") int minutos);

    // Seguirlo: deja el que siguiera y crea sus rutinas. body: {minutos}. La respuesta no
    // se usa: al volver, Entrenar pide el programa que sigue.
    @POST("programas/{codigo}/seguir")
    Call<Void> seguir(@Path("codigo") String codigo, @Body Map<String, Object> body);

    // El que sigue; 204 (cuerpo null) si no sigue ninguno.
    @GET("programas/seguido")
    Call<ProgramaQueSigue> seguido();

    // Dejarlo: sus rutinas salen de Entrenar; sesiones y récords se quedan.
    @DELETE("programas/seguido")
    Call<Void> dejar();
}
