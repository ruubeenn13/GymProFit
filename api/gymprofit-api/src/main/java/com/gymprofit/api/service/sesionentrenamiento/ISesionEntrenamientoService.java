package com.gymprofit.api.service.sesionentrenamiento;


import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoDTO;

import java.time.LocalDate;
import java.util.List;

// ============================================================
// ISesionEntrenamientoService — contrato del servicio de sesiones de entrenamiento.
// Define el CRUD de sesiones (entrenamientos concretos que un usuario realiza,
// opcionalmente vinculados a una rutina) más consultas de filtrado/conteo.
// ============================================================
public interface ISesionEntrenamientoService {

    // Lista todas las sesiones de entrenamiento (uso administrativo).
    List<SesionEntrenamientoDTO> findAll();

    // Busca una sesión por su id.
    SesionEntrenamientoDTO findById(Integer id);
    // Crea una nueva sesión de entrenamiento.
    SesionEntrenamientoDTO save(SesionEntrenamientoCreateDTO sesionEntrenamientoCreateDTO);
    // Sustituye los datos de una sesión existente.
    SesionEntrenamientoDTO modify(SesionEntrenamientoDTO sesionEntrenamientoDTO);

    // Elimina una sesión de entrenamiento por id.
    void deleteById(Integer id);

    // Marca una sesión como completada, registrando calorías quemadas y notas.
    SesionEntrenamientoDTO completarSesion(Integer id, Integer caloriasQuemadas, String notas);

    // Lista las sesiones de un usuario.
    List<SesionEntrenamientoDTO> findByUsuarioId(Integer usuarioId);
    // Lista las sesiones asociadas a una rutina.
    List<SesionEntrenamientoDTO> findByRutinaId(Integer rutinaId);
    // Lista todas las sesiones completadas.
    List<SesionEntrenamientoDTO> findCompletadas();
    // Lista todas las sesiones pendientes de completar.
    List<SesionEntrenamientoDTO> findPendientes();
    // Lista las sesiones completadas de un usuario.
    List<SesionEntrenamientoDTO> findByUsuarioIdAndCompletadas(Integer usuarioId);
    // Lista las sesiones pendientes de un usuario.
    List<SesionEntrenamientoDTO> findByUsuarioIdAndPendientes(Integer usuarioId);
    // Lista las sesiones de un usuario en una fecha concreta.
    List<SesionEntrenamientoDTO> findByUsuarioIdAndFecha(Integer usuarioId, LocalDate fecha);
    // Lista las sesiones de todos los usuarios en una fecha concreta.
    List<SesionEntrenamientoDTO> findByFecha(LocalDate fecha);
    // Lista las sesiones de un usuario asociadas a una rutina concreta.
    List<SesionEntrenamientoDTO> findByUsuarioIdAndRutinaId(Integer usuarioId, Integer rutinaId);

    // Cuenta las sesiones totales de un usuario.
    Long countByUsuarioId(Integer usuarioId);
    // Cuenta las sesiones completadas de un usuario.
    Long countCompletadasByUsuario(Integer usuarioId);
    // Cuenta las sesiones asociadas a una rutina.
    Long countByRutinaId(Integer rutinaId);

    // Lista las sesiones de un usuario ordenadas por fecha.
    List<SesionEntrenamientoDTO> findByUsuarioIdOrderByFecha(Integer usuarioId);
    // Lista las sesiones completadas de un usuario ordenadas por fecha.
    List<SesionEntrenamientoDTO> findCompletadasByUsuario(Integer usuarioId);

    // Actualiza parcialmente una sesión de entrenamiento con los campos no nulos del patch.
    SesionEntrenamientoDTO patch(Integer id, com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoPatchDTO patchDTO);
}
