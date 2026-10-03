package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

// ============================================================
// IFavoritoRepository — los favoritos de cada cuenta (lote 1.6.3)
// ============================================================
@Repository
@RepositoryRestResource(exported = false)
public interface IFavoritoRepository extends JpaRepository<Favorito, Integer> {

    boolean existsByUsuarioIdAndAlimentoId(Integer usuarioId, Integer alimentoId);

    // Repetible: si no estaba, no borra nada y no falla.
    @Modifying
    @Query("DELETE FROM Favorito f WHERE f.usuarioId = :usuarioId AND f.alimentoId = :alimentoId")
    int quitar(Integer usuarioId, Integer alimentoId);
}
