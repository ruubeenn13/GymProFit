package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.ObjetivoPersonal;
import com.gymprofit.api.enums.TipoObjetivo;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

// ============================================================
// IObjetivoPersonalRepository — repositorio JPA de la entidad ObjetivoPersonal
// Acceso a datos de los objetivos personales que se marca el usuario (peso, rendimiento, etc.).
// Permite consultar objetivos completados/pendientes y su histórico.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IObjetivoPersonalRepository extends JpaRepository<ObjetivoPersonal, Integer> {

    // Busca todos los objetivos de un usuario.
    List<ObjetivoPersonal> findByUsuarioId(Integer usuarioId);

    // Busca los objetivos pendientes (no completados) de un usuario.
    List<ObjetivoPersonal> findByUsuarioIdAndCompletadoFalse(Integer usuarioId);

    // Busca los objetivos ya completados de un usuario.
    List<ObjetivoPersonal> findByUsuarioIdAndCompletadoTrue(Integer usuarioId);

    // Busca los objetivos de un tipo concreto.
    List<ObjetivoPersonal> findByTipoObjetivo(TipoObjetivo tipoObjetivo);

    // Cuenta el número total de objetivos de un usuario.
    Long countByUsuarioId(Integer usuarioId);

    // Cuenta el número de objetivos completados de un usuario.
    Long countByUsuarioIdAndCompletadoTrue(Integer usuarioId);

    // Cuenta el número de objetivos pendientes de un usuario.
    Long countByUsuarioIdAndCompletadoFalse(Integer usuarioId);

    // Busca los objetivos de un usuario ordenados por fecha de inicio, de más reciente a más antigua.
    List<ObjetivoPersonal> findByUsuarioIdOrderByFechaInicioDesc(Integer usuarioId);

    // Objetivos pendientes del usuario cuya fecha límite cae dentro de un rango
    // (recordatorio "tu objetivo está cerca de vencer": hoy → hoy+3 días).
    List<ObjetivoPersonal> findByUsuarioIdAndCompletadoFalseAndFechaLimiteBetween(Integer usuarioId, LocalDate desde, LocalDate hasta);
}