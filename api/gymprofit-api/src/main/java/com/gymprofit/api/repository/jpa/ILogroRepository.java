package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Logro;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

// ============================================================
// ILogroRepository — repositorio JPA de la entidad Logro
// Acceso a datos del catálogo de logros/badges que puede desbloquear el usuario en la app.
// Sin consultas adicionales: solo hereda las operaciones CRUD básicas.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface ILogroRepository extends CrudRepository<Logro, Integer> {
}
