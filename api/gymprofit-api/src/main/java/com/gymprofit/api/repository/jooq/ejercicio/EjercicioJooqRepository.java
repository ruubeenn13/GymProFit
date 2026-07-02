package com.gymprofit.api.repository.jooq.ejercicio;

import com.gymprofit.api.dto.jooq.EjercicioJooqDTO;
import com.gymprofit.api.jooq.enums.EjerciciosDificultad;
import com.gymprofit.api.jooq.enums.EjerciciosGrupoMuscular;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.gymprofit.api.jooq.Tables.EJERCICIOS;

// ============================================================
// EjercicioJooqRepository — implementación jOOQ de consultas sobre ejercicios
// Ejecuta las consultas SQL tipadas (vía DSLContext) sobre la tabla
// ejercicios: listados, filtros por grupo muscular/dificultad/calorías
// y búsquedas dinámicas para catálogo de usuario y panel admin.
// ============================================================
@Repository
@RequiredArgsConstructor
public class EjercicioJooqRepository implements IEjercicioJooqRepository {

    // Contexto DSL de jOOQ inyectado, punto de entrada para construir consultas SQL tipadas.
    private final DSLContext dsl;


    // Devuelve todos los ejercicios sin filtrar.
    @Override
    public List<EjercicioJooqDTO> findAll() {
        return dsl
                .select()
                .from(EJERCICIOS)
                .fetchInto(EjercicioJooqDTO.class);

        // SQL generado:
        // SELECT * FROM ejercicios
    }

    // Devuelve solo los ejercicios activos, ordenados por nombre.
    @Override
    public List<EjercicioJooqDTO> findActivos() {
        return dsl
                .select()
                .from(EJERCICIOS)
                .where(EJERCICIOS.ACTIVO.eq((byte) 1))
                .fetchInto(EjercicioJooqDTO.class);

        // SQL generado:
        // SELECT * FROM ejercicios WHERE activo = 1
    }

    // Filtra ejercicios por grupo muscular y dificultad exactos.
    @Override
    public List<EjercicioJooqDTO> findByGrupoMuscularAndDificultad(String grupoMuscular, String dificultad) {
        return dsl
                .select()
                .from(EJERCICIOS)
                .where(
                        EJERCICIOS.GRUPO_MUSCULAR.eq(EjerciciosGrupoMuscular.valueOf(grupoMuscular.toUpperCase())),
                        EJERCICIOS.DIFICULTAD.eq(EjerciciosDificultad.valueOf(dificultad.toUpperCase()))
                )
                .fetchInto(EjercicioJooqDTO.class);

        // SQL generado:
        // SELECT * FROM ejercicios
        // WHERE grupo_muscular = 'grupoMuscular' AND dificultad = 'dificultad'
    }

    // Filtra ejercicios cuyo gasto calórico está entre min y max, ordenados ascendentemente.
    @Override
    public List<EjercicioJooqDTO> findByCaloriasQuemadasBetween(Integer min, Integer max) {
        return dsl
                .select()
                .from(EJERCICIOS)
                .where(EJERCICIOS.CALORIAS_QUEMADAS.between(min, max))
                .orderBy(EJERCICIOS.CALORIAS_QUEMADAS.asc())
                .fetchInto(EjercicioJooqDTO.class);

        // SQL generado:
        // SELECT * FROM ejercicios
        // WHERE calorias_quemadas BETWEEN min AND max
        // ORDER BY calorias_quemadas ASC
    }

    // Búsqueda combinada con filtros opcionales (nombre, grupo muscular, dificultad, calorías máx.) para catálogo.
    @Override
    public List<EjercicioJooqDTO> busquedaAvanzada(String nombre, String grupoMuscular, String dificultad, Integer caloriasMax) {
        var conditions = new ArrayList<Condition>();

        // Filtrado dinámico por nombre
        if (nombre != null && !nombre.isBlank()) {
            conditions.add(EJERCICIOS.NOMBRE.containsIgnoreCase(nombre));
        }

        // Filtrado dinámico por grupo muscular
        if (grupoMuscular != null && !grupoMuscular.isBlank()) {
            conditions.add(EJERCICIOS.GRUPO_MUSCULAR.eq(EjerciciosGrupoMuscular.valueOf(grupoMuscular.toUpperCase())));
        }

        // Filtrado dinámico por dificultad
        if (dificultad != null && !dificultad.isBlank()) {
            conditions.add(EJERCICIOS.DIFICULTAD.eq(EjerciciosDificultad.valueOf(dificultad.toUpperCase())));
        }

        // Filtrado dinámico por calorías máximas
        if (caloriasMax != null) {
            conditions.add(EJERCICIOS.CALORIAS_QUEMADAS.le(caloriasMax));
        }

        return dsl
                .select()
                .from(EJERCICIOS)
                .where(conditions)
                .orderBy(EJERCICIOS.NOMBRE.asc())
                .fetchInto(EjercicioJooqDTO.class);
    }

    // Búsqueda combinada con filtros opcionales (incluye estado activo) para el panel admin.
    @Override
    public List<EjercicioJooqDTO> busquedaAdmin(String nombre, String grupoMuscular, String dificultad, Boolean activo) {
        var conditions = new ArrayList<Condition>();

        if (nombre != null && !nombre.isBlank()) {
            conditions.add(EJERCICIOS.NOMBRE.containsIgnoreCase(nombre));
        }
        if (grupoMuscular != null && !grupoMuscular.isBlank()) {
            conditions.add(EJERCICIOS.GRUPO_MUSCULAR.eq(EjerciciosGrupoMuscular.valueOf(grupoMuscular.toUpperCase())));
        }
        if (dificultad != null && !dificultad.isBlank()) {
            conditions.add(EJERCICIOS.DIFICULTAD.eq(EjerciciosDificultad.valueOf(dificultad.toUpperCase())));
        }
        if (activo != null) {
            conditions.add(EJERCICIOS.ACTIVO.eq(activo ? (byte) 1 : (byte) 0));
        }

        return dsl
                .select()
                .from(EJERCICIOS)
                .where(conditions)
                .orderBy(EJERCICIOS.NOMBRE.asc())
                .fetchInto(EjercicioJooqDTO.class);
    }
}
