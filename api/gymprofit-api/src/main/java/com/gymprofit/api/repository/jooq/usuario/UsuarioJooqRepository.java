package com.gymprofit.api.repository.jooq.usuario;

import com.gymprofit.api.dto.admin.AdminEstadisticasDTO;
import com.gymprofit.api.dto.admin.AdminUsuarioDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioEstadisticasDTO;
import com.gymprofit.api.dto.jooq.UsuarioJooqDTO;
import com.gymprofit.api.jooq.enums.RolesNombre;
import com.gymprofit.api.jooq.enums.UsuariosNivelExperiencia;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.gymprofit.api.jooq.Tables.*;
import static org.jooq.impl.DSL.*;

// ============================================================
// UsuarioJooqRepository — implementación jOOQ de consultas sobre usuarios
// Ejecuta consultas SQL tipadas sobre la tabla usuarios: listados, filtros
// dinámicos, cálculo de estadísticas individuales (rachas, calorías, etc.)
// y estadísticas globales agregadas para el panel admin.
// ============================================================
@Repository
@RequiredArgsConstructor
public class UsuarioJooqRepository implements IUsuarioJooqRepository {

    // Contexto DSL de jOOQ inyectado, punto de entrada para construir consultas SQL tipadas.
    private final DSLContext dsl;


    // Devuelve todos los usuarios con sus datos básicos.
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

    // Devuelve solo los usuarios activos, ordenados por username.
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

    // Filtra usuarios por su nivel de experiencia.
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

    // Filtra usuarios cuya edad está entre edadMin y edadMax, ordenados por edad.
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

    // Búsqueda combinada con filtros opcionales por username, nivel de experiencia y edad máxima.
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

