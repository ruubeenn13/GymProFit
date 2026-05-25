package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Role;
import com.gymprofit.api.enums.RoleType;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IRoleRepository extends CrudRepository<Role, Integer> {

    @Query(value = "SELECT * FROM roles WHERE id IN :ids", nativeQuery = true)
    List<Role> findByNombreIn(@Param("ids") List<Integer> ids);

    Optional<Role> findByNombre(RoleType nombre);
}
