package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.ObjetivoPersonal;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Hidden
@Repository
public interface IObjetivoPersonalRepository extends CrudRepository<ObjetivoPersonal, Integer> {

    List<ObjetivoPersonal> findByUsuarioId(Integer usuarioId);

    List<ObjetivoPersonal> findByUsuarioIdAndCompletadoFalse(Integer usuarioId);

    List<ObjetivoPersonal> findByUsuarioIdAndCompletadoTrue(Integer usuarioId);

    List<ObjetivoPersonal> findByTipoObjetivo(String tipoObjetivo);
}
