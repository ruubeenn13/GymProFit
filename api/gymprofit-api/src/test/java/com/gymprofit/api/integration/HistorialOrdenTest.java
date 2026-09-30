package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// HistorialOrdenTest — GET /sesiones/usuario/{id} sale por fecha (GP-141), con el
// contexto completo y JWT real. Con «Apuntar un entrenamiento hecho» una sesión de
// ayer se guarda después que la de hoy: el orden de guardado (el id, o lo que la
// base quiera devolver) no es el del historial. De la más reciente a la más antigua
// por fecha de inicio y, a igual fecha, la guardada después primero.
// ============================================================
@DisplayName("Historial de sesiones por fecha (GP-141)")
class HistorialOrdenTest extends AbstractOwnershipTest {

    private Integer press;

    @BeforeEach
    void catalogo() {
        press = crearEjercicioCatalogo().getId();
    }

    /** Guarda por /sesiones/completa una sesión terminada y devuelve su id. */
    private int guardar(Usuario quien, String fecha) throws Exception {
        String cuerpo = """
                {"claveIdempotencia":"%s","fechaInicio":"%s","duracionMinutos":45,"completada":true,
                 "ejercicios":[{"ejercicioId":%d,"series":[
                   {"numero":1,"repeticiones":10,"peso":50,"completada":true}]}]}
                """.formatted(UUID.randomUUID(), fecha, press);
        String json = pedir(quien, "POST /sesiones/completa", cuerpo)
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asInt();
    }

    private List<Integer> historial(Usuario quien) throws Exception {
        String json = pedir(quien, "GET /sesiones/usuario/" + quien.getId())
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = new ArrayList<>();
        for (JsonNode s : objectMapper.readTree(json)) ids.add(s.get("id").asInt());
        return ids;
    }

    @Test
    @DisplayName("Una de ayer apuntada después de la de hoy sale debajo")
    void ayerApuntadaDespues_saleDebajo() throws Exception {
        int hoy = guardar(owner, "2026-09-30T18:00:00");
        int ayer = guardar(owner, "2026-09-29T18:00:00");
        int anteayer = guardar(owner, "2026-09-28T09:00:00");

        assertThat(historial(owner)).containsExactly(hoy, ayer, anteayer);
    }

    @Test
    @DisplayName("A igual fecha de inicio, la guardada después primero")
    void igualFecha_porIdDescendente() throws Exception {
        int primera = guardar(owner, "2026-09-30T18:00:00");
        int segunda = guardar(owner, "2026-09-30T18:00:00");
        int antes = guardar(owner, "2026-09-30T08:00:00");

        assertThat(historial(owner)).containsExactly(segunda, primera, antes);
    }
}
