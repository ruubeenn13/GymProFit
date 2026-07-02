package com.gymprofit.api.repository.jooq.rutina;

import com.gymprofit.api.dto.admin.AdminRutinaDTO;
import com.gymprofit.api.jooq.enums.RutinasNivel;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.gymprofit.api.jooq.Tables.RUTINA_EJERCICIO;
import static com.gymprofit.api.jooq.Tables.RUTINAS;

// ============================================================
// AdminRutinaJooqRepository — implementación jOOQ de consultas sobre rutinas predefinidas
// Ejecuta la búsqueda con filtros dinámicos sobre rutinas marcadas como
// predefinidas, incluyendo un subselect con el número de ejercicios de cada una.
// ============================================================
@Repository
@RequiredArgsConstructor
public class AdminRutinaJooqRepository implements IAdminRutinaJooqRepository {

    // Contexto DSL de jOOQ inyectado, punto de entrada para construir consultas SQL tipadas.
    private final DSLContext dsl;

    // Busca rutinas predefinidas aplicando filtros opcionales de nombre, nivel, categoría y estado activo.
    @Override
    public List<AdminRutinaDTO> busquedaRutinasPredefinidas(String nombre, String nivel, String categoria, Boolean activa) {
        var conditions = new ArrayList<Condition>();
        conditions.add(RUTINAS.ES_PREDEFINIDA.eq((byte) 1));

        if (nombre != null && !nombre.isBlank()) {
            conditions.add(RUTINAS.NOMBRE.containsIgnoreCase(nombre));
        }
        if (nivel != null && !nivel.isBlank()) {
            conditions.add(RUTINAS.NIVEL.eq(RutinasNivel.valueOf(nivel.toUpperCase())));
        }
        if (categoria != null && !categoria.isBlank()) {
            conditions.add(RUTINAS.CATEGORIA.containsIgnoreCase(categoria));
        }
        if (activa != null) {
            conditions.add(RUTINAS.ACTIVA.eq(activa ? (byte) 1 : (byte) 0));
        }

        return dsl
                .select(
                        RUTINAS.ID,
                        RUTINAS.NOMBRE,
                        RUTINAS.DESCRIPCION,
                        RUTINAS.NIVEL,
                        RUTINAS.CATEGORIA,
                        RUTINAS.DIAS_SEMANA,
                        RUTINAS.DURACION_MINUTOS,
                        RUTINAS.ACTIVA,
                        RUTINAS.ES_PREDEFINIDA,
                        RUTINAS.FECHA_CREACION,
                        // Subselect: cuenta los ejercicios asociados a cada rutina
                        dsl.selectCount()
                                .from(RUTINA_EJERCICIO)
                                .where(RUTINA_EJERCICIO.RUTINA_ID.eq(RUTINAS.ID))
                                .asField("num_ejercicios")
                )
                .from(RUTINAS)
                .where(conditions)
                .orderBy(RUTINAS.NOMBRE.asc())
                .fetchInto(AdminRutinaDTO.class);
    }
}
