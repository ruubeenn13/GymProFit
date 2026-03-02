package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.RutinaEjercicio;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Hidden
@Repository
public interface IRutinaEjercicioRepository extends CrudRepository<RutinaEjercicio, Integer> {

    List<RutinaEjercicio> findByRutinaId(Integer rutinaId);

    List<RutinaEjercicio> findByEjercicioId(Integer ejercicioId);

    List<RutinaEjercicio> findByRutinaIdOrderByOrdenAsc(Integer rutinaId);
}
