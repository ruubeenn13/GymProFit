package com.gymprofit.api.service.admin;

import com.gymprofit.api.dto.admin.AdminCuentaDTO;
import com.gymprofit.api.dto.admin.AdminCuentaDetalleDTO;
import com.gymprofit.api.dto.admin.BorrarCuentaAdminDTO;
import com.gymprofit.api.dto.common.PageDTO;

/**
 * Cuentas en la web de administración (GP-085). Solo datos de cuenta: nada de salud.
 */
public interface IAdminCuentaService {

    /**
     * Cuentas paginadas, de la más nueva a la más antigua.
     *
     * @param q      texto que busca en el usuario o en el correo; vacío no filtra
     * @param rol    ADMIN, USER o GUEST; vacío no filtra
     * @param activo estado; null no filtra
     * @param page   página, desde 0
     * @param size   tamaño, de 1 a 100
     */
    PageDTO<AdminCuentaDTO> listar(String q, String rol, Boolean activo, int page, int size);

    /**
     * La cuenta y cuántas sesiones y comidas tiene.
     *
     * @param id cuenta
     */
    AdminCuentaDetalleDTO detalle(Integer id);

    /**
     * Borra una cuenta a petición de su titular, con el mismo borrado que GP-008.
     * No vale para la propia cuenta ni para otra de administración (409), y la
     * confirmación tiene que ser el nombre de usuario de la cuenta (400).
     *
     * @param id  cuenta que se borra
     * @param dto confirmación y motivo
     */
    void borrarAPeticion(Integer id, BorrarCuentaAdminDTO dto);
}
