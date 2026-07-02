package com.gymprofit.api.repository.jooq.rutina;

import com.gymprofit.api.dto.admin.AdminRutinaDTO;

import java.util.List;

// ============================================================
// IAdminRutinaJooqRepository — contrato de consultas jOOQ para rutinas predefinidas (admin)
// Define la búsqueda con filtros dinámicos sobre rutinas predefinidas,
// incluyendo el conteo de ejercicios por rutina, para el panel admin.
// ============================================================
public interface IAdminRutinaJooqRepository {

    // Busca rutinas predefinidas aplicando filtros opcionales de nombre, nivel, categoría y estado activo.
    List<AdminRutinaDTO> busquedaRutinasPredefinidas(String nombre, String nivel, String categoria, Boolean activa);
}
