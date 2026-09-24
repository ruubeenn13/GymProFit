package com.gymprofit.api.integration;

import com.gymprofit.api.dto.usuario.EliminarCuentaDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// BorradoCuentaTest — DELETE /usuarios/me no deja ni una fila
//
// El criterio de aceptación de GP-008: se siembra al usuario en TODAS las tablas que
// lo referencian, se borra la cuenta, y se comprueba tabla por tabla que no queda
// nada suyo. La siembra va en SQL nativo a propósito: los services fuerzan reglas de
// propiedad y algunas de las filas que hay que sembrar son justo las que la
// aplicación correcta no deja crear (las cruzadas entre usuarios).
//
// La parte que aguanta el paso del tiempo es el primer test: la lista de tablas NO
// está escrita a mano contra la que se compara, sale de information_schema. Si
// alguien añade mañana una tabla con FK a usuarios y no la mete en el borrado, ese
// test se pone rojo y dice qué falta. Sin eso, el resto solo prueba lo que hoy
// sabemos que existe, que es exactamente el fallo que este criterio quería evitar.
// ============================================================
@DisplayName("Borrado de cuenta — DELETE /usuarios/me no deja ni una fila")
class BorradoCuentaTest extends AbstractOwnershipTest {

    private static final String PASSWORD = "Test1234";

    // Tablas con FK directa a usuarios que el borrado conoce y que este test siembra.
    // Fuente de verdad: el esquema. Esto es solo la copia que se compara contra él.
    private static final Set<String> TABLAS_CONOCIDAS = Set.of(
            "alimentos",
            "comidas",
            "device_tokens",
            "fotos_perfil",
            "mediciones_corporales",
            "notificaciones",
            "objetivos_personales",
            "password_reset_codigos",
            "refresh_tokens",
            "rutinas",
            "sesiones_entrenamiento",
            "usuario_logros",
            "usuario_roles");

    @PersistenceContext
    private EntityManager em;

    // Ids sembrados que hacen falta para comprobar las tablas sin usuario_id propio.
    private Integer comidaId;
    private Integer alimentoPersonalId;
    private Integer rutinaId;
    private Integer sesionId;
    private Integer ejercicioRealizadoId;
    private Integer plantillaId;
    private Integer comidaAjenaId;
    private Integer sesionAjenaId;

    @BeforeEach
    void sembrarTodo() {
        Integer ejercicioId = crearEjercicioCatalogo().getId();
        Integer alimentoCatalogoId = crearAlimentoCatalogo().getId();
        Integer logroId = crearLogro();
        em.flush();

        // --- Lo del usuario que se va ---------------------------------------
        alimentoPersonalId = insertar("""
                INSERT INTO alimentos (nombre, calorias, usuario_id, activo)
                VALUES ('Tortilla de la abuela', 300, %d, true)""".formatted(owner.getId()));

        comidaId = insertar("""
                INSERT INTO comidas (usuario_id, tipo_comida, total_calorias)
                VALUES (%d, 'DESAYUNO', 300)""".formatted(owner.getId()));

        ejecutar("""
                INSERT INTO alimentos_comida (comida_id, alimento_id, cantidad_gramos)
                VALUES (%d, %d, 100)""".formatted(comidaId, alimentoCatalogoId));

        rutinaId = insertar("""
                INSERT INTO rutinas (nombre, nivel, usuario_id)
                VALUES ('Rutina propia', 'PRINCIPIANTE', %d)""".formatted(owner.getId()));

        ejecutar("""
                INSERT INTO rutina_ejercicio (rutina_id, ejercicio_id)
                VALUES (%d, %d)""".formatted(rutinaId, ejercicioId));

        sesionId = insertar("""
                INSERT INTO sesiones_entrenamiento (usuario_id, rutina_id, fecha_inicio, fecha_fin)
                VALUES (%d, %d, NOW(), NOW())""".formatted(owner.getId(), rutinaId));

        ejercicioRealizadoId = insertar("""
                INSERT INTO ejercicios_realizados (sesion_id, ejercicio_id)
                VALUES (%d, %d)""".formatted(sesionId, ejercicioId));

        ejecutar("""
                INSERT INTO series_realizadas (ejercicio_realizado_id, numero, repeticiones)
                VALUES (%d, 1, 10)""".formatted(ejercicioRealizadoId));

        ejecutar("INSERT INTO mediciones_corporales (usuario_id, peso, fecha) VALUES (%d, 80.5, NOW())"
                .formatted(owner.getId()));
        ejecutar("""
                INSERT INTO objetivos_personales (usuario_id, tipo_objetivo, descripcion, valor_objetivo, fecha_inicio)
                VALUES (%d, 'PESO', 'Bajar peso', 75, CURDATE())""".formatted(owner.getId()));
        ejecutar("""
                INSERT INTO notificaciones (usuario_id, titulo, mensaje, tipo)
                VALUES (%d, 'Hola', 'Mensaje', 'SISTEMA')""".formatted(owner.getId()));
        ejecutar("INSERT INTO usuario_logros (usuario_id, logro_id, fecha_obtenido) VALUES (%d, %d, NOW())"
                .formatted(owner.getId(), logroId));
        ejecutar("""
                INSERT INTO fotos_perfil (usuario_id, datos, content_type, fecha_actualizacion)
                VALUES (%d, 0x00, 'image/jpeg', NOW())""".formatted(owner.getId()));
        ejecutar("""
                INSERT INTO device_tokens (usuario_id, token, fecha_registro, fecha_actualizacion)
                VALUES (%d, 'token-fcm-borrado', NOW(), NOW())""".formatted(owner.getId()));
        ejecutar("""
                INSERT INTO refresh_tokens (usuario_id, token, fecha_expiracion)
                VALUES (%d, 'refresh-borrado', NOW())""".formatted(owner.getId()));
        ejecutar("""
                INSERT INTO password_reset_codigos (usuario_id, codigo_hash, fecha_creacion, fecha_expiracion, usado, intentos)
                VALUES (%d, 'hash', NOW(), NOW(), false, 0)""".formatted(owner.getId()));

        // --- Plantilla del sistema: no es de nadie y tiene que sobrevivir ----
        plantillaId = insertar("""
                INSERT INTO rutinas (nombre, nivel, usuario_id)
                VALUES ('Plantilla del sistema', 'PRINCIPIANTE', NULL)""");
        ejecutar("INSERT INTO rutina_ejercicio (rutina_id, ejercicio_id) VALUES (%d, %d)"
                .formatted(plantillaId, ejercicioId));

        // --- Filas cruzadas de OTRO usuario ---------------------------------
        // Estas son las que la aplicación correcta no deja crear. Se insertan a mano
        // porque representan lo que pudo quedar de antes de cerrar la IDOR de escritura.
        comidaAjenaId = insertar("""
                INSERT INTO comidas (usuario_id, tipo_comida, total_calorias)
                VALUES (%d, 'CENA', 300)""".formatted(attacker.getId()));
        ejecutar("""
                INSERT INTO alimentos_comida (comida_id, alimento_id, cantidad_gramos)
                VALUES (%d, %d, 100)""".formatted(comidaAjenaId, alimentoPersonalId));

        sesionAjenaId = insertar("""
                INSERT INTO sesiones_entrenamiento (usuario_id, rutina_id, fecha_inicio, fecha_fin)
                VALUES (%d, %d, NOW(), NOW())""".formatted(attacker.getId(), rutinaId));
    }

