package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.NivelExperiencia;
import com.gymprofit.api.enums.TipoObjetivo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// ProgramaSeguidoTest — lo que la API añade en el lote 1.2.1 (GP-074)
//
// El programa que se sigue (uno a la vez), la rutina que toca, dejarlo, cambiar el
// tiempo, el recomendado, la vista previa (igual a lo que crea seguir y sin guardar
// nada), rutinaNombre con la rutina desactivada, la migración que cierra los duplicados
// y el acceso ajeno en cada ruta nueva. Con el contexto entero y JWT real, sobre el
// catálogo re-sembrado con sus ejercicios.
// ============================================================
@DisplayName("GP-074 — el programa que se sigue")
class ProgramaSeguidoTest extends AbstractOwnershipTest {

    private static final String CIERRE = "db/migration/V202609301001__Programa_seguido_uno_abierto.sql";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private int dia = 1;

    @BeforeEach
    void sembrar() {
        CatalogoPlantillasDePrueba.sembrarCompleto(jdbc, dataSource);
        perfil(owner, NivelExperiencia.INTERMEDIO, TipoObjetivo.GANAR_MASA_MUSCULAR);
    }

    // --- Seguir y la rutina que toca ----------------------------------------

    @Test
    @DisplayName("sin programa, 204; al seguir uno, toca la primera y sus rutinas salen en el orden en que tocan")
    void seguido() throws Exception {
        assertThat(pedir(owner, "es", "GET /programas/seguido", null).getStatus()).isEqualTo(204);
        seguir(owner, "GIM-TP", 60);

        JsonNode s = json(pedir(owner, "es", "GET /programas/seguido", null));
        assertThat(s.get("programa").get("codigo").asText()).isEqualTo("GIM-TP");
        assertThat(s.get("minutos").asInt()).isEqualTo(60);
        assertThat(s.get("posicionInicial").asInt()).isEqualTo(1);
        assertThat(s.get("posicionHoy").asInt()).isEqualTo(1);
        assertThat(s.get("ciclo")).hasSize(4);
        assertThat(s.get("ciclo").get(1).get("rutinaNombre").asText()).isEqualTo("Pierna A");
        assertThat(nombres(s.get("rutinas"))).containsExactly("Torso A", "Pierna A", "Torso B", "Pierna B");
        assertThat(s.get("rutinas").get(0).get("ejercicios")).isNotEmpty();
        // Cada copia dice de qué programa es, en el idioma de la petición.
        assertThat(s.get("rutinas").get(0).get("programaNombre").asText()).isEqualTo("Torso y pierna");
        JsonNode suya = json(pedir(owner, "en", "GET /rutinas/" + s.get("rutinas").get(0).get("id").asInt(), null));
        assertThat(suya.get("programaNombre").asText()).isEqualTo("Upper/lower");
        assertThat(suya.get("programaCodigo").asText()).isEqualTo("GIM-TP");
    }

    @Test
    @DisplayName("cada sesión lleva a la siguiente, una fuera de orden salta las de en medio, y se marcan las hechas")
    void la_que_toca() throws Exception {
        seguir(owner, "GIM-TP", 60);
        entrenar(owner, copia(owner, "GIM-TORSO-A"));
        JsonNode s = json(pedir(owner, "es", "GET /programas/seguido", null));
        assertThat(s.get("posicionHoy").asInt()).isEqualTo(2);
        assertThat(s.get("ciclo").get(0).get("hecha").asBoolean()).isTrue();
        assertThat(nombres(s.get("rutinas"))).containsExactly("Pierna A", "Torso B", "Pierna B", "Torso A");

        entrenar(owner, copia(owner, "GIM-TORSO-B"));
        assertThat(hoy(owner)).isEqualTo(4);
    }

    @Test
    @DisplayName("el de 6 días: Empuje y Tirón se repiten y cuenta la aparición más próxima")
    void repetidas() throws Exception {
        seguir(owner, "GIM-ETP", 60);
        for (String r : List.of("GIM-EMPUJE", "GIM-TIRON", "GIM-PIERNA-A", "GIM-EMPUJE")) entrenar(owner, copia(owner, r));
        assertThat(hoy(owner)).isEqualTo(5);
        JsonNode s = json(pedir(owner, "es", "GET /programas/seguido", null));
        // Cada rutina una vez: Tirón, Pierna B, Empuje, Pierna A.
        assertThat(nombres(s.get("rutinas"))).containsExactly("Tirón", "Pierna B", "Empuje", "Pierna A");
    }

