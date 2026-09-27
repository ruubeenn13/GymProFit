package com.gymprofit.api.service.admin;

import com.gymprofit.api.dto.admin.AdminEjercicioDTO;
import com.gymprofit.api.dto.admin.AdminEjercicioDetalleDTO;
import com.gymprofit.api.dto.admin.AdminEjercicioUpdateDTO;
import com.gymprofit.api.dto.admin.AdminEjerciciosResumenDTO;
import com.gymprofit.api.dto.common.PageDTO;

/**
 * El catálogo de ejercicios en la web de administración (GP-085).
 */
public interface IAdminEjercicioService {

    /**
     * Ejercicios paginados por nombre, activos o no.
     *
     * @param q            texto que busca en el nombre en español y en inglés
     * @param grupo        grupo muscular, o vacío
     * @param equipamiento equipamiento, o vacío
     * @param sinRevisar   true deja solo los activos con el nombre sin revisar
     * @param page         página, desde 0
     * @param size         tamaño, de 1 a 100
     */
    PageDTO<AdminEjercicioDTO> listar(String q, String grupo, String equipamiento, boolean sinRevisar,
                                      int page, int size);

    /** Cuántos hay activos y sin revisar, y las opciones de equipamiento. */
    AdminEjerciciosResumenDTO resumen();

    /**
     * El ejercicio para el editor, con en cuántas rutinas se usa.
     *
     * @param id ejercicio
     */
    AdminEjercicioDetalleDTO detalle(Integer id);

    /**
     * Guarda el ejercicio entero. Un nombre en español distinto del inglés lo deja
     * revisado.
     *
     * @param id  ejercicio
     * @param dto campos editables
     */
    AdminEjercicioDetalleDTO guardar(Integer id, AdminEjercicioUpdateDTO dto);
}
