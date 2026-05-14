package com.gymprofit.api.service.ejerciciorealizado;

import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoCreateDTO;
import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoDTO;
import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoPatchDTO;

import java.util.List;

public interface IEjercicioRealizadoService {

    List<EjercicioRealizadoDTO> findAll();

    EjercicioRealizadoDTO findById(Integer id);
    EjercicioRealizadoDTO save(EjercicioRealizadoCreateDTO ejercicioRealizadoCreateDTO);
    EjercicioRealizadoDTO modify(EjercicioRealizadoDTO ejercicioRealizadoDTO);

    void deleteById(Integer id);

    List<EjercicioRealizadoDTO> findBySesionId(Integer sesionId);
    List<EjercicioRealizadoDTO> findByEjercicioId(Integer ejercicioId);
    List<EjercicioRealizadoDTO> findBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId);

    Long countBySesionId(Integer sesionId);
    Long countByEjercicioId(Integer ejercicioId);

    void deleteBySesionId(Integer sesionId);
    void deleteBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId);

    boolean existsBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId);

    EjercicioRealizadoDTO patch(Integer id, EjercicioRealizadoPatchDTO patchDTO);
}
