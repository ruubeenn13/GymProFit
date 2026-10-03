package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.AvisoAlimento;
import com.gymprofit.api.enums.MotivoAviso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// ============================================================
// IAvisoAlimentoRepository — los avisos de alimentos (lote 1.6.1)
// ============================================================
@Repository
public interface IAvisoAlimentoRepository extends JpaRepository<AvisoAlimento, Integer> {

    // El aviso abierto de ese alimento (o código) y motivo, si lo hay.
    Optional<AvisoAlimento> findByClaveAndMotivoAndAbiertoTrue(String clave, MotivoAviso motivo);

    // Los pendientes, con su alimento, para la lista de administración.
    @EntityGraph(attributePaths = "alimento")
    Page<AvisoAlimento> findByAbiertoTrue(Pageable pageable);
}
