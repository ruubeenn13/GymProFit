package com.gymprofit.api.service.logro;

import com.gymprofit.api.dto.entity.logro.LogroCreateDTO;
import com.gymprofit.api.dto.entity.logro.LogroDTO;
import com.gymprofit.api.dto.entity.logro.UsuarioLogroDTO;

import java.util.List;

// ============================================================
// ILogroService — contrato del servicio de logros/achievements
// Define la gestión del catálogo de logros y la evaluación automática
// de qué logros ha desbloqueado un usuario según su progreso (sesiones,
// ejercicios realizados, objetivos cumplidos).
// ============================================================
public interface ILogroService {

    // Devuelve el catálogo completo de logros disponibles.
    List<LogroDTO> findAll();

    // Devuelve los logros obtenidos por un usuario concreto.
    List<UsuarioLogroDTO> findByUsuarioId(Integer usuarioId);

    // Crea un nuevo logro en el catálogo.
    LogroDTO save(LogroCreateDTO createDTO);

    // Actualiza un logro existente del catálogo.
    LogroDTO update(Integer id, LogroCreateDTO updateDTO);

    // Evalúa el progreso del usuario y otorga los nuevos logros que cumpla; devuelve sus nombres.
    List<String> evaluarLogros(Integer usuarioId);
}
