package es.pmdm.gymprofit.network;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.PageDTO;
import es.pmdm.gymprofit.model.alimento.Alimento;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

// ============================================================
// AlimentoApi — interfaz Retrofit tipada del dominio "alimentos" (etapa 2).
// Cubre el catálogo nutricional (activos, búsqueda por nombre, alimentos propios
// del usuario, categorías) y el CRUD de alimentos personalizados usado por las
// pantallas de nutrición. Los listados se deserializan a POJOs (Alimento) vía
// Gson, sin UtilJSONParser; las categorías llegan como lista de String. Los
// cuerpos de escritura viajan como Map<String,Object> (BigDecimal en decimales)
// para conservar la semántica de creación/edición parcial. El POST devuelve el
// alimento creado con su id generado. Paths relativos a BuildConfig.BASE_URL (.../api/).
// ============================================================
public interface AlimentoApi {

    // Catálogo de alimentos activos (globales + propios del usuario).
    @GET("alimentos/activos")
    Call<List<Alimento>> getActivos();

    // Búsqueda paginada del catálogo (globales + propios, solo activos).
    // q/categoria opcionales (null = sin filtro). Devuelve 200 con content=[]
    // si no hay resultados (nunca 404) — apto para scroll infinito.
    @GET("alimentos/buscar")
    Call<PageDTO<Alimento>> buscar(@Query("q") String q,
                                   @Query("categoria") String categoria,
                                   @Query("page") int page,
                                   @Query("size") int size);

    // Busca alimentos cuyo nombre contenga el texto indicado.
    @GET("alimentos/nombre/{nombre}")
    Call<List<Alimento>> buscarPorNombre(@Path("nombre") String nombre);

    // Alimentos personalizados creados por un usuario concreto.
    @GET("alimentos/usuario/{usuarioId}")
    Call<List<Alimento>> getDeUsuario(@Path("usuarioId") int usuarioId);

    // Categorías de alimentos disponibles (lista de nombres).
    @GET("alimentos/categorias")
    Call<List<String>> getCategorias();

    // Crea un alimento nuevo. body: nombre, categoria, calorias, proteinas,
    // carbohidratos, grasas, usuarioId. La respuesta incluye el id generado.
    @POST("alimentos")
    Call<Alimento> crear(@Body Map<String, Object> body);

    // Importa (o recupera) un producto de Open Food Facts al catálogo local.
    // body: {"barcode": "..."} — se llama al seleccionar un resultado externo
    // (id 0) para materializarlo con id local antes de añadirlo a una comida.
    @POST("alimentos/importar")
    Call<Alimento> importar(@Body Map<String, Object> body);

    // Reactiva un alimento previamente desactivado.
    @PUT("alimentos/{id}/activar")
    Call<Void> activar(@Path("id") int id);

    // Actualiza parcialmente un alimento (nombre, calorías, macros...).
    // La respuesta se ignora en las pantallas actuales.
    @PATCH("alimentos/{id}")
    Call<Void> patch(@Path("id") int id, @Body Map<String, Object> body);

    // Elimina (borrado lógico) un alimento por su id.
    @DELETE("alimentos/{id}")
    Call<Void> eliminar(@Path("id") int id);
}