    // --- El test que sobrevive a las tablas que aún no existen --------------

    @Test
    @DisplayName("el esquema no tiene ninguna tabla con FK a usuarios que el borrado desconozca")
    void el_borrado_conoce_todas_las_tablas_del_esquema() {
        @SuppressWarnings("unchecked")
        List<String> delEsquema = em.createNativeQuery("""
                        SELECT DISTINCT TABLE_NAME FROM information_schema.KEY_COLUMN_USAGE
                        WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME = 'usuarios'""")
                .getResultList();

        Set<String> encontradas = delEsquema.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(encontradas)
                .as("""
                        El esquema tiene tablas que apuntan a usuarios y que este test no conoce. \
                        Cuando aparece una tabla nueva hay que hacer TRES cosas, no una: sembrarla \
                        en este test, borrarla en BorradoCuentaService y añadirla aquí. Si solo se \
                        añade aquí, el borrado seguirá dejando filas y nadie se enterará.""")
                .containsExactlyInAnyOrderElementsOf(TABLAS_CONOCIDAS);
    }

    // --- El borrado ---------------------------------------------------------

    @Test
    @DisplayName("tras borrar la cuenta no queda ni una fila suya en ninguna tabla")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void borrar_la_cuenta_no_deja_ni_una_fila() throws Exception {
        borrarCuenta(PASSWORD).andExpect(status().isOk());

        // Tablas con usuario_id: se recorren desde la lista, no una a una a mano, para que
        // añadir una tabla nueva a TABLAS_CONOCIDAS la meta también en esta comprobación.
        for (String tabla : TABLAS_CONOCIDAS) {
            assertThat(contar("SELECT COUNT(*) FROM " + tabla + " WHERE usuario_id = " + owner.getId()))
                    .as("quedan filas del usuario en %s", tabla)
                    .isZero();
        }

        // Tablas sin usuario_id propio: cuelgan de algo suyo y se comprueban por el padre.
        assertThat(contar("SELECT COUNT(*) FROM alimentos_comida WHERE comida_id = " + comidaId))
                .as("líneas de su comida").isZero();
        assertThat(contar("SELECT COUNT(*) FROM rutina_ejercicio WHERE rutina_id = " + rutinaId))
                .as("ejercicios de su rutina").isZero();
        assertThat(contar("SELECT COUNT(*) FROM ejercicios_realizados WHERE sesion_id = " + sesionId))
                .as("ejercicios de su sesión").isZero();
        assertThat(contar("SELECT COUNT(*) FROM series_realizadas WHERE ejercicio_realizado_id = "
                + ejercicioRealizadoId)).as("series de su ejercicio").isZero();

        // Y el usuario.
        assertThat(contar("SELECT COUNT(*) FROM usuarios WHERE id = " + owner.getId()))
                .as("el propio usuario").isZero();
    }

