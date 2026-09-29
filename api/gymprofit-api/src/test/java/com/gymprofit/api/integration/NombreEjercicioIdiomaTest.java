package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioCreateDTO;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.enums.Dificultad;
import com.gymprofit.api.enums.GrupoMuscular;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.service.rutina.IRutinaService;
import com.gymprofit.api.service.rutinaejercicio.IRutinaEjercicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// NombreEjercicioIdiomaTest — GP-132: el nombre de un ejercicio sale en el idioma de
// la petición en todas las respuestas que lo llevan ya escrito.
//
// Con Accept-Language: en, el nombre en inglés; si el ejercicio no lo tiene, el
// español. Sin cabecera o en español, el español. Se piden como la app, con JWT real:
// los ejercicios de una rutina (/rutinas-ejercicios), el récord destacado y el
// ejercicio más frecuente de las estadísticas.
// ============================================================
@DisplayName("GP-132 — el nombre del ejercicio, en el idioma de la petición")
class NombreEjercicioIdiomaTest extends AbstractOwnershipTest {

    @Autowired
    private IEjercicioRepository ejercicioRepository;

    @Autowired
    private IRutinaService rutinaService;

    @Autowired
    private IRutinaEjercicioService rutinaEjercicioService;

    private Integer plancha;
    private Integer soloEspanol;
    private Integer rutina;
    private Integer primeraDeLaRutina;

    @BeforeEach
    void sembrar() {
        plancha = ejercicio("Plancha", "Plank");
        soloEspanol = ejercicio("Remo con banda", null);
        runAs(owner, () -> {
            RutinaCreateDTO r = new RutinaCreateDTO();
            r.setUsuarioId(owner.getId());
            r.setNombre("Rutina GP-132");
            r.setNivel("INTERMEDIO");
            r.setEsPredefinida(false);
            rutina = rutinaService.save(r).getId();
            primeraDeLaRutina = enRutina(plancha, 1);
            enRutina(soloEspanol, 2);
        });
    }

    private Integer ejercicio(String es, String en) {
        Ejercicio e = new Ejercicio();
        e.setNombre(es);
        e.setNombreEn(en);
        e.setGrupoMuscular(GrupoMuscular.values()[0]);
        e.setDificultad(Dificultad.values()[0]);
        e.setCaloriasQuemadas(10);
        e.setActivo(true);
        return ejercicioRepository.save(e).getId();
    }

    private Integer enRutina(Integer ejercicioId, int orden) {
        RutinaEjercicioCreateDTO re = new RutinaEjercicioCreateDTO();
        re.setRutinaId(rutina);
        re.setEjercicioId(ejercicioId);
        re.setSeries(3);
        re.setRepeticiones(10);
        re.setPesoRecomendado(new BigDecimal("0"));
        re.setTiempoDescanso(60);
        re.setOrden(orden);
        return rutinaEjercicioService.save(re).getId();
    }

    /** GET como el dueño, con el idioma dado (null: sin cabecera). */
    private JsonNode leer(String idioma, String ruta) throws Exception {
        var peticion = MockMvcRequestBuilders.get(ruta)
                .header("Authorization", "Bearer " + token(owner))
                .accept(MediaType.APPLICATION_JSON);
        if (idioma != null) peticion.header("Accept-Language", idioma);
        var respuesta = mockMvc.perform(peticion).andReturn().getResponse();
        assertThat(respuesta.getStatus()).as(ruta).isEqualTo(200);
        return objectMapper.readTree(respuesta.getContentAsString());
    }

    private static List<String> nombres(JsonNode lista) {
        List<String> r = new ArrayList<>();
        lista.forEach(n -> r.add(n.get("nombreEjercicio").asText()));
        return r;
    }

    @Test
    @DisplayName("los ejercicios de una rutina: «Plank» en inglés, y el español si no hay inglés")
    void ejercicios_de_una_rutina() throws Exception {
        assertThat(nombres(leer("en", "/rutinas-ejercicios/rutina/" + rutina + "/ordenados")))
                .containsExactly("Plank", "Remo con banda");
        assertThat(nombres(leer("en", "/rutinas-ejercicios/rutina/" + rutina)))
                .containsExactlyInAnyOrder("Plank", "Remo con banda");
        assertThat(leer("en", "/rutinas-ejercicios/" + primeraDeLaRutina).get("nombreEjercicio").asText())
                .isEqualTo("Plank");

        assertThat(nombres(leer("es", "/rutinas-ejercicios/rutina/" + rutina + "/ordenados")))
                .containsExactly("Plancha", "Remo con banda");
        assertThat(nombres(leer(null, "/rutinas-ejercicios/rutina/" + rutina + "/ordenados")))
                .containsExactly("Plancha", "Remo con banda");
    }

    /** Guarda una sesión con una serie del ejercicio dado. */
    private void entrenar(Integer ejercicioId, String fecha, String peso) throws Exception {
        String cuerpo = """
                {"claveIdempotencia":"%s","fechaInicio":"%sT18:00:00","duracionMinutos":45,
                 "completada":true,"ejercicios":[{"ejercicioId":%d,
                 "series":[{"numero":1,"repeticiones":5,"peso":%s,"completada":true}]}]}
                """.formatted(UUID.randomUUID(), fecha, ejercicioId, peso);
        assertThat(pedir(owner, "POST /sesiones/completa", cuerpo).andReturn().getResponse().getStatus())
                .isBetween(200, 201);
    }

    @Test
    @DisplayName("el récord destacado y el ejercicio más frecuente, en el idioma de la petición")
    void destacado_y_mas_frecuente() throws Exception {
        entrenar(plancha, "2026-09-14", "10");
        entrenar(plancha, "2026-09-16", "20");

        String destacado = "/progreso-ejercicios/usuario/" + owner.getId() + "/record-destacado";
        assertThat(leer("en", destacado).get("ejercicioNombre").asText()).isEqualTo("Plank");
        assertThat(leer("es", destacado).get("ejercicioNombre").asText()).isEqualTo("Plancha");

        String estadisticas = "/usuarios/" + owner.getId() + "/estadisticas";
        assertThat(leer("en", estadisticas).get("ejercicioMasFrecuente").asText()).isEqualTo("Plank");
        assertThat(leer("es", estadisticas).get("ejercicioMasFrecuente").asText()).isEqualTo("Plancha");
    }

    @Test
    @DisplayName("sin nombre en inglés, el récord destacado y el más frecuente salen en español")
    void sin_ingles_el_espanol() throws Exception {
        entrenar(soloEspanol, "2026-09-14", "10");
        entrenar(soloEspanol, "2026-09-16", "20");

        assertThat(leer("en", "/progreso-ejercicios/usuario/" + owner.getId() + "/record-destacado")
                .get("ejercicioNombre").asText()).isEqualTo("Remo con banda");
        assertThat(leer("en", "/usuarios/" + owner.getId() + "/estadisticas")
                .get("ejercicioMasFrecuente").asText()).isEqualTo("Remo con banda");
    }
}
