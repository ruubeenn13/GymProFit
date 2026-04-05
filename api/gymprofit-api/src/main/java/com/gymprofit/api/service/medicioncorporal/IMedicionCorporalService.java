package com.gymprofit.api.service.medicioncorporal;

import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalCreateDTO;
import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface IMedicionCorporalService {

    List<MedicionCorporalDTO> findAll();

    MedicionCorporalDTO findById(Integer id);
    MedicionCorporalDTO save(MedicionCorporalCreateDTO medicionCorporalCreateDTO);
    MedicionCorporalDTO modify(MedicionCorporalDTO medicionCorporalDTO);

    void deleteById(Integer id);

    List<MedicionCorporalDTO> findByUsuarioId(Integer usuarioId);
    List<MedicionCorporalDTO> findByUsuarioIdOrdenadas(Integer usuarioId);
    List<MedicionCorporalDTO> findByUsuarioIdAndFechaBetween(Integer usuarioId, LocalDateTime inicio, LocalDateTime fin);
    List<MedicionCorporalDTO> getUltimasMediciones(Integer usuarioId);
}
