package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.UsuarioLogro;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IUsuarioLogroRepository extends CrudRepository<UsuarioLogro, Integer> {

    List<UsuarioLogro> findByUsuarioId(Integer usuarioId);

    @Query("SELECT ul.logro.id FROM UsuarioLogro ul WHERE ul.usuario.id = :usuarioId")
    List<Integer> findLogroIdsByUsuarioId(@Param("usuarioId") Integer usuarioId);
}
