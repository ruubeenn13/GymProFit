package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.ProductoOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// ============================================================
// IProductoOffRepository — productos de Open Food Facts (GP-164)
// La escritura masiva va por JDBC en ProductoOffService; esto es para leer.
// ============================================================
@Repository
public interface IProductoOffRepository extends JpaRepository<ProductoOff, Integer> {

    Optional<ProductoOff> findByCodigo(String codigo);

    // Los productos de esos códigos (los avisos pendientes de administración, lote 1.6.1).
    java.util.List<ProductoOff> findByCodigoIn(java.util.Collection<String> codigos);
}
