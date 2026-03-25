package com.gymprofit.api.service.comida;

import com.gymprofit.api.dto.entity.comida.ComidaCreateDTO;
import com.gymprofit.api.dto.entity.comida.ComidaDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface IComidaService {

    List<ComidaDTO> findAll();

    ComidaDTO findById(Integer id);
    ComidaDTO save(ComidaCreateDTO comidaCreateDTO);
    ComidaDTO modify(ComidaDTO comidaDTO);

    void deleteById(Integer id);

    List<ComidaDTO> findByUsuarioId(Integer usuarioId);
    List<ComidaDTO> findByTipoComida(String tipoComida);
    List<ComidaDTO> findByFecha(LocalDate fecha);
    List<ComidaDTO> findByUsuarioIdAndFecha(Integer usuarioId, LocalDate fecha);
    List<ComidaDTO> findByUsuarioIdAndTipoComida(Integer usuarioId, String tipoComida);

    Long countByUsuarioId(Integer usuarioId);
    Long countByTipoComida(String tipoComida);
    Long countByUsuarioIdAndTipoComida(Integer usuarioId, String tipoComida);
}
