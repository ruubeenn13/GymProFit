package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// RecordsTest — récords sacados de lo que se entrena (GP-088), con el contexto
// completo y JWT real, de punta a punta: se guardan sesiones por POST
// /sesiones/completa, como la app, y se leen por /records.
//
// Cubre los criterios del encargo: la primera marca no cuenta, igualar no es
// récord y superar sí, el guardado devuelve los récords batidos, borrar la
// sesión del récord lo devuelve al anterior, las rutas viejas leen de la fuente
// nueva, y aislamiento: otro usuario ve los suyos, nunca los ajenos (DEC-027,
// los ids de ejercicio son del catálogo público).
// ============================================================
@DisplayName("Récords (GP-088): se calculan de las series y cada uno ve los suyos")
class RecordsTest extends AbstractOwnershipTest {

    private Integer press;

    @BeforeEach
    void catalogo() {
        press = crearEjercicioCatalogo().getId();
    }

    /** Guarda una sesión completada con una serie de press y devuelve la respuesta. */
    private JsonNode entrenar(Usuario quien, String fecha, String peso, int reps) throws Exception {
        String cuerpo = """
                {"claveIdempotencia":"%s","fechaInicio":"%sT18:00:00","duracionMinutos":45,
                 "completada":true,"ejercicios":[{"ejercicioId":%d,
                 "series":[{"numero":1,"repeticiones":%d,"peso":%s,"completada":true}]}]}
                """.formatted(UUID.randomUUID(), fecha, press, reps, peso);
        String json = pedir(quien, "POST /sesiones/completa", cuerpo)
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private JsonNode leer(Usuario quien, String ruta) throws Exception {
        return objectMapper.readTree(pedir(quien, "GET " + ruta).andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("La primera marca vuelve como punto de partida, no como récord")
    void primeraMarca_noEsRecord() throws Exception {
        JsonNode r = entrenar(owner, "2026-09-14", "80", 5);

        assertThat(r.has("recordsBatidos")).isFalse();
        assertThat(r.get("primerasMarcas")).hasSize(1);
        assertThat(leer(owner, "/records").get("records")).isEmpty();
        assertThat(estado(owner, "GET /progreso-ejercicios/usuario/" + owner.getId() + "/record-destacado"))
                .as("la ruta vieja tampoco lo da por récord").isEqualTo(204);
    }

    @Test
    @DisplayName("Igualar no es récord; superar sí, y el guardado lo devuelve con la marca anterior")
    void igualarNo_superarSi() throws Exception {
        entrenar(owner, "2026-09-14", "80", 5);

        JsonNode igual = entrenar(owner, "2026-09-16", "80", 5);
        assertThat(igual.has("recordsBatidos")).as("igualar").isFalse();
        assertThat(igual.has("primerasMarcas")).isFalse();

        JsonNode mejor = entrenar(owner, "2026-09-18", "82.5", 5);
        JsonNode record = mejor.get("recordsBatidos").get(0);
        assertThat(record.get("peso").decimalValue()).isEqualByComparingTo("82.5");
        assertThat(record.get("pesoAnterior").decimalValue()).isEqualByComparingTo("80");
        assertThat(record.get("repeticionesAnterior").asInt()).isEqualTo(5);
        assertThat(record.get("unoRmEstimado").decimalValue()).isEqualByComparingTo("96.3");
    }

    @Test
    @DisplayName("Borrar la sesión del récord lo devuelve al anterior")
    void borrarSesionDelRecord_vuelveAlAnterior() throws Exception {
        entrenar(owner, "2026-09-14", "80", 5);
        entrenar(owner, "2026-09-16", "82.5", 5);
        int ultima = entrenar(owner, "2026-09-18", "85", 5).get("id").asInt();

        assertThat(leer(owner, "/records").get("records").get(0).get("peso").decimalValue())
                .isEqualByComparingTo("85");

        assertThat(estado(owner, "DELETE /sesiones/" + ultima)).isBetween(200, 299);

        JsonNode vigente = leer(owner, "/records").get("records").get(0);
        assertThat(vigente.get("peso").decimalValue()).isEqualByComparingTo("82.5");
        assertThat(vigente.get("pesoAnterior").decimalValue()).isEqualByComparingTo("80");
    }

    @Test
    @DisplayName("Recientes: solo los récords desde la fecha pedida")
    void recientes_desdeFecha() throws Exception {
        entrenar(owner, "2026-09-07", "70", 5);
        entrenar(owner, "2026-09-10", "75", 5);
        entrenar(owner, "2026-09-22", "77.5", 5);

        JsonNode r = leer(owner, "/records?desde=2026-09-21");
        assertThat(r.get("recientes")).hasSize(1);
        assertThat(r.get("recientes").get(0).get("peso").decimalValue()).isEqualByComparingTo("77.5");
        assertThat(r.get("records")).hasSize(1);
    }

    @Test
    @DisplayName("La gráfica sale de la mejor serie de cada sesión")
    void progresion_deLasSesiones() throws Exception {
        entrenar(owner, "2026-09-14", "80", 5);
        entrenar(owner, "2026-09-16", "70", 8);

        JsonNode puntos = leer(owner, "/records/ejercicio/" + press + "/progresion");
        assertThat(puntos).hasSize(2);
        assertThat(puntos.get(1).get("peso").decimalValue()).isEqualByComparingTo("70");

        JsonNode legado = leer(owner, "/progreso-ejercicios/usuario/" + owner.getId()
                + "/ejercicio/" + press + "/historial");
        assertThat(legado).as("la ruta vieja, de la más reciente a la más antigua").hasSize(2);
        assertThat(legado.get(0).get("mejorPeso").decimalValue()).isEqualByComparingTo("70");
    }

    @Test
    @DisplayName("Aislamiento: otro usuario ve los suyos, nunca los ajenos")
    void aislamiento() throws Exception {
        entrenar(owner, "2026-09-14", "80", 5);
        entrenar(owner, "2026-09-16", "90", 5);
        entrenar(attacker, "2026-09-15", "40", 5);
        entrenar(attacker, "2026-09-17", "45", 5);

        JsonNode suyos = leer(attacker, "/records?desde=2026-09-01");
        assertThat(suyos.get("records")).hasSize(1);
        assertThat(suyos.get("records").get(0).get("peso").decimalValue()).isEqualByComparingTo("45");
        assertThat(suyos.get("recientes")).hasSize(1);

        JsonNode puntos = leer(attacker, "/records/ejercicio/" + press + "/progresion");
        assertThat(puntos).hasSize(2);
        assertThat(puntos.findValuesAsText("peso")).doesNotContain("80", "90", "80.00", "90.00");

        // Un tercero sin entrenar no ve nada del catálogo compartido.
        Usuario tercero = crearUsuario("__records_tercero__", com.gymprofit.api.enums.RoleType.USER);
        assertThat(leer(tercero, "/records").get("records")).isEmpty();
        assertThat(leer(tercero, "/records/ejercicio/" + press + "/progresion")).isEmpty();
    }

    @Test
    @DisplayName("Rutas viejas con el id de otro usuario → 403")
    void rutasViejas_idAjeno() throws Exception {
        entrenar(owner, "2026-09-14", "80", 5);
        assertThat(estado(attacker, "GET /progreso-ejercicios/usuario/" + owner.getId() + "/record-destacado"))
                .isEqualTo(403);
        assertThat(estado(attacker, "GET /progreso-ejercicios/usuario/" + owner.getId()
                + "/ejercicio/" + press + "/historial")).isEqualTo(403);
        assertThat(estado(owner, "GET /progreso-ejercicios/usuario/" + owner.getId()
                + "/ejercicio/" + press + "/historial")).isEqualTo(200);
    }

    @Test
    @DisplayName("Récord destacado de la ruta vieja: el de más peso, leído de las series")
    void recordDestacado_deLasSeries() throws Exception {
        entrenar(owner, "2026-09-14", "80", 5);
        entrenar(owner, "2026-09-16", "85", 3);

        JsonNode r = leer(owner, "/progreso-ejercicios/usuario/" + owner.getId() + "/record-destacado");
        assertThat(r.get("peso").decimalValue()).isEqualByComparingTo("85");
        assertThat(r.get("repeticiones").asInt()).isEqualTo(3);
        assertThat(r.get("ejercicioNombre").asText()).isEqualTo("Ejercicio IDOR test");
    }

    @Test
    @DisplayName("Un invitado no tiene récords: 403")
    void invitado_403() throws Exception {
        assertThat(estado(guest, "GET /records")).isEqualTo(403);
        assertThat(estado(guest, "GET /records/ejercicio/" + press + "/progresion")).isEqualTo(403);
    }

    @Test
    @DisplayName("Progresión de un ejercicio que no existe → 404")
    void progresion_ejercicioInexistente_404() throws Exception {
        assertThat(estado(owner, "GET /records/ejercicio/999999999/progresion")).isEqualTo(404);
    }
}
