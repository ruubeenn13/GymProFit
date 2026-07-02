package com.gymprofit.api.repository.jooq.alimento;

import com.gymprofit.api.dto.jooq.AlimentoJooqDTO;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.gymprofit.api.jooq.Tables.ALIMENTOS;

// ============================================================
// AlimentoJooqRepository — repositorio jOOQ para consultas complejas sobre alimentos.
// Implementa búsquedas administrativas de alimentos con filtros dinámicos y
// opcionales (nombre, categoría, activo) que no encajan bien en JPA derived
// queries; construye la consulta SQL de forma condicional con jOOQ.
// ============================================================
@Repository
@RequiredArgsConstructor
public class AlimentoJooqRepository implements IAlimentoJooqRepository {

    // Contexto DSL de jOOQ inyectado para construir y ejecutar consultas SQL tipadas.
    private final DSLContext dsl;

    // Búsqueda de alimentos para el panel de administración con filtros
    // opcionales por nombre (contiene, insensible a mayúsculas), categoría
    // exacta y estado activo/inactivo; devuelve resultados ordenados por nombre.
    @Override
    public List<AlimentoJooqDTO> busquedaAdmin(String nombre, String categoria, Boolean activo) {
        var conditions = new ArrayList<Condition>();

        // Filtro por nombre solo si se proporciona un valor no vacío.
        if (nombre != null && !nombre.isBlank()) {
            conditions.add(ALIMENTOS.NOMBRE.containsIgnoreCase(nombre));
        }
        // Filtro por categoría exacta solo si se proporciona un valor no vacío.
        if (categoria != null && !categoria.isBlank()) {
            conditions.add(ALIMENTOS.CATEGORIA.eq(categoria));
        }
        // Filtro por estado activo/inactivo; el booleano se traduce a byte (0/1) en BD.
        if (activo != null) {
            conditions.add(ALIMENTOS.ACTIVO.eq(activo ? (byte) 1 : (byte) 0));
        }

        return dsl
                .select()
                .from(ALIMENTOS)
                .where(conditions)
                .orderBy(ALIMENTOS.NOMBRE.asc())
                .fetchInto(AlimentoJooqDTO.class);
    }
}
