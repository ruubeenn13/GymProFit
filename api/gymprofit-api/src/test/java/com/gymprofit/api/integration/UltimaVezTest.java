package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// UltimaVezTest — GET /sesiones/ultima-vez (GP-014), con el contexto completo y
// JWT real. Es lo que la sesión en vivo enseña como «Anterior» y como pista en
// cada campo, así que lo que se comprueba es de qué sesión salen esas series:
// la terminada más reciente que tenga series de ese ejercicio, del usuario del
// token y de nadie más. Los ids son del catálogo público (DEC-027): lo que se
// afirma sobre el otro usuario es aislamiento, no un 403.
// ============================================================
@DisplayName("Última vez de cada ejercicio (GP-014)")
class UltimaVezTest extends AbstractOwnershipTest {

    private Integer press;
    private Integer plancha;

    @BeforeEach
    void catalogo() {
        press = crearEjercicioCatalogo().getId();
        plancha = crearEjercicioCatalogo().getId();
    }

    /** Guarda por /sesiones/completa una sesión con las series dadas (JSON) de un ejercicio. */
    private void entrenar(Usuario quien, String fecha, boolean completada, int ejercicioId, String series)
            throws Exception {
        String cuerpo = """
                {"claveIdempotencia":"%s","fechaInicio":"%s","duracionMinutos":45,
                 "completada":%s,"ejercicios":[{"ejercicioId":%d,"series":[%s]}]}
                """.formatted(UUID.randomUUID(), fecha, completada, ejercicioId, series);
        assertThat(estadoCon(quien, cuerpo)).isEqualTo(200);
    }

    private int estadoCon(Usuario quien, String cuerpo) throws Exception {
        return pedir(quien, "POST /sesiones/completa", cuerpo).andReturn().getResponse().getStatus();
    }

    private static String serie(int numero, String peso, int reps) {
        return """
                {"numero":%d,"repeticiones":%d,"peso":%s,"completada":true}""".formatted(numero, reps, peso);
    }

