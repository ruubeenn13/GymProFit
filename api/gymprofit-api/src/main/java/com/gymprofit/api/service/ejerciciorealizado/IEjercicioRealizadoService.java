package com.gymprofit.api.service.ejerciciorealizado;

import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoCreateDTO;
import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoDTO;
import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoPatchDTO;

import java.util.List;

// ============================================================
// IEjercicioRealizadoService — contrato del servicio de registro de ejercicios realizados
// Define las operaciones para gestionar los ejercicios efectivamente
// ejecutados dentro de una sesión de entrenamiento (series, repeticiones,
// peso usado, tiempo), incluyendo consultas y borrados asociados a sesión/ejercicio.
// ============================================================
public interface IEjercicioRealizadoService {

    // Devuelve todos los ejercicios realizados (uso administrativo).
    List<EjercicioRealizadoDTO> findAll();

    // Busca un ejercicio realizado por su id.
    EjercicioRealizadoDTO findById(Integer id);
    // Registra un nuevo ejercicio realizado dentro de una sesión.
    EjercicioRealizadoDTO save(EjercicioRealizadoCreateDTO ejercicioRealizadoCreateDTO);
    // Actualiza los datos de un ejercicio realizado existente.
    EjercicioRealizadoDTO modify(EjercicioRealizadoDTO ejercicioRealizadoDTO);

    // Elimina un ejercicio realizado por su id.
    void deleteById(Integer id);

    // Lista los ejercicios realizados de una sesión concreta.
    List<EjercicioRealizadoDTO> findBySesionId(Integer sesionId);
    // Lista los registros de un ejercicio concreto en cualquier sesión.
    List<EjercicioRealizadoDTO> findByEjercicioId(Integer ejercicioId);
    // Lista los registros de un ejercicio dentro de una sesión concreta.
    List<EjercicioRealizadoDTO> findBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId);

    // Cuenta los ejercicios realizados en una sesión.
    Long countBySesionId(Integer sesionId);
    // Cuenta cuántas veces se ha realizado un ejercicio.
    Long countByEjercicioId(Integer ejercicioId);

    // Elimina todos los ejercicios realizados de una sesión.
    void deleteBySesionId(Integer sesionId);
    // Elimina los registros de un ejercicio concreto dentro de una sesión.
    void deleteBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId);

    // Comprueba si existe un registro de ese ejercicio en esa sesión.
    boolean existsBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId);

    // Actualiza parcialmente un ejercicio realizado (solo campos no nulos).
    EjercicioRealizadoDTO patch(Integer id, EjercicioRealizadoPatchDTO patchDTO);
}
