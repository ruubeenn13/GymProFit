package com.gymprofit.api.service.medicioncorporal;

import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalCreateDTO;
import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalDTO;

import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// IMedicionCorporalService — contrato del servicio de mediciones corporales
// Define la gestión del historial de mediciones corporales del usuario
// (peso, altura, IMC, grasa corporal, medidas de partes del cuerpo)
// usadas para el seguimiento de progreso físico dentro de GymProFit.
// ============================================================
public interface IMedicionCorporalService {

    // Devuelve todas las mediciones corporales (uso administrativo).
    List<MedicionCorporalDTO> findAll();

    // Busca una medición corporal por su id.
    MedicionCorporalDTO findById(Integer id);
    // Registra una nueva medición corporal, calculando el IMC si procede.
    MedicionCorporalDTO save(MedicionCorporalCreateDTO medicionCorporalCreateDTO);
    // Actualiza una medición corporal existente.
    MedicionCorporalDTO modify(MedicionCorporalDTO medicionCorporalDTO);

    // Elimina una medición corporal por su id.
    void deleteById(Integer id);

    // Lista todas las mediciones de un usuario.
    List<MedicionCorporalDTO> findByUsuarioId(Integer usuarioId);
    // Lista las mediciones de un usuario ordenadas por fecha descendente.
    List<MedicionCorporalDTO> findByUsuarioIdOrdenadas(Integer usuarioId);
    // Lista las mediciones de un usuario dentro de un rango de fechas.
    List<MedicionCorporalDTO> findByUsuarioIdAndFechaBetween(Integer usuarioId, LocalDateTime inicio, LocalDateTime fin);
    // Devuelve las mediciones más recientes del usuario.
    List<MedicionCorporalDTO> getUltimasMediciones(Integer usuarioId);

    // Actualiza parcialmente una medición (solo campos no nulos, recalculando IMC si aplica).
    MedicionCorporalDTO patch(Integer id, com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalPatchDTO patchDTO);
}
