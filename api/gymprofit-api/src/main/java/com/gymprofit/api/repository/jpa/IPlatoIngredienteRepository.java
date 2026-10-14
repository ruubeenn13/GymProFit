package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.PlatoIngrediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

// ============================================================
// IPlatoIngredienteRepository — los ingredientes de los platos (lote 1.6.6)
// ============================================================
@Repository
public interface IPlatoIngredienteRepository extends JpaRepository<PlatoIngrediente, Integer> {

    // Los de un plato, en su orden, con su alimento y su ración en la misma consulta.
    @Query("SELECT i FROM PlatoIngrediente i JOIN FETCH i.alimento LEFT JOIN FETCH i.racion "
            + "WHERE i.plato.id = :plato ORDER BY i.orden")
    List<PlatoIngrediente> dePlato(@Param("plato") Integer plato);

    // Cambiar un plato sustituye sus ingredientes: se borran en una sentencia antes de
    // poner los nuevos.
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("DELETE FROM PlatoIngrediente i WHERE i.plato.id = :plato")
    int borrarDePlato(@Param("plato") Integer plato);

    // ¿Está este alimento en algún plato? (borrarlo del todo daría un plato roto).
    boolean existsByAlimentoId(Integer alimentoId);

    // Cuántos ingredientes tiene un plato (para «Tu plato · 6 ingredientes»).
    long countByPlatoId(Integer platoId);
}
