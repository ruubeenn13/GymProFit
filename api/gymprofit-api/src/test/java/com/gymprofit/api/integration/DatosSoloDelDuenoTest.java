package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// DatosSoloDelDuenoTest — GP-198: los datos de un usuario fuera de la nutrición
// (sesiones, ejercicios realizados, mediciones, objetivos, notificaciones, rutinas propias
// y sus ejercicios, y su cuenta por id) son solo suyos, también para un ADMIN.
// Un id de otro da el mismo 403 que a un USER (DEC-014); las rutas que siguen siendo de
// ADMIN fuera de /admin devuelven lo de quien llama. Se conserva lo que es administración:
// el catálogo y las rutinas sin dueño, y enviar una notificación a otra cuenta.
// Con el contexto levantado y JWT de verdad.
// ============================================================
@DisplayName("GP-198 — los datos de un usuario son solo suyos, también para un ADMIN")
class DatosSoloDelDuenoTest extends AbstractOwnershipTest {

    private Usuario admin;
    private Integer ejercicio;
    private Integer o;
    private Integer sesion, realizado, medicion, objetivo, notificacion, rutina, rutinaEjercicio;
    private Integer sesionAdmin, realizadoAdmin, medicionAdmin, objetivoAdmin, notificacionAdmin, rutinaAdmin,
            rutinaEjercicioAdmin, predefinida;

    @BeforeEach
    void sembrar() throws Exception {
        admin = crearUsuario("__datos_admin__", RoleType.ADMIN);
        ejercicio = crearEjercicioCatalogo().getId();
        o = owner.getId();

        sesion = crear(owner, "POST /sesiones", "{\"usuarioId\":" + o + ",\"completada\":false}");
        realizado = crear(owner, "POST /ejercicios-realizados",
                "{\"sesionId\":" + sesion + ",\"ejercicioId\":" + ejercicio + ",\"seriesCompletadas\":3}");
        medicion = crear(owner, "POST /mediciones-corporales", "{\"usuarioId\":" + o + ",\"peso\":70}");
        objetivo = crear(owner, "POST /objetivos-personales", "{\"usuarioId\":" + o + ",\"tipoObjetivo\":\"PERDER_PESO\",\"descripcion\":\"d\",\"valorObjetivo\":65}");
        notificacion = crear(owner, "POST /notificaciones",
                "{\"usuarioId\":" + o + ",\"titulo\":\"t\",\"mensaje\":\"m\",\"tipo\":\"RECORDATORIO\"}");
        rutina = crear(owner, "POST /rutinas", "{\"usuarioId\":" + o + ",\"nombre\":\"Kzdatos\",\"esPredefinida\":false,\"nivel\":\"PRINCIPIANTE\"}");
        rutinaEjercicio = crear(owner, "POST /rutinas-ejercicios",
                "{\"rutinaId\":" + rutina + ",\"ejercicioId\":" + ejercicio + ",\"series\":3,\"repeticiones\":10,\"orden\":1}");

        Integer a = admin.getId();
        sesionAdmin = crear(admin, "POST /sesiones", "{\"usuarioId\":" + a + ",\"completada\":false}");
        realizadoAdmin = crear(admin, "POST /ejercicios-realizados",
                "{\"sesionId\":" + sesionAdmin + ",\"ejercicioId\":" + ejercicio + ",\"seriesCompletadas\":3}");
        medicionAdmin = crear(admin, "POST /mediciones-corporales", "{\"usuarioId\":" + a + ",\"peso\":80}");
        objetivoAdmin = crear(admin, "POST /objetivos-personales", "{\"usuarioId\":" + a + ",\"tipoObjetivo\":\"PERDER_PESO\",\"descripcion\":\"d\",\"valorObjetivo\":65}");
        notificacionAdmin = crear(admin, "POST /notificaciones",
                "{\"usuarioId\":" + a + ",\"titulo\":\"t\",\"mensaje\":\"m\",\"tipo\":\"RECORDATORIO\"}");
        rutinaAdmin = crear(admin, "POST /rutinas", "{\"usuarioId\":" + a + ",\"nombre\":\"Kzdatos admin\",\"esPredefinida\":false,\"nivel\":\"PRINCIPIANTE\"}");
        rutinaEjercicioAdmin = crear(admin, "POST /rutinas-ejercicios",
                "{\"rutinaId\":" + rutinaAdmin + ",\"ejercicioId\":" + ejercicio + ",\"series\":3,\"repeticiones\":10,\"orden\":1}");
        predefinida = crear(admin, "POST /rutinas", "{\"nombre\":\"Kzdatos predefinida\",\"esPredefinida\":true,\"nivel\":\"PRINCIPIANTE\"}");
    }

