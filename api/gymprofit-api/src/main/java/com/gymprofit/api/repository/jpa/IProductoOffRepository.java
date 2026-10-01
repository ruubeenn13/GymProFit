package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.ProductoOff;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// ============================================================
// IProductoOffRepository — productos de Open Food Facts (GP-164)
// La escritura masiva va por JDBC en ProductoOffService; esto es para leer.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IProductoOffRepository extends JpaRepository<ProductoOff, Integer> {

    Optional<ProductoOff> findByCodigo(String codigo);
}