    @Test
    @DisplayName("una rutina borrada se salta; si las borra todas, no toca ninguna")
    void borrada() throws Exception {
        seguir(owner, "GIM-TP", 60);
        assertThat(pedir(owner, "es", "DELETE /rutinas/" + copia(owner, "GIM-TORSO-A"), null).getStatus()).isEqualTo(200);
        assertThat(hoy(owner)).isEqualTo(2);
        for (String r : List.of("GIM-PIERNA-A", "GIM-TORSO-B", "GIM-PIERNA-B")) {
            pedir(owner, "es", "DELETE /rutinas/" + copia(owner, r), null);
        }
        JsonNode s = json(pedir(owner, "es", "GET /programas/seguido", null));
        assertThat(s.get("posicionHoy").isNull()).isTrue();
        assertThat(s.get("rutinas")).isEmpty();
        assertThat(s.get("ciclo").get(0).get("activa").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("seguir otro deja el anterior: sus rutinas se desactivan y sus sesiones no cuentan")
    void seguir_otro() throws Exception {
        seguir(owner, "GIM-TP", 60);
        Integer torso = copia(owner, "GIM-TORSO-A");
        Integer sesion = entrenar(owner, torso);

        JsonNode nuevo = seguir(owner, "MAN-TP", 45);
        assertThat(nuevo.get("posicionInicial").asInt()).isEqualTo(1);
        assertThat(hoy(owner)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT activa FROM rutinas WHERE id = ?", Boolean.class, torso)).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM programas_usuario WHERE usuario_id = ? AND fecha_fin IS NULL",
                Integer.class, owner.getId())).isOne();
        // La sesión sigue, y con el nombre de su rutina aunque esté desactivada.
        JsonNode s = json(pedir(owner, "es", "GET /sesiones/" + sesion, null));
        assertThat(s.get("rutinaNombre").asText()).isEqualTo("Torso A");
        assertThat(json(pedir(owner, "es", "GET /sesiones/usuario/" + owner.getId(), null)).get(0)
                .get("rutinaNombre").asText()).isEqualTo("Torso A");
    }

    @Test
    @DisplayName("cambiar el tiempo del mismo programa: rutinas nuevas y sigue tocando la que tocaba")
    void cambiar_el_tiempo() throws Exception {
        seguir(owner, "GIM-TP", 60);
        Integer torso = copia(owner, "GIM-TORSO-A");
        entrenar(owner, torso);
        assertThat(hoy(owner)).isEqualTo(2);

        JsonNode nuevo = seguir(owner, "GIM-TP", 45);
        assertThat(nuevo.get("posicionInicial").asInt()).isEqualTo(2);
        JsonNode s = json(pedir(owner, "es", "GET /programas/seguido", null));
        assertThat(s.get("minutos").asInt()).isEqualTo(45);
        assertThat(s.get("posicionHoy").asInt()).isEqualTo(2);
        assertThat(copia(owner, "GIM-TORSO-A")).isNotEqualTo(torso);
        for (JsonNode r : s.get("rutinas")) assertThat(r.get("duracionMinutos").asInt()).isLessThanOrEqualTo(45);
    }

    @Test
    @DisplayName("dejarlo: 204, sus rutinas fuera, las sesiones y los récords se quedan; sin programa también es 204")
    void dejar() throws Exception {
        assertThat(pedir(owner, "es", "DELETE /programas/seguido", null).getStatus()).isEqualTo(204);
        seguir(owner, "GIM-TP", 60);
        Integer sesion = entrenar(owner, copia(owner, "GIM-TORSO-A"));

        assertThat(pedir(owner, "es", "DELETE /programas/seguido", null).getStatus()).isEqualTo(204);
        assertThat(pedir(owner, "es", "GET /programas/seguido", null).getStatus()).isEqualTo(204);
        assertThat(json(pedir(owner, "es", "GET /rutinas/usuario/" + owner.getId() + "/activas", null))).isEmpty();
        assertThat(pedir(owner, "es", "GET /sesiones/" + sesion, null).getStatus()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT fecha_fin IS NOT NULL FROM programas_usuario WHERE usuario_id = ?",
                Boolean.class, owner.getId())).isTrue();
    }

    // --- Vista previa --------------------------------------------------------

