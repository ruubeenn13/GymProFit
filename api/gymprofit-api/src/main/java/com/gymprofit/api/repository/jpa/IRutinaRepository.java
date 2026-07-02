package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.Nivel;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

// ============================================================
// IRutinaRepository — repositorio JPA de rutinas de entrenamiento
// Acceso a las rutinas de los usuarios y a las rutinas predefinidas del
// sistema, filtrables por nivel, nombre o estado activo. No exportado como REST.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IRutinaRepository extends CrudRepository<Rutina, Integer> {

    // Rutinas de un usuario dado (por entidad Usuario).
    List<Rutina> findByUsuario(Usuario usuario);

    // Rutinas de un usuario dado (por id).
    List<Rutina> findByUsuarioId(Integer usuarioId);

    // Rutinas predefinidas del sistema (no creadas por un usuario).
    List<Rutina> findByEsPredefinidaTrue();

    // Rutinas filtradas por nivel de dificultad.
    List<Rutina> findByNivel(Nivel nivel);

    // Rutinas actualmente activas.
    List<Rutina> findByActivaTrue();

    // Búsqueda de rutinas por nombre, ignorando mayúsculas/minúsculas.
    List<Rutina> findByNombreContainingIgnoreCase(String nombre);

    // Rutinas activas de un usuario concreto.
    List<Rutina> findByUsuarioIdAndActivaTrue(Integer usuarioId);

    // Consulta JPQL: rutinas predefinidas filtradas por nivel.
    @Query("SELECT r " +
            "FROM Rutina r " +
            "WHERE r.esPredefinida = true " +
            "AND r.nivel = :nivel")
    List<Rutina> getRutinasPredefinidas(@Param("nivel") Nivel nivel);
}