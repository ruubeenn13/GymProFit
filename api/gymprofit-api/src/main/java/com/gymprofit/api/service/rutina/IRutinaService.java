package com.gymprofit.api.service.rutina;

import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.dto.entity.rutina.RutinaDTO;

import java.util.List;

public interface IRutinaService {

    List<RutinaDTO> findAll();

    RutinaDTO findById(Integer id);
    RutinaDTO save(RutinaCreateDTO rutinaCreateDTO);

    void deleteById(Integer id);
    void activateById(Integer id);
    void permanentDeleteById(Integer id);

    RutinaDTO modify(RutinaDTO rutinaDTO);

    List<RutinaDTO> findByUsuarioId(Integer usuarioId);
    List<RutinaDTO> findByNivel(String nivel);
    List<RutinaDTO> findByNombre(String nombre);
    List<RutinaDTO> findActivas();
    List<RutinaDTO> findPredefinidas();
    List<RutinaDTO> findByUsuarioIdAndActivas(Integer usuarioId);
    List<RutinaDTO> findPredefinidasByNivel(String nivel);
}
