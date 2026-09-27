package com.gymprofit.api.service.admin;

import com.gymprofit.api.dto.admin.AdminAlimentoDTO;
import com.gymprofit.api.dto.admin.AdminAlimentosResumenDTO;
import com.gymprofit.api.dto.common.PageDTO;

/**
 * El catálogo de alimentos en la web de administración (GP-085). Solo lee: se edita
 * con PATCH /alimentos/{id}, que ya exige ADMIN para el catálogo (DEC-032).
 */
public interface IAdminAlimentoService {

    /**
     * Alimentos del catálogo paginados por nombre, activos o no. Los de cada usuario, nunca.
     *
     * @param q         nombre, nombre en inglés o código de barras
     * @param categoria categoría exacta, o vacío
     * @param sinIngles true deja los que no tienen nombre en inglés
     * @param origen    OPEN_FOOD_FACTS o MANUAL, o vacío
     * @param page      página, desde 0
     * @param size      tamaño, de 1 a 100
     */
    PageDTO<AdminAlimentoDTO> listar(String q, String categoria, boolean sinIngles, String origen, int page, int size);

    /** Tamaño del catálogo, cuántos sin inglés y las categorías que usa. */
    AdminAlimentosResumenDTO resumen();
}
