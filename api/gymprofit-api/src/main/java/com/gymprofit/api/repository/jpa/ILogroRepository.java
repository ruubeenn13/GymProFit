package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Logro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// ============================================================
// ILogroRepository — repositorio JPA de la entidad Logro
// Acceso a datos del catálogo de logros/badges que puede desbloquear el usuario en la app.
// Sin consultas adicionales: solo hereda las operaciones CRUD básicas.
// ============================================================
@Repository
public interface ILogroRepository extends JpaRepository<Logro, Integer> {
}
