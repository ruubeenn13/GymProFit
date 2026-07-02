package com.gymprofit.api.service.ejercicio;

import com.gymprofit.api.dto.entity.ejercicio.EjercicioCreateDTO;
import com.gymprofit.api.dto.entity.ejercicio.EjercicioDTO;
import com.gymprofit.api.dto.jooq.EjercicioJooqDTO;

import java.util.List;

// ============================================================
// IEjercicioService — contrato del servicio de gestión de ejercicios del catálogo
// Define las operaciones CRUD, de activación/desactivación (borrado lógico)
// y de búsqueda por grupo muscular, dificultad o nombre para el catálogo
// de ejercicios que se usan en rutinas y sesiones de entrenamiento.
// ============================================================
public interface IEjercicioService {

    // Devuelve todos los ejercicios del catálogo.
    List<EjercicioDTO> findAll();

    // Busca un ejercicio por su id.
    EjercicioDTO findById(Integer id);
    // Crea un nuevo ejercicio en el catálogo.
    EjercicioDTO save(EjercicioCreateDTO ejercicioCreateDTO);

    // Borrado lógico del ejercicio (lo desactiva).
    void deleteById(Integer id);
    // Reactiva un ejercicio previamente desactivado.
    void activateById(Integer id);
    // Elimina definitivamente el ejercicio de la base de datos.
    void permanentDeleteById(Integer id);

    // Actualiza los datos de un ejercicio existente.
    EjercicioDTO modify(EjercicioDTO ejercicioDTO);

    // Filtra ejercicios por grupo muscular trabajado.
    List<EjercicioDTO> findByGrupoMuscular(String grupoMuscular);
    // Filtra ejercicios por nivel de dificultad.
    List<EjercicioDTO> findByDificultad(String dificultad);
    // Busca ejercicios cuyo nombre coincida (parcial).
    List<EjercicioDTO> findByNombre(String nombre);
    // Devuelve solo los ejercicios activos (no eliminados lógicamente).
    List<EjercicioDTO> findActivos();

    // Actualiza parcialmente un ejercicio (solo los campos no nulos del DTO).
    EjercicioDTO patch(Integer id, com.gymprofit.api.dto.entity.ejercicio.EjercicioPatchDTO patchDTO);

    // Búsqueda de ejercicios para el panel admin (incluye inactivos) mediante jOOQ.
    List<EjercicioJooqDTO> busquedaAdmin(String nombre, String grupoMuscular, String dificultad, Boolean activo);
}
