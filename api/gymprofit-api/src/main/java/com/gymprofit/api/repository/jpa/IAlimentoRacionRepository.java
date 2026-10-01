package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.AlimentoRacion;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

// ============================================================
// IAlimentoRacionRepository — raciones con nombre de los alimentos (GP-127)
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IAlimentoRacionRepository extends JpaRepository<AlimentoRacion, Integer> {
}
