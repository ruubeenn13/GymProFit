package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// VolumenSemanalTest — GET /sesiones/usuario/{id}/volumen-semanal (GP-216, lote 1)
//
// «Tu progreso» de Inicio dibuja los kilos movidos por semana. La lista de sesiones no
// trae kilos y pedirlos de uno en uno son tantas llamadas como sesiones, así que los suma
// el servidor con la definición de GP-011 (serie a serie y, en las sesiones anteriores al
// registro por serie, el resumen del ejercicio). Como el lunes de «Esta semana», el inicio
// lo manda la app en su hora: la API no sabe el huso del usuario.
// Con el contexto levantado y JWT de verdad (DEC-014); el id va en la ruta, así que
// también tiene su fila en SesionOwnershipTest.
// ============================================================
@DisplayName("GET /sesiones/usuario/{id}/volumen-semanal — kilos por semana")
class VolumenSemanalTest extends AbstractOwnershipTest {

    private static final String LUNES = "2026-09-14T00:00:00";

    @Autowired
    private JdbcTemplate jdbc;

    private Integer press;

    @BeforeEach
    void catalogo() {
        press = crearEjercicioCatalogo().getId();
    }

    private JsonNode semanas(Usuario quien, String query) throws Exception {
        String json = pedir(quien, "GET /sesiones/usuario/" + quien.getId() + "/volumen-semanal?" + query)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    @Test
    @DisplayName("una entrada por semana, en orden, con ceros donde no se entrenó")
    void kilos_por_semana() throws Exception {
        guardar(owner, "2026-09-15T18:00:00", press, 60, 8);   // semana 1: 480
        guardar(owner, "2026-09-17T18:00:00", press, 70, 5);   // semana 1: +350
        guardar(owner, "2026-09-30T18:00:00", press, 80, 10);  // semana 3: 800

        JsonNode r = semanas(owner, "desde=" + LUNES + "&semanas=3");

        assertThat(r).hasSize(3);
        assertThat(r.get(0).get("inicio").asText()).startsWith("2026-09-14");
        assertThat(r.get(0).get("volumenKg").asDouble()).isEqualTo(830);
        assertThat(r.get(0).get("sesiones").asInt()).isEqualTo(2);
        assertThat(r.get(1).get("inicio").asText()).startsWith("2026-09-21");
        assertThat(r.get(1).get("volumenKg").asDouble()).isZero();
        assertThat(r.get(1).get("sesiones").asInt()).isZero();
        assertThat(r.get(2).get("inicio").asText()).startsWith("2026-09-28");
        assertThat(r.get(2).get("volumenKg").asDouble()).isEqualTo(800);
    }

    @Test
    @DisplayName("lo anterior al rango, lo posterior y lo sin terminar no cuentan; el lunes a las 00:00 sí")
    void limites() throws Exception {
        guardar(owner, "2026-09-13T23:59:00", press, 100, 10);  // domingo anterior: fuera
        guardar(owner, LUNES, press, 50, 10);                   // el lunes a las 00:00: dentro
        guardar(owner, "2026-09-21T00:00:00", press, 40, 10);   // semana 2
        guardar(owner, "2026-09-28T00:00:00", press, 100, 10);  // pasado el rango: fuera
        int sin = guardar(owner, "2026-09-15T10:00:00", press, 200, 10);
        jdbc.update("UPDATE sesiones_entrenamiento SET completada = FALSE WHERE id = ?", sin);

        JsonNode r = semanas(owner, "desde=" + LUNES + "&semanas=2");

        assertThat(r).hasSize(2);
        assertThat(r.get(0).get("volumenKg").asDouble()).isEqualTo(500);
        assertThat(r.get(0).get("sesiones").asInt()).isEqualTo(1);
        assertThat(r.get(1).get("volumenKg").asDouble()).isEqualTo(400);
    }

    @Test
    @DisplayName("una sesión anterior al registro por serie cae al resumen por ejercicio, como GP-011")
    void sesion_antigua_sin_series() throws Exception {
        int s = guardar(owner, "2026-09-16T18:00:00", press, 1, 1);
        jdbc.update("DELETE FROM series_realizadas WHERE ejercicio_realizado_id IN "
                + "(SELECT id FROM ejercicios_realizados WHERE sesion_id = ?)", s);
        jdbc.update("UPDATE ejercicios_realizados SET series_completadas = 3, repeticiones_rutinas = 10, "
                + "peso_usado = 50 WHERE sesion_id = ?", s);

        JsonNode r = semanas(owner, "desde=" + LUNES + "&semanas=1");

        assertThat(r.get(0).get("volumenKg").asDouble()).isEqualTo(1500);
    }

    @Test
    @DisplayName("aislamiento: lo de otro usuario no entra en mis semanas")
    void aislamiento() throws Exception {
        guardar(attacker, "2026-09-15T18:00:00", press, 200, 10);
        guardar(owner, "2026-09-15T18:00:00", press, 60, 8);

        assertThat(semanas(owner, "desde=" + LUNES + "&semanas=1").get(0).get("volumenKg").asDouble())
                .isEqualTo(480);
        assertThat(semanas(attacker, "desde=" + LUNES + "&semanas=1").get(0).get("volumenKg").asDouble())
                .isEqualTo(2000);
    }

    @Test
    @DisplayName("sin sesiones: las semanas pedidas, a cero (200 y no 404)")
    void vacio() throws Exception {
        JsonNode r = semanas(owner, "desde=" + LUNES + "&semanas=4");

        assertThat(r).hasSize(4);
        r.forEach(s -> assertThat(s.get("volumenKg").asDouble()).isZero());
    }

    @Test
    @DisplayName("el usuario ajeno → 403; semanas fuera de 1 a 12 → 400")
    void ajeno_y_limites() throws Exception {
        pedir(attacker, "GET /sesiones/usuario/" + owner.getId() + "/volumen-semanal?desde=" + LUNES + "&semanas=6")
                .andExpect(status().isForbidden());
        pedir(owner, "GET /sesiones/usuario/" + owner.getId() + "/volumen-semanal?desde=" + LUNES + "&semanas=0")
                .andExpect(status().isBadRequest());
        pedir(owner, "GET /sesiones/usuario/" + owner.getId() + "/volumen-semanal?desde=" + LUNES + "&semanas=13")
                .andExpect(status().isBadRequest());
    }

    // --- Siembra ------------------------------------------------------------

    private int guardar(Usuario quien, String inicio, Integer ejercicio, double peso, int repeticiones) throws Exception {
        String cuerpo = """
                {"claveIdempotencia":"%s","fechaInicio":"%s","duracionMinutos":50,"completada":true,
                 "ejercicios":[{"ejercicioId":%d,"series":[{"numero":1,"repeticiones":%d,"peso":%s,"completada":true}]}]}
                """.formatted(UUID.randomUUID(), inicio, ejercicio, repeticiones, peso);
        String json = pedir(quien, "POST /sesiones/completa", cuerpo).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asInt();
    }
}
