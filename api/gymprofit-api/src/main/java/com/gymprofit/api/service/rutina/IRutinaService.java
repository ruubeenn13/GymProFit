package com.gymprofit.api.service.rutina;

import com.gymprofit.api.dto.admin.AdminRutinaDTO;
import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.dto.entity.rutina.RutinaDTO;

import java.util.List;

// ============================================================
// IRutinaService — contrato del servicio de rutinas de entrenamiento
// Define las operaciones CRUD y de consulta sobre rutinas, tanto
// predefinidas (creadas por ADMIN, visibles para todos) como propias
// de cada usuario, incluyendo activación/desactivación.
// ============================================================
public interface IRutinaService {

    List<RutinaDTO> findAll();

    RutinaDTO findById(Integer id);
    RutinaDTO save(RutinaCreateDTO rutinaCreateDTO);

    // Baja lógica: desactiva la rutina (activa=false).
    void deleteById(Integer id);
    // Reactiva una rutina previamente desactivada.
    void activateById(Integer id);
    // Elimina la rutina de forma permanente en base de datos.
    void permanentDeleteById(Integer id);

    RutinaDTO modify(RutinaDTO rutinaDTO);

    List<RutinaDTO> findByUsuarioId(Integer usuarioId);
    List<RutinaDTO> findByNivel(String nivel);
    List<RutinaDTO> findByNombre(String nombre);
    List<RutinaDTO> findActivas();
    List<RutinaDTO> findPredefinidas();
    List<RutinaDTO> findByUsuarioIdAndActivas(Integer usuarioId);
    List<RutinaDTO> findPredefinidasByNivel(String nivel);

    RutinaDTO patch(Integer id, com.gymprofit.api.dto.entity.rutina.RutinaPatchDTO patchDTO);

    // Búsqueda de rutinas predefinidas para el panel admin (incluye inactivas) mediante jOOQ.
    List<AdminRutinaDTO> busquedaRutinasPredefinidas(String nombre, String nivel, String categoria, Boolean activa);
}
