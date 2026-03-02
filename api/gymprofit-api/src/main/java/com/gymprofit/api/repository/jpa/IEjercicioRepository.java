package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.enums.Dificultad;
import com.gymprofit.api.enums.GrupoMuscular;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Hidden
@Repository
public interface IEjercicioRepository extends CrudRepository<Ejercicio, Integer> {

    List<Ejercicio> findByGrupoMuscular(GrupoMuscular grupoMuscular);

    List<Ejercicio> findByDificultad(Dificultad dificultad);

    List<Ejercicio> findByGrupoMuscularAndDificultad(GrupoMuscular grupoMuscular, Dificultad dificultad);

    List<Ejercicio> findByActivoTrue();

    List<Ejercicio> findByNombreContainingIgnoreCase(String nombre);

    @Query("SELECT e " +
            "FROM Ejercicio e " +
            "WHERE e.grupoMuscular = :grupo " +
            "AND e.activo = true")
    List<Ejercicio> getEjerciciosActivosPorGrupo(@Param("grupo") GrupoMuscular grupo);
}
