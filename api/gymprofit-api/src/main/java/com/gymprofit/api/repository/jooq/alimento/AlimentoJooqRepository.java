package com.gymprofit.api.repository.jooq.alimento;

import com.gymprofit.api.dto.jooq.AlimentoJooqDTO;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.gymprofit.api.jooq.Tables.ALIMENTOS;

@Repository
@RequiredArgsConstructor
public class AlimentoJooqRepository implements IAlimentoJooqRepository {

    private final DSLContext dsl;

    @Override
    public List<AlimentoJooqDTO> busquedaAdmin(String nombre, String categoria, Boolean activo) {
        var conditions = new ArrayList<Condition>();

        if (nombre != null && !nombre.isBlank()) {
            conditions.add(ALIMENTOS.NOMBRE.containsIgnoreCase(nombre));
        }
        if (categoria != null && !categoria.isBlank()) {
            conditions.add(ALIMENTOS.CATEGORIA.eq(categoria));
        }
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
