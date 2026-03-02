package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.EjercicioRealizado;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Hidden
@Repository
public interface IEjercicioRealizadosRepository extends CrudRepository<EjercicioRealizado, Integer> {

    List<EjercicioRealizado> findBySesionId(Integer sesionId);

    List<EjercicioRealizado> findByEjercicioId(Integer ejercicioId);
}
