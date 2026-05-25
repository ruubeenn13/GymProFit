package com.gymprofit.api.repository.jooq.rutina;

import com.gymprofit.api.dto.admin.AdminRutinaDTO;

import java.util.List;

public interface IAdminRutinaJooqRepository {

    List<AdminRutinaDTO> busquedaRutinasPredefinidas(String nombre, String nivel, String categoria, Boolean activa);
}
