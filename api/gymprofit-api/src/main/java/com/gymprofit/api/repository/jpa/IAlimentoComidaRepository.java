package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.AlimentoComida;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Hidden
@Repository
public interface IAlimentoComidaRepository extends CrudRepository<AlimentoComida, Integer> {

    List<AlimentoComida> fomByComidaId(Integer comidaId);

    List<AlimentoComida> findByAlimentoId(Integer alimentoId);
}