    @ParameterizedTest(name = "{0} · {1} · {2} · {3} min")
    @CsvSource({
            "GIM-TP,INTERMEDIO,GANAR_MASA_MUSCULAR,45",
            "GIM-ETP,AVANZADO,MEJORAR_FUERZA,30",
            "MAN-CC,EXPERTO,PERDER_PESO,75",
            "PC-PE,PRINCIPIANTE,MEJORAR_FUERZA,60"})
    @DisplayName("la vista previa es igual a lo que crea seguir, y no guarda nada")
    void vista_previa_igual_a_seguir(String programa, NivelExperiencia nivel, TipoObjetivo objetivo, int minutos)
            throws Exception {
        perfil(owner, nivel, objetivo);
        int rutinas = contar("SELECT COUNT(*) FROM rutinas");
        int filas = contar("SELECT COUNT(*) FROM rutina_ejercicio");
        int sigue = contar("SELECT COUNT(*) FROM programas_usuario");

        JsonNode vista = json(pedir(owner, "es", "GET /programas/" + programa + "/vista-previa?minutos=" + minutos, null));
        assertThat(contar("SELECT COUNT(*) FROM rutinas")).isEqualTo(rutinas);
        assertThat(contar("SELECT COUNT(*) FROM rutina_ejercicio")).isEqualTo(filas);
        assertThat(contar("SELECT COUNT(*) FROM programas_usuario")).isEqualTo(sigue);

        JsonNode creado = seguir(owner, programa, minutos);
        assertThat(vista.get("rutinas")).hasSize(creado.get("rutinas").size());
        for (int i = 0; i < vista.get("rutinas").size(); i++) {
            JsonNode v = vista.get("rutinas").get(i);
            JsonNode c = creado.get("rutinas").get(i);
            assertThat(v.get("codigo").asText()).isEqualTo(c.get("plantillaCodigo").asText());
            assertThat(v.get("nombre").asText()).isEqualTo(c.get("nombre").asText());
            assertThat(v.get("duracionMinutos").asInt()).isEqualTo(c.get("duracionMinutos").asInt());
            assertThat(pauta(v.get("ejercicios"))).isEqualTo(pauta(c.get("ejercicios")));
        }
    }

    @Test
    @DisplayName("GIM-TP a 45 en intermedio: 45, 45, 45 y 40, lo que se quita y sin ajustes del perfil")
    void vista_previa_gim_tp_45() throws Exception {
        JsonNode vista = json(pedir(owner, "es", "GET /programas/GIM-TP/vista-previa?minutos=45", null));
        List<Integer> minutos = new ArrayList<>();
        vista.get("rutinas").forEach(r -> minutos.add(r.get("duracionMinutos").asInt()));
        assertThat(minutos).containsExactly(45, 45, 45, 40);
        assertThat(vista.get("ajustes")).isEmpty();
        JsonNode torso = vista.get("rutinas").get(0);
        assertThat(torso.get("duracionPlantilla").asInt()).isEqualTo(55);
        assertThat(torso.get("quitados")).hasSize(7 - torso.get("ejercicios").size());
        assertThat(torso.get("seriesBasicos").isNull()).isTrue();
    }

    @Test
    @DisplayName("avanzado con fuerza a 30: los dos ajustes, y las series de los básicos porque cambian")
    void vista_previa_ajustes() throws Exception {
        perfil(owner, NivelExperiencia.AVANZADO, TipoObjetivo.MEJORAR_FUERZA);
        JsonNode vista = json(pedir(owner, "en", "GET /programas/GIM-TP/vista-previa?minutos=30", null));
        assertThat(vista.get("ajustes")).extracting(JsonNode::asText).containsExactly("AVANZADO", "FUERZA");
        JsonNode torso = vista.get("rutinas").get(0);
        assertThat(torso.get("nombre").asText()).isEqualTo("Upper A");
        assertThat(torso.get("seriesBasicos").asInt()).isEqualTo(2);
        assertThat(pedir(owner, "es", "GET /programas/GIM-TP/vista-previa?minutos=50", null).getStatus()).isEqualTo(400);
        assertThat(pedir(owner, "es", "GET /programas/NO-EXISTE/vista-previa", null).getStatus()).isEqualTo(404);
    }

    // --- Recomendado ---------------------------------------------------------

