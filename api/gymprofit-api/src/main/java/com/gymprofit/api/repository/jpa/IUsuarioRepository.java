package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Usuario;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Hidden
@Repository
public interface IUsuarioRepository extends CrudRepository<Usuario, Integer> {

    Usuario findByUsername(String username);

    Usuario findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    List<Usuario> findByActivoTrue();

    Usuario getUsuarioByUsername(String username);
}
