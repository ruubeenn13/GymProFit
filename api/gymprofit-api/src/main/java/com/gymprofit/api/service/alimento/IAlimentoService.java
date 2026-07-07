package com.gymprofit.api.service.alimento;

import com.gymprofit.api.dto.common.PageDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoCreateDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.dto.jooq.AlimentoJooqDTO;

import java.util.List;

// ============================================================
// IAlimentoService — contrato del servicio de alimentos del catálogo nutricional
// Define las operaciones CRUD y de consulta sobre alimentos (base de datos
// nutricional usada para componer comidas). Implementada por AlimentoService.
// ============================================================
public interface IAlimentoService {

    // Devuelve todos los alimentos del catálogo.
    List<AlimentoDTO> findAll();

    // Busca un alimento por su id.
    AlimentoDTO findById(Integer id);
    // Crea un nuevo alimento.
    AlimentoDTO save(AlimentoCreateDTO alimentoCreateDTO);

    // Desactiva (borrado lógico) un alimento.
    void deleteById(Integer id);
    // Reactiva un alimento previamente desactivado.
    void activateById(Integer id);
    // Elimina definitivamente un alimento de la base de datos.
    void permanentDeleteById(Integer id);

    // Modifica un alimento existente.
    AlimentoDTO modify(AlimentoDTO alimentoDTO);

    // Busca alimentos cuyo nombre contenga el texto indicado.
    List<AlimentoDTO> findByNombre(String nombre);
    // Busca alimentos por categoría.
    List<AlimentoDTO> findByCategoria(String categoria);
    // Devuelve solo los alimentos activos.
    List<AlimentoDTO> findActivos();
    // Busca alimentos cuyas calorías estén en el rango indicado.
    List<AlimentoDTO> findByCaloriasBetween(Integer min, Integer max);

    // Cuenta los alimentos activos.
    Long countActivos();
    // Cuenta los alimentos de una categoría.
    Long countByCategoria(String categoria);

    // Busca alimentos creados/asociados a un usuario concreto.
    List<AlimentoDTO> findByUsuarioId(Integer usuarioId);

    // Aplica una actualización parcial (PATCH) sobre un alimento.
    AlimentoDTO patch(Integer id, com.gymprofit.api.dto.entity.alimento.AlimentoPatchDTO patchDTO);

    // Búsqueda de alimentos para el panel admin (incluye inactivos) mediante jOOQ.
    List<AlimentoJooqDTO> busquedaAdmin(String nombre, String categoria, Boolean activo);

    // Búsqueda paginada del catálogo visible para el usuario autenticado
    // (globales + propios, solo activos) con filtro por texto y categoría.
    PageDTO<AlimentoDTO> buscarCatalogo(String q, String categoria, int page, int size);
}