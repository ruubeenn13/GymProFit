package com.gymprofit.api.service.objetivopersonal;

import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalCreateDTO;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalDTO;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalUpdateDTO;

import java.util.List;

// ============================================================
// IObjetivoPersonalService — contrato del servicio de objetivos personales
// Define las operaciones CRUD y de consulta sobre los objetivos que los
// usuarios se marcan (peso, repeticiones, etc.), incluyendo completado
// y contadores de progreso.
// ============================================================
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

    // Marca un objetivo como completado y dispara la evaluación de logros del usuario.
    ObjetivoPersonalDTO completar(Integer id);

    Long countByUsuarioId(Integer usuarioId);
    Long countCompletadosByUsuarioId(Integer usuarioId);
    Long countPendientesByUsuarioId(Integer usuarioId);

    ObjetivoPersonalDTO patch(Integer id, com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalPatchDTO patchDTO);
}