    private JsonNode ultimaVez(Usuario quien, String ids) throws Exception {
        String json = pedir(quien, "GET /sesiones/ultima-vez?ejercicios=" + ids)
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    @Test
    @DisplayName("Sale la sesión más reciente, con sus series en orden y su fecha")
    void saleLaMasReciente() throws Exception {
        entrenar(owner, "2026-09-10T18:00:00", true, press, serie(1, "50", 10));
        entrenar(owner, "2026-09-20T18:00:00", true, press,
                serie(1, "57.5", 12) + "," + serie(2, "60", 10));
        entrenar(owner, "2026-09-15T18:00:00", true, press, serie(1, "55", 8));

        JsonNode r = ultimaVez(owner, String.valueOf(press));

        assertThat(r).hasSize(1);
        JsonNode ultima = r.get(0);
        assertThat(ultima.get("ejercicioId").asInt()).isEqualTo(press);
        assertThat(ultima.get("fecha").asText()).startsWith("2026-09-20T18:00");
        assertThat(ultima.get("series")).hasSize(2);
        assertThat(ultima.get("series").get(0).get("numero").asInt()).isEqualTo(1);
        assertThat(ultima.get("series").get(0).get("peso").decimalValue()).isEqualByComparingTo("57.5");
        assertThat(ultima.get("series").get(0).get("repeticiones").asInt()).isEqualTo(12);
        assertThat(ultima.get("series").get(1).get("peso").decimalValue()).isEqualByComparingTo("60");
    }

    @Test
    @DisplayName("El ejercicio dos veces en la misma sesión: solo las series de la primera")
    void repetidoEnLaSesion_laPrimera() throws Exception {
        String cuerpo = """
                {"claveIdempotencia":"%s","fechaInicio":"2026-09-20T18:00:00","duracionMinutos":45,
                 "completada":true,"ejercicios":[
                   {"ejercicioId":%d,"series":[%s,%s]},
                   {"ejercicioId":%d,"series":[%s]}]}
                """.formatted(UUID.randomUUID(), press, serie(1, "60", 10), serie(2, "60", 9),
                press, serie(1, "40", 15));
        assertThat(estadoCon(owner, cuerpo)).isEqualTo(200);

        JsonNode series = ultimaVez(owner, String.valueOf(press)).get(0).get("series");

        assertThat(series).hasSize(2);
        assertThat(series.findValuesAsText("peso")).doesNotContain("40", "40.00");
    }

    @Test
    @DisplayName("Series por tiempo: vuelven con sus segundos")
    void seriesPorTiempo() throws Exception {
        entrenar(owner, "2026-09-20T18:00:00", true, plancha,
                "{\"numero\":1,\"repeticiones\":0,\"segundos\":40,\"completada\":true}");

        JsonNode serie = ultimaVez(owner, String.valueOf(plancha)).get(0).get("series").get(0);

        assertThat(serie.get("segundos").asInt()).isEqualTo(40);
        assertThat(serie.get("repeticiones").asInt()).isZero();
    }

    @Test
    @DisplayName("Una sesión no terminada no cuenta, aunque sea más reciente")
    void noTerminadas_noCuentan() throws Exception {
        entrenar(owner, "2026-09-10T18:00:00", true, press, serie(1, "50", 10));
        entrenar(owner, "2026-09-25T18:00:00", false, press, serie(1, "90", 10));

        JsonNode r = ultimaVez(owner, String.valueOf(press));

        assertThat(r.get(0).get("series").get(0).get("peso").decimalValue()).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("Los ejercicios sin ninguna vez no salen; varios ids a la vez")
    void sinNinguna_noSale() throws Exception {
        entrenar(owner, "2026-09-10T18:00:00", true, press, serie(1, "50", 10));

        JsonNode r = ultimaVez(owner, press + "," + plancha);

        assertThat(r).hasSize(1);
        assertThat(r.get(0).get("ejercicioId").asInt()).isEqualTo(press);
        assertThat(ultimaVez(owner, String.valueOf(plancha))).as("200 con []").isEmpty();
    }

    @Test
    @DisplayName("Aislamiento: las series de otro usuario no cuentan")
    void otroUsuario_noCuenta() throws Exception {
        entrenar(owner, "2026-09-10T18:00:00", true, press, serie(1, "50", 10));
        entrenar(attacker, "2026-09-25T18:00:00", true, press, serie(1, "120", 3));

        JsonNode delDueno = ultimaVez(owner, String.valueOf(press));
        assertThat(delDueno.get(0).get("series").get(0).get("peso").decimalValue()).isEqualByComparingTo("50");

        JsonNode delOtro = ultimaVez(attacker, String.valueOf(press));
        assertThat(delOtro.get(0).get("series").get(0).get("peso").decimalValue()).isEqualByComparingTo("120");

        Usuario tercero = crearUsuario("__ultima_vez_tercero__", com.gymprofit.api.enums.RoleType.USER);
        assertThat(ultimaVez(tercero, String.valueOf(press))).isEmpty();
    }

    @Test
    @DisplayName("Sin token, 401; con el de invitado, 403")
    void sinToken_401() throws Exception {
        mockMvc.perform(get("/sesiones/ultima-vez").param("ejercicios", String.valueOf(press)))
                .andExpect(status().isUnauthorized());
        assertThat(estado(guest, "GET /sesiones/ultima-vez?ejercicios=" + press)).isEqualTo(403);
    }

    @Test
    @DisplayName("Hasta 30 ids: 31 es un 400, y sin ids también")
    void hasta30Ids() throws Exception {
        String treinta = IntStream.rangeClosed(1, 30).mapToObj(String::valueOf).collect(Collectors.joining(","));
        assertThat(estado(owner, "GET /sesiones/ultima-vez?ejercicios=" + treinta)).isEqualTo(200);
        assertThat(estado(owner, "GET /sesiones/ultima-vez?ejercicios=" + treinta + ",31")).isEqualTo(400);
        assertThat(estado(owner, "GET /sesiones/ultima-vez")).isEqualTo(400);
        assertThat(estado(owner, "GET /sesiones/ultima-vez?ejercicios=")).isEqualTo(400);
    }
}
