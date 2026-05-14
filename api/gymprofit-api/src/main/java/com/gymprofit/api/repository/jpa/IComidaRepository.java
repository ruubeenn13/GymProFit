package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Comida;
import com.gymprofit.api.enums.TipoComida;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IComidaRepository extends CrudRepository<Comida, Integer> {

    List<Comida> findByUsuarioId(Integer usuarioId);

    List<Comida> findByTipoComida(TipoComida tipoComida);

    List<Comida> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    List<Comida> findByUsuarioIdAndFechaBetween(Integer usuarioId, LocalDateTime inicio, LocalDateTime fin);

    List<Comida> findByUsuarioIdAndTipoComida(Integer usuarioId, TipoComida tipoComida);

    Long countByUsuarioId(Integer usuarioId);

    Long countByTipoComida(TipoComida tipoComida);

    Long countByUsuarioIdAndTipoComida(Integer usuarioId, TipoComida tipoComida);
}