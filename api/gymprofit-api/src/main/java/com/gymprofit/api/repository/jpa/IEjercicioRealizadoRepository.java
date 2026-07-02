package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.EjercicioRealizado;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

// ============================================================
// IEjercicioRealizadoRepository — repositorio JPA de la entidad EjercicioRealizado
// Acceso a datos de los ejercicios efectivamente realizados dentro de una sesión de entrenamiento.
// Usado para calcular progreso y estadísticas de rendimiento del usuario.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IEjercicioRealizadoRepository extends CrudRepository<EjercicioRealizado, Integer> {

    // Busca los ejercicios realizados en una sesión de entrenamiento.
    List<EjercicioRealizado> findBySesionId(Integer sesionId);

    // Busca los registros de un ejercicio concreto en todas las sesiones.
    List<EjercicioRealizado> findByEjercicioId(Integer ejercicioId);

    // Busca el registro de un ejercicio concreto dentro de una sesión concreta.
    List<EjercicioRealizado> findBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId);

    // Cuenta cuántos ejercicios se han realizado en una sesión.
    Long countBySesionId(Integer sesionId);

    // Cuenta en cuántas sesiones se ha realizado un ejercicio concreto.
    Long countByEjercicioId(Integer ejercicioId);

    // Cuenta cuántos ejercicios ha realizado un usuario (a través de sus sesiones).
    Long countBySesionUsuarioId(Integer usuarioId);

    // Elimina todos los ejercicios realizados asociados a una sesión.
    void deleteBySesionId(Integer sesionId);

    // Elimina el registro de un ejercicio concreto dentro de una sesión.
    void deleteBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId);

    // Comprueba si un ejercicio ya fue registrado en una sesión concreta.
    boolean existsBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId);
}
