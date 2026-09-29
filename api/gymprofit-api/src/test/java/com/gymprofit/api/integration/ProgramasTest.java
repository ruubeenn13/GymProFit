package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.MedidaSerie;
import com.gymprofit.api.enums.Nivel;
import com.gymprofit.api.enums.NivelExperiencia;
import com.gymprofit.api.enums.PorLado;
import com.gymprofit.api.enums.TipoEjercicioRutina;
import com.gymprofit.api.enums.TipoObjetivo;
import com.gymprofit.api.service.programa.ReglasPrograma;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// ProgramasTest — GET /programas, GET /programas/{codigo} y seguir uno (GP-074)
//
// Con el contexto entero y JWT real, sobre el catálogo re-sembrado con sus ejercicios.
// Lo que se comprueba de seguir un programa: las copias (una por rutina distinta, del
// usuario del token, enlazadas a la plantilla y a su «programa que sigue»), las reglas
// de avanzado, de fuerza y de los cuatro tiempos aplicadas por la API, el idioma de las
// copias y que seguirlo otra vez no pisa lo que había.
//
// Acceso ajeno (DEC-014, DEC-027): el código de un programa es del catálogo y no tiene
// dueño, así que no hay 403 que dar en las lecturas; lo que se comprueba es el
// aislamiento. Las copias sí tienen dueño: 403 a quien no lo es.
// ============================================================
@DisplayName("GP-074 — programas: catálogo y seguir uno")
class ProgramasTest extends AbstractOwnershipTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private JsonNode catalogo;

    @BeforeEach
    void sembrar() {
        CatalogoPlantillasDePrueba.sembrarCompleto(jdbc, dataSource);
        catalogo = CatalogoPlantillasDePrueba.catalogo();
        perfil(owner, "INTERMEDIO", "GANAR_MASA_MUSCULAR");
    }

    // --- Catálogo ------------------------------------------------------------

    @Test
    @DisplayName("GET /programas da los 13, en el orden del catálogo, a un usuario y al invitado")
    void lista_los_13() throws Exception {
        List<String> esperados = new ArrayList<>();
        catalogo.get("programas").forEach(p -> esperados.add(p.get("codigo").asText()));
        for (Usuario quien : List.of(owner, guest)) {
            assertThat(codigos(json(pedir(quien, "es", "GET /programas", null)))).isEqualTo(esperados);
        }
        JsonNode tp = porCodigo(json(pedir(owner, "es", "GET /programas", null)), "GIM-TP");
        assertThat(tp.get("nombre").asText()).isEqualTo("Torso y pierna");
        assertThat(tp.get("semana")).hasSize(4);
        assertThat(tp.get("semana").get(0).get("rutinaCodigo").asText()).isEqualTo("GIM-TORSO-A");
        assertThat(tp.get("semana").get(0).get("rutinaNombre").asText()).isEqualTo("Torso A");
    }

    @Test
    @DisplayName("en inglés, los textos del catálogo en inglés")
    void lista_en_ingles() throws Exception {
        JsonNode tp = porCodigo(json(pedir(owner, "en", "GET /programas", null)), "GIM-TP");
        assertThat(tp.get("nombre").asText()).isEqualTo("Upper/lower");
        assertThat(tp.get("descripcion").asText()).startsWith("Two upper-body and two lower-body days");
        assertThat(tp.get("semana").get(0).get("rutinaNombre").asText()).isEqualTo("Upper A");
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = ';', value = {
            "equipamiento=PESO_CORPORAL;PC-PE,PC-CC,PC-TP",
            "dias=4;GIM-TP,MAN-TP,PC-TP",
            "dias=6;GIM-ETP,MAN-ETP",
            "nivel=PRINCIPIANTE;GIM-PE,MAN-PE,PC-PE",
            "nivel=avanzado&equipamiento=GIMNASIO;GIM-CC,GIM-TP,GIM-TPET,GIM-ETP",
            "nivel=EXPERTO&dias=5;GIM-TPET,MAN-TPET",
            "equipamiento=MANCUERNAS&dias=2&nivel=INTERMEDIO;MAN-CC",
            "equipamiento=PESO_CORPORAL&dias=6;"})
    @DisplayName("filtra por equipamiento, días y nivel; el avanzado y el experto dan los de intermedio")
    void filtros(String filtros, String esperados) throws Exception {
        List<String> lista = esperados == null ? List.of() : List.of(esperados.split(","));
        assertThat(codigos(json(pedir(owner, "es", "GET /programas?" + filtros, null)))).isEqualTo(lista);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"equipamiento=KETTLEBELL", "nivel=LEYENDA", "dias=0", "dias=8"})
    @DisplayName("un filtro no válido es 400")
    void filtro_no_valido(String filtro) throws Exception {
        assertThat(pedir(owner, "es", "GET /programas?" + filtro, null).getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("GET /programas/{codigo}: la semana, que repite, y cada rutina distinta con los ejercicios del JSON")
    void detalle() throws Exception {
        JsonNode etp = json(pedir(owner, "es", "GET /programas/GIM-ETP", null));
        List<String> semana = new ArrayList<>();
        etp.get("semana").forEach(d -> semana.add(d.get("rutinaCodigo").asText()));
        assertThat(semana).containsExactly("GIM-EMPUJE", "GIM-TIRON", "GIM-PIERNA-A", "GIM-EMPUJE", "GIM-TIRON",
                "GIM-PIERNA-B");
        assertThat(codigos(etp.get("rutinas"))).containsExactly("GIM-EMPUJE", "GIM-TIRON", "GIM-PIERNA-A",
                "GIM-PIERNA-B");

        Map<Integer, String> fedIds = fedIds();
        for (JsonNode r : etp.get("rutinas")) {
            JsonNode enJson = rutinaJson(r.get("codigo").asText());
            assertThat(r.get("duracionMinutos").asInt()).isEqualTo(enJson.get("duracionMinutos").asInt());
            assertThat(r.get("ejercicios")).hasSize(enJson.get("ejercicios").size());
            for (int i = 0; i < enJson.get("ejercicios").size(); i++) {
                JsonNode e = r.get("ejercicios").get(i);
                JsonNode j = enJson.get("ejercicios").get(i);
                assertThat(fedIds.get(e.get("ejercicioId").asInt())).isEqualTo(j.get("fedId").asText());
                assertThat(e.get("series").asInt()).isEqualTo(j.get("series").asInt());
                assertThat(e.get("repeticiones").asInt()).isEqualTo(j.get("max").asInt());
                assertThat(e.get("repeticionesMin").asInt()).isEqualTo(j.get("min").asInt());
                assertThat(e.get("repeticionesMax").asInt()).isEqualTo(j.get("max").asInt());
                assertThat(e.get("tiempoDescanso").asInt()).isEqualTo(j.get("descansoSegundos").asInt());
                assertThat(e.get("medida").asText()).isEqualTo(j.get("medida").asText());
                assertThat(e.get("tipo").asText()).isEqualTo(j.get("tipo").asText());
            }
        }
        assertThat(pedir(owner, "es", "GET /programas/NO-EXISTE", null).getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("el detalle en inglés trae la nota y el nombre del ejercicio en inglés")
    void detalle_en_ingles() throws Exception {
        jdbc.update("UPDATE ejercicios SET nombre_en = 'Pull-ups' WHERE fed_id = 'Pullups'");
        JsonNode torso = porCodigo(json(pedir(owner, "en", "GET /programas/GIM-TP", null)).get("rutinas"), "GIM-TORSO-B");
        JsonNode dominadas = torso.get("ejercicios").get(1);
        assertThat(dominadas.get("nombreEjercicio").asText()).isEqualTo("Pull-ups");
        assertThat(dominadas.get("notas").asText()).isEqualTo(rutinaJson("GIM-TORSO-B").get("ejercicios").get(1)
                .get("notaEn").asText());
        assertThat(torso.get("nombre").asText()).isEqualTo("Upper B");
    }

    // --- Seguir un programa --------------------------------------------------

    @Test
    @DisplayName("seguir GIM-TP con 45 min crea sus cuatro rutinas, del usuario, ninguna de más de 45")
    void seguir_crea_las_copias() throws Exception {
        MockHttpServletResponse r = pedir(owner, "es", "POST /programas/GIM-TP/seguir", "{\"minutos\":45}");
        assertThat(r.getStatus()).isEqualTo(201);
        JsonNode creado = objectMapper.readTree(r.getContentAsString());

        assertThat(creado.get("programaCodigo").asText()).isEqualTo("GIM-TP");
        assertThat(creado.get("minutos").asInt()).isEqualTo(45);
        JsonNode copias = creado.get("rutinas");
        assertThat(copias).hasSize(4);
        Map<String, Object> sigue = jdbc.queryForMap("SELECT usuario_id, minutos FROM programas_usuario WHERE id = ?",
                creado.get("id").asInt());
        assertThat(sigue).containsEntry("usuario_id", owner.getId()).containsEntry("minutos", 45);

        List<String> plantillas = new ArrayList<>();
        for (JsonNode c : copias) {
            plantillas.add(c.get("plantillaCodigo").asText());
            assertThat(c.get("usuarioId").asText()).isEqualTo(String.valueOf(owner.getId()));
            assertThat(c.get("programaUsuarioId").asInt()).isEqualTo(creado.get("id").asInt());
            assertThat(c.get("programaCodigo").asText()).isEqualTo("GIM-TP");
            assertThat(c.get("esPredefinida").asBoolean()).isFalse();
            assertThat(c.get("duracionMinutos").asInt()).isLessThanOrEqualTo(45);
            assertThat(c.get("numEjercicios").asInt()).isEqualTo(c.get("ejercicios").size());
            esperarReglas(c, NivelExperiencia.INTERMEDIO, TipoObjetivo.GANAR_MASA_MUSCULAR, 45);
        }
        assertThat(plantillas).containsExactly("GIM-TORSO-A", "GIM-PIERNA-A", "GIM-TORSO-B", "GIM-PIERNA-B");

        // Y el usuario las ve en sus rutinas, con de dónde salen.
        JsonNode suyas = json(pedir(owner, "es", "GET /rutinas/usuario/" + owner.getId() + "/activas", null));
        assertThat(suyas).hasSize(4);
        for (JsonNode s : suyas) {
            assertThat(s.get("programaCodigo").asText()).isEqualTo("GIM-TP");
            assertThat(s.get("plantillaCodigo").asText()).startsWith("GIM-");
            JsonNode ejercicios = json(pedir(owner, "es", "GET /rutinas-ejercicios/rutina/" + s.get("id").asInt(), null));
            assertThat(ejercicios).isNotEmpty();
            assertThat(ejercicios.get(0).get("tipo").asText()).isEqualTo("BASICO");
        }
    }

    @ParameterizedTest(name = "{0} min")
    @ValueSource(ints = {30, 45, 60, 75})
    @DisplayName("los cuatro tiempos: cada copia es la plantilla con las reglas del catálogo y cabe en el tiempo")
    void los_cuatro_tiempos(int minutos) throws Exception {
        for (String programa : List.of("GIM-ETP", "MAN-TPET", "PC-CC")) {
            JsonNode creado = json(pedir(owner, "es", "POST /programas/" + programa + "/seguir",
                    "{\"minutos\":" + minutos + "}"));
            for (JsonNode c : creado.get("rutinas")) {
                assertThat(c.get("duracionMinutos").asInt()).isLessThanOrEqualTo(minutos);
                esperarReglas(c, NivelExperiencia.INTERMEDIO, TipoObjetivo.GANAR_MASA_MUSCULAR, minutos);
            }
        }
    }

    @Test
    @DisplayName("avanzado: en un programa de intermedio, básicos a 4 series y extras a 3 (con 75, una más a los básicos)")
    void avanzado() throws Exception {
        perfil(owner, "AVANZADO", "PERDER_PESO");
        JsonNode creado = json(pedir(owner, "es", "POST /programas/GIM-TP/seguir", "{\"minutos\":75}"));
        for (JsonNode c : creado.get("rutinas")) {
            assertThat(c.get("nivel").asText()).isEqualTo("AVANZADO");
            for (JsonNode e : c.get("ejercicios")) {
                assertThat(e.get("series").asInt()).isEqualTo(e.get("tipo").asText().equals("BASICO") ? 5 : 3);
            }
            esperarReglas(c, NivelExperiencia.AVANZADO, TipoObjetivo.PERDER_PESO, 75);
        }
        // En uno de principiante, el avanzado no cambia nada.
        JsonNode pe = json(pedir(owner, "es", "POST /programas/GIM-PE/seguir", "{\"minutos\":60}"));
        for (JsonNode c : pe.get("rutinas")) {
            assertThat(c.get("nivel").asText()).isEqualTo("PRINCIPIANTE");
            esperarReglas(c, NivelExperiencia.AVANZADO, TipoObjetivo.PERDER_PESO, 60);
        }
    }

    @Test
    @DisplayName("«Ganar fuerza»: los básicos con carga a 4–6 y 3 min; las dominadas, que son de peso corporal, no")
    void fuerza() throws Exception {
        perfil(owner, "INTERMEDIO", "MEJORAR_FUERZA");
        JsonNode creado = json(pedir(owner, "es", "POST /programas/GIM-CC/seguir", "{\"minutos\":75}"));
        Map<Integer, String> fedIds = fedIds();
        for (JsonNode c : creado.get("rutinas")) {
            for (JsonNode e : c.get("ejercicios")) {
                if (!e.get("tipo").asText().equals("BASICO")) continue;
                boolean pesoCorporal = CatalogoPlantillasDePrueba.PESO_CORPORAL
                        .contains(fedIds.get(e.get("ejercicioId").asInt()));
                assertThat(e.get("repeticionesMin").asInt() == 4 && e.get("repeticionesMax").asInt() == 6
                        && e.get("repeticiones").asInt() == 6 && e.get("tiempoDescanso").asInt() == 180)
                        .as(fedIds.get(e.get("ejercicioId").asInt())).isEqualTo(!pesoCorporal);
            }
            esperarReglas(c, NivelExperiencia.INTERMEDIO, TipoObjetivo.MEJORAR_FUERZA, 75);
        }
    }

    @Test
    @DisplayName("sin cuerpo, 60 min; con minutos que no son 30, 45, 60 ni 75, 400 y no se crea nada")
    void minutos() throws Exception {
        JsonNode creado = json(pedir(owner, "es", "POST /programas/GIM-PE/seguir", null));
        assertThat(creado.get("minutos").asInt()).isEqualTo(60);
        int antes = contarRutinas(owner);
        for (String cuerpo : List.of("{\"minutos\":50}", "{\"minutos\":0}", "{\"minutos\":90}")) {
            assertThat(pedir(owner, "es", "POST /programas/GIM-PE/seguir", cuerpo).getStatus()).as(cuerpo).isEqualTo(400);
        }
        assertThat(pedir(owner, "es", "POST /programas/NO-EXISTE/seguir", "{}").getStatus()).isEqualTo(404);
        assertThat(contarRutinas(owner)).isEqualTo(antes);
    }

    @Test
    @DisplayName("seguirlo otra vez crea otras copias y no toca las que había")
    void seguir_dos_veces() throws Exception {
        JsonNode primera = json(pedir(owner, "es", "POST /programas/PC-PE/seguir", "{\"minutos\":45}"));
        Integer copia = primera.get("rutinas").get(0).get("id").asInt();
        jdbc.update("UPDATE rutinas SET nombre = 'La mía' WHERE id = ?", copia);

        JsonNode segunda = json(pedir(owner, "es", "POST /programas/PC-PE/seguir", "{\"minutos\":30}"));
        assertThat(segunda.get("id").asInt()).isNotEqualTo(primera.get("id").asInt());
        assertThat(contarRutinas(owner)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT nombre FROM rutinas WHERE id = ?", String.class, copia)).isEqualTo("La mía");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM programas_usuario WHERE usuario_id = ?", Integer.class,
                owner.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("las copias van en el idioma de la petición y ya son del usuario")
    void idioma_de_las_copias() throws Exception {
        JsonNode en = json(pedir(owner, "en", "POST /programas/PC-PE/seguir", "{\"minutos\":60}"));
        JsonNode a = en.get("rutinas").get(0);
        assertThat(a.get("nombre").asText()).isEqualTo("Getting started A");
        assertThat(a.get("categoria").asText()).isEqualTo("Bodyweight");
        assertThat(a.get("ejercicios").get(1).get("notas").asText())
                .isEqualTo(rutinaJson("PC-PE-A").get("ejercicios").get(1).get("notaEn").asText());
        // Leída después en español sigue en inglés: el texto es suyo, no del catálogo.
        JsonNode leida = json(pedir(owner, "es", "GET /rutinas/" + a.get("id").asInt(), null));
        assertThat(leida.get("nombre").asText()).isEqualTo("Getting started A");

        JsonNode es = json(pedir(owner, "es", "POST /programas/PC-PE/seguir", "{\"minutos\":60}"));
        assertThat(es.get("rutinas").get(0).get("nombre").asText()).isEqualTo("Para empezar A");
        assertThat(es.get("rutinas").get(0).get("ejercicios").get(1).get("notas").asText())
                .isEqualTo(rutinaJson("PC-PE-A").get("ejercicios").get(1).get("nota").asText());
    }

    // --- Acceso ajeno --------------------------------------------------------

    @Test
    @DisplayName("el invitado lee el catálogo pero no puede seguir un programa: 403 y nada creado")
    void invitado() throws Exception {
        assertThat(pedir(guest, "es", "GET /programas/GIM-TP", null).getStatus()).isEqualTo(200);
        assertThat(pedir(guest, "es", "POST /programas/GIM-TP/seguir", "{\"minutos\":45}").getStatus()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM programas_usuario WHERE usuario_id = ?", Integer.class,
                guest.getId())).isZero();
    }

    @Test
    @DisplayName("seguir un programa solo escribe para el usuario del token; sus copias son 403 para otro")
    void acceso_ajeno() throws Exception {
        String detalleAntes = pedir(attacker, "es", "GET /programas/GIM-TP", null).getContentAsString();
        String listaAntes = pedir(attacker, "es", "GET /programas", null).getContentAsString();

        JsonNode delDueno = json(pedir(owner, "es", "POST /programas/GIM-TP/seguir", "{\"minutos\":45}"));
        Integer copia = delDueno.get("rutinas").get(0).get("id").asInt();

        // Catálogo: el id no tiene dueño, así que lo que se comprueba es el aislamiento:
        // que el otro vea exactamente lo mismo que antes, sin rastro de lo del dueño.
        assertThat(pedir(attacker, "es", "GET /programas/GIM-TP", null).getContentAsString()).isEqualTo(detalleAntes);
        assertThat(pedir(attacker, "es", "GET /programas", null).getContentAsString()).isEqualTo(listaAntes);

        // Las copias sí tienen dueño.
        assertThat(pedir(attacker, "es", "GET /rutinas/" + copia, null).getStatus()).isEqualTo(403);
        assertThat(pedir(attacker, "es", "GET /rutinas-ejercicios/rutina/" + copia, null).getStatus()).isEqualTo(403);
        assertThat(pedir(attacker, "es", "PATCH /rutinas/" + copia, "{\"nombre\":\"x\"}").getStatus()).isEqualTo(403);
        assertThat(pedir(attacker, "es", "GET /rutinas/usuario/" + owner.getId() + "/activas", null).getStatus())
                .isEqualTo(403);

        // Lo que sigue el otro es suyo y no toca lo del dueño.
        JsonNode delOtro = json(pedir(attacker, "es", "POST /programas/GIM-TP/seguir", "{\"minutos\":45}"));
        for (JsonNode c : delOtro.get("rutinas")) {
            assertThat(c.get("usuarioId").asText()).isEqualTo(String.valueOf(attacker.getId()));
        }
        assertThat(contarRutinas(owner)).isEqualTo(4);
        assertThat(contarRutinas(attacker)).isEqualTo(4);
    }

    // --- Ayudas --------------------------------------------------------------

    // La copia tiene que ser exactamente la plantilla del JSON pasada por las reglas.
    private void esperarReglas(JsonNode copia, NivelExperiencia nivel, TipoObjetivo objetivo, int minutos) {
        JsonNode plantilla = rutinaJson(copia.get("plantillaCodigo").asText());
        Nivel nivelPrograma = Nivel.valueOf(plantilla.get("nivel").asText());
        List<ReglasPrograma.Ejercicio> entrada = new ArrayList<>();
        int i = 0;
        for (JsonNode e : plantilla.get("ejercicios")) {
            entrada.add(new ReglasPrograma.Ejercicio(i++, TipoEjercicioRutina.valueOf(e.get("tipo").asText()),
                    e.get("series").asInt(), e.get("min").asInt(), e.get("max").asInt(),
                    MedidaSerie.valueOf(e.get("medida").asText()),
                    e.get("porLado").isNull() ? null : PorLado.valueOf(e.get("porLado").asText()),
                    e.get("descansoSegundos").asInt(),
                    CatalogoPlantillasDePrueba.PESO_CORPORAL.contains(e.get("fedId").asText())));
        }
        ReglasPrograma.Resultado esperado = ReglasPrograma.aplicar(entrada, nivelPrograma, nivel, objetivo, minutos);

        assertThat(copia.get("duracionMinutos").asInt()).isEqualTo(esperado.duracion());
        JsonNode ejercicios = copia.get("ejercicios");
        assertThat(ejercicios).hasSize(esperado.ejercicios().size());
        for (int k = 0; k < ejercicios.size(); k++) {
            JsonNode e = ejercicios.get(k);
            ReglasPrograma.Ejercicio x = esperado.ejercicios().get(k);
            assertThat(e.get("orden").asInt()).isEqualTo(k + 1);
            assertThat(e.get("series").asInt()).isEqualTo(x.series());
            assertThat(e.get("repeticionesMin").asInt()).isEqualTo(x.min());
            assertThat(e.get("repeticionesMax").asInt()).isEqualTo(x.max());
            assertThat(e.get("repeticiones").asInt()).isEqualTo(x.max());
            assertThat(e.get("tiempoDescanso").asInt()).isEqualTo(x.descanso());
            assertThat(e.get("tipo").asText()).isEqualTo(x.tipo().name());
        }
    }

    // Sobre la entidad y no por JDBC: el usuario ya está en el contexto de persistencia
    // de la transacción del test, y el servicio lo leería de ahí sin ver el UPDATE.
    private void perfil(Usuario u, String nivel, String objetivo) {
        u.setNivelExperiencia(NivelExperiencia.valueOf(nivel));
        u.setObjetivo(TipoObjetivo.valueOf(objetivo));
    }

    private int contarRutinas(Usuario u) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM rutinas WHERE usuario_id = ?", Integer.class, u.getId());
    }

    private Map<Integer, String> fedIds() {
        Map<Integer, String> mapa = new HashMap<>();
        jdbc.query("SELECT id, fed_id FROM ejercicios WHERE fed_id IS NOT NULL",
                rs -> { mapa.put(rs.getInt("id"), rs.getString("fed_id")); });
        return mapa;
    }

    private JsonNode rutinaJson(String codigo) {
        for (JsonNode r : catalogo.get("rutinas")) {
            if (r.get("codigo").asText().equals(codigo)) return r;
        }
        throw new IllegalArgumentException(codigo);
    }

    private static JsonNode porCodigo(JsonNode lista, String codigo) {
        for (JsonNode p : lista) {
            if (p.get("codigo").asText().equals(codigo)) return p;
        }
        throw new AssertionError("No está " + codigo);
    }

    private static List<String> codigos(JsonNode lista) {
        List<String> c = new ArrayList<>();
        lista.forEach(p -> c.add(p.get("codigo").asText()));
        return c;
    }

    private JsonNode json(MockHttpServletResponse r) throws Exception {
        assertThat(r.getStatus()).as(r.getContentAsString()).isBetween(200, 201);
        return objectMapper.readTree(r.getContentAsString());
    }

    // "VERBO /ruta" como {@code quien}, con JWT real y el idioma indicado.
    private MockHttpServletResponse pedir(Usuario quien, String idioma, String verboYRuta, String cuerpo)
            throws Exception {
        String[] partes = verboYRuta.split(" ", 2);
        MockHttpServletRequestBuilder peticion = MockMvcRequestBuilders
                .request(org.springframework.http.HttpMethod.valueOf(partes[0]), partes[1])
                .header("Authorization", "Bearer " + token(quien))
                .header("Accept-Language", idioma)
                .accept(MediaType.APPLICATION_JSON);
        if (cuerpo != null) peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        return mockMvc.perform(peticion).andReturn().getResponse();
    }
}
