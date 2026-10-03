package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.Nivel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

// ============================================================
// IRutinaRepository — repositorio JPA de rutinas de entrenamiento
// Acceso a las rutinas de los usuarios y a las rutinas predefinidas del
// sistema, filtrables por nivel, nombre o estado activo.
// ============================================================
@Repository
public interface IRutinaRepository extends JpaRepository<Rutina, Integer> {

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

    // Copias de un «programa que sigue» (GP-074), activas o no.
    List<Rutina> findByProgramaUsuarioId(Integer programaUsuarioId);

    // Rutinas activas de un usuario concreto.
    List<Rutina> findByUsuarioIdAndActivaTrue(Integer usuarioId);

    // Consulta JPQL: rutinas predefinidas filtradas por nivel.
    @Query("SELECT r " +
            "FROM Rutina r " +
            "WHERE r.esPredefinida = true " +
            "AND r.nivel = :nivel")
    List<Rutina> getRutinasPredefinidas(@Param("nivel") Nivel nivel);
}