    @Test
    @DisplayName("los listados de ADMIN fuera de /admin: solo lo suyo")
    void listados_admin() throws Exception {
        String hoy = LocalDate.now().toString();
        comprobarLista("/sesiones", sesionAdmin, sesion);
        comprobarLista("/sesiones/pendientes", sesionAdmin, sesion);
        comprobarLista("/sesiones/fecha/" + hoy, sesionAdmin, sesion);
        comprobarLista("/ejercicios-realizados", realizadoAdmin, realizado);
        comprobarLista("/ejercicios-realizados/ejercicio/" + ejercicio, realizadoAdmin, realizado);
        comprobarLista("/mediciones-corporales", medicionAdmin, medicion);
        comprobarLista("/objetivos-personales", objetivoAdmin, objetivo);
        comprobarLista("/objetivos-personales/tipo/PERDER_PESO", objetivoAdmin, objetivo);
        comprobarLista("/notificaciones", notificacionAdmin, notificacion);
        comprobarLista("/rutinas", rutinaAdmin, rutina);
        comprobarLista("/rutinas/activas", rutinaAdmin, rutina);
        comprobarLista("/rutinas-ejercicios", rutinaEjercicioAdmin, rutinaEjercicio);
        comprobarLista("/rutinas-ejercicios/ejercicio/" + ejercicio, rutinaEjercicioAdmin, rutinaEjercicio);
        comprobarLista("/usuarios", admin.getId(), o);
        comprobarLista("/usuarios/activos", admin.getId(), o);
        comprobarLista("/jooq/usuarios", admin.getId(), o);
        comprobarLista("/jooq/usuarios/activos", admin.getId(), o);
        comprobarLista("/jooq/usuarios/busqueda", admin.getId(), o);

        // Los contadores por id del catálogo cuentan lo de quien llama.
        assertThat(contar(admin, "/ejercicios-realizados/count/ejercicio/" + ejercicio)).isEqualTo(1);
        assertThat(contar(admin, "/rutinas-ejercicios/count/ejercicio/" + ejercicio)).isEqualTo(1);
    }

