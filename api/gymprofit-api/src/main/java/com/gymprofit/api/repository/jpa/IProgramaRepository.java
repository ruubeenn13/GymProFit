package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Programa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ============================================================
// IProgramaRepository — repositorio JPA de los programas del catálogo (GP-074)
// Son pocos (13 en el catálogo v1): los filtros se aplican en el servicio.
// ============================================================
@Repository
public interface IProgramaRepository extends JpaRepository<Programa, Integer> {

    // Programas activos, en el orden del catálogo.
    List<Programa> findByActivoTrueOrderByIdAsc();

    // Un programa activo por su código.
    Optional<Programa> findByCodigoAndActivoTrue(String codigo);
}
