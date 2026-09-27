package com.gymprofit.api.service.admin;

import com.gymprofit.api.dto.admin.AdminResumenDTO;

import java.time.ZonedDateTime;

/**
 * Los números de la pantalla de Resumen de la web de administración (GP-085).
 */
public interface IAdminResumenService {

    /** El resumen de ahora mismo. */
    AdminResumenDTO resumen();

    /**
     * El resumen tal como se vería en un momento dado.
     *
     * @param ahora el instante que cuenta como «ahora»; se lleva a hora de Madrid
     */
    AdminResumenDTO resumen(ZonedDateTime ahora);
}
