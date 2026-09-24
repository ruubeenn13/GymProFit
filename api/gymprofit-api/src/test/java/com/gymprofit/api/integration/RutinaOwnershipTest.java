package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.Nivel;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IRutinaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// RutinaOwnershipTest — cobertura IDOR de /rutinas/** (GP-048)
//
// RutinaService no usa SecurityUtils: tiene su comprobación propia (canView para leer,
// checkOwnership para escribir, filterViewable para los listados). Por eso importa
// tener tests: una comprobación a mano es justo la que se pierde en un refactor.
//
// Tres criterios, según quién es el dueño del id:
//   · rutina privada ajena → 403 para leer y escribir;
//   · rutina predefinida → la lee cualquiera, pero solo ADMIN la modifica;
//   · listados por nivel, nombre o activas → no es un id con dueño, así que no hay 403:
//     lo que se comprueba es que la rutina privada ajena no sale (aislamiento, DEC-027).
// GET /rutinas/** está abierto a GUEST a propósito (catálogo de predefinidas), así que
// aquí la regla de rol que se prueba es la de escritura.
// ============================================================
@DisplayName("IDOR /rutinas — cobertura de todas las rutas con id (GP-048)")
class RutinaOwnershipTest extends AbstractOwnershipTest {

    private static final String NOMBRE_PRIVADA = "Rutina privada gp048";

    @Autowired
    private IRutinaRepository rutinaRepository;

    private Usuario admin;
    private Integer rutinaOwner;
    private Integer predefinida;
    private Map<String, Object> ids;

    @BeforeEach
    void sembrar() {
        admin = crearUsuario("__idor_admin__", RoleType.ADMIN);
        rutinaOwner = crearRutina(NOMBRE_PRIVADA, owner, false).getId();
        predefinida = crearRutina("Predefinida gp048", null, true).getId();
        ids = Map.of("rutina", rutinaOwner, "predefinida", predefinida, "owner", owner.getId(),
                "nivel", Nivel.values()[0].name());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "GET /rutinas/{rutina}",
            "PATCH /rutinas/{rutina}",
            "PUT /rutinas/{rutina}/activar",
            "DELETE /rutinas/{rutina}",
            "DELETE /rutinas/{rutina}/permanente",
            "GET /rutinas/usuario/{owner}",
            "GET /rutinas/usuario/{owner}/activas"
    })
    @DisplayName("rutina o usuario ajeno → 403 al atacante; el dueño no recibe 403")
    void idAjeno_403(String plantillaRuta) throws Exception {
        String ruta = rellenar(plantillaRuta, ids);
        assertThat(estado(attacker, ruta)).as("atacante en " + ruta).isEqualTo(403);
        assertThat(estado(owner, ruta)).as("dueño en " + ruta).isNotEqualTo(403);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "PATCH /rutinas/{predefinida}",
            "PUT /rutinas/{predefinida}/activar",
            "DELETE /rutinas/{predefinida}",
            "DELETE /rutinas/{predefinida}/permanente"
    })
    @DisplayName("escribir una rutina predefinida → 403 a un USER; ADMIN no")
    void predefinida_soloAdminEscribe(String plantillaRuta) throws Exception {
        String ruta = rellenar(plantillaRuta, ids);
        assertThat(estado(attacker, ruta)).as("USER en " + ruta).isEqualTo(403);
        assertThat(estado(admin, ruta)).as("ADMIN en " + ruta).isNotEqualTo(403);
    }

    @Test
    @DisplayName("una rutina predefinida la lee cualquier USER (control)")
    void predefinida_seLee() throws Exception {
        assertThat(estado(attacker, "GET /rutinas/" + predefinida)).isEqualTo(200);
    }

    @Test
    @DisplayName("GET /rutinas (todas) → 403 a un USER; ADMIN no")
    void listadoGlobal_soloAdmin() throws Exception {
        assertThat(estado(attacker, "GET /rutinas")).isEqualTo(403);
        assertThat(estado(admin, "GET /rutinas")).isEqualTo(200);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "GET /rutinas/nivel/{nivel}",
            "GET /rutinas/nombre/gp048",
            "GET /rutinas/activas"
    })
    @DisplayName("listado sin dueño → la rutina privada ajena no sale; al dueño sí")
    void listado_aislado(String plantillaRuta) throws Exception {
        String ruta = rellenar(plantillaRuta, ids);
        String alAtacante = pedir(attacker, ruta).andReturn().getResponse().getContentAsString();
        String alDueno = pedir(owner, ruta).andReturn().getResponse().getContentAsString();
        assertThat(alAtacante).as("atacante en " + ruta).doesNotContain(NOMBRE_PRIVADA);
        assertThat(alDueno).as("dueño en " + ruta).contains(NOMBRE_PRIVADA);
    }

    @Test
    @DisplayName("PUT /rutinas con el id de una rutina ajena en el cuerpo → 403 y no la toca")
    void putConIdAjeno_403() throws Exception {
        pedir(attacker, "PUT /rutinas", "{\"id\":" + rutinaOwner + ",\"nombre\":\"pisada\"}")
                .andExpect(status().isForbidden());

        assertThat(rutinaRepository.findById(rutinaOwner).orElseThrow().getNombre()).isEqualTo(NOMBRE_PRIVADA);
    }

    @Test
    @DisplayName("POST /rutinas para otro usuario → 403")
    void postParaOtro_403() throws Exception {
        pedir(attacker, "POST /rutinas",
                "{\"usuarioId\":" + owner.getId() + ",\"nombre\":\"colada\",\"esPredefinida\":false}")
                .andExpect(status().isForbidden());

        assertThat(rutinaRepository.findByUsuarioId(owner.getId())).hasSize(1);
    }

    @Test
    @DisplayName("GUEST creando una rutina a su nombre → 403 (lo para la regla de rol)")
    void guestNoCrea_403() throws Exception {
        pedir(guest, "POST /rutinas",
                "{\"usuarioId\":" + guest.getId() + ",\"nombre\":\"del invitado\",\"esPredefinida\":false}")
                .andExpect(status().isForbidden());
    }

    private Rutina crearRutina(String nombre, Usuario dueno, boolean esPredefinida) {
        Rutina rutina = new Rutina();
        rutina.setNombre(nombre);
        rutina.setNivel(Nivel.values()[0]);
        rutina.setUsuario(dueno);
        rutina.setEsPredefinida(esPredefinida);
        rutina.setActiva(true);
        return rutinaRepository.save(rutina);
    }
}
