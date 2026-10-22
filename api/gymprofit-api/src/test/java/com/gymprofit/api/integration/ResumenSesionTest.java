package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// ResumenSesionTest — GET /sesiones/{id}/resumen (GP-210 paso 1, GP-023 y GP-011)
//
// El resumen del entreno sale del servidor, como el volumen de GP-011: kilos y series por
// ejercicio, lo de la última vez de cada ejercicio y la sesión anterior de la misma rutina.
// Los kilos usan la definición de GP-011: peso × repeticiones de las series sin segundos
// y, en las sesiones anteriores al registro por serie, el resumen del ejercicio.
// Con el contexto levantado y JWT de verdad (DEC-014). La sesión es de un dueño: el
// ajeno recibe 403, igual que en el resto de /sesiones (su fila está también en
// SesionOwnershipTest).
// ============================================================
@DisplayName("GET /sesiones/{id}/resumen — el resumen del entreno")
class ResumenSesionTest extends AbstractOwnershipTest {

    @Autowired
    private JdbcTemplate jdbc;

    private Integer press;
    private Integer sentadilla;
    private Integer plancha;
    private Integer rutina;

    @BeforeEach
    void catalogo() {
        press = crearEjercicioCatalogo().getId();
        sentadilla = crearEjercicioCatalogo().getId();
        plancha = crearEjercicioCatalogo().getId();
        rutina = insertarRutina(owner, "Fuerza A");
    }

