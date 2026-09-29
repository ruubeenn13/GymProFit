package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// SeriesPorTiempoTest — GP-125 en la API: series por tiempo, récords y volumen
//
// Por /sesiones/completa, como la app. Una serie por tiempo guarda sus segundos y va
// con 0 repeticiones: repeticiones sigue sin poder ser null, para no romper a nadie. El
// récord de un ejercicio por tiempo es su serie más larga, y el volumen (kilos movidos,
// DEC-006) no cuenta esas series aunque lleven peso.
// ============================================================
@DisplayName("GP-125 — series por tiempo")
class SeriesPorTiempoTest extends AbstractOwnershipTest {

    private Integer plancha;
    private Integer press;

    @BeforeEach
    void catalogo() {
        plancha = crearEjercicioCatalogo().getId();
        press = crearEjercicioCatalogo().getId();
    }

    private JsonNode guardar(Usuario quien, String fecha, String ejercicios) throws Exception {
        String cuerpo = """
                {"claveIdempotencia":"%s","fechaInicio":"%sT18:00:00","duracionMinutos":45,
                 "completada":true,"ejercicios":[%s]}""".formatted(UUID.randomUUID(), fecha, ejercicios);
        var r = pedir(quien, "POST /sesiones/completa", cuerpo).andReturn().getResponse();
        assertThat(r.getStatus()).as(r.getContentAsString()).isEqualTo(200);
        return objectMapper.readTree(r.getContentAsString());
    }

    private String planchaDe(int... segundos) {
        StringBuilder series = new StringBuilder();
        for (int i = 0; i < segundos.length; i++) {
            if (i > 0) series.append(',');
            series.append("{\"numero\":%d,\"repeticiones\":0,\"segundos\":%d,\"completada\":true}"
                    .formatted(i + 1, segundos[i]));
        }
        return "{\"ejercicioId\":%d,\"series\":[%s]}".formatted(plancha, series);
    }

    private JsonNode leer(Usuario quien, String ruta) throws Exception {
        return objectMapper.readTree(pedir(quien, "GET " + ruta).andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("una serie por tiempo se guarda con sus segundos y 0 repeticiones, y se devuelve así")
    void se_guarda() throws Exception {
        JsonNode sesion = guardar(owner, "2026-09-21", planchaDe(45, 50));
        JsonNode ejercicios = leer(owner, "/ejercicios-realizados/sesion/" + sesion.get("id").asInt());
        JsonNode series = ejercicios.get(0).get("series");
        assertThat(series).hasSize(2);
        assertThat(series.get(0).get("segundos").asInt()).isEqualTo(45);
        assertThat(series.get(0).get("repeticiones").asInt()).isZero();
        assertThat(series.get(1).get("segundos").asInt()).isEqualTo(50);
        assertThat(ejercicios.get(0).get("seriesCompletadas").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("una serie de siempre, sin segundos, sigue igual")
    void la_de_siempre() throws Exception {
        JsonNode sesion = guardar(owner, "2026-09-21", """
                {"ejercicioId":%d,"series":[{"numero":1,"repeticiones":8,"peso":60,"completada":true}]}"""
                .formatted(press));
        JsonNode serie = leer(owner, "/ejercicios-realizados/sesion/" + sesion.get("id").asInt())
                .get(0).get("series").get(0);
        assertThat(serie.get("repeticiones").asInt()).isEqualTo(8);
        assertThat(serie.get("segundos").isNull()).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "{\"numero\":1,\"repeticiones\":10,\"segundos\":30}",
            "{\"numero\":1,\"repeticiones\":0,\"segundos\":0}",
            "{\"numero\":1,\"repeticiones\":0,\"segundos\":3601}",
            "{\"numero\":1,\"segundos\":30}"})
    @DisplayName("400: segundos con repeticiones, fuera de rango, o sin repeticiones (van a 0, no a null)")
    void no_valida(String serie) throws Exception {
        String cuerpo = """
                {"claveIdempotencia":"%s","fechaInicio":"2026-09-21T18:00:00","completada":true,
                 "ejercicios":[{"ejercicioId":%d,"series":[%s]}]}""".formatted(UUID.randomUUID(), plancha, serie);
        assertThat(estadoCon(owner, cuerpo)).isEqualTo(400);
    }

    @Test
    @DisplayName("récord por tiempo: la serie más larga; igualarla no cuenta; superarla sí")
    void record_por_tiempo() throws Exception {
        JsonNode primera = guardar(owner, "2026-09-21", planchaDe(30, 40));
        assertThat(primera.get("primerasMarcas").get(0).get("tipo").asText()).isEqualTo("TIEMPO");
        assertThat(primera.get("primerasMarcas").get(0).get("segundos").asInt()).isEqualTo(40);

        JsonNode igual = guardar(owner, "2026-09-23", planchaDe(40));
        assertThat(igual.has("recordsBatidos")).isFalse();

        JsonNode mejor = guardar(owner, "2026-09-25", planchaDe(35, 55));
        JsonNode record = mejor.get("recordsBatidos").get(0);
        assertThat(record.get("tipo").asText()).isEqualTo("TIEMPO");
        assertThat(record.get("segundos").asInt()).isEqualTo(55);
        assertThat(record.get("segundosAnterior").asInt()).isEqualTo(40);
        assertThat(record.get("repeticiones").asInt()).isZero();
        assertThat(record.has("peso")).isFalse();

        JsonNode vigente = leer(owner, "/records").get("records").get(0);
        assertThat(vigente.get("tipo").asText()).isEqualTo("TIEMPO");
        assertThat(vigente.get("segundos").asInt()).isEqualTo(55);

        JsonNode puntos = leer(owner, "/records/ejercicio/" + plancha + "/progresion");
        assertThat(puntos).hasSize(3);
        java.util.List<Integer> segundos = new java.util.ArrayList<>();
        for (JsonNode p : puntos) {
            assertThat(p.get("tipo").asText()).isEqualTo("TIEMPO");
            segundos.add(p.get("segundos").asInt());
        }
        assertThat(segundos).containsExactlyInAnyOrder(40, 40, 55);

        // Aislamiento: el otro no ve nada de esto.
        assertThat(leer(attacker, "/records").get("records")).isEmpty();
        assertThat(leer(attacker, "/records/ejercicio/" + plancha + "/progresion")).isEmpty();
    }

    @Test
    @DisplayName("el volumen no cuenta las series por tiempo, ni aunque lleven peso")
    void volumen() throws Exception {
        String pressDosSeries = """
                {"ejercicioId":%d,"series":[{"numero":1,"repeticiones":10,"peso":50,"completada":true},
                 {"numero":2,"repeticiones":10,"peso":50,"completada":true}]}""".formatted(press);
        String planchaLastrada = """
                {"ejercicioId":%d,"series":[{"numero":1,"repeticiones":0,"segundos":60,"peso":20,"completada":true}]}"""
                .formatted(plancha);
        JsonNode sesion = guardar(owner, "2026-09-21", pressDosSeries + "," + planchaLastrada);
        JsonNode volumen = leer(owner, "/sesiones/" + sesion.get("id").asInt() + "/volumen");
        assertThat(volumen.get("volumenKg").decimalValue()).isEqualByComparingTo("1000");
    }

    private int estadoCon(Usuario quien, String cuerpo) throws Exception {
        return pedir(quien, "POST /sesiones/completa", cuerpo).andReturn().getResponse().getStatus();
    }
}
