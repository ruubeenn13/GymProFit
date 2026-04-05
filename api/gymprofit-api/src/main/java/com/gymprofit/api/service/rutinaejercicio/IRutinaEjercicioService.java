package com.gymprofit.api.service.rutinaejercicio;

import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioCreateDTO;
import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioDTO;

import java.util.List;

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
}