    @Test
    @DisplayName("un id de otro: 403 también a un ADMIN")
    void ids_admin() throws Exception {
        for (String ruta : List.of(
                "GET /sesiones/" + sesion, "PATCH /sesiones/" + sesion, "DELETE /sesiones/" + sesion,
                "GET /sesiones/" + sesion + "/volumen", "GET /sesiones/usuario/" + o,
                "GET /sesiones/count/usuario/" + o, "GET /sesiones/rutina/" + rutina,
                "GET /sesiones/count/rutina/" + rutina,
                "GET /ejercicios-realizados/" + realizado, "PATCH /ejercicios-realizados/" + realizado,
                "GET /ejercicios-realizados/sesion/" + sesion,
                "GET /mediciones-corporales/" + medicion, "PATCH /mediciones-corporales/" + medicion,
                "GET /mediciones-corporales/usuario/" + o,
                "GET /objetivos-personales/" + objetivo, "PATCH /objetivos-personales/" + objetivo,
                "GET /objetivos-personales/usuario/" + o,
                "GET /notificaciones/" + notificacion, "PUT /notificaciones/" + notificacion + "/leer",
                "GET /notificaciones/usuario/" + o,
                "GET /rutinas/" + rutina, "PATCH /rutinas/" + rutina, "DELETE /rutinas/" + rutina,
                "GET /rutinas/usuario/" + o, "GET /rutinas/usuario/" + o + "/activas",
                "GET /rutinas-ejercicios/" + rutinaEjercicio, "GET /rutinas-ejercicios/rutina/" + rutina,
                "GET /logros/usuario/" + o,
                "GET /usuarios/" + o, "PATCH /usuarios/" + o, "GET /usuarios/" + o + "/estadisticas",
                "GET /usuarios/" + o + "/foto", "GET /usuarios/username/" + owner.getUsername(),
                "GET /usuarios/email/" + owner.getEmail(), "DELETE /usuarios/" + o,
                "PUT /usuarios/" + o + "/activar", "DELETE /usuarios/" + o + "/permanente")) {
            assertThat(estado(admin, ruta)).as(ruta).isEqualTo(403);
        }
        String put = "{\"id\":" + o + ",\"username\":\"x\",\"email\":\"x@x.es\"}";
        assertThat(pedir(admin, "PUT /usuarios", put).andReturn().getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("lo que se crea es de quien llama, también para un ADMIN")
    void crear_es_de_quien_llama() throws Exception {
        assertThat(usuarioDe(admin, "POST /sesiones", "{\"usuarioId\":" + o + "}")).isEqualTo(admin.getId());
        assertThat(usuarioDe(admin, "POST /mediciones-corporales", "{\"usuarioId\":" + o + ",\"peso\":70}"))
                .isEqualTo(admin.getId());
        assertThat(usuarioDe(admin, "POST /objetivos-personales", "{\"usuarioId\":" + o + ",\"tipoObjetivo\":\"PERDER_PESO\",\"descripcion\":\"d\",\"valorObjetivo\":65}"))
                .isEqualTo(admin.getId());
        // Una rutina para otro ya daba 403 a un USER; ahora también a un ADMIN.
        assertThat(pedir(admin, "POST /rutinas", "{\"usuarioId\":" + o + ",\"nombre\":\"x\",\"esPredefinida\":false,\"nivel\":\"PRINCIPIANTE\"}")
                .andReturn().getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("se conserva la administración: notificar a otro y las rutinas sin dueño")
    void administracion_se_conserva() throws Exception {
        // Enviar una notificación a otra cuenta sigue siendo del ADMIN, y le llega a su dueño.
        Integer enviada = crear(admin, "POST /notificaciones",
                "{\"usuarioId\":" + o + ",\"titulo\":\"Aviso\",\"mensaje\":\"m\",\"tipo\":\"RECORDATORIO\"}");
        assertThat(ids(owner, "/notificaciones/usuario/" + o)).contains(enviada);
        // Las rutinas predefinidas, sin dueño: el ADMIN las ve, las cambia y las activa.
        assertThat(estado(admin, "GET /rutinas/" + predefinida)).isEqualTo(200);
        assertThat(estado(admin, "PATCH /rutinas/" + predefinida)).isEqualTo(200);
        assertThat(estado(admin, "DELETE /rutinas/" + predefinida)).isIn(200, 204);
        assertThat(estado(admin, "PUT /rutinas/" + predefinida + "/activar")).isIn(200, 204);
        assertThat(estado(admin, "GET /rutinas-ejercicios/rutina/" + predefinida)).isEqualTo(200);
        // Y un USER, como siempre, no las cambia.
        assertThat(estado(owner, "PATCH /rutinas/" + predefinida)).isEqualTo(403);
    }

    @Test
    @DisplayName("el dueño sigue viendo lo suyo")
    void el_dueno_si() throws Exception {
        for (String ruta : List.of("GET /sesiones/" + sesion, "GET /ejercicios-realizados/" + realizado,
                "GET /mediciones-corporales/" + medicion, "GET /objetivos-personales/" + objetivo,
                "GET /notificaciones/" + notificacion, "GET /rutinas/" + rutina,
                "GET /rutinas-ejercicios/" + rutinaEjercicio, "GET /usuarios/" + o,
                "GET /usuarios/" + o + "/estadisticas")) {
            assertThat(estado(owner, ruta)).as(ruta).isEqualTo(200);
        }
        assertThat(ids(owner, "/sesiones/usuario/" + o)).contains(sesion).doesNotContain(sesionAdmin);
    }

    private void comprobarLista(String ruta, Integer suyo, Integer ajeno) throws Exception {
        assertThat(ids(admin, ruta)).as(ruta).contains(suyo).doesNotContain(ajeno);
    }

    private Integer crear(Usuario quien, String verboYRuta, String cuerpo) throws Exception {
        var r = pedir(quien, verboYRuta, cuerpo).andReturn().getResponse();
        assertThat(r.getStatus()).as(verboYRuta + " " + r.getContentAsString()).isIn(200, 201);
        return objectMapper.readTree(r.getContentAsString(StandardCharsets.UTF_8)).get("id").asInt();
    }

    private Integer usuarioDe(Usuario quien, String verboYRuta, String cuerpo) throws Exception {
        var r = pedir(quien, verboYRuta, cuerpo).andReturn().getResponse();
        assertThat(r.getStatus()).as(verboYRuta).isIn(200, 201);
        return objectMapper.readTree(r.getContentAsString(StandardCharsets.UTF_8)).get("usuarioId").asInt();
    }

    private JsonNode cuerpo(Usuario quien, String ruta) throws Exception {
        var r = pedir(quien, "GET " + ruta).andReturn().getResponse();
        assertThat(r.getStatus()).as(ruta).isEqualTo(200);
        return objectMapper.readTree(r.getContentAsString(StandardCharsets.UTF_8));
    }

    private long contar(Usuario quien, String ruta) throws Exception {
        return cuerpo(quien, ruta).get("count").asLong();
    }

    private List<Integer> ids(Usuario quien, String ruta) throws Exception {
        List<Integer> ids = new ArrayList<>();
        cuerpo(quien, ruta).forEach(n -> ids.add(n.get("id").asInt()));
        return ids;
    }
}
