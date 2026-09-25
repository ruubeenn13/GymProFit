package com.gymprofit.api.service.record;

import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioDTO;
import com.gymprofit.api.dto.entity.progresoejercicio.RecordDestacadoDTO;
import com.gymprofit.api.dto.entity.record.PuntoProgresionDTO;
import com.gymprofit.api.dto.entity.record.RecordDTO;
import com.gymprofit.api.dto.entity.record.RecordsDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// ============================================================
// IRecordService — récords y progresión sacados de las series (GP-088)
// ============================================================
public interface IRecordService {

    /**
     * Récords del usuario del token.
     *
     * @param desde si no es null, también los récords batidos desde ese día.
     * @return el récord vigente de cada ejercicio y, si se pidió, los recientes.
     */
    RecordsDTO recordsDelUsuarioActual(LocalDate desde);

    /**
     * Mejor serie de cada sesión del usuario del token en un ejercicio.
     *
     * @param ejercicioId ejercicio del catálogo (404 si no existe).
     * @return puntos en orden cronológico; vacío si nunca lo ha entrenado.
     */
    List<PuntoProgresionDTO> progresionDelUsuarioActual(Integer ejercicioId);

    /**
     * Récords y primeras marcas de una sesión recién guardada.
     *
     * @param usuarioId dueño de la sesión, ya comprobado por quien llama.
     * @param sesionId  sesión.
     * @return {@code [recordsBatidos, primerasMarcas]}.
     */
    List<List<RecordDTO>> cambiosDeMarcaDeSesion(Integer usuarioId, Integer sesionId);

    /**
     * El récord de más peso del usuario, para la ruta vieja de la tarjeta de inicio.
     *
     * @param usuarioId usuario (se comprueba la propiedad).
     * @return el récord, o vacío si todavía no ha batido ninguno.
     */
    Optional<RecordDestacadoDTO> recordDestacado(Integer usuarioId);

    /**
     * Historial de un ejercicio con la forma de la ruta vieja, para las builds ya repartidas.
     *
     * @param usuarioId   usuario (se comprueba la propiedad; 404 si no existe).
     * @param ejercicioId ejercicio (404 si no existe).
     * @return la mejor serie de cada sesión, de la más reciente a la más antigua.
     */
    List<ProgresoEjercicioDTO> historialLegado(Integer usuarioId, Integer ejercicioId);
}
