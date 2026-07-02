package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Comida;
import com.gymprofit.api.enums.TipoComida;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// IComidaRepository — repositorio JPA de la entidad Comida
// Acceso a datos de las comidas registradas por los usuarios (desayuno, almuerzo, cena, etc.).
// Permite filtrar por usuario, tipo de comida y rango de fechas para el histórico nutricional.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IComidaRepository extends CrudRepository<Comida, Integer> {

    // Busca todas las comidas registradas por un usuario.
    List<Comida> findByUsuarioId(Integer usuarioId);

    // Busca comidas por su tipo (desayuno, comida, cena, etc.).
    List<Comida> findByTipoComida(TipoComida tipoComida);

    // Busca comidas registradas dentro de un rango de fechas.
    List<Comida> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    // Busca las comidas de un usuario dentro de un rango de fechas.
    List<Comida> findByUsuarioIdAndFechaBetween(Integer usuarioId, LocalDateTime inicio, LocalDateTime fin);

    // Busca las comidas de un usuario de un tipo concreto.
    List<Comida> findByUsuarioIdAndTipoComida(Integer usuarioId, TipoComida tipoComida);

    // Cuenta el número de comidas registradas por un usuario.
    Long countByUsuarioId(Integer usuarioId);

    // Cuenta el número de comidas de un tipo concreto.
    Long countByTipoComida(TipoComida tipoComida);

    // Cuenta el número de comidas de un usuario para un tipo concreto.
    Long countByUsuarioIdAndTipoComida(Integer usuarioId, TipoComida tipoComida);
}