package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Role;
import com.gymprofit.api.enums.RoleType;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Hidden
@Repository
public interface IRoleRepository extends CrudRepository<Role, Integer> {

    Optional<Role> findByNombre(RoleType nombre);

    List<Role> findByNombreIn(List<RoleType> nombres);

    boolean existsByNombre(RoleType nombre);
}
