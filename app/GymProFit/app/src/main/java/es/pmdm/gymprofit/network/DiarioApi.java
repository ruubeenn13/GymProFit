package es.pmdm.gymprofit.network;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.comida.AlimentoApuntado;
import es.pmdm.gymprofit.model.comida.DiaDiario;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

// ============================================================
// DiarioApi — el agua, «Hoy no» y los días del diario (lote 1.6.8)
// Todo de la cuenta del token. Las fechas, yyyy-MM-dd; las comidas, los tipos de
// ComidaQueToca. Escribir es repetible: el agua manda el total del día, no el vaso.
// ============================================================
public interface DiarioApi {

    // Cada día del rango, ambos incluidos y en orden (como mucho 31).
    @GET("diario")
    Call<List<DiaDiario>> dias(@Query("desde") String desde, @Query("hasta") String hasta);

    // Los tres alimentos más apuntados del rango, con las veces (como mucho 7 días).
    @GET("diario/mas-apuntados")
    Call<List<AlimentoApuntado>> masApuntados(@Query("desde") String desde, @Query("hasta") String hasta);

    // body: mililitros (0..20000), el total del día. Devuelve fecha y mililitros.
    @PUT("diario/{fecha}/agua")
    Call<Map<String, Object>> guardarAgua(@Path("fecha") String fecha, @Body Map<String, Object> body);

    @PUT("diario/{fecha}/hoy-no/{tipoComida}")
    Call<Void> marcarHoyNo(@Path("fecha") String fecha, @Path("tipoComida") String tipoComida);

    @DELETE("diario/{fecha}/hoy-no/{tipoComida}")
    Call<Void> quitarHoyNo(@Path("fecha") String fecha, @Path("tipoComida") String tipoComida);
}
