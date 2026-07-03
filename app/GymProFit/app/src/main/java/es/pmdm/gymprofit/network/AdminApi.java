package es.pmdm.gymprofit.network;

import java.util.List;

import es.pmdm.gymprofit.model.admin.EstadisticasGlobales;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.usuario.Usuario;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Query;

// ============================================================
// AdminApi — interfaz Retrofit tipada de los endpoints admin/* (etapa 2, Fase 6).
// Cubre el panel de administración: listado/filtrado de usuarios, toggle de estado
// y cambio de rol, estadísticas globales y las búsquedas jOOQ (rutinas, ejercicios,
// alimentos) que incluyen elementos inactivos. Los filtros viajan como @Query; los
// nulos se omiten automáticamente (equivale a los "append condicional" del API viejo).
//
// NOTA de diseño: GET admin/usuarios devuelve en esta API una lista plana
// (List<AdminUsuarioDTO>), NO un objeto Page de Spring — la paginación se resuelve
// en el servidor por page/size. Por eso se tipa Call<List<Usuario>> y no se necesita
// un PagedResponse. Las respuestas de las búsquedas jOOQ se deserializan a los mismos
// POJOs de dominio (el campo "activo" llega como 0/1 y lo resuelve BooleanNumericAdapter).
// Paths relativos a BuildConfig.BASE_URL (.../api/).
// ============================================================
public interface AdminApi {

    // ── USUARIOS ──────────────────────────────────────────────

    // Usuarios paginados/filtrados por activo/rol/username. Devuelve la página como lista plana.
    @GET("admin/usuarios")
    Call<List<Usuario>> getUsuarios(
            @Query("activo") Boolean activo,
            @Query("rol") String rol,
            @Query("username") String username,
            @Query("page") int page,
            @Query("size") int size);

    // Activa/desactiva la cuenta de un usuario. La respuesta (mensaje) se ignora.
    @PATCH("admin/usuarios/{id}/toggle-activo")
    Call<Void> toggleActivoUsuario(@Path("id") int id);

    // Cambia el rol (ROLE_USER / ROLE_ADMIN) del usuario. La respuesta (mensaje) se ignora.
    @PATCH("admin/usuarios/{id}/rol")
    Call<Void> cambiarRol(@Path("id") int id, @Query("nuevoRol") String nuevoRol);

    // ── ESTADÍSTICAS ──────────────────────────────────────────

    // Métricas globales agregadas del dashboard de administración.
    @GET("admin/estadisticas-globales")
    Call<EstadisticasGlobales> getEstadisticas();

    // ── BÚSQUEDAS jOOQ (incluyen inactivos) ───────────────────

    // Rutinas predefinidas filtradas por nombre/nivel/categoría/activa.
    @GET("admin/rutinas/predefinidas/busqueda")
    Call<List<Rutina>> buscarRutinasPredefinidas(
            @Query("nombre") String nombre,
            @Query("nivel") String nivel,
            @Query("categoria") String categoria,
            @Query("activa") Boolean activa);

    // Ejercicios filtrados por nombre/grupo muscular/dificultad/activo.
    @GET("admin/ejercicios/busqueda")
    Call<List<Ejercicio>> buscarEjercicios(
            @Query("nombre") String nombre,
            @Query("grupoMuscular") String grupoMuscular,
            @Query("dificultad") String dificultad,
            @Query("activo") Boolean activo);

    // Alimentos filtrados por nombre/categoría/activo.
    @GET("admin/alimentos/busqueda")
    Call<List<Alimento>> buscarAlimentos(
            @Query("nombre") String nombre,
            @Query("categoria") String categoria,
            @Query("activo") Boolean activo);
}
