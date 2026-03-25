package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.RutinaEjercicio;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Hidden
@Repository
public interface IRutinaEjercicioRepository extends CrudRepository<RutinaEjercicio, Integer> {

    List<RutinaEjercicio> findByRutinaId(Integer rutinaId);

    List<RutinaEjercicio> findByEjercicioId(Integer ejercicioId);

    List<RutinaEjercicio> findByRutinaIdOrderByOrdenAsc(Integer rutinaId);

    Optional<RutinaEjercicio> findByRutinaIdAndEjercicioId(Integer rutinaId, Integer ejercicioId);

    Long countByRutinaId(Integer rutinaId);

    Long countByEjercicioId(Integer ejercicioId);

    void deleteByRutinaId(Integer rutinaId);

    void deleteByRutinaIdAndEjercicioId(Integer rutinaId, Integer ejercicioId);

    boolean existsByRutinaIdAndEjercicioId(Integer rutinaId, Integer ejercicioId);
}
