package com.gymprofit.api.service.alimentocomida;

import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaCreateDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaPatchDTO;

import java.util.List;

// ============================================================
// IAlimentoComidaService — contrato del servicio de la relación alimento-comida
// Define las operaciones sobre la tabla intermedia que asocia alimentos a
// comidas con una cantidad en gramos, usada para calcular calorías y macros.
// ============================================================
public interface IAlimentoComidaService {

    // Devuelve todas las relaciones alimento-comida (solo ADMIN).
    List<AlimentoComidaDTO> findAll();

    // Busca una relación por su id.
    AlimentoComidaDTO findById(Integer id);
    // Crea una nueva relación alimento-comida, calculando sus calorías.
    AlimentoComidaDTO save(AlimentoComidaCreateDTO alimentoComidaCreateDTO);
    // Modifica una relación existente.
    AlimentoComidaDTO modify(AlimentoComidaDTO alimentoComidaDTO);

    // Elimina una relación por su id.
    void deleteById(Integer id);

    // Busca todos los alimentos de una comida concreta.
    List<AlimentoComidaDTO> findByComidaId(Integer comidaId);
    // Busca todas las comidas que contienen un alimento concreto.
    List<AlimentoComidaDTO> findByAlimentoId(Integer alimentoId);

    // Busca la relación entre una comida y un alimento concretos.
    AlimentoComidaDTO findByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId);

    // Elimina todos los alimentos de una comida.
    void deleteByComidaId(Integer comidaId);
    // Elimina la relación entre una comida y un alimento concretos.
    void deleteByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId);
    // Comprueba si existe relación entre una comida y un alimento.
    boolean existsByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId);

    // Cuenta los alimentos asociados a una comida.
    Long countByComidaId(Integer comidaId);
    // Cuenta las comidas que contienen un alimento.
    Long countByAlimentoId(Integer alimentoId);

    // Aplica una actualización parcial (PATCH) sobre una relación.
    AlimentoComidaDTO patch(Integer id, AlimentoComidaPatchDTO patchDTO);
}
