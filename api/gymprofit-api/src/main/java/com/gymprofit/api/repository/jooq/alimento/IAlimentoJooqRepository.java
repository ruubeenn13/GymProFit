package com.gymprofit.api.repository.jooq.alimento;

import com.gymprofit.api.dto.jooq.AlimentoJooqDTO;

import java.util.List;

public interface IAlimentoJooqRepository {
    List<AlimentoJooqDTO> busquedaAdmin(String nombre, String categoria, Boolean activo);
}
