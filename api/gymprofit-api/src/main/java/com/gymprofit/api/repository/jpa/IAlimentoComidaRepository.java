package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.AlimentoComida;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Hidden
@Repository
public interface IAlimentoComidaRepository extends CrudRepository<AlimentoComida, Integer> {

    List<AlimentoComida> findByComidaId(Integer comidaId);

    List<AlimentoComida> findByAlimentoId(Integer alimentoId);

    Optional<AlimentoComida> findByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId);

    void deleteByComidaId(Integer comidaId);

    void deleteByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId);

    boolean existsByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId);

    Long countByComidaId(Integer comidaId);

    Long countByAlimentoId(Integer alimentoId);
}
