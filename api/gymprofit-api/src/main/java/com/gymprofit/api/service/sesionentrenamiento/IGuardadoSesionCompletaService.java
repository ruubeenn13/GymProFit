package com.gymprofit.api.service.sesionentrenamiento;

import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionCompletaCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoDTO;

// ============================================================
// IGuardadoSesionCompletaService — contrato de POST /sesiones/completa (GP-076)
// ============================================================
public interface IGuardadoSesionCompletaService {

    /**
     * Guarda la sesión entera de forma atómica e idempotente, también cuando dos
     * intentos con la misma clave llegan a la vez.
     *
     * @param dto la sesión completa.
     * @return la sesión creada, o la que ya existía para esa clave.
     */
    SesionEntrenamientoDTO guardar(SesionCompletaCreateDTO dto);
}
