package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// MacrosLineaTest — lo apuntado guarda sus macros (lote 1.6.6, A1)
// Antes, la proteína, los carbohidratos y la grasa de una línea se calculaban al leerla
// con los valores del alimento de ese día: corregir un alimento propio cambiaba los
// macros de días pasados (y no sus kcal). Ahora se guardan al escribir la línea.
// ============================================================
@DisplayName("Lote 1.6.6 — cada línea guarda sus macros")
class MacrosLineaTest extends AbstractOwnershipTest {

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    @Test
    @DisplayName("corregir los macros de un alimento propio no cambia una línea ya apuntada ni el total de su comida")
    void corregir_no_cambia_lo_apuntado() throws Exception {
        Alimento salsa = propio("Salsa kzmacros", 200, "10", "20", "5");
        JsonNode r = anadir(salsa.getId(), "\"cantidadGramos\":200");
        int linea = r.get("linea").get("id").asInt();
        int comida = r.get("comida").get("id").asInt();

        pedir(owner, "PATCH /alimentos/" + salsa.getId(),
                "{\"calorias\":300,\"proteinas\":30,\"carbohidratos\":40,\"grasas\":15}").andExpect(status().isOk());
        limpiar();

        JsonNode l = json(pedir(owner, "GET /alimentos-comida/" + linea));
        assertThat(l.get("caloriasTotales").asInt()).isEqualTo(400);
        assertThat(l.get("proteinasTotales").decimalValue()).isEqualByComparingTo("20");
        assertThat(l.get("carbohidratosTotales").decimalValue()).isEqualByComparingTo("40");
        assertThat(l.get("grasasTotales").decimalValue()).isEqualByComparingTo("10");
        JsonNode c = json(pedir(owner, "GET /comidas/" + comida));
        assertThat(c.get("totalProteinas").decimalValue()).isEqualByComparingTo("20");
        assertThat(c.get("totalGrasas").decimalValue()).isEqualByComparingTo("10");
        // La lista de la comida, igual.
        JsonNode lista = json(pedir(owner, "GET /alimentos-comida/comida/" + comida));
        assertThat(lista.get(0).get("proteinasTotales").decimalValue()).isEqualByComparingTo("20");

        // Y añadir otra línea a esa comida no recalcula la vieja con los valores nuevos.
        Alimento pan = propio("Pan kzmacros", 250, "8", "50", "2");
        anadir(pan.getId(), "\"cantidadGramos\":100");
        limpiar();
        c = json(pedir(owner, "GET /comidas/" + comida));
        assertThat(c.get("totalProteinas").decimalValue()).isEqualByComparingTo("28");
    }

    @Test
    @DisplayName("cambiar la cantidad de una línea la recalcula entera con los valores de hoy")
    void cambiar_cantidad_recalcula() throws Exception {
        Alimento salsa = propio("Salsa kzmacros", 200, "10", "20", "5");
        JsonNode r = anadir(salsa.getId(), "\"cantidadGramos\":200");
        int linea = r.get("linea").get("id").asInt();
        int comida = r.get("comida").get("id").asInt();
        pedir(owner, "PATCH /alimentos/" + salsa.getId(), "{\"calorias\":300,\"proteinas\":30}").andExpect(status().isOk());
        limpiar();

        JsonNode l = json(pedir(owner, "PATCH /alimentos-comida/" + linea, "{\"cantidadGramos\":100}"));
        assertThat(l.get("caloriasTotales").asInt()).isEqualTo(300);
        assertThat(l.get("proteinasTotales").decimalValue()).isEqualByComparingTo("30");
        assertThat(l.get("carbohidratosTotales").decimalValue()).isEqualByComparingTo("20");
        limpiar();
        assertThat(json(pedir(owner, "GET /comidas/" + comida)).get("totalProteinas").decimalValue())
                .isEqualByComparingTo("30");
    }

    @Test
    @DisplayName("añadir y la ruta antigua guardan los macros al escribir, redondeados a dos decimales")
    void todas_las_rutas_guardan() throws Exception {
        Alimento nueces = propio("Nueces kzmacros", 654, "15.23", "13.71", "65.21");
        JsonNode r = anadir(nueces.getId(), "\"cantidadGramos\":33");
        // 15,23 × 0,33 = 5,0259 → 5,03
        assertThat(r.get("linea").get("proteinasTotales").decimalValue()).isEqualByComparingTo("5.03");
        int comidaId = r.get("comida").get("id").asInt();

        Alimento otro = propio("Avena kzmacros", 380, "13", "60", "7");
        JsonNode vieja = objectMapper.readTree(pedir(owner, "POST /alimentos-comida",
                "{\"comidaId\":" + comidaId + ",\"alimentoId\":" + otro.getId() + ",\"cantidadGramos\":50}")
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(vieja.get("proteinasTotales").decimalValue()).isEqualByComparingTo("6.5");
        limpiar();
        Integer guardadas = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM alimentos_comida WHERE comida_id = ? AND proteinas_totales > 0")
                .setParameter(1, comidaId).getSingleResult()).intValue();
        assertThat(guardadas).isEqualTo(2);
    }

    @Test
    @DisplayName("unas kcal escritas a mano en el PATCH de una línea, 400 (ninguna build las manda)")
    void calorias_a_mano() throws Exception {
        Alimento salsa = propio("Salsa kzmacros", 200, "10", "20", "5");
        int linea = anadir(salsa.getId(), "\"cantidadGramos\":200").get("linea").get("id").asInt();
        pedir(owner, "PATCH /alimentos-comida/" + linea, "{\"caloriasTotales\":5}").andExpect(status().isBadRequest());
        limpiar();
        assertThat(json(pedir(owner, "GET /alimentos-comida/" + linea)).get("caloriasTotales").asInt()).isEqualTo(400);
    }

    // --- Ayudas -------------------------------------------------------------

    private Alimento propio(String nombre, int kcal, String p, String c, String g) {
        Alimento a = new Alimento();
        a.setNombre(nombre);
        a.setCalorias(kcal);
        a.setProteinas(new BigDecimal(p));
        a.setCarbohidratos(new BigDecimal(c));
        a.setGrasas(new BigDecimal(g));
        a.setActivo(true);
        a.setUsuario(owner);
        return alimentoRepository.save(a);
    }

    private JsonNode anadir(int alimento, String cantidad) throws Exception {
        JsonNode r = json(pedir(owner, "POST /comidas/anadir", "{\"fecha\":\"" + LocalDate.now().minusDays(1)
                + "\",\"tipoComida\":\"CENA\",\"alimentoId\":" + alimento + "," + cantidad + "}"));
        limpiar();
        return r;
    }

    private void limpiar() {
        em.flush();
        em.clear();
    }

    private JsonNode json(ResultActions r) throws Exception {
        return objectMapper.readTree(r.andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
    }
}