    // Calcula estadísticas agregadas de un usuario: sesiones, minutos, calorías,
    // ejercicio más frecuente, rachas de entrenamiento, última medición e IMC, y objetivos completados.
    @Override
    public UsuarioEstadisticasDTO getEstadisticas(Integer usuarioId) {

        // Sesiones
        Integer totalSesiones = dsl
                .selectCount()
                .from(SESIONES_ENTRENAMIENTO)
                .where(SESIONES_ENTRENAMIENTO.USUARIO_ID.eq(usuarioId))
                .fetchOne(0, Integer.class);

        Integer sesionesCompletadas = dsl
                .selectCount()
                .from(SESIONES_ENTRENAMIENTO)
                .where(SESIONES_ENTRENAMIENTO.USUARIO_ID.eq(usuarioId)
                        .and(SESIONES_ENTRENAMIENTO.COMPLETADA.eq((byte) 1)))
                .fetchOne(0, Integer.class);

        Integer totalMinutos = dsl
                .select(coalesce(sum(SESIONES_ENTRENAMIENTO.DURACION_MINUTOS), 0))
                .from(SESIONES_ENTRENAMIENTO)
                .where(SESIONES_ENTRENAMIENTO.USUARIO_ID.eq(usuarioId)
                        .and(SESIONES_ENTRENAMIENTO.COMPLETADA.eq((byte) 1)))
                .fetchOne(0, Integer.class);

        Integer totalCalorias = dsl
                .select(coalesce(sum(SESIONES_ENTRENAMIENTO.CALORIAS_QUEMADAS), 0))
                .from(SESIONES_ENTRENAMIENTO)
                .where(SESIONES_ENTRENAMIENTO.USUARIO_ID.eq(usuarioId)
                        .and(SESIONES_ENTRENAMIENTO.COMPLETADA.eq((byte) 1)))
                .fetchOne(0, Integer.class);

        // Ejercicio más frecuente
        String ejercicioMasFrecuente = dsl
                .select(EJERCICIOS.NOMBRE)
                .from(EJERCICIOS_REALIZADOS)
                .join(SESIONES_ENTRENAMIENTO).on(EJERCICIOS_REALIZADOS.SESION_ID.eq(SESIONES_ENTRENAMIENTO.ID))
                .join(EJERCICIOS).on(EJERCICIOS_REALIZADOS.EJERCICIO_ID.eq(EJERCICIOS.ID))
                .where(SESIONES_ENTRENAMIENTO.USUARIO_ID.eq(usuarioId))
                .groupBy(EJERCICIOS.ID, EJERCICIOS.NOMBRE)
                .orderBy(count(EJERCICIOS_REALIZADOS.ID).desc())
                .limit(1)
                .fetchOne(EJERCICIOS.NOMBRE);

        // Total ejercicios realizados
        Integer totalEjercicios = dsl
                .selectCount()
                .from(EJERCICIOS_REALIZADOS)
                .join(SESIONES_ENTRENAMIENTO).on(EJERCICIOS_REALIZADOS.SESION_ID.eq(SESIONES_ENTRENAMIENTO.ID))
                .where(SESIONES_ENTRENAMIENTO.USUARIO_ID.eq(usuarioId))
                .fetchOne(0, Integer.class);

        // Última medición corporal
        var ultimaMedicion = dsl
                .select(MEDICIONES_CORPORALES.PESO, MEDICIONES_CORPORALES.IMC)
                .from(MEDICIONES_CORPORALES)
                .where(MEDICIONES_CORPORALES.USUARIO_ID.eq(usuarioId))
                .orderBy(MEDICIONES_CORPORALES.FECHA.desc())
                .limit(1)
                .fetchOne();

        BigDecimal peso = ultimaMedicion != null ? ultimaMedicion.get(MEDICIONES_CORPORALES.PESO) : null;
        BigDecimal imc = ultimaMedicion != null ? ultimaMedicion.get(MEDICIONES_CORPORALES.IMC) : null;

        // Objetivos completados
        Integer objetivosCompletados = dsl
                .selectCount()
                .from(OBJETIVOS_PERSONALES)
                .where(OBJETIVOS_PERSONALES.USUARIO_ID.eq(usuarioId)
                        .and(OBJETIVOS_PERSONALES.COMPLETADO.eq((byte) 1)))
                .fetchOne(0, Integer.class);

        // Rachas — fechas únicas de sesiones completadas ordenadas desc
        List<LocalDate> fechas = dsl
                .selectDistinct(SESIONES_ENTRENAMIENTO.FECHA_INICIO.cast(org.jooq.impl.SQLDataType.LOCALDATE))
                .from(SESIONES_ENTRENAMIENTO)
                .where(SESIONES_ENTRENAMIENTO.USUARIO_ID.eq(usuarioId)
                        .and(SESIONES_ENTRENAMIENTO.COMPLETADA.eq((byte) 1)))
                .orderBy(field(name("fecha_inicio")).cast(org.jooq.impl.SQLDataType.LOCALDATE).desc())
                .fetch(0, LocalDate.class);

        int rachaActual = calcularRachaActual(fechas);
        int mejorRacha = calcularMejorRacha(fechas);

        return new UsuarioEstadisticasDTO(
                totalSesiones,
                sesionesCompletadas,
                totalMinutos,
                totalCalorias,
                ejercicioMasFrecuente,
                rachaActual,
                mejorRacha,
                totalEjercicios,
                peso,
                imc,
                objetivosCompletados
        );
    }

