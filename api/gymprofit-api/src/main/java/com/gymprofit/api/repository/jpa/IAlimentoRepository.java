package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Alimento;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

// ============================================================
// IAlimentoRepository — repositorio JPA de la entidad Alimento
// Acceso a datos de los alimentos de la base de datos nutricional (catálogo global y personalizados por usuario).
// Oculto de Spring Data REST (exported = false); solo se usa desde los Services.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IAlimentoRepository extends JpaRepository<Alimento, Integer> {

    /**
     * Alimentos del CATÁLOGO para la web de administración (GP-085): solo los que no
     * tienen dueño. Los que crea cada usuario son su dieta y no salen nunca por aquí.
     * Un parámetro null no filtra.
     *
     * @param patron       "%texto%" en minúsculas: nombre, nombre en inglés o código de barras
     * @param sinIngles    true deja los que no tienen nombre en inglés
     * @param openFoodFacts true, los que tienen código de barras; false, los hechos a mano
     */
    @Query("""
            SELECT a FROM Alimento a
            WHERE a.usuario IS NULL
              AND (:patron IS NULL OR LOWER(a.nombre) LIKE :patron OR LOWER(a.nombreEn) LIKE :patron
                   OR LOWER(a.barcode) LIKE :patron)
              AND (:categoria IS NULL OR a.categoria = :categoria)
              AND (:sinIngles = false OR a.nombreEn IS NULL OR TRIM(a.nombreEn) = '')
              AND (:openFoodFacts IS NULL
                   OR (:openFoodFacts = true AND a.barcode IS NOT NULL)
                   OR (:openFoodFacts = false AND a.barcode IS NULL))
            """)
    Page<Alimento> buscarCatalogoAdmin(@Param("patron") String patron, @Param("categoria") String categoria,
                                       @Param("sinIngles") boolean sinIngles,
                                       @Param("openFoodFacts") Boolean openFoodFacts, Pageable pageable);

    // Tamaño del catálogo (sin los alimentos de los usuarios), GP-085.
    long countByUsuarioIsNull();

    // Catálogo sin nombre en inglés, GP-085.
    @Query("SELECT COUNT(a) FROM Alimento a WHERE a.usuario IS NULL AND (a.nombreEn IS NULL OR TRIM(a.nombreEn) = '')")
    long contarCatalogoSinIngles();

    // Categorías que usa el catálogo, para el filtro de la web (GP-085).
    @Query("SELECT DISTINCT a.categoria FROM Alimento a WHERE a.usuario IS NULL AND a.categoria IS NOT NULL ORDER BY a.categoria")
    List<String> categoriasDelCatalogo();

    // ¿Usa este código de barras otro alimento? (edición desde la web, GP-085)
    boolean existsByBarcodeAndIdNot(String barcode, Integer id);

    // Busca alimentos por categoría (ej. "Fruta", "Lácteo").
    List<Alimento> findByCategoria(String categoria);

    // Busca los alimentos marcados como activos (visibles/usables).
    List<Alimento> findByActivoTrue();

    // Busca alimentos cuyo nombre contenga el texto dado, sin distinguir mayúsculas/minúsculas.
    List<Alimento> findByNombreContainingIgnoreCase(String nombre);

    // Busca alimentos cuyas calorías estén dentro del rango [min, max].
    List<Alimento> findByCaloriasBetween(Integer min, Integer max);

    // Cuenta el número de alimentos activos.
    Long countByActivoTrue();

    // Cuenta el número de alimentos de una categoría concreta.
    Long countByCategoria(String categoria);

    // Busca los alimentos personalizados creados por un usuario concreto.
    List<Alimento> findByUsuarioId(Integer usuarioId);

    // El producto del CATÁLOGO con ese código de barras (upsert del import OFF). Solo
    // catálogo: un alimento propio con código es de su dueño y no sale por aquí.
    java.util.Optional<Alimento> findByBarcodeAndUsuarioIsNull(String barcode);

    // Alimentos propios del usuario (activos) cuyo nombre contiene el texto:
    // se antepone a los resultados externos en la búsqueda con query.
    @Query("SELECT a FROM Alimento a " +
            "WHERE a.activo = true AND a.usuario.id = :usuarioId " +
            "AND LOWER(a.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "ORDER BY a.nombre")
    List<Alimento> buscarPropios(@Param("q") String q, @Param("usuarioId") Integer usuarioId);

    // Búsqueda paginada del catálogo visible para un usuario: alimentos activos
    // globales (usuario null) o propios del usuario. Filtra opcionalmente por
    // texto en el nombre (ES o EN, sin mayúsculas) y por categoría exacta.
    @Query("SELECT a FROM Alimento a " +
            "WHERE a.activo = true " +
            "AND (a.usuario IS NULL OR a.usuario.id = :usuarioId) " +
            "AND (:q IS NULL OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(a.nombreEn) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "AND (:categoria IS NULL OR a.categoria = :categoria) " +
            "ORDER BY a.nombre")
    Page<Alimento> buscarCatalogo(@Param("q") String q,
                                  @Param("categoria") String categoria,
                                  @Param("usuarioId") Integer usuarioId,
                                  Pageable pageable);
}
