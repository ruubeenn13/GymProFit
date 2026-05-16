package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Logro;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface ILogroRepository extends CrudRepository<Logro, Integer> {
}
