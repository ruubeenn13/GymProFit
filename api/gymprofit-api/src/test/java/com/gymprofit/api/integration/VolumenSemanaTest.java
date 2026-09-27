package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// VolumenSemanaTest — series por zona de la semana natural (GP-105, «Esta semana»)
//
// La tarjeta de Inicio cuenta la semana desde el lunes a las 00:00 del usuario, no
// los últimos 7 días. La app manda ese lunes en ?desde= con su hora local, la misma
// con la que guarda fechaInicio, y la API cuenta las sesiones que EMPIEZAN desde ahí:
// una sesión del domingo que acaba pasada la medianoche es del domingo.
//
// Aditivo: sin ?desde= la ruta sigue con ?dias=, que es lo que mandan las builds
// repartidas. La ruta lleva el id del usuario, y su 403 al ajeno está en
// SesionOwnershipTest, también con ?desde=.
// ============================================================
@DisplayName("GET /sesiones/usuario/{id}/volumen-muscular?desde= — semana natural")
class VolumenSemanaTest extends AbstractOwnershipTest {

    private static final String LUNES = "2026-09-21T00:00:00";

    private Integer press;

    @BeforeEach
    void catalogo() {
        press = crearEjercicioCatalogo().getId();
    }

    private void entrenar(Usuario quien, String inicio, int minutos, int series) throws Exception {
        StringBuilder lista = new StringBuilder();
        for (int i = 1; i <= series; i++) {
            if (i > 1) lista.append(',');
            lista.append("{\"numero\":%d,\"repeticiones\":8,\"peso\":50,\"completada\":true}".formatted(i));
        }
        String cuerpo = """
                {"claveIdempotencia":"%s","fechaInicio":"%s","duracionMinutos":%d,
                 "completada":true,"ejercicios":[{"ejercicioId":%d,"series":[%s]}]}
                """.formatted(UUID.randomUUID(), inicio, minutos, press, lista);
        pedir(quien, "POST /sesiones/completa", cuerpo);
    }

    private int seriesTotales(Usuario quien, String consulta) throws Exception {
        String json = pedir(quien, "GET /sesiones/usuario/" + quien.getId() + "/volumen-muscular" + consulta)
                .andReturn().getResponse().getContentAsString();
        int total = 0;
        for (JsonNode fila : objectMapper.readTree(json)) total += fila.get("series").asInt();
        return total;
    }

    @Test
    @DisplayName("cuenta desde el lunes: el domingo anterior no entra, aunque acabe el lunes")
    void desde_el_lunes() throws Exception {
        entrenar(owner, "2026-09-20T18:00:00", 45, 2);   // domingo
        entrenar(owner, "2026-09-20T23:30:00", 45, 4);   // domingo que acaba el lunes 00:15
        entrenar(owner, "2026-09-21T09:00:00", 45, 3);   // lunes

        assertThat(seriesTotales(owner, "?desde=" + LUNES)).isEqualTo(3);
    }

    @Test
    @DisplayName("semana nueva sin nada → lista vacía, no 404")
    void semana_vacia() throws Exception {
        entrenar(owner, "2026-09-20T18:00:00", 45, 2);

        String json = pedir(owner, "GET /sesiones/usuario/" + owner.getId()
                + "/volumen-muscular?desde=2026-09-28T00:00:00")
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(json)).isEmpty();
    }

    @Test
    @DisplayName("sin ?desde= sigue mandando ?dias=, como en las builds repartidas")
    void dias_sigue_igual() throws Exception {
        entrenar(owner, "2026-09-20T18:00:00", 45, 2);
        entrenar(owner, "2026-09-21T09:00:00", 45, 3);

        assertThat(seriesTotales(owner, "?dias=36500")).isEqualTo(5);
    }

    @Test
    @DisplayName("aislamiento: las series del otro no cuentan")
    void aislamiento() throws Exception {
        entrenar(attacker, "2026-09-21T09:00:00", 45, 3);

        assertThat(seriesTotales(owner, "?desde=" + LUNES)).isZero();
    }
}