    @Test
    @DisplayName("sus alimentos personalizados se borran, NO se quedan publicados como catálogo")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void los_alimentos_personalizados_no_acaban_en_el_catalogo() throws Exception {
        borrarCuenta(PASSWORD).andExpect(status().isOk());

        // La FK dice ON DELETE SET NULL, y en esta tabla usuario_id NULL significa
        // «catálogo público». Dejar actuar a la cascada publicaría la dieta de quien se va.
        assertThat(contar("SELECT COUNT(*) FROM alimentos WHERE id = " + alimentoPersonalId))
                .as("el alimento personal sigue existiendo (¿publicado como catálogo?)")
                .isZero();
    }

    @Test
    @DisplayName("las plantillas del sistema (usuario_id NULL) no se tocan")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void las_plantillas_del_sistema_sobreviven() throws Exception {
        borrarCuenta(PASSWORD).andExpect(status().isOk());

        assertThat(contar("SELECT COUNT(*) FROM rutinas WHERE id = " + plantillaId))
                .as("la plantilla del sistema").isOne();
        assertThat(contar("SELECT COUNT(*) FROM rutina_ejercicio WHERE rutina_id = " + plantillaId))
                .as("los ejercicios de la plantilla").isOne();
    }

    @Test
    @DisplayName("las filas cruzadas de otros usuarios se resuelven sin borrarles la sesión")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void las_filas_cruzadas_se_resuelven() throws Exception {
        borrarCuenta(PASSWORD).andExpect(status().isOk());

        // La línea ajena que usaba su alimento se va con él, y la comida del otro sigue ahí
        // con sus totales recalculados.
        assertThat(contar("SELECT COUNT(*) FROM alimentos_comida WHERE comida_id = " + comidaAjenaId))
                .as("línea ajena que referenciaba su alimento").isZero();
        assertThat(contar("SELECT COUNT(*) FROM comidas WHERE id = " + comidaAjenaId))
                .as("la comida del otro usuario NO se borra").isOne();
        assertThat(contar("SELECT COUNT(*) FROM comidas WHERE id = " + comidaAjenaId
                + " AND total_calorias = 0")).as("sus totales se recalculan").isOne();

        // La sesión ajena se conserva entera: solo pierde el enlace a una rutina que ya no existe.
        assertThat(contar("SELECT COUNT(*) FROM sesiones_entrenamiento WHERE id = " + sesionAjenaId))
                .as("la sesión del otro usuario NO se borra").isOne();
        assertThat(contar("SELECT COUNT(*) FROM sesiones_entrenamiento WHERE id = " + sesionAjenaId
                + " AND rutina_id IS NULL")).as("y queda desvinculada de la rutina").isOne();
    }

    // --- Reautenticación ----------------------------------------------------

    @Test
    @DisplayName("con la contraseña equivocada no se borra nada")
    @WithUserDetails(value = OWNER, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void sin_la_contrasena_correcta_no_se_borra_nada() throws Exception {
        borrarCuenta("NoEsLaSuya1!").andExpect(status().isForbidden());

        // Ni la cuenta ni un solo dato: la comprobación va antes de tocar la primera tabla.
        assertThat(contar("SELECT COUNT(*) FROM usuarios WHERE id = " + owner.getId())).isOne();
        assertThat(contar("SELECT COUNT(*) FROM comidas WHERE id = " + comidaId)).isOne();
        assertThat(contar("SELECT COUNT(*) FROM series_realizadas WHERE ejercicio_realizado_id = "
                + ejercicioRealizadoId)).isOne();
    }

    // --- Andamiaje ----------------------------------------------------------

    private org.springframework.test.web.servlet.ResultActions borrarCuenta(String password) throws Exception {
        return mockMvc.perform(delete("/usuarios/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EliminarCuentaDTO(password))));
    }

    private Integer crearLogro() {
        return insertar("""
                INSERT INTO logros (nombre, descripcion, tipo)
                VALUES ('Logro de prueba', 'Descripción', 'SESIONES')""");
    }

    private void ejecutar(String sql) {
        em.createNativeQuery(sql).executeUpdate();
    }

    // Inserta y devuelve el id generado.
    private Integer insertar(String sql) {
        ejecutar(sql);
        return ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).intValue();
    }

    // Cuenta filas saltándose el contexto de persistencia: lo que importa es lo que hay
    // en la base de datos, no lo que Hibernate crea recordar.
    private long contar(String sql) {
        em.flush();
        Object resultado = em.createNativeQuery(sql).getSingleResult();
        return resultado instanceof BigInteger grande ? grande.longValue() : ((Number) resultado).longValue();
    }
}
