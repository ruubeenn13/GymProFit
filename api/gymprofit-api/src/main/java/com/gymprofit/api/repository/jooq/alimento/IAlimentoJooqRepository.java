package com.gymprofit.api.repository.jooq.alimento;

import com.gymprofit.api.dto.jooq.AlimentoJooqDTO;

import java.util.List;

// ============================================================
// IAlimentoJooqRepository — contrato de consultas jOOQ sobre alimentos
// Define las búsquedas complejas/dinámicas sobre la tabla de alimentos
// que no encajan bien en JPA. Implementado con jOOQ para el panel admin.
// ============================================================
public interface IAlimentoJooqRepository {
    // Búsqueda de alimentos para el panel admin, con filtros dinámicos opcionales.
    List<AlimentoJooqDTO> busquedaAdmin(String nombre, String categoria, Boolean activo);
}
