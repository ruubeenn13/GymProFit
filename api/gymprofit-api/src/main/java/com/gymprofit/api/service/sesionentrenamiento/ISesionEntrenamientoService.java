package com.gymprofit.api.service.sesionentrenamiento;


import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoDTO;

import java.time.LocalDate;
import java.util.List;

public interface ISesionEntrenamientoService {

    List<SesionEntrenamientoDTO> findAll();

    SesionEntrenamientoDTO findById(Integer id);
    SesionEntrenamientoDTO save(SesionEntrenamientoCreateDTO sesionEntrenamientoCreateDTO);
    SesionEntrenamientoDTO modify(SesionEntrenamientoDTO sesionEntrenamientoDTO);

    void deleteById(Integer id);

    SesionEntrenamientoDTO completarSesion(Integer id, Integer caloriasQuemadas, String notas);

    List<SesionEntrenamientoDTO> findByUsuarioId(Integer usuarioId);
    List<SesionEntrenamientoDTO> findByRutinaId(Integer rutinaId);
    List<SesionEntrenamientoDTO> findCompletadas();
    List<SesionEntrenamientoDTO> findPendientes();
    List<SesionEntrenamientoDTO> findByUsuarioIdAndCompletadas(Integer usuarioId);
    List<SesionEntrenamientoDTO> findByUsuarioIdAndPendientes(Integer usuarioId);
    List<SesionEntrenamientoDTO> findByUsuarioIdAndFecha(Integer usuarioId, LocalDate fecha);
    List<SesionEntrenamientoDTO> findByFecha(LocalDate fecha);
    List<SesionEntrenamientoDTO> findByUsuarioIdAndRutinaId(Integer usuarioId, Integer rutinaId);

    Long countByUsuarioId(Integer usuarioId);
    Long countCompletadasByUsuario(Integer usuarioId);
    Long countByRutinaId(Integer rutinaId);

    List<SesionEntrenamientoDTO> findByUsuarioIdOrderByFecha(Integer usuarioId);
    List<SesionEntrenamientoDTO> findCompletadasByUsuario(Integer usuarioId);

    SesionEntrenamientoDTO patch(Integer id, com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoPatchDTO patchDTO);
}
