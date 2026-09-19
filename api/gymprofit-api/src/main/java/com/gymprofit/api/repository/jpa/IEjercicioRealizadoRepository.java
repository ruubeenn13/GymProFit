package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.EjercicioRealizado;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// IEjercicioRealizadoRepository — repositorio JPA de la entidad EjercicioRealizado
// Acceso a datos de los ejercicios efectivamente realizados dentro de una sesión de entrenamiento.
// Usado para calcular progreso y estadísticas de rendimiento del usuario.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IEjercicioRealizadoRepository extends JpaRepository<EjercicioRealizado, Integer> {

    // Busca los ejercicios realizados en una sesión de entrenamiento.
    List<EjercicioRealizado> findBySesionId(Integer sesionId);

    // Busca los registros de un ejercicio concreto en todas las sesiones, de TODOS los
    // usuarios. Solo para ADMIN: el servicio no la expone a un USER, que usa la variante
    // filtrada por propietario.
    List<EjercicioRealizado> findByEjercicioId(Integer ejercicioId);

    // Busca los registros de un ejercicio concreto que pertenecen a un usuario, a través
    // de la sesión en la que se ejecutaron. Es la consulta que usa un USER: el histórico
    // de un ejercicio es un dato privado —cuánto levanta cada uno y cuándo entrena—, y
    // filtrarlo aquí evita tener que descartar filas ajenas más arriba.
    List<EjercicioRealizado> findByEjercicioIdAndSesionUsuarioId(Integer ejercicioId, Integer usuarioId);

    // Busca el registro de un ejercicio concreto dentro de una sesión concreta.
    List<EjercicioRealizado> findBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId);

    // Cuenta cuántos ejercicios se han realizado en una sesión.
    Long countBySesionId(Integer sesionId);

    // Cuenta en cuántas sesiones se ha realizado un ejercicio concreto, contando las de
    // TODOS los usuarios. Solo para ADMIN, por el mismo motivo que findByEjercicioId.
    Long countByEjercicioId(Integer ejercicioId);

    // Cuenta cuántas veces ha realizado un usuario un ejercicio concreto. Un contador
    // global también es información ajena: revela la actividad del resto aunque no
    // devuelva ni una fila suya.
    Long countByEjercicioIdAndSesionUsuarioId(Integer ejercicioId, Integer usuarioId);

    // Cuenta cuántos ejercicios ha realizado un usuario (a través de sus sesiones).
    Long countBySesionUsuarioId(Integer usuarioId);

    /**
     * Series por músculo en las sesiones COMPLETADAS de un usuario desde una fecha.
     * <p>
     * Alimenta la silueta de Home. Se devuelven los dos campos sin combinar
     * ({@code musculoPrimario} y {@code grupoMuscular}) porque el catálogo importado de
     * wger tiene filas sin músculo primario, y decidir cuál manda es trabajo del
     * servicio, no de la consulta.
     * <p>
     * Se suma {@code seriesCompletadas} y no las filas de {@code series_realizadas}: el
     * resumen lo calcula el servidor a partir de las series cuando existen, y sigue
     * estando relleno en las sesiones antiguas, anteriores al registro por serie.
     *
     * @return filas {@code [musculoPrimario, grupoMuscular, series]}.
     */
    @Query("SELECT e.musculoPrimario, e.grupoMuscular, SUM(COALESCE(er.seriesCompletadas, 0)) " +
           "FROM EjercicioRealizado er JOIN er.ejercicio e JOIN er.sesion s " +
           "WHERE s.usuario.id = :usuarioId AND s.completada = true AND s.fechaFin >= :desde " +
           "GROUP BY e.musculoPrimario, e.grupoMuscular")
    List<Object[]> seriesPorMusculoDesde(@Param("usuarioId") Integer usuarioId,
                                         @Param("desde") LocalDateTime desde);

    /**
     * Kilos movidos en una sesión, sumando serie a serie (peso × repeticiones).
     * <p>
     * Solo cubre las sesiones con registro por serie. Las anteriores no tienen filas
     * en {@code series_realizadas} y se calculan aparte, desde el resumen del ejercicio.
     */
    @Query("SELECT COALESCE(SUM(sr.peso * sr.repeticiones), 0) " +
           "FROM SerieRealizada sr WHERE sr.ejercicioRealizado.sesion.id = :sesionId")
    java.math.BigDecimal volumenDeSeries(@Param("sesionId") Integer sesionId);

    /**
     * Kilos movidos en una sesión según el resumen por ejercicio
     * (peso × repeticiones × series), para las sesiones anteriores al registro por serie.
     */
    @Query("SELECT COALESCE(SUM(er.pesoUsado * er.repeticionesReales * er.seriesCompletadas), 0) " +
           "FROM EjercicioRealizado er WHERE er.sesion.id = :sesionId")
    java.math.BigDecimal volumenDeResumen(@Param("sesionId") Integer sesionId);

    // Elimina todos los ejercicios realizados asociados a una sesión.
    void deleteBySesionId(Integer sesionId);

    // Elimina el registro de un ejercicio concreto dentro de una sesión.
    void deleteBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId);

    // Comprueba si un ejercicio ya fue registrado en una sesión concreta.
    boolean existsBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId);
}
