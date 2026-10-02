package es.pmdm.gymprofit.network;

import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.PageDTO;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.alimento.Favoritos;
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

    // El alimento de un código de barras (GP-160, lote 1.6.1): el tuyo si lo tienes; si
    // no, el del catálogo, materializándolo. 404 si no existe; 503 con Retry-After si
    // hacía falta leerlo de Open Food Facts y no queda cupo.
    @GET("alimentos/codigo/{codigo}")
    Call<Alimento> porCodigo(@Path("codigo") String codigo);

    // Un alimento por id (la ficha al editar una línea de una comida).
    @GET("alimentos/{id}")
    Call<Alimento> porId(@Path("id") int id);

    // Reporta un problema de un alimento del catálogo (lote 1.6.1), sin guardar quién.
    // body: alimentoId o barcode, y motivo (VALORES, NOMBRE, RACION, REPETIDO, OTRO). 204.
    @POST("alimentos/avisos")
    Call<Void> reportar(@Body Map<String, Object> body);

    // --- Favoritos (lote 1.6.3) -------------------------------------------------

    // Los favoritos de la cuenta, por uso, y la propuesta de uno nuevo (o null).
    @GET("favoritos")
    Call<Favoritos> favoritos();

    // Marca un alimento como favorito. Repetible. Devuelve el alimento.
    @PUT("favoritos/{id}")
    Call<Alimento> marcarFavorito(@Path("id") int id);

    // Marca por su código un producto que aún no está en el catálogo: la API lo
    // materializa y devuelve el alimento con su id.
    @PUT("favoritos/codigo/{codigo}")
    Call<Alimento> marcarFavoritoPorCodigo(@Path("codigo") String codigo);

    // Quita un favorito. Repetible. 204.
    @DELETE("favoritos/{id}")
    Call<Void> quitarFavorito(@Path("id") int id);

    // Rechaza la propuesta de ese alimento: no se vuelve a proponer. 204.
    @PUT("favoritos/rechazados/{id}")
    Call<Void> rechazarPropuesta(@Path("id") int id);

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
