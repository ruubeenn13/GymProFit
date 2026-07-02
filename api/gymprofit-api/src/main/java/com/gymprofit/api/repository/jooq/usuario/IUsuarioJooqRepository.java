package com.gymprofit.api.repository.jooq.usuario;

import com.gymprofit.api.dto.admin.AdminEstadisticasDTO;
import com.gymprofit.api.dto.admin.AdminUsuarioDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioEstadisticasDTO;
import com.gymprofit.api.dto.jooq.UsuarioJooqDTO;

import java.util.List;

// ============================================================
// IUsuarioJooqRepository — contrato de consultas jOOQ sobre usuarios
// Define búsquedas/filtros dinámicos sobre usuarios y agregaciones de
// estadísticas (individuales y globales) usadas por perfil y panel admin.
// ============================================================
public interface IUsuarioJooqRepository {

    // Devuelve todos los usuarios con sus datos básicos.
    List<UsuarioJooqDTO> findAll();

    // Devuelve solo los usuarios activos, ordenados por username.
    List<UsuarioJooqDTO> findActivos();

    // Filtra usuarios por su nivel de experiencia.
    List<UsuarioJooqDTO> findByNivelExperiencia(String nivelExperiencia);

    // Filtra usuarios cuya edad está entre edadMin y edadMax.
    List<UsuarioJooqDTO> findByEdadBetween(Integer edadMin, Integer edadMax);

    // Búsqueda combinada con filtros opcionales por username, nivel y edad máxima.
    List<UsuarioJooqDTO> busquedaAvanzada(String username, String nivelExperiencia, Integer edadMax);

    // Calcula estadísticas agregadas (sesiones, calorías, rachas, etc.) de un usuario.
    UsuarioEstadisticasDTO getEstadisticas(Integer usuarioId);

    // Lista paginada de usuarios para el panel admin, con filtros por activo/rol/username.
    List<AdminUsuarioDTO> getUsuariosAdmin(Boolean activo, String rol, String username, int page, int size);

    // Calcula estadísticas globales de la plataforma (totales, activos, sesiones de hoy, etc.).
    AdminEstadisticasDTO getEstadisticasGlobales();
}
