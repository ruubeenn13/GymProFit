package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Alimento;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Hidden
@Repository
public interface IAlimentoRepository extends CrudRepository<Alimento, Integer> {

    List<Alimento> findByCategoria(String categoria);

    List<Alimento> findByActivoTrue();

    List<Alimento> findByNombreContainingIgnoreCase(String nombre);

    List<Alimento> findByCaloriasBetween(Integer min, Integer max);

    Long countByActivoTrue();

    Long countByCategoria(String categoria);
}
