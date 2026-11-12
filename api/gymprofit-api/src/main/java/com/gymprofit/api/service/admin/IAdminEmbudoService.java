package com.gymprofit.api.service.admin;

import com.gymprofit.api.dto.admin.AdminEmbudoDTO;

import java.time.ZonedDateTime;

/**
 * El embudo del alta y el núcleo de las cuentas nuevas, para la web de administración
 * (GP-019).
 */
public interface IAdminEmbudoService {

    /** El embudo y el núcleo de ahora mismo. */
    AdminEmbudoDTO embudo();

    /**
     * El embudo y el núcleo tal como se verían en un momento dado.
     *
     * @param ahora el instante que cuenta como «ahora»; se lleva a hora de Madrid
     */
    AdminEmbudoDTO embudo(ZonedDateTime ahora);
}
