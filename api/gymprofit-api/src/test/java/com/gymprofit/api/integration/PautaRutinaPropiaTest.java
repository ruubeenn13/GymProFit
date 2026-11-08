package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.service.rutina.IRutinaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// PautaRutinaPropiaTest — la pauta de un ejercicio en una rutina propia (GP-244)
//
// Crear y editar una rutina guardan «3 series de 8 a 12» o «3 series de 30 a 45 s»:
// POST y PATCH de /rutinas-ejercicios aceptan, opcionales, repeticionesMin,
// repeticionesMax y medida. Con el rango, repeticiones vale el máximo (como en las
// copias de ProgramaService), que es lo único que leen las builds viejas. Una build
// vieja manda solo repeticiones: el rango pasa a ser ese número y la medida no cambia,
// para que no quede un rango viejo contra lo editado. 1 ≤ mín ≤ máx y medida
// REPETICIONES o SEGUNDOS; si no, 400. Contexto completo, sin servicio simulado.
// ============================================================
@DisplayName("GP-244 — la pauta de una rutina propia: rango, segundos y builds viejas")
class PautaRutinaPropiaTest extends AbstractOwnershipTest {

    @Autowired
    private IRutinaService rutinaService;

    private Integer rutinaId;
    private Integer ejercicioId;

    @BeforeEach
    void sembrar() {
        runAs(owner, () -> {
            RutinaCreateDTO r = new RutinaCreateDTO();
            r.setUsuarioId(owner.getId());
            r.setNombre("Pecho y hombros");
            r.setNivel("INTERMEDIO");
            r.setEsPredefinida(false);
            rutinaId = rutinaService.save(r).getId();
        });
        ejercicioId = crearEjercicioCatalogo().getId();
    }

    private JsonNode crear(String extra) throws Exception {
        String cuerpo = "{\"rutinaId\":" + rutinaId + ",\"ejercicioId\":" + ejercicioId
                + ",\"series\":3,\"tiempoDescanso\":90,\"orden\":1" + extra + "}";
        String json = pedir(owner, "POST /rutinas-ejercicios", cuerpo)
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private JsonNode patch(int id, String cuerpo) throws Exception {
        String json = pedir(owner, "PATCH /rutinas-ejercicios/" + id, cuerpo)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    @Test
    @DisplayName("crear con rango: se guarda de 8 a 12 y repeticiones es el máximo")
    void crear_con_rango() throws Exception {
        JsonNode r = crear(",\"repeticionesMin\":8,\"repeticionesMax\":12,\"medida\":\"REPETICIONES\"");
        assertThat(r.get("repeticionesMin").asInt()).isEqualTo(8);
        assertThat(r.get("repeticionesMax").asInt()).isEqualTo(12);
        assertThat(r.get("repeticiones").asInt()).isEqualTo(12);
        assertThat(r.get("medida").asText()).isEqualTo("REPETICIONES");
    }

    @Test
    @DisplayName("crear por segundos y pasar a repeticiones con PATCH")
    void segundos_y_vuelta() throws Exception {
        JsonNode r = crear(",\"repeticionesMin\":30,\"repeticionesMax\":45,\"medida\":\"SEGUNDOS\"");
        assertThat(r.get("medida").asText()).isEqualTo("SEGUNDOS");
        assertThat(r.get("repeticiones").asInt()).isEqualTo(45);
        JsonNode p = patch(r.get("id").asInt(), "{\"repeticionesMin\":6,\"repeticionesMax\":10,\"medida\":\"REPETICIONES\"}");
        assertThat(p.get("medida").asText()).isEqualTo("REPETICIONES");
        assertThat(p.get("repeticionesMin").asInt()).isEqualTo(6);
        assertThat(p.get("repeticionesMax").asInt()).isEqualTo(10);
        assertThat(p.get("repeticiones").asInt()).isEqualTo(10);
    }

    @Test
    @DisplayName("una build vieja: solo repeticiones deja el rango en ese número y no toca la medida")
    void build_vieja() throws Exception {
        JsonNode r = crear(",\"repeticiones\":10");
        assertThat(r.get("repeticionesMin").asInt()).isEqualTo(10);
        assertThat(r.get("repeticionesMax").asInt()).isEqualTo(10);
        assertThat(r.get("repeticiones").asInt()).isEqualTo(10);

        JsonNode s = crear(",\"repeticionesMin\":30,\"repeticionesMax\":45,\"medida\":\"SEGUNDOS\"");
        JsonNode p = patch(s.get("id").asInt(), "{\"repeticiones\":40}");
        assertThat(p.get("repeticionesMin").asInt()).isEqualTo(40);
        assertThat(p.get("repeticionesMax").asInt()).isEqualTo(40);
        assertThat(p.get("medida").asText()).isEqualTo("SEGUNDOS");
        // Un PATCH que no toca la pauta (el orden) la deja como estaba.
        JsonNode o = patch(s.get("id").asInt(), "{\"orden\":2}");
        assertThat(o.get("repeticionesMin").asInt()).isEqualTo(40);
        assertThat(o.get("orden").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("400: mínimo mayor que el máximo, mínimo 0, medida que no existe o el rango a medias")
    void pautas_invalidas() throws Exception {
        String base = "{\"rutinaId\":" + rutinaId + ",\"ejercicioId\":" + ejercicioId + ",\"series\":3,\"orden\":1";
        for (String malo : new String[]{
                ",\"repeticionesMin\":12,\"repeticionesMax\":8}",
                ",\"repeticionesMin\":0,\"repeticionesMax\":8}",
                ",\"repeticionesMin\":8,\"repeticionesMax\":12,\"medida\":\"KILOS\"}",
                ",\"repeticionesMin\":8}"}) {
            pedir(owner, "POST /rutinas-ejercicios", base + malo).andExpect(status().isBadRequest());
        }
        int id = crear(",\"repeticionesMin\":8,\"repeticionesMax\":12").get("id").asInt();
        pedir(owner, "PATCH /rutinas-ejercicios/" + id, "{\"repeticionesMin\":12,\"repeticionesMax\":8}")
                .andExpect(status().isBadRequest());
        pedir(owner, "PATCH /rutinas-ejercicios/" + id, "{\"repeticionesMax\":15}")
                .andExpect(status().isBadRequest());
        pedir(owner, "PATCH /rutinas-ejercicios/" + id, "{\"medida\":\"KILOS\"}")
                .andExpect(status().isBadRequest());
    }
}
