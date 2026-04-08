package com.gymprofit.api.repository.jooq.usuario;

import com.gymprofit.api.dto.jooq.UsuarioJooqDTO;
import com.gymprofit.api.jooq.enums.UsuariosNivelExperiencia;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.gymprofit.api.jooq.Tables.USUARIOS;

@Repository
@RequiredArgsConstructor
public class UsuarioJooqRepository implements IUsuarioJooqRepository {

    private final DSLContext dsl;


    @Override
    public List<UsuarioJooqDTO> findAll() {
        return dsl
                .select(
                        USUARIOS.ID,
                        USUARIOS.USERNAME,
                        USUARIOS.EMAIL,
                        USUARIOS.PESO,
                        USUARIOS.ALTURA,
                        USUARIOS.EDAD,
                        USUARIOS.NIVEL_EXPERIENCIA,
                        USUARIOS.OBJETIVO,
                        USUARIOS.ACTIVO
                )
                .from(USUARIOS)
                .fetchInto(UsuarioJooqDTO.class);

        // SQL generado:
        // SELECT id, username, email, peso, altura, edad,
        //        nivel_experiencia, objetivo, activo
        // FROM usuarios
    }

    @Override
    public List<UsuarioJooqDTO> findActivos() {
        return dsl
                .select(
                        USUARIOS.ID,
                        USUARIOS.USERNAME,
                        USUARIOS.EMAIL,
                        USUARIOS.NIVEL_EXPERIENCIA,
                        USUARIOS.OBJETIVO,
                        USUARIOS.ACTIVO
                )
                .from(USUARIOS)
                .where(USUARIOS.ACTIVO.eq((byte) 1))
                .orderBy(USUARIOS.USERNAME.asc())
                .fetchInto(UsuarioJooqDTO.class);

        // SQL generado:
        // SELECT id, username, email, nivel_experiencia, objetivo, activo
        // FROM usuarios
        // WHERE activo = 1
        // ORDER BY username ASC
    }

    @Override
    public List<UsuarioJooqDTO> findByNivelExperiencia(String nivelExperiencia) {
        return dsl
                .select(
                        USUARIOS.ID,
                        USUARIOS.USERNAME,
                        USUARIOS.EMAIL,
                        USUARIOS.NIVEL_EXPERIENCIA,
                        USUARIOS.EDAD,
                        USUARIOS.ACTIVO
                )
                .from(USUARIOS)
                .where(USUARIOS.NIVEL_EXPERIENCIA.eq(
                        UsuariosNivelExperiencia.valueOf(nivelExperiencia.toUpperCase())))
                .fetchInto(UsuarioJooqDTO.class);

        // SQL generado:
        // SELECT id, username, email, nivel_experiencia, edad, activo
        // FROM usuarios
        // WHERE nivel_experiencia = 'nivelExperiencia'
    }

    @Override
    public List<UsuarioJooqDTO> findByEdadBetween(Integer edadMin, Integer edadMax) {
        return dsl
                .select(
                        USUARIOS.ID,
                        USUARIOS.USERNAME,
                        USUARIOS.EMAIL,
                        USUARIOS.EDAD,
                        USUARIOS.NIVEL_EXPERIENCIA,
                        USUARIOS.ACTIVO
                )
                .from(USUARIOS)
                .where(USUARIOS.EDAD.between(edadMin, edadMax))
                .orderBy(USUARIOS.EDAD.asc())
                .fetchInto(UsuarioJooqDTO.class);

        // SQL generado:
        // SELECT id, username, email, edad, nivel_experiencia, activo
        // FROM usuarios
        // WHERE edad BETWEEN edadMin AND edadMax
        // ORDER BY edad ASC
    }

    @Override
    public List<UsuarioJooqDTO> busquedaAvanzada(String username, String nivelExperiencia, Integer edadMax) {
        var conditions = new ArrayList<Condition>();

        // Filtrado dinámico por username
        if (username != null && !username.isBlank()) {
            conditions.add(USUARIOS.USERNAME.containsIgnoreCase(username));
        }

        // Filtrado dinámico por nivel de experiencia
        if (nivelExperiencia != null && !nivelExperiencia.isBlank()) {
            conditions.add(USUARIOS.NIVEL_EXPERIENCIA.eq(
                    UsuariosNivelExperiencia.valueOf(nivelExperiencia.toUpperCase())));
        }

        // Filtrado dinámico por edad máxima
        if (edadMax != null) {
            conditions.add(USUARIOS.EDAD.le(edadMax));
        }

        return dsl
                .select(
                        USUARIOS.ID,
                        USUARIOS.USERNAME,
                        USUARIOS.EMAIL,
                        USUARIOS.EDAD,
                        USUARIOS.NIVEL_EXPERIENCIA,
                        USUARIOS.ACTIVO
                )
                .from(USUARIOS)
                .where(conditions)
                .orderBy(USUARIOS.USERNAME.asc())
                .fetchInto(UsuarioJooqDTO.class);

        // SQL generado (ejemplo con todos los filtros):
        // SELECT id, username, email, edad, nivel_experiencia, activo
        // FROM usuarios
        // WHERE username LIKE '%username%'
        //    AND nivel_experiencia = 'nivelExperiencia'
        //    AND edad <= edadMax
        // ORDER BY username ASC
    }
}