    // Lista paginada de usuarios para el panel admin, con filtros por activo/username/rol y join a roles.
    @Override
    public List<AdminUsuarioDTO> getUsuariosAdmin(Boolean activo, String rol, String username, int page, int size) {
        var conditions = new ArrayList<Condition>();

        if (activo != null) {
            conditions.add(USUARIOS.ACTIVO.eq(activo ? (byte) 1 : (byte) 0));
        }
        if (username != null && !username.isBlank()) {
            conditions.add(USUARIOS.USERNAME.containsIgnoreCase(username));
        }
        if (rol != null && !rol.isBlank()) {
            String rolNombre = rol.toUpperCase().replace("ROLE_", "");
            conditions.add(ROLES.NOMBRE.eq(RolesNombre.valueOf(rolNombre)));
        }

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
                        USUARIOS.ACTIVO,
                        USUARIOS.FECHA_REGISTRO,
                        ROLES.NOMBRE.as("rol")
                )
                .from(USUARIOS)
                .leftJoin(USUARIO_ROLES).on(USUARIO_ROLES.USUARIO_ID.eq(USUARIOS.ID))
                .leftJoin(ROLES).on(ROLES.ID.eq(USUARIO_ROLES.ROLE_ID))
                .where(conditions)
                .orderBy(USUARIOS.USERNAME.asc())
                .limit(size)
                .offset((long) page * size)
                .fetchInto(AdminUsuarioDTO.class);
    }

    // Calcula estadísticas globales de la plataforma para el dashboard admin
    // (totales y activos de usuarios, sesiones, ejercicios, objetivos, logros y rutinas).
    @Override
    public AdminEstadisticasDTO getEstadisticasGlobales() {
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(23, 59, 59);

        Long totalUsuarios = dsl
                .selectCount()
                .from(USUARIOS)
                .fetchOne(0, Long.class);

        Long usuariosActivos = dsl
                .selectCount()
                .from(USUARIOS)
                .where(USUARIOS.ACTIVO.eq((byte) 1))
                .fetchOne(0, Long.class);

        Long totalSesiones = dsl
                .selectCount()
                .from(SESIONES_ENTRENAMIENTO)
                .fetchOne(0, Long.class);

        Long sesionesHoy = dsl
                .selectCount()
                .from(SESIONES_ENTRENAMIENTO)
                .where(SESIONES_ENTRENAMIENTO.FECHA_INICIO.between(inicioHoy, finHoy))
                .fetchOne(0, Long.class);

        Long totalEjercicios = dsl
                .selectCount()
                .from(EJERCICIOS_REALIZADOS)
                .fetchOne(0, Long.class);

        Long totalObjetivos = dsl
                .selectCount()
                .from(OBJETIVOS_PERSONALES)
                .where(OBJETIVOS_PERSONALES.COMPLETADO.eq((byte) 1))
                .fetchOne(0, Long.class);

        Long totalLogros = dsl
                .selectCount()
                .from(table("usuario_logros"))
                .fetchOne(0, Long.class);

        Long rutinasPredefinidas = dsl
                .selectCount()
                .from(RUTINAS)
                .where(RUTINAS.ES_PREDEFINIDA.eq((byte) 1))
                .fetchOne(0, Long.class);

        Long ejerciciosActivos = dsl
                .selectCount()
                .from(EJERCICIOS)
                .where(EJERCICIOS.ACTIVO.eq((byte) 1))
                .fetchOne(0, Long.class);

        return new AdminEstadisticasDTO(
                totalUsuarios,
                usuariosActivos,
                totalSesiones,
                sesionesHoy,
                totalEjercicios,
                totalObjetivos,
                totalLogros,
                rutinasPredefinidas,
                ejerciciosActivos
        );
    }

    // Calcula la racha de días consecutivos con sesión completada hasta hoy/ayer.
    private int calcularRachaActual(List<LocalDate> fechas) {
        if (fechas.isEmpty()) return 0;

        LocalDate hoy = LocalDate.now();
        LocalDate primera = fechas.get(0);

        // Si la última sesión no fue hoy ni ayer, racha rota
        if (primera.isBefore(hoy.minusDays(1))) return 0;

        int racha = 1;
        for (int i = 1; i < fechas.size(); i++) {
            if (fechas.get(i - 1).minusDays(1).equals(fechas.get(i))) {
                racha++;
            } else {
                break;
            }
        }
        return racha;
    }

    // Calcula la racha más larga de días consecutivos con sesión completada en todo el historial.
    private int calcularMejorRacha(List<LocalDate> fechas) {
        if (fechas.isEmpty()) return 0;

        int mejor = 1;
        int actual = 1;

        for (int i = 1; i < fechas.size(); i++) {
            if (fechas.get(i - 1).minusDays(1).equals(fechas.get(i))) {
                actual++;
                if (actual > mejor) mejor = actual;
            } else {
                actual = 1;
            }
        }
        return mejor;
    }
}
