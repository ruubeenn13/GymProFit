package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.AlimentoRacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// ============================================================
// IAlimentoRacionRepository — raciones con nombre de los alimentos (GP-127)
// ============================================================
@Repository
public interface IAlimentoRacionRepository extends JpaRepository<AlimentoRacion, Integer> {
}
