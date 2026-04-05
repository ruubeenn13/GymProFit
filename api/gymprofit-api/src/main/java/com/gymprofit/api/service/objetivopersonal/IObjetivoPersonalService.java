package com.gymprofit.api.service.objetivopersonal;

import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalCreateDTO;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalDTO;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalUpdateDTO;

import java.util.List;

public interface IObjetivoPersonalService {

    List<ObjetivoPersonalDTO> findAll();

    ObjetivoPersonalDTO findById(Integer id);
    ObjetivoPersonalDTO save(ObjetivoPersonalCreateDTO objetivoPersonalCreateDTO);
    ObjetivoPersonalDTO update(ObjetivoPersonalUpdateDTO objetivoPersonalUpdateDTO);

    void deleteById(Integer id);

    List<ObjetivoPersonalDTO> findByUsuarioId(Integer usuarioId);
    List<ObjetivoPersonalDTO> findByUsuarioIdOrdenados(Integer usuarioId);
    List<ObjetivoPersonalDTO> findPendientesByUsuarioId(Integer usuarioId);
    List<ObjetivoPersonalDTO> findCompletadosByUsuarioId(Integer usuarioId);
    List<ObjetivoPersonalDTO> findByTipoObjetivo(String tipoObjetivo);

    ObjetivoPersonalDTO completar(Integer id);

    Long countByUsuarioId(Integer usuarioId);
    Long countCompletadosByUsuarioId(Integer usuarioId);
    Long countPendientesByUsuarioId(Integer usuarioId);
}