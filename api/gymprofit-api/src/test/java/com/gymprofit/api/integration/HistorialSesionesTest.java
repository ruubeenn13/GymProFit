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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// HistorialSesionesTest — GET /sesiones/historial (GP-232)
//
// Las tarjetas de sesión del Historial, de Inicio y de Progreso salen de una sola
// petición por página de semanas: kilos, series, ejercicios, récords batidos ese día,
// la anterior de la misma rutina, las series por músculo y las barras de sus últimas 6
// veces con esa rutina. Con el contexto levantado y JWT de verdad (DEC-014): el usuario
// sale del token, así que no hay id ajeno que rechazar y lo que se prueba es el
// aislamiento (DEC-027): lo de otro usuario, aunque esté en la base, no sale.
// ============================================================
@DisplayName("GET /sesiones/historial — las tarjetas de sesión, por semanas")
class HistorialSesionesTest extends AbstractOwnershipTest {

    @Autowired
    private JdbcTemplate jdbc;

    private Integer press;
    private Integer sentadilla;
    private Integer rutina;

    @BeforeEach
    void catalogo() {
        press = crearEjercicioCatalogo().getId();
        sentadilla = crearEjercicioCatalogo().getId();
        jdbc.update("UPDATE ejercicios SET musculo_primario = 'Pecho' WHERE id = ?", press);
        jdbc.update("UPDATE ejercicios SET musculo_primario = 'Cuádriceps' WHERE id = ?", sentadilla);
        rutina = insertarRutina(owner, "Pierna A");
    }

    // --- Aislamiento --------------------------------------------------------

