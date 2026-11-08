package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.NivelExperiencia;
import com.gymprofit.api.enums.TipoObjetivo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// NombreRutinaCopiaIdiomaTest — los textos de las rutinas de un programa siguen el idioma
// de la petición (GP-245)
//
// Al seguir un programa, cada plantilla se copia con el nombre, la descripción y la categoría
// en el idioma de esa petición. Quien lo siguió en inglés y pasa la app a español seguía viendo
// «Upper A». Si el texto de la copia es el de su plantilla en cualquiera de los dos idiomas, la
// API devuelve el del idioma de la petición; si el usuario lo cambió, el suyo. Sin columnas
// nuevas. Se mira cada sitio que da el nombre de una rutina: sus rutinas, el programa que
// sigue, la sesión, el Historial y el diario; y, de paso, que el Historial traduzca también
// las rutinas del catálogo, que antes daba siempre en español.
// ============================================================
@DisplayName("GP-245 — el nombre de las rutinas de un programa, en el idioma de la petición")
class NombreRutinaCopiaIdiomaTest extends AbstractOwnershipTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    // El Historial lee con JDBC: lo cambiado por JPA en la transacción del test se vuelca antes.
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    @BeforeEach
    void sembrar() {
        CatalogoPlantillasDePrueba.sembrarCompleto(jdbc, dataSource);
        owner.setNivelExperiencia(NivelExperiencia.INTERMEDIO);
        owner.setObjetivo(TipoObjetivo.GANAR_MASA_MUSCULAR);
    }

    @Test
    @DisplayName("seguido en inglés y pedido en español: sus rutinas, en español (y al revés)")
    void seguido_en_ingles_pedido_en_espanol() throws Exception {
        seguir(owner, "en");

        JsonNode es = json(pedir(owner, "es", "GET /rutinas/usuario/" + owner.getId() + "/activas", null));
        JsonNode torso = porCodigo(es, "GIM-TORSO-A");
        assertThat(torso.get("nombre").asText()).isEqualTo("Torso A");
        assertThat(torso.get("categoria").asText()).isEqualTo("Gimnasio");
        assertThat(torso.get("descripcion").asText()).startsWith("Básicos:");

        JsonNode en = json(pedir(owner, "en", "GET /rutinas/usuario/" + owner.getId() + "/activas", null));
        assertThat(porCodigo(en, "GIM-TORSO-A").get("nombre").asText()).isEqualTo("Upper A");
        assertThat(porCodigo(en, "GIM-TORSO-A").get("categoria").asText()).isEqualTo("Gym");
    }

    @Test
    @DisplayName("seguido en español y pedido en inglés: en inglés")
    void seguido_en_espanol_pedido_en_ingles() throws Exception {
        seguir(owner, "es");
        Integer id = copia(owner, "GIM-TORSO-A");

        assertThat(json(pedir(owner, "en", "GET /rutinas/" + id, null)).get("nombre").asText()).isEqualTo("Upper A");
        assertThat(json(pedir(owner, "es", "GET /rutinas/" + id, null)).get("nombre").asText()).isEqualTo("Torso A");
    }

    @Test
    @DisplayName("el programa que sigue: el ciclo y las rutinas que tocan, en el idioma de la petición")
    void programa_seguido() throws Exception {
        seguir(owner, "en");

        JsonNode s = json(pedir(owner, "es", "GET /programas/seguido", null));
        assertThat(s.get("ciclo").get(0).get("rutinaNombre").asText()).isEqualTo("Torso A");
        assertThat(s.get("rutinas").get(0).get("nombre").asText()).isEqualTo("Torso A");
    }

    @Test
    @DisplayName("la sesión, el Historial y el diario: el nombre de la rutina en el idioma de la petición")
    void sesion_historial_y_diario() throws Exception {
        seguir(owner, "en");
        Integer sesion = entrenar(owner, copia(owner, "GIM-TORSO-A"));

        assertThat(json(pedir(owner, "es", "GET /sesiones/" + sesion, null)).get("rutinaNombre").asText())
                .isEqualTo("Torso A");
        assertThat(nombresDelHistorial(owner, "es")).containsExactly("Torso A");
        assertThat(nombresDelHistorial(owner, "en")).containsExactly("Upper A");
        JsonNode diario = json(pedir(owner, "es", "GET /diario?desde=2026-10-12&hasta=2026-10-18", null));
        assertThat(diario.toString()).contains("\"rutinaEntreno\":\"Torso A\"").doesNotContain("Upper A");
    }

    @Test
    @DisplayName("una rutina renombrada se queda como la dejó, en los dos idiomas")
    void renombrada() throws Exception {
        seguir(owner, "en");
        Integer id = copia(owner, "GIM-TORSO-A");
        MockHttpServletResponse r = pedir(owner, "es", "PATCH /rutinas/" + id, "{\"nombre\":\"Mi torso\"}");
        assertThat(r.getStatus()).as(r.getContentAsString()).isEqualTo(200);
        em.flush();
        Integer sesion = entrenar(owner, id);

        for (String idioma : List.of("es", "en")) {
            assertThat(json(pedir(owner, idioma, "GET /rutinas/" + id, null)).get("nombre").asText()).isEqualTo("Mi torso");
            assertThat(json(pedir(owner, idioma, "GET /sesiones/" + sesion, null)).get("rutinaNombre").asText())
                    .isEqualTo("Mi torso");
            assertThat(nombresDelHistorial(owner, idioma)).containsExactly("Mi torso");
            assertThat(json(pedir(owner, idioma, "GET /programas/seguido", null)).get("ciclo").get(0)
                    .get("rutinaNombre").asText()).isEqualTo("Mi torso");
        }
        // Lo que no tocó (la categoría) sigue el idioma de la petición.
        assertThat(json(pedir(owner, "es", "GET /rutinas/" + id, null)).get("categoria").asText()).isEqualTo("Gimnasio");
    }

    @Test
    @DisplayName("una rutina propia sin plantilla se queda como se escribió")
    void propia_sin_plantilla() throws Exception {
        Integer id = jdbc.queryForObject("SELECT id FROM rutinas WHERE codigo = 'GIM-TORSO-A'", Integer.class);
        jdbc.update("INSERT INTO rutinas (nombre, duracion_minutos, nivel, activa, usuario_id, es_plantilla) "
                + "VALUES ('Torso A', 45, 'INTERMEDIO', 1, ?, 0)", owner.getId());
        Integer propia = jdbc.queryForObject("SELECT MAX(id) FROM rutinas WHERE usuario_id = ?", Integer.class, owner.getId());
        assertThat(propia).isNotEqualTo(id);

        assertThat(json(pedir(owner, "en", "GET /rutinas/" + propia, null)).get("nombre").asText()).isEqualTo("Torso A");
    }

    @Test
    @DisplayName("el Historial traduce también una rutina del catálogo")
    void historial_rutina_del_catalogo() throws Exception {
        Integer plantilla = jdbc.queryForObject("SELECT id FROM rutinas WHERE codigo = 'GIM-PIERNA-A'", Integer.class);
        jdbc.update("UPDATE rutinas SET es_plantilla = 0, activa = 1 WHERE id = ?", plantilla);
        entrenar(owner, plantilla);

        assertThat(nombresDelHistorial(owner, "en")).containsExactly("Lower A");
        assertThat(nombresDelHistorial(owner, "es")).containsExactly("Pierna A");
    }

    // --- Las notas de los ejercicios de una copia (revisor del lote 10) ----------

    @Test
    @DisplayName("las notas de los ejercicios de una copia siguen el idioma de la petición; la cambiada se queda")
    void notas_de_la_copia() throws Exception {
        MockHttpServletResponse r = pedir(owner, "en", "POST /programas/PC-PE/seguir", "{\"minutos\":60}");
        assertThat(r.getStatus()).as(r.getContentAsString()).isEqualTo(201);
        Integer copia = copia(owner, "PC-PE-A");
        java.util.Map<String, Object> nota = jdbc.queryForMap("""
                SELECT p.notas, p.notas_en FROM rutina_ejercicio p JOIN rutinas pr ON pr.id = p.rutina_id
                WHERE pr.codigo = 'PC-PE-A' AND p.notas IS NOT NULL AND p.notas_en IS NOT NULL
                ORDER BY p.orden LIMIT 1""");
        String es = (String) nota.get("notas");
        String en = (String) nota.get("notas_en");

        assertThat(notas(owner, "es", copia)).contains(es).doesNotContain(en);
        assertThat(notas(owner, "en", copia)).contains(en).doesNotContain(es);
        JsonNode seguido = json(pedir(owner, "es", "GET /programas/seguido", null));
        assertThat(seguido.get("rutinas").toString()).contains(es).doesNotContain(en);

        // La que el usuario cambió se queda como la dejó.
        jdbc.update("UPDATE rutina_ejercicio SET notas = 'Mi nota' WHERE rutina_id = ? AND notas = ?", copia, en);
        em.clear();
        assertThat(notas(owner, "es", copia)).contains("Mi nota").doesNotContain(es);
        assertThat(notas(owner, "en", copia)).contains("Mi nota");
    }

    private List<String> notas(Usuario quien, String idioma, Integer rutina) throws Exception {
        List<String> n = new ArrayList<>();
        json(pedir(quien, idioma, "GET /rutinas-ejercicios/rutina/" + rutina + "/ordenados", null))
                .forEach(e -> n.add(e.get("notas").isNull() ? null : e.get("notas").asText()));
        return n;
    }

    // --- Ayudas --------------------------------------------------------------

    private void seguir(Usuario quien, String idioma) throws Exception {
        MockHttpServletResponse r = pedir(quien, idioma, "POST /programas/GIM-TP/seguir", "{\"minutos\":60}");
        assertThat(r.getStatus()).as(r.getContentAsString()).isEqualTo(201);
    }

    private Integer entrenar(Usuario quien, Integer rutinaId) throws Exception {
        String cuerpo = """
                {"claveIdempotencia":"%s","rutinaId":%d,"fechaInicio":"2026-10-14T18:00:00","duracionMinutos":45,
                 "completada":true,"ejercicios":[]}""".formatted(UUID.randomUUID(), rutinaId);
        return json(pedir(quien, "es", "POST /sesiones/completa", cuerpo)).get("id").asInt();
    }

    private Integer copia(Usuario quien, String plantilla) {
        return jdbc.queryForObject("""
                SELECT r.id FROM rutinas r JOIN rutinas p ON p.id = r.plantilla_id
                JOIN programas_usuario pu ON pu.id = r.programa_usuario_id
                WHERE r.usuario_id = ? AND p.codigo = ? AND pu.fecha_fin IS NULL""", Integer.class, quien.getId(), plantilla);
    }

    private List<String> nombresDelHistorial(Usuario quien, String idioma) throws Exception {
        List<String> nombres = new ArrayList<>();
        json(pedir(quien, idioma, "GET /sesiones/historial", null)).get("semanas")
                .forEach(s -> s.get("sesiones").forEach(x -> nombres.add(x.get("rutinaNombre").asText())));
        return nombres;
    }

    private static JsonNode porCodigo(JsonNode rutinas, String plantilla) {
        for (JsonNode r : rutinas) if (plantilla.equals(r.get("plantillaCodigo").asText())) return r;
        throw new AssertionError("sin copia de " + plantilla + " en " + rutinas);
    }

    private JsonNode json(MockHttpServletResponse r) throws Exception {
        assertThat(r.getStatus()).as(r.getContentAsString()).isBetween(200, 201);
        return objectMapper.readTree(r.getContentAsString());
    }

    private MockHttpServletResponse pedir(Usuario quien, String idioma, String verboYRuta, String cuerpo)
            throws Exception {
        String[] partes = verboYRuta.split(" ", 2);
        MockHttpServletRequestBuilder peticion = MockMvcRequestBuilders.request(HttpMethod.valueOf(partes[0]), partes[1])
                .header("Authorization", "Bearer " + token(quien))
                .header("Accept-Language", idioma)
                .accept(MediaType.APPLICATION_JSON);
        if (cuerpo != null) peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        return mockMvc.perform(peticion).andReturn().getResponse();
    }
}