    private JsonNode resumen(Usuario quien, int sesion) throws Exception {
        String json = pedir(quien, "GET /sesiones/" + sesion + "/resumen").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private JsonNode ejercicio(JsonNode resumen, Integer id) {
        for (JsonNode e : resumen.get("ejercicios")) {
            if (e.get("ejercicioId").asInt() == id) return e;
        }
        throw new AssertionError("el resumen no trae el ejercicio " + id);
    }

    @Test
    @DisplayName("kilos y series por ejercicio; el de tiempo no suma kilos y se marca")
    void kilos_y_series() throws Exception {
        int s = guardar(owner, "2026-09-21T18:00:00", rutina,
                ejercicio(press, serie(60, 8), serie(65, 8), serie(70, 5)),
                ejercicio(plancha, tiempo(60), tiempo(45)));

        JsonNode r = resumen(owner, s);

        assertThat(r.get("sesionId").asInt()).isEqualTo(s);
        assertThat(r.get("volumenKg").asDouble()).isEqualTo(60 * 8 + 65 * 8 + 70 * 5);
        assertThat(r.get("series").asInt()).isEqualTo(5);
        JsonNode p = ejercicio(r, press);
        assertThat(p.get("volumenKg").asDouble()).isEqualTo(1350);
        assertThat(p.get("series").asInt()).isEqualTo(3);
        assertThat(p.get("porTiempo").asBoolean()).isFalse();
        JsonNode pl = ejercicio(r, plancha);
        assertThat(pl.get("volumenKg").asDouble()).isZero();
        assertThat(pl.get("series").asInt()).isEqualTo(2);
        assertThat(pl.get("porTiempo").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("la primera vez no trae comparaciones: ni sesión anterior ni última vez")
    void primera_vez() throws Exception {
        int s = guardar(owner, "2026-09-21T18:00:00", rutina, ejercicio(press, serie(60, 8)));

        JsonNode r = resumen(owner, s);

        assertThat(r.get("anterior").isNull()).isTrue();
        assertThat(ejercicio(r, press).get("anterior").isNull()).isTrue();
    }

    @Test
    @DisplayName("compara con la sesión anterior de la MISMA rutina y con la última vez de cada ejercicio")
    void compara() throws Exception {
        int a = guardar(owner, "2026-09-17T18:00:00", rutina,
                ejercicio(press, serie(60, 8), serie(60, 8)),
                ejercicio(sentadilla, serie(100, 5)));
        int b = guardar(owner, "2026-09-21T18:00:00", rutina,
                ejercicio(press, serie(65, 8), serie(65, 8)),
                ejercicio(sentadilla, serie(100, 5)));

        JsonNode r = resumen(owner, b);

        assertThat(r.get("anterior").get("sesionId").asInt()).isEqualTo(a);
        assertThat(r.get("anterior").get("volumenKg").asDouble()).isEqualTo(960 + 500);
        assertThat(r.get("anterior").get("fecha").asText()).startsWith("2026-09-17T18:00");
        JsonNode p = ejercicio(r, press);
        assertThat(p.get("anterior").get("volumenKg").asDouble()).isEqualTo(960);
        assertThat(p.get("anterior").get("fecha").asText()).startsWith("2026-09-17T18:00");
        assertThat(ejercicio(r, sentadilla).get("anterior").get("volumenKg").asDouble()).isEqualTo(500);
    }

    @Test
    @DisplayName("abrir una sesión vieja compara con lo suyo de entonces, no con lo que vino después")
    void sesion_vieja() throws Exception {
        int a = guardar(owner, "2026-09-17T18:00:00", rutina, ejercicio(press, serie(60, 8)));
        guardar(owner, "2026-09-21T18:00:00", rutina, ejercicio(press, serie(65, 8)));

        JsonNode r = resumen(owner, a);

        assertThat(r.get("anterior").isNull()).isTrue();
        assertThat(ejercicio(r, press).get("anterior").isNull()).isTrue();
    }

    @Test
    @DisplayName("otra rutina o una sesión sin terminar no son «la sesión anterior»")
    void anterior_solo_de_la_rutina_y_terminada() throws Exception {
        Integer otra = insertarRutina(owner, "Pierna B");
        guardar(owner, "2026-09-15T18:00:00", otra, ejercicio(press, serie(50, 8)));
        sembrarSesionSinTerminar(owner, "2026-09-18T18:00:00", rutina);
        int s = guardar(owner, "2026-09-21T18:00:00", rutina, ejercicio(press, serie(65, 8)));

        JsonNode r = resumen(owner, s);

        // La sesión de OTRA rutina no es la anterior de esta, pero el press sí se hizo antes.
        assertThat(r.get("anterior").isNull()).isTrue();
        assertThat(ejercicio(r, press).get("anterior").get("volumenKg").asDouble()).isEqualTo(400);
    }

    @Test
    @DisplayName("un entrenamiento libre no tiene sesión anterior, pero cada ejercicio sí su última vez")
    void libre() throws Exception {
        guardar(owner, "2026-09-17T18:00:00", null, ejercicio(press, serie(60, 8)));
        int s = guardar(owner, "2026-09-21T18:00:00", null, ejercicio(press, serie(65, 8)));

        JsonNode r = resumen(owner, s);

        assertThat(r.get("anterior").isNull()).isTrue();
        assertThat(ejercicio(r, press).get("anterior").get("volumenKg").asDouble()).isEqualTo(480);
    }

    @Test
    @DisplayName("una sesión anterior al registro por serie cae al resumen por ejercicio, como GP-011")
    void sesion_antigua_sin_series() throws Exception {
        int s = sembrarSesionSinTerminar(owner, "2026-08-01T18:00:00", rutina);
        jdbc.update("UPDATE sesiones_entrenamiento SET completada = TRUE WHERE id = ?", s);
        jdbc.update("INSERT INTO ejercicios_realizados (series_completadas, repeticiones_rutinas, peso_usado, "
                + "sesion_id, ejercicio_id) VALUES (3, 10, 50, ?, ?)", s, press);

        JsonNode r = resumen(owner, s);

        assertThat(r.get("volumenKg").asDouble()).isEqualTo(1500);
        assertThat(r.get("series").asInt()).isEqualTo(3);
        assertThat(ejercicio(r, press).get("volumenKg").asDouble()).isEqualTo(1500);
    }

    @Test
    @DisplayName("la sesión de otro → 403; una que no existe → 404; lo del otro no se cuela en las comparaciones")
    void ajeno_y_aislamiento() throws Exception {
        guardar(attacker, "2026-09-17T18:00:00", null, ejercicio(press, serie(200, 10)));
        int s = guardar(owner, "2026-09-21T18:00:00", rutina, ejercicio(press, serie(65, 8)));

        pedir(attacker, "GET /sesiones/" + s + "/resumen").andExpect(status().isForbidden());
        pedir(guest, "GET /sesiones/" + s + "/resumen").andExpect(status().isForbidden());
        pedir(owner, "GET /sesiones/99999999/resumen").andExpect(status().isNotFound());
        assertThat(ejercicio(resumen(owner, s), press).get("anterior").isNull()).isTrue();
    }

    // --- Siembra ------------------------------------------------------------

    private String serie(double peso, int repeticiones) {
        return "{\"repeticiones\":%d,\"peso\":%s,\"completada\":true}".formatted(repeticiones, peso);
    }

    private String tiempo(int segundos) {
        return "{\"repeticiones\":0,\"segundos\":%d,\"completada\":true}".formatted(segundos);
    }

    private String ejercicio(Integer id, String... series) {
        StringBuilder lista = new StringBuilder();
        for (int i = 0; i < series.length; i++) {
            if (i > 0) lista.append(',');
            lista.append(series[i], 0, series[i].length() - 1).append(",\"numero\":").append(i + 1).append('}');
        }
        return "{\"ejercicioId\":%d,\"series\":[%s]}".formatted(id, lista);
    }

    private int guardar(Usuario quien, String inicio, Integer rutinaId, String... ejercicios) throws Exception {
        String cuerpo = """
                {"claveIdempotencia":"%s","fechaInicio":"%s","duracionMinutos":50,"completada":true,%s
                 "ejercicios":[%s]}
                """.formatted(UUID.randomUUID(), inicio,
                rutinaId == null ? "" : "\"rutinaId\":" + rutinaId + ",", String.join(",", ejercicios));
        String json = pedir(quien, "POST /sesiones/completa", cuerpo).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asInt();
    }

    private Integer insertarRutina(Usuario quien, String nombre) {
        jdbc.update("INSERT INTO rutinas (nombre, nivel, usuario_id) VALUES (?, 'PRINCIPIANTE', ?)", nombre, quien.getId());
        return jdbc.queryForObject("SELECT MAX(id) FROM rutinas WHERE usuario_id = ?", Integer.class, quien.getId());
    }

    private int sembrarSesionSinTerminar(Usuario quien, String inicio, Integer rutinaId) {
        LocalDateTime t = LocalDateTime.parse(inicio);
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement("INSERT INTO sesiones_entrenamiento (usuario_id, fecha_inicio, "
                    + "fecha_fin, duracion_minutos, completada, rutina_id) VALUES (?, ?, ?, 45, FALSE, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, quien.getId());
            ps.setTimestamp(2, Timestamp.valueOf(t));
            ps.setTimestamp(3, Timestamp.valueOf(t.plusMinutes(45)));
            ps.setObject(4, rutinaId);
            return ps;
        }, llave);
        return llave.getKey().intValue();
    }
}
