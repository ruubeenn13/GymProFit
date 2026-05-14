package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.ObjetivoPersonal;
import com.gymprofit.api.enums.TipoObjetivo;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IObjetivoPersonalRepository extends CrudRepository<ObjetivoPersonal, Integer> {

    List<ObjetivoPersonal> findByUsuarioId(Integer usuarioId);

    List<ObjetivoPersonal> findByUsuarioIdAndCompletadoFalse(Integer usuarioId);

    List<ObjetivoPersonal> findByUsuarioIdAndCompletadoTrue(Integer usuarioId);

    List<ObjetivoPersonal> findByTipoObjetivo(TipoObjetivo tipoObjetivo);

    Long countByUsuarioId(Integer usuarioId);

    Long countByUsuarioIdAndCompletadoTrue(Integer usuarioId);

    Long countByUsuarioIdAndCompletadoFalse(Integer usuarioId);

    List<ObjetivoPersonal> findByUsuarioIdOrderByFechaInicioDesc(Integer usuarioId);
}