    @Test
    @DisplayName("el recomendado cambia con el nivel y los días, con su porqué en ES y EN")
    void recomendado() throws Exception {
        perfil(owner, null, null);
        JsonNode r = json(pedir(owner, "es", "GET /programas/recomendado?equipamiento=GIMNASIO&dias=4", null));
        assertThat(r.get("programa").get("codigo").asText()).isEqualTo("GIM-PE");
        assertThat(r.get("nivel").asText()).isEqualTo("PRINCIPIANTE");
        assertThat(r.get("nivelEnPerfil").asBoolean()).isFalse();
        assertThat(r.get("motivo").asText()).contains("Editar perfil");
        JsonNode en = json(pedir(owner, "en", "GET /programas/recomendado?equipamiento=GIMNASIO&dias=4", null));
        assertThat(en.get("motivo").asText()).contains("Edit profile");
        assertThat(en.get("programa").get("nombre").asText()).isEqualTo("Getting started");

        perfil(owner, NivelExperiencia.INTERMEDIO, null);
        r = json(pedir(owner, "es", "GET /programas/recomendado?equipamiento=MANCUERNAS&dias=5", null));
        assertThat(r.get("programa").get("codigo").asText()).isEqualTo("MAN-TPET");
        assertThat(r.has("motivo") && !r.get("motivo").isNull()).isFalse();

        r = json(pedir(owner, "es", "GET /programas/recomendado?equipamiento=PESO_CORPORAL&dias=6", null));
        assertThat(r.get("programa").get("codigo").asText()).isEqualTo("PC-TP");
        assertThat(r.get("motivo").asText()).contains("4 días");

        for (String mal : List.of("equipamiento=GIMNASIO&dias=1", "equipamiento=GIMNASIO&dias=7", "dias=3",
                "equipamiento=GIMNASIO", "equipamiento=KETTLEBELL&dias=3")) {
            assertThat(pedir(owner, "es", "GET /programas/recomendado?" + mal, null).getStatus()).as(mal).isEqualTo(400);
        }
    }

    // --- Migración -----------------------------------------------------------

    @Test
    @DisplayName("la migración deja abierto el más reciente y cierra los demás, con sus rutinas desactivadas")
    void migracion_cierra_los_duplicados() throws Exception {
        Integer programa = jdbc.queryForObject("SELECT id FROM programas WHERE codigo = 'GIM-TP'", Integer.class);
        jdbc.update("INSERT INTO programas_usuario (usuario_id, programa_id, minutos, fecha_inicio) VALUES (?, ?, 60, NOW())",
                owner.getId(), programa);
        Integer viejo = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        jdbc.update("INSERT INTO rutinas (nombre, nivel, usuario_id, programa_usuario_id, activa) VALUES ('Vieja', 'INTERMEDIO', ?, ?, 1)",
                owner.getId(), viejo);
        Integer rutinaVieja = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        jdbc.update("INSERT INTO programas_usuario (usuario_id, programa_id, minutos, fecha_inicio) VALUES (?, ?, 45, NOW())",
                owner.getId(), programa);
        Integer nuevo = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        // El de otro usuario, solo y abierto: no se toca.
        jdbc.update("INSERT INTO programas_usuario (usuario_id, programa_id, minutos, fecha_inicio) VALUES (?, ?, 30, NOW())",
                attacker.getId(), programa);
        Integer ajeno = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);

        ScriptUtils.executeSqlScript(DataSourceUtils.getConnection(dataSource), new ClassPathResource(CIERRE));

