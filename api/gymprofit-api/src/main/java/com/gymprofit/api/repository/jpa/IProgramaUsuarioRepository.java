package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.ProgramaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ============================================================
// IProgramaUsuarioRepository — los programas que sigue cada usuario (GP-074)
// ============================================================
@Repository
public interface IProgramaUsuarioRepository extends JpaRepository<ProgramaUsuario, Integer> {

    // Programas que sigue o siguió un usuario.
    List<ProgramaUsuario> findByUsuarioId(Integer usuarioId);

    // El que sigue ahora (sin fecha de fin). Si hubiera más de uno, el más reciente.
    Optional<ProgramaUsuario> findFirstByUsuarioIdAndFechaFinIsNullOrderByIdDesc(Integer usuarioId);
}
