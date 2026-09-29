package com.gymprofit.api.service.programa;

import com.gymprofit.api.dto.entity.programa.ProgramaDTO;
import com.gymprofit.api.dto.entity.programa.ProgramaDetalleDTO;
import com.gymprofit.api.dto.entity.programa.ProgramaQueSigueDTO;
import com.gymprofit.api.dto.entity.programa.ProgramaSeguidoDTO;
import com.gymprofit.api.dto.entity.programa.RecomendadoDTO;
import com.gymprofit.api.dto.entity.programa.VistaPreviaDTO;

import java.util.List;
import java.util.Optional;

// ============================================================
// IProgramaService — catálogo de programas y seguir uno (GP-074)
// ============================================================
public interface IProgramaService {

    /**
     * Programas activos del catálogo, filtrados. Los filtros nulos no filtran.
     *
     * @param equipamiento GIMNASIO, MANCUERNAS o PESO_CORPORAL.
     * @param dias         días por semana, de 1 a 7: salen los programas que los admiten.
     * @param nivel        PRINCIPIANTE o INTERMEDIO; AVANZADO y EXPERTO dan los de intermedio.
     * @return los programas, en el orden del catálogo; lista vacía si no hay ninguno.
     */
    List<ProgramaDTO> listar(String equipamiento, Integer dias, String nivel);

    /**
     * Un programa con su semana y cada rutina con sus ejercicios.
     *
     * @param codigo código del programa.
     * @return el detalle; 404 si no existe.
     */
    ProgramaDetalleDTO detalle(String codigo);

    /**
     * El usuario del token sigue un programa: se lleva una copia de cada rutina distinta,
     * ajustada a su nivel, su objetivo y el tiempo por sesión. Seguirlo otra vez crea
     * otras copias y no toca las que ya tiene.
     *
     * @param codigo  código del programa.
     * @param minutos 30, 45, 60 o 75; null es 60.
     * @return lo creado.
     */
    ProgramaSeguidoDTO seguir(String codigo, Integer minutos);

    /**
     * Cómo quedaría cada rutina al seguir el programa, con las mismas reglas y el perfil
     * del token, sin guardar nada.
     *
     * @param codigo  código del programa.
     * @param minutos 30, 45, 60 o 75; null es 60.
     */
    VistaPreviaDTO vistaPrevia(String codigo, Integer minutos);

    /**
     * El programa de la tabla del catálogo para el nivel del perfil del token.
     *
     * @param equipamiento GIMNASIO, MANCUERNAS o PESO_CORPORAL (obligatorio).
     * @param dias         de 2 a 6 (obligatorio).
     */
    RecomendadoDTO recomendado(String equipamiento, Integer dias);

    /** El programa que sigue el usuario del token, o vacío si no sigue ninguno. */
    Optional<ProgramaQueSigueDTO> seguido();

    /** Deja el programa que sigue, si sigue alguno: fecha de fin y sus rutinas desactivadas. */
    void dejarSeguido();
}
