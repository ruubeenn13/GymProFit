package com.gymprofit.api.service.alimento;

import com.gymprofit.api.dto.entity.alimento.AlimentoCreateDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;

import java.util.List;

public interface IAlimentoService {

    List<AlimentoDTO> findAll();

    AlimentoDTO findById(Integer id);
    AlimentoDTO save(AlimentoCreateDTO alimentoCreateDTO);

    void deleteById(Integer id);
    void activateById(Integer id);
    void permanentDeleteById(Integer id);

    AlimentoDTO modify(AlimentoDTO alimentoDTO);

    List<AlimentoDTO> findByNombre(String nombre);
    List<AlimentoDTO> findByCategoria(String categoria);
    List<AlimentoDTO> findActivos();
    List<AlimentoDTO> findByCaloriasBetween(Integer min, Integer max);

    Long countActivos();
    Long countByCategoria(String categoria);

    AlimentoDTO patch(Integer id, com.gymprofit.api.dto.entity.alimento.AlimentoPatchDTO patchDTO);
}