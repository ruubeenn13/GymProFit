package com.gymprofit.api.service.alimentocomida;

import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaCreateDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaPatchDTO;

import java.util.List;

public interface IAlimentoComidaService {

    List<AlimentoComidaDTO> findAll();

    AlimentoComidaDTO findById(Integer id);
    AlimentoComidaDTO save(AlimentoComidaCreateDTO alimentoComidaCreateDTO);
    AlimentoComidaDTO modify(AlimentoComidaDTO alimentoComidaDTO);

    void deleteById(Integer id);

    List<AlimentoComidaDTO> findByComidaId(Integer comidaId);
    List<AlimentoComidaDTO> findByAlimentoId(Integer alimentoId);

    AlimentoComidaDTO findByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId);

    void deleteByComidaId(Integer comidaId);
    void deleteByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId);
    boolean existsByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId);

    Long countByComidaId(Integer comidaId);
    Long countByAlimentoId(Integer alimentoId);

    AlimentoComidaDTO patch(Integer id, AlimentoComidaPatchDTO patchDTO);
}
