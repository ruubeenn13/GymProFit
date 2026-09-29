package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.ProgramaUsuario;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

// ============================================================
// IProgramaUsuarioRepository — los programas que sigue cada usuario (GP-074)
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IProgramaUsuarioRepository extends JpaRepository<ProgramaUsuario, Integer> {

    // Programas que sigue un usuario.
    List<ProgramaUsuario> findByUsuarioId(Integer usuarioId);
}
