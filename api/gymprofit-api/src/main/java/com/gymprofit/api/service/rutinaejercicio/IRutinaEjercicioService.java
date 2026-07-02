package com.gymprofit.api.service.rutinaejercicio;

import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioCreateDTO;
import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioDTO;

import java.util.List;

// ============================================================
// IRutinaEjercicioService — contrato del servicio de la relación rutina-ejercicio
// Define las operaciones CRUD sobre los ejercicios que componen cada
// rutina (orden, series, repeticiones), permitiendo consultarlos por
// rutina o por ejercicio.
// ============================================================
public interface IRutinaEjercicioService {

    List<RutinaEjercicioDTO> findAll();

    RutinaEjercicioDTO findById(Integer id);
    RutinaEjercicioDTO save(RutinaEjercicioCreateDTO rutinaEjercicioCreateDTO);
    RutinaEjercicioDTO modify(RutinaEjercicioDTO rutinaEjercicioDTO);

    void deleteById(Integer id);

    List<RutinaEjercicioDTO> findByRutinaId(Integer rutinaId);
    List<RutinaEjercicioDTO> findByEjercicioId(Integer ejercicioId);
    List<RutinaEjercicioDTO> findByRutinaIdOrdenado(Integer rutinaId);

    RutinaEjercicioDTO findByRutinaIdAndEjercicioId(Integer rutinaId, Integer ejercicioId);

    Long countByRutinaId(Integer rutinaId);
    Long countByEjercicioId(Integer ejercicioId);

    void deleteByRutinaId(Integer rutinaId);
    void deleteByRutinaIdAndEjercicioId(Integer rutinaId, Integer ejercicioId);

    boolean existsByRutinaIdAndEjercicioId(Integer rutinaId, Integer ejercicioId);

    RutinaEjercicioDTO patch(Integer id, com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioPatchDTO patchDTO);
}
