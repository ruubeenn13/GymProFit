package com.gymprofit.api.service.progresoejercicio;

import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioCreateDTO;
import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioDTO;
import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioPatchDTO;

import java.util.List;

public interface IProgresoEjercicioService {

    List<ProgresoEjercicioDTO> findAll();

    ProgresoEjercicioDTO findById(Integer id);
    ProgresoEjercicioDTO save(ProgresoEjercicioCreateDTO progresoEjercicioCreateDTO);
    ProgresoEjercicioDTO modify(ProgresoEjercicioDTO progresoEjercicioDTO);

    void deleteById(Integer id);

    List<ProgresoEjercicioDTO> findByUsuarioId(Integer usuarioId);
    List<ProgresoEjercicioDTO> findByEjercicioId(Integer ejercicioId);
    List<ProgresoEjercicioDTO> findByUsuarioIdAndEjercicioId(Integer usuarioId, Integer ejercicioId);
    List<ProgresoEjercicioDTO> findByUsuarioIdOrdenado(Integer usuarioId);
    List<ProgresoEjercicioDTO> getProgresoByUsuarioAndEjercicio(Integer usuarioId, Integer ejercicioId);

    ProgresoEjercicioDTO getUltimoProgresoByUsuarioAndEjercicio(Integer usuarioId, Integer ejercicioId);

    Long countByUsuarioId(Integer usuarioId);
    Long countByEjercicioId(Integer ejercicioId);

    void deleteByUsuarioId(Integer usuarioId);
    void deleteByUsuarioIdAndEjercicioId(Integer usuarioId, Integer ejercicioId);

    boolean existsByUsuarioIdAndEjercicioId(Integer usuarioId, Integer ejercicioId);

    ProgresoEjercicioDTO patch(Integer id, ProgresoEjercicioPatchDTO patchDTO);
}
