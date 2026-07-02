package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Alimento;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
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
public interface IAlimentoRepository extends CrudRepository<Alimento, Integer> {

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
}
