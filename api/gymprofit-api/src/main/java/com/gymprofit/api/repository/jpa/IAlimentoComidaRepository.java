package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.AlimentoComida;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ============================================================
// IAlimentoComidaRepository — acceso JPA a la tabla intermedia alimento-comida
// Gestiona la relación N:M entre alimentos y comidas (cantidad de cada
// alimento dentro de una comida registrada). No expuesto vía REST directo.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IAlimentoComidaRepository extends JpaRepository<AlimentoComida, Integer> {

    // Devuelve todos los alimentos asociados a una comida.
    List<AlimentoComida> findByComidaId(Integer comidaId);

    // Devuelve todas las comidas en las que aparece un alimento.
    List<AlimentoComida> findByAlimentoId(Integer alimentoId);

    // Busca la asociación concreta entre una comida y un alimento.
    Optional<AlimentoComida> findByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId);

    // Elimina todos los alimentos asociados a una comida (p. ej. al borrar la comida).
    void deleteByComidaId(Integer comidaId);

    // Elimina la asociación concreta entre una comida y un alimento.
    void deleteByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId);

    // Comprueba si un alimento ya está asociado a una comida.
    boolean existsByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId);

    // Cuenta cuántos alimentos tiene registrados una comida.
    Long countByComidaId(Integer comidaId);

    // Cuenta en cuántas comidas aparece un alimento.
    Long countByAlimentoId(Integer alimentoId);
}