    @Test
    @DisplayName("solo salen las sesiones del usuario del token: las de otro, en la base, no")
    void aislamiento() throws Exception {
        int ajena = guardar(attacker, "2026-11-03T18:00:00", null, ejercicio(press, serie(200, 10)));
        int propia = guardar(owner, "2026-11-03T19:00:00", rutina, ejercicio(press, serie(60, 8)));

        JsonNode delDueno = historial(owner, null);
        assertThat(idsDe(delDueno)).containsExactly(propia);

        JsonNode delOtro = historial(attacker, null);
        assertThat(idsDe(delOtro)).containsExactly(ajena);

        pedir(guest, "GET /sesiones/historial").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("sin sesiones: ninguna semana y sin página siguiente")
    void vacio() throws Exception {
        guardar(attacker, "2026-11-03T18:00:00", null, ejercicio(press, serie(200, 10)));

        JsonNode h = historial(owner, null);

        assertThat(h.get("semanas")).isEmpty();
        assertThat(h.get("siguiente").isNull()).isTrue();
    }

    // --- Lo que lleva cada sesión ------------------------------------------

    @Test
    @DisplayName("kilos, series, ejercicios, minutos, músculos y la anterior de la misma rutina")
    void campos_de_la_sesion() throws Exception {
        guardar(owner, "2026-10-27T18:00:00", rutina, ejercicio(press, serie(50, 10)));
        int s = guardar(owner, "2026-11-03T18:05:00", rutina,
                ejercicio(press, serie(60, 8), serie(60, 8)),
                ejercicio(sentadilla, serie(100, 5), serie(100, 5), serie(100, 5)));

        JsonNode x = sesion(historial(owner, null), s);

        assertThat(x.get("rutinaId").asInt()).isEqualTo(rutina);
        assertThat(x.get("rutinaNombre").asText()).isEqualTo("Pierna A");
        assertThat(x.get("fechaInicio").asText()).startsWith("2026-11-03T18:05");
        assertThat(x.get("duracionMinutos").asInt()).isEqualTo(50);
        assertThat(x.get("kilos").asDouble()).isEqualTo(960 + 1500);
        assertThat(x.get("series").asInt()).isEqualTo(5);
        assertThat(x.get("ejercicios").asInt()).isEqualTo(2);
        assertThat(x.get("kilosAnterior").asDouble()).isEqualTo(500);
        // De más a menos series: tres de cuádriceps antes que dos de pecho.
        assertThat(x.get("musculos").get(0).get("musculo").asText()).isEqualTo("cuadriceps");
        assertThat(x.get("musculos").get(0).get("series").asInt()).isEqualTo(3);
        assertThat(x.get("musculos").get(1).get("musculo").asText()).isEqualTo("pecho");
        assertThat(x.get("musculos").get(1).get("series").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("un entrenamiento libre no tiene barras ni anterior; una sesión sin ejercicios, 0 kilos")
    void libre_y_sin_ejercicios() throws Exception {
        guardar(owner, "2026-11-02T10:00:00", null, ejercicio(press, serie(40, 10)));
        int libre = guardar(owner, "2026-11-03T10:00:00", null, ejercicio(press, serie(50, 10)));
        int vacia = guardar(owner, "2026-11-04T10:00:00", rutina);

        JsonNode h = historial(owner, null);

        JsonNode l = sesion(h, libre);
        assertThat(l.get("rutinaId").isNull()).isTrue();
        assertThat(l.get("barras")).isEmpty();
        assertThat(l.get("kilosAnterior").isNull()).isTrue();
        JsonNode v = sesion(h, vacia);
        assertThat(v.get("ejercicios").asInt()).isZero();
        assertThat(v.get("kilos").asDouble()).isZero();
        assertThat(v.get("musculos")).isEmpty();
    }

    @Test
    @DisplayName("una sesión sin terminar no sale")
    void sin_terminar() throws Exception {
        sembrarSesionSinTerminar(owner, "2026-11-03T18:00:00", rutina);

        assertThat(historial(owner, null).get("semanas")).isEmpty();
    }

    // --- Récords -----------------------------------------------------------

    @Test
    @DisplayName("un récord superado después sigue contando en su sesión; la primera marca no es récord")
    void record_superado_sigue_en_su_sesion() throws Exception {
        int a = guardar(owner, "2026-10-20T18:00:00", rutina, ejercicio(press, serie(60, 8)));
        int b = guardar(owner, "2026-10-27T18:00:00", rutina, ejercicio(press, serie(70, 8)));
        int c = guardar(owner, "2026-11-03T18:00:00", rutina, ejercicio(press, serie(80, 8)));

        JsonNode h = historial(owner, null);

        assertThat(sesion(h, a).get("records").asInt()).isZero();
        assertThat(sesion(h, b).get("records").asInt()).isEqualTo(1);
        assertThat(sesion(h, c).get("records").asInt()).isEqualTo(1);
        // Y las barras de la última lo dicen igual: la primera sin récord, las otras dos con él.
        JsonNode barras = sesion(h, c).get("barras");
        assertThat(barras).hasSize(3);
        assertThat(barras.get(0).get("sesionId").asInt()).isEqualTo(a);
        assertThat(barras.get(0).get("record").asBoolean()).isFalse();
        assertThat(barras.get(1).get("record").asBoolean()).isTrue();
        assertThat(barras.get(2).get("sesionId").asInt()).isEqualTo(c);
        assertThat(barras.get(2).get("record").asBoolean()).isTrue();
        assertThat(barras.get(2).get("kilos").asDouble()).isEqualTo(640);
    }

    // --- Semanas y páginas ------------------------------------------------

    @Test
    @DisplayName("agrupa por lunes, con entrenos y kilos de la semana, de la más nueva a la más vieja")
    void semanas() throws Exception {
        int lunes = guardar(owner, "2026-11-02T19:10:00", rutina, ejercicio(press, serie(50, 10)));
        int martes = guardar(owner, "2026-11-03T18:05:00", rutina, ejercicio(press, serie(60, 10)));
        int domingoAnterior = guardar(owner, "2026-11-01T23:30:00", rutina, ejercicio(press, serie(40, 10)));

        JsonNode h = historial(owner, null);

        assertThat(h.get("semanas")).hasSize(2);
        JsonNode esta = h.get("semanas").get(0);
        assertThat(esta.get("lunes").asText()).isEqualTo("2026-11-02");
        assertThat(esta.get("entrenos").asInt()).isEqualTo(2);
        assertThat(esta.get("kilos").asDouble()).isEqualTo(1100);
        assertThat(esta.get("sesiones").get(0).get("id").asInt()).isEqualTo(martes);
        assertThat(esta.get("sesiones").get(1).get("id").asInt()).isEqualTo(lunes);
        JsonNode pasada = h.get("semanas").get(1);
        assertThat(pasada.get("lunes").asText()).isEqualTo("2026-10-26");
        assertThat(pasada.get("sesiones").get(0).get("id").asInt()).isEqualTo(domingoAnterior);
    }

    @Test
    @DisplayName("hasta 8 semanas por página; el cursor lleva a la siguiente y ninguna semana se parte")
    void paginas() throws Exception {
        List<Integer> todas = new ArrayList<>();
        // Diez semanas seguidas con dos sesiones cada una, del lunes 31 de agosto al 2 de noviembre.
        LocalDateTime lunes = LocalDateTime.parse("2026-08-31T18:00:00");
        for (int i = 0; i < 10; i++) {
            todas.add(guardar(owner, lunes.plusWeeks(i).toString(), rutina, ejercicio(press, serie(50, 10))));
            todas.add(guardar(owner, lunes.plusWeeks(i).plusDays(2).toString(), rutina, ejercicio(press, serie(50, 10))));
        }

        JsonNode p1 = historial(owner, null);
        assertThat(p1.get("semanas")).hasSize(8);
        assertThat(p1.get("semanas").get(0).get("lunes").asText()).isEqualTo("2026-11-02");
        for (JsonNode w : p1.get("semanas")) assertThat(w.get("sesiones")).hasSize(2);
        String cursor = p1.get("siguiente").asText();
        assertThat(cursor).isEqualTo("2026-09-14");

        JsonNode p2 = historial(owner, cursor);
        assertThat(p2.get("semanas")).hasSize(2);
        assertThat(p2.get("siguiente").isNull()).isTrue();

        Set<Integer> vistas = new HashSet<>(idsDe(p1));
        vistas.addAll(idsDe(p2));
        assertThat(vistas).containsExactlyInAnyOrderElementsOf(todas);
    }

    @Test
    @DisplayName("la sesión más antigua de una página trae sus barras y su anterior de páginas siguientes")
    void barras_de_otras_paginas() throws Exception {
        LocalDateTime lunes = LocalDateTime.parse("2026-08-31T18:00:00");
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(guardar(owner, lunes.plusWeeks(i).toString(), rutina, ejercicio(press, serie(50 + i, 10))));
        }

        JsonNode p1 = historial(owner, null);
        JsonNode semanas = p1.get("semanas");
        JsonNode masAntigua = semanas.get(semanas.size() - 1).get("sesiones").get(0);
        // La octava semana empezando por la de hoy es la tercera de las diez (índice 2).
        assertThat(masAntigua.get("id").asInt()).isEqualTo(ids.get(2));

        JsonNode barras = masAntigua.get("barras");
        assertThat(barras).hasSize(3);
        assertThat(barras.get(0).get("sesionId").asInt()).isEqualTo(ids.get(0));
        assertThat(barras.get(0).get("kilos").asDouble()).isEqualTo(500);
        assertThat(barras.get(2).get("sesionId").asInt()).isEqualTo(ids.get(2));
        assertThat(masAntigua.get("kilosAnterior").asDouble()).isEqualTo(510);

        // Y la de hoy, como mucho seis, de la más vieja a ella.
        JsonNode hoy = semanas.get(0).get("sesiones").get(0);
        assertThat(hoy.get("barras")).hasSize(6);
        assertThat(hoy.get("barras").get(0).get("sesionId").asInt()).isEqualTo(ids.get(4));
        assertThat(hoy.get("barras").get(5).get("sesionId").asInt()).isEqualTo(ids.get(9));
    }

    // --- Rutinas activas: musculos y ultimas ---------------------------------

    @Test
    @DisplayName("/rutinas/usuario/{id}/activas trae los músculos y las 6 últimas veces de cada rutina")
    void rutinas_activas() throws Exception {
        jdbc.update("INSERT INTO rutina_ejercicio (series, repeticiones, orden, rutina_id, ejercicio_id) "
                + "VALUES (2, 10, 1, ?, ?), (4, 8, 2, ?, ?)", rutina, press, rutina, sentadilla);
        Integer sinHacer = insertarRutina(owner, "Pierna en casa");
        List<Integer> ids = new ArrayList<>();
        LocalDateTime lunes = LocalDateTime.parse("2026-09-21T18:00:00");
        for (int i = 0; i < 7; i++) {
            ids.add(guardar(owner, lunes.plusWeeks(i).toString(), rutina, ejercicio(press, serie(50 + i * 10, 8))));
        }
        // Lo de otro usuario no cuenta, aunque esté en la base.
        guardar(attacker, "2026-11-04T18:00:00", null, ejercicio(press, serie(200, 10)));

        String json = pedir(owner, "GET /rutinas/usuario/" + owner.getId() + "/activas")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode lista = objectMapper.readTree(json);

        JsonNode r = rutinaDe(lista, rutina);
        assertThat(r.get("musculos").get(0).get("musculo").asText()).isEqualTo("cuadriceps");
        assertThat(r.get("musculos").get(0).get("series").asInt()).isEqualTo(4);
        assertThat(r.get("musculos").get(1).get("musculo").asText()).isEqualTo("pecho");
        JsonNode ultimas = r.get("ultimas");
        assertThat(ultimas).hasSize(6);
        assertThat(ultimas.get(0).get("sesionId").asInt()).isEqualTo(ids.get(1));
        assertThat(ultimas.get(5).get("sesionId").asInt()).isEqualTo(ids.get(6));
        assertThat(ultimas.get(5).get("fecha").asText()).startsWith("2026-11-02T18:00");
        assertThat(ultimas.get(5).get("kilos").asDouble()).isEqualTo(110 * 8);
        assertThat(ultimas.get(5).get("record").asBoolean()).isTrue();

        JsonNode nunca = rutinaDe(lista, sinHacer);
        assertThat(nunca.get("ultimas")).isEmpty();
        assertThat(nunca.get("musculos")).isEmpty();

        // Solo en esa ruta: la ficha de la rutina sigue como estaba.
        String ficha = pedir(owner, "GET /rutinas/" + rutina).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(ficha).has("ultimas")).isFalse();
        assertThat(objectMapper.readTree(ficha).has("musculos")).isFalse();
    }

    // --- Ayudas -------------------------------------------------------------

    private JsonNode historial(Usuario quien, String antes) throws Exception {
        String ruta = "GET /sesiones/historial" + (antes == null ? "" : "?antes=" + antes);
        String json = pedir(quien, ruta).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private static List<Integer> idsDe(JsonNode historial) {
        List<Integer> ids = new ArrayList<>();
        for (JsonNode w : historial.get("semanas")) {
            for (JsonNode s : w.get("sesiones")) ids.add(s.get("id").asInt());
        }
        return ids;
    }

    private static JsonNode sesion(JsonNode historial, int id) {
        for (JsonNode w : historial.get("semanas")) {
            for (JsonNode s : w.get("sesiones")) if (s.get("id").asInt() == id) return s;
        }
        throw new AssertionError("el historial no trae la sesión " + id);
    }

    private static JsonNode rutinaDe(JsonNode lista, Integer id) {
        for (JsonNode r : lista) if (r.get("id").asInt() == id) return r;
        throw new AssertionError("la lista no trae la rutina " + id);
    }

    private String serie(double peso, int repeticiones) {
        return "{\"repeticiones\":%d,\"peso\":%s,\"completada\":true}".formatted(repeticiones, peso);
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
        jdbc.update("INSERT INTO rutinas (nombre, nivel, usuario_id, activa) VALUES (?, 'PRINCIPIANTE', ?, TRUE)",
                nombre, quien.getId());
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
