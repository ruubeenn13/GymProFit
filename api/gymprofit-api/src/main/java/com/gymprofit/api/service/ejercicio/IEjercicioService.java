package com.gymprofit.api.service.ejercicio;

import com.gymprofit.api.dto.entity.ejercicio.EjercicioCreateDTO;
import com.gymprofit.api.dto.entity.ejercicio.EjercicioDTO;

import java.util.List;

public interface IEjercicioService {

    List<EjercicioDTO> findAll();

    EjercicioDTO findById(Integer id);
    EjercicioDTO save(EjercicioCreateDTO ejercicioCreateDTO);

    void deleteById(Integer id);
    void activateById(Integer id);
    void permanentDeleteById(Integer id);

    EjercicioDTO modify(EjercicioDTO ejercicioDTO);

    List<EjercicioDTO> findByGrupoMuscular(String grupoMuscular);
    List<EjercicioDTO> findByDificultad(String dificultad);
    List<EjercicioDTO> findByNombre(String nombre);
    List<EjercicioDTO> findActivos();
}
