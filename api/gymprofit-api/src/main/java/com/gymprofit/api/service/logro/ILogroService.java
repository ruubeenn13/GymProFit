package com.gymprofit.api.service.logro;

import com.gymprofit.api.dto.entity.logro.LogroCreateDTO;
import com.gymprofit.api.dto.entity.logro.LogroDTO;
import com.gymprofit.api.dto.entity.logro.UsuarioLogroDTO;

import java.util.List;

public interface ILogroService {

    List<LogroDTO> findAll();

    List<UsuarioLogroDTO> findByUsuarioId(Integer usuarioId);

    LogroDTO save(LogroCreateDTO createDTO);

    LogroDTO update(Integer id, LogroCreateDTO updateDTO);

    List<String> evaluarLogros(Integer usuarioId);
}
