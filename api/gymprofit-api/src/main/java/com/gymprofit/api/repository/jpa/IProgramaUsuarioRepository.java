package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.ProgramaUsuario;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ============================================================
// IProgramaUsuarioRepository — los programas que sigue cada usuario (GP-074)
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IProgramaUsuarioRepository extends JpaRepository<ProgramaUsuario, Integer> {

    // Programas que sigue o siguió un usuario.
    List<ProgramaUsuario> findByUsuarioId(Integer usuarioId);

    // El que sigue ahora (sin fecha de fin). Si hubiera más de uno, el más reciente.
    Optional<ProgramaUsuario> findFirstByUsuarioIdAndFechaFinIsNullOrderByIdDesc(Integer usuarioId);
}