        assertThat(jdbc.queryForObject("SELECT fecha_fin IS NOT NULL FROM programas_usuario WHERE id = ?", Boolean.class, viejo)).isTrue();
        assertThat(jdbc.queryForObject("SELECT fecha_fin IS NULL FROM programas_usuario WHERE id = ?", Boolean.class, nuevo)).isTrue();
        assertThat(jdbc.queryForObject("SELECT fecha_fin IS NULL FROM programas_usuario WHERE id = ?", Boolean.class, ajeno)).isTrue();
        assertThat(jdbc.queryForObject("SELECT activa FROM rutinas WHERE id = ?", Boolean.class, rutinaVieja)).isFalse();
        assertThat(jdbc.queryForObject("SELECT posicion_inicial FROM programas_usuario WHERE id = ?", Integer.class, nuevo))
                .isOne();
    }

    // --- Acceso ajeno --------------------------------------------------------

    @Test
    @DisplayName("el invitado no tiene programa que seguir ni dejar: 403")
    void invitado() throws Exception {
        assertThat(pedir(guest, "es", "GET /programas/seguido", null).getStatus()).isEqualTo(403);
        assertThat(pedir(guest, "es", "DELETE /programas/seguido", null).getStatus()).isEqualTo(403);
        assertThat(pedir(guest, "es", "GET /programas/recomendado?equipamiento=GIMNASIO&dias=3", null).getStatus())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("cada ruta nueva es del usuario del token: el otro no ve, no deja ni mueve el programa del dueño")
    void acceso_ajeno() throws Exception {
        seguir(owner, "GIM-TP", 60);
        Integer torso = copia(owner, "GIM-TORSO-A");

        // No sigue nada: no ve el del dueño.
        assertThat(pedir(attacker, "es", "GET /programas/seguido", null).getStatus()).isEqualTo(204);
        // Dejar no toca el del dueño.
        assertThat(pedir(attacker, "es", "DELETE /programas/seguido", null).getStatus()).isEqualTo(204);
        assertThat(pedir(owner, "es", "GET /programas/seguido", null).getStatus()).isEqualTo(200);
        // Una sesión con la rutina del dueño es 403, y no mueve su ciclo.
        assertThat(pedir(attacker, "es", "POST /sesiones/completa", sesion(torso)).getStatus()).isEqualTo(403);
        assertThat(hoy(owner)).isEqualTo(1);
        // Recomendado y vista previa salen de su perfil, no del dueño.
        perfil(attacker, NivelExperiencia.AVANZADO, TipoObjetivo.MEJORAR_FUERZA);
        assertThat(json(pedir(attacker, "es", "GET /programas/GIM-TP/vista-previa?minutos=60", null)).get("ajustes"))
                .hasSize(2);
        assertThat(json(pedir(owner, "es", "GET /programas/GIM-TP/vista-previa?minutos=60", null)).get("ajustes"))
                .isEmpty();
        assertThat(json(pedir(attacker, "es", "GET /programas/recomendado?equipamiento=GIMNASIO&dias=4", null))
                .get("programa").get("codigo").asText()).isEqualTo("GIM-TP");
        // Seguir el suyo no cierra el del dueño.
        seguir(attacker, "PC-PE", 30);
        assertThat(pedir(owner, "es", "GET /programas/seguido", null).getStatus()).isEqualTo(200);
        assertThat(json(pedir(attacker, "es", "GET /programas/seguido", null)).get("programa").get("codigo").asText())
                .isEqualTo("PC-PE");
    }

    // --- Ayudas --------------------------------------------------------------

    private JsonNode seguir(Usuario quien, String programa, int minutos) throws Exception {
        MockHttpServletResponse r = pedir(quien, "es", "POST /programas/" + programa + "/seguir",
                "{\"minutos\":" + minutos + "}");
        assertThat(r.getStatus()).as(r.getContentAsString()).isEqualTo(201);
        return objectMapper.readTree(r.getContentAsString());
    }

    private String sesion(Integer rutinaId) {
        return """
                {"claveIdempotencia":"%s","rutinaId":%d,"fechaInicio":"2026-10-%02dT18:00:00","duracionMinutos":45,
                 "completada":true,"ejercicios":[]}""".formatted(UUID.randomUUID(), rutinaId, dia++);
    }

    private Integer entrenar(Usuario quien, Integer rutinaId) throws Exception {
        return json(pedir(quien, "es", "POST /sesiones/completa", sesion(rutinaId))).get("id").asInt();
    }

    private Integer copia(Usuario quien, String plantilla) {
        return jdbc.queryForObject("""
                SELECT r.id FROM rutinas r JOIN rutinas p ON p.id = r.plantilla_id
                JOIN programas_usuario pu ON pu.id = r.programa_usuario_id
                WHERE r.usuario_id = ? AND p.codigo = ? AND pu.fecha_fin IS NULL""", Integer.class, quien.getId(), plantilla);
    }

    private Integer hoy(Usuario quien) throws Exception {
        return json(pedir(quien, "es", "GET /programas/seguido", null)).get("posicionHoy").asInt();
    }

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    // Sobre la entidad: el servicio la lee del contexto de persistencia del test.
    private static void perfil(Usuario u, NivelExperiencia nivel, TipoObjetivo objetivo) {
        u.setNivelExperiencia(nivel);
        u.setObjetivo(objetivo);
    }

    private static List<String> nombres(JsonNode rutinas) {
        List<String> n = new ArrayList<>();
        rutinas.forEach(r -> n.add(r.get("nombre").asText()));
        return n;
    }

    private static List<String> pauta(JsonNode ejercicios) {
        List<String> p = new ArrayList<>();
        ejercicios.forEach(e -> p.add(String.join("|", e.get("orden").asText(), e.get("ejercicioId").asText(),
                e.get("series").asText(), e.get("repeticiones").asText(), e.get("repeticionesMin").asText(),
                e.get("repeticionesMax").asText(), e.get("tiempoDescanso").asText(), e.get("tipo").asText(),
                String.valueOf(e.get("notas")))));
        return p;
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
