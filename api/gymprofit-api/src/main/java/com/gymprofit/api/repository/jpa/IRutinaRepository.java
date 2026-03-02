package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.Nivel;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Hidden
@Repository
public interface IRutinaRepository extends CrudRepository<Rutina, Integer> {

    List<Rutina> findByUsuario(Usuario usuario);

    List<Rutina> findByUsuarioId(Integer usuarioId);

    List<Rutina> findByEsPredefinidaTrue();

    List<Rutina> findByNivel(Nivel nivel);

    List<Rutina> findByActivaTrue();

    List<Rutina> findByUsuarioIdAndActivaTrue(Integer usuarioId);

    @Query("SELECT r " +
            "FROM Rutina r " +
            "WHERE r.esPredefinida = true " +
            "AND r.nivel = :nivel")
    List<Rutina> getRutinasPredefinidas(@Param("nivel") Nivel nivel);
}
