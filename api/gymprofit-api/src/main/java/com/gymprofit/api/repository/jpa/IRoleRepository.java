package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Role;
import com.gymprofit.api.enums.RoleType;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ============================================================
// IRoleRepository — repositorio JPA de roles de usuario
// Acceso a los roles del sistema (ADMIN, USER, GUEST, etc.) usados en
// autenticación y control de acceso. No exportado como recurso REST.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IRoleRepository extends JpaRepository<Role, Integer> {

    // Consulta nativa: obtiene los roles cuyo id está en la lista dada.
    @Query(value = "SELECT * FROM roles WHERE id IN :ids", nativeQuery = true)
    List<Role> findByNombreIn(@Param("ids") List<Integer> ids);

    // Busca un rol por su nombre (enum RoleType).
    Optional<Role> findByNombre(RoleType nombre);
}
