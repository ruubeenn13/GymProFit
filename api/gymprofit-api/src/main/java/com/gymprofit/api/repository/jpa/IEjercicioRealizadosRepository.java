package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.EjercicioRealizado;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Hidden
@Repository
public interface IEjercicioRealizadosRepository extends CrudRepository<EjercicioRealizado, Integer> {

    List<EjercicioRealizado> findBySesionId(Integer sesionId);

    List<EjercicioRealizado> findByEjercicioId(Integer ejercicioId);

    List<EjercicioRealizado> findBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId);

    Optional<EjercicioRealizado> findFirstBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId);

    Long countBySesionId(Integer sesionId);

    Long countByEjercicioId(Integer ejercicioId);

    void deleteBySesionId(Integer sesionId);

    void deleteBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId);

    boolean existsBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId);
}
