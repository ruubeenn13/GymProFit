package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// DiarioTest — el agua, «Hoy no» y los días del diario (lote 1.6.8, GP-162 y GP-166)
// Con el contexto levantado y JWT de verdad (DEC-014). Todo es de la cuenta del token
// (DEC-013): la fecha y el tipo de comida de la ruta no son ids de nadie, así que lo que
// se comprueba es el aislamiento (DEC-027): lo de una cuenta no se ve ni se toca desde
// otra, que con la misma ruta lee lo suyo, vacío.
// ============================================================
class DiarioTest extends AbstractOwnershipTest {

    @Autowired
    private JdbcTemplate jdbc;

    private LocalDate hoy;
    private LocalDate ayer;

    @BeforeEach
    void fechas() {
        hoy = LocalDate.now();
        ayer = hoy.minusDays(1);
    }

    // --- Agua ---------------------------------------------------------------

    @Test
    @DisplayName("el agua se guarda por día, se puede bajar a 0 y el día la devuelve")
    void agua() throws Exception {
        pedir(owner, "PUT /diario/" + hoy + "/agua", "{\"mililitros\":500}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value(hoy.toString()))
                .andExpect(jsonPath("$.mililitros").value(500));
        pedir(owner, "PUT /diario/" + hoy + "/agua", "{\"mililitros\":750}").andExpect(status().isOk());

        pedir(owner, "GET /diario?desde=" + hoy + "&hasta=" + hoy)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].aguaMl").value(750));
        assertThat(filas("agua_diaria", owner)).isEqualTo(1);

        pedir(owner, "PUT /diario/" + hoy + "/agua", "{\"mililitros\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mililitros").value(0));
        pedir(owner, "GET /diario?desde=" + hoy + "&hasta=" + hoy)
                .andExpect(jsonPath("$[0].aguaMl").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"mililitros\":-1}", "{\"mililitros\":20001}", "{}"})
    @DisplayName("el agua fuera de rango o sin cifra es 400 y no guarda nada")
    void aguaInvalida(String cuerpo) throws Exception {
        pedir(owner, "PUT /diario/" + hoy + "/agua", cuerpo).andExpect(status().isBadRequest());
        assertThat(filas("agua_diaria", owner)).isZero();
    }

    @Test
    @DisplayName("un día que no ha llegado no admite agua ni «Hoy no»")
    void futuro() throws Exception {
        LocalDate pasado = hoy.plusDays(2);
        pedir(owner, "PUT /diario/" + pasado + "/agua", "{\"mililitros\":250}").andExpect(status().isBadRequest());
        pedir(owner, "PUT /diario/" + pasado + "/hoy-no/CENA").andExpect(status().isBadRequest());
        // Mañana sí: la API puede ir por detrás de la hora del móvil.
        pedir(owner, "PUT /diario/" + hoy.plusDays(1) + "/agua", "{\"mililitros\":250}").andExpect(status().isOk());
    }

    @Test
    @DisplayName("el agua de una cuenta no se ve ni se pisa desde otra (aislamiento)")
    void aguaAislada() throws Exception {
        pedir(owner, "PUT /diario/" + hoy + "/agua", "{\"mililitros\":1250}").andExpect(status().isOk());

        pedir(attacker, "GET /diario?desde=" + hoy + "&hasta=" + hoy)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].aguaMl").value(0));
        pedir(attacker, "PUT /diario/" + hoy + "/agua", "{\"mililitros\":250}").andExpect(status().isOk());

        pedir(owner, "GET /diario?desde=" + hoy + "&hasta=" + hoy)
                .andExpect(jsonPath("$[0].aguaMl").value(1250));
    }

    // --- «Hoy no» -----------------------------------------------------------

    @Test
    @DisplayName("«Hoy no» se marca y se quita por día y comida, repetible y sin duplicar")
    void hoyNo() throws Exception {
        pedir(owner, "PUT /diario/" + hoy + "/hoy-no/MERIENDA").andExpect(status().isNoContent());
        pedir(owner, "PUT /diario/" + hoy + "/hoy-no/MERIENDA").andExpect(status().isNoContent());
        pedir(owner, "PUT /diario/" + hoy + "/hoy-no/desayuno").andExpect(status().isNoContent());
        assertThat(filas("comidas_hoy_no", owner)).isEqualTo(2);

        pedir(owner, "GET /diario?desde=" + hoy + "&hasta=" + hoy)
                .andExpect(jsonPath("$[0].hoyNo", contains("DESAYUNO", "MERIENDA")));

        pedir(owner, "DELETE /diario/" + hoy + "/hoy-no/MERIENDA").andExpect(status().isNoContent());
        pedir(owner, "DELETE /diario/" + hoy + "/hoy-no/MERIENDA").andExpect(status().isNoContent());
        pedir(owner, "GET /diario?desde=" + hoy + "&hasta=" + hoy)
                .andExpect(jsonPath("$[0].hoyNo", contains("DESAYUNO")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SNACK", "MERIENDITA"})
    @DisplayName("«Hoy no» solo vale para las cinco comidas del diario")
    void hoyNoTipoInvalido(String tipo) throws Exception {
        pedir(owner, "PUT /diario/" + hoy + "/hoy-no/" + tipo).andExpect(status().isBadRequest());
        assertThat(filas("comidas_hoy_no", owner)).isZero();
    }

    @Test
    @DisplayName("el «Hoy no» de una cuenta no se ve ni se quita desde otra (aislamiento)")
    void hoyNoAislado() throws Exception {
        pedir(owner, "PUT /diario/" + hoy + "/hoy-no/CENA").andExpect(status().isNoContent());

        pedir(attacker, "GET /diario?desde=" + hoy + "&hasta=" + hoy)
                .andExpect(jsonPath("$[0].hoyNo", empty()));
        pedir(attacker, "DELETE /diario/" + hoy + "/hoy-no/CENA").andExpect(status().isNoContent());

        pedir(owner, "GET /diario?desde=" + hoy + "&hasta=" + hoy)
                .andExpect(jsonPath("$[0].hoyNo", contains("CENA")));
    }

    // --- Los días -----------------------------------------------------------

    @Test
    @DisplayName("cada día del rango, en orden: comidas con algo apuntado, «Hoy no», agua y entreno")
    void dias() throws Exception {
        Integer alimento = crearAlimentoCatalogo().getId();
        comidaConAlimento(owner, ayer, "DESAYUNO", alimento);
        comidaConAlimento(owner, ayer, "COMIDA", alimento);
        // Una comida sin líneas no está apuntada, aunque exista.
        jdbc.update("INSERT INTO comidas (usuario_id, fecha, tipo_comida, total_calorias) VALUES (?, ?, 'CENA', 0)",
                owner.getId(), Timestamp.valueOf(ayer.atTime(21, 0)));
        // Lo de otra cuenta no cuenta.
        comidaConAlimento(attacker, ayer, "MERIENDA", alimento);
        sesion(attacker, ayer.atTime(10, 0), true, null);

        Integer rutina = jdbc.queryForObject("SELECT MAX(id) FROM rutinas", Integer.class);
        Integer rutinaPropia = insertarRutina(owner, "Cuerpo completo B");
        sesion(owner, ayer.atTime(9, 0), true, rutinaPropia);
        // Una sin terminar no es un día de entreno.
        sesion(owner, hoy.atTime(8, 0), false, rutina);

        pedir(owner, "GET /diario?desde=" + ayer + "&hasta=" + hoy)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fecha").value(ayer.toString()))
                .andExpect(jsonPath("$[0].comidasApuntadas", contains("DESAYUNO", "COMIDA")))
                .andExpect(jsonPath("$[0].entrenos").value(1))
                .andExpect(jsonPath("$[0].rutinaEntreno").value("Cuerpo completo B"))
                .andExpect(jsonPath("$[1].fecha").value(hoy.toString()))
                .andExpect(jsonPath("$[1].comidasApuntadas", empty()))
                .andExpect(jsonPath("$[1].entrenos").value(0))
                .andExpect(jsonPath("$[1].rutinaEntreno", nullValue()));
    }

    @Test
    @DisplayName("un entreno sin rutina cuenta, sin nombre")
    void entrenoLibre() throws Exception {
        sesion(owner, hoy.atTime(7, 30), true, null);
        pedir(owner, "GET /diario?desde=" + hoy + "&hasta=" + hoy)
                .andExpect(jsonPath("$[0].entrenos").value(1))
                .andExpect(jsonPath("$[0].rutinaEntreno", nullValue()));
    }

    @Test
    @DisplayName("el nombre de la rutina va en el idioma de la petición si tiene traducción")
    void rutinaEnIngles() throws Exception {
        Integer rutina = insertarRutina(owner, "Cuerpo completo B");
        jdbc.update("UPDATE rutinas SET nombre_en = 'Full body B' WHERE id = ?", rutina);
        sesion(owner, hoy.atTime(7, 30), true, rutina);
        mockMvc.perform(MockMvcRequestBuilders.get("/diario?desde=" + hoy + "&hasta=" + hoy)
                        .header("Authorization", "Bearer " + token(owner))
                        .header("Accept-Language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].rutinaEntreno").value("Full body B"));
    }

    @Test
    @DisplayName("un rango al revés, de más de 31 días o sin fechas es 400")
    void rangoInvalido() throws Exception {
        pedir(owner, "GET /diario?desde=" + hoy + "&hasta=" + ayer).andExpect(status().isBadRequest());
        pedir(owner, "GET /diario?desde=" + hoy.minusDays(31) + "&hasta=" + hoy).andExpect(status().isBadRequest());
        pedir(owner, "GET /diario?desde=" + hoy.minusDays(30) + "&hasta=" + hoy)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(31)));
        pedir(owner, "GET /diario").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("el invitado no tiene diario")
    void invitado() throws Exception {
        pedir(guest, "GET /diario?desde=" + hoy + "&hasta=" + hoy).andExpect(status().isForbidden());
        pedir(guest, "PUT /diario/" + hoy + "/agua", "{\"mililitros\":250}").andExpect(status().isForbidden());
        pedir(guest, "PUT /diario/" + hoy + "/hoy-no/CENA").andExpect(status().isForbidden());
    }

    // --- Siembra ------------------------------------------------------------

    private int filas(String tabla, Usuario quien) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla + " WHERE usuario_id = ?",
                Integer.class, quien.getId());
        return n != null ? n : 0;
    }

    private void comidaConAlimento(Usuario quien, LocalDate dia, String tipo, Integer alimento) {
        jdbc.update("INSERT INTO comidas (usuario_id, fecha, tipo_comida, total_calorias) VALUES (?, ?, ?, 100)",
                quien.getId(), Timestamp.valueOf(dia.atTime(12, 0)), tipo);
        Integer comida = jdbc.queryForObject("SELECT MAX(id) FROM comidas WHERE usuario_id = ?", Integer.class, quien.getId());
        jdbc.update("INSERT INTO alimentos_comida (comida_id, alimento_id, cantidad_gramos) VALUES (?, ?, 100)",
                comida, alimento);
    }

    private Integer insertarRutina(Usuario quien, String nombre) {
        jdbc.update("INSERT INTO rutinas (nombre, nivel, usuario_id) VALUES (?, 'PRINCIPIANTE', ?)", nombre, quien.getId());
        return jdbc.queryForObject("SELECT MAX(id) FROM rutinas WHERE usuario_id = ?", Integer.class, quien.getId());
    }

    private void sesion(Usuario quien, LocalDateTime inicio, boolean completada, Integer rutina) {
        jdbc.update("INSERT INTO sesiones_entrenamiento (usuario_id, fecha_inicio, fecha_fin, duracion_minutos, "
                        + "completada, rutina_id) VALUES (?, ?, ?, 45, ?, ?)",
                quien.getId(), Timestamp.valueOf(inicio), Timestamp.valueOf(inicio.plusMinutes(45)), completada, rutina);
    }
}
