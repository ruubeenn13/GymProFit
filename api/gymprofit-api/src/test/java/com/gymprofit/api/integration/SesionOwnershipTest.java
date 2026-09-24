package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.SesionEntrenamiento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.Nivel;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IRutinaRepository;
import com.gymprofit.api.repository.jpa.ISesionEntrenamientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// SesionOwnershipTest — cobertura IDOR de /sesiones/** (GP-048)
//
// Todas estas rutas tenían su comprobación en SesionEntrenamientoService y ni un test
// que impidiera perderla. Cuatro grupos:
//   · rutas con un id de sesión o de usuario ajeno → 403 al atacante; el dueño no;
//   · listados globales → solo ADMIN;
//   · ids que llegan en el CUERPO de una escritura (PUT /sesiones, POST /sesiones/completa);
//   · la regla de rol: GUEST fuera, probada con SU PROPIO id (si pidiera uno ajeno, el
//     403 lo daría ya la propiedad y el test no probaría la regla).
//
// Cada caso se validó quitando la comprobación que defiende y viéndolo en rojo.
// ============================================================
@DisplayName("IDOR /sesiones — cobertura de todas las rutas con id (GP-048)")
class SesionOwnershipTest extends AbstractOwnershipTest {

    private static final String FECHA = "2026-09-24";

    @Autowired
    private ISesionEntrenamientoRepository sesionRepository;

    @Autowired
    private IRutinaRepository rutinaRepository;

    private Usuario admin;
    private Integer sesionOwner;
    private Integer rutinaOwner;
    private Integer plantilla;
    private Map<String, Object> ids;

    @BeforeEach
    void sembrar() {
        admin = crearUsuario("__idor_admin__", RoleType.ADMIN);
        rutinaOwner = crearRutina("Rutina privada del owner", owner).getId();
        plantilla = crearRutina("Plantilla del sistema", null).getId();
        sesionOwner = crearSesion(owner, rutinaOwner).getId();
        ids = Map.of("sesion", sesionOwner, "owner", owner.getId(), "rutina", rutinaOwner, "fecha", FECHA);
    }

    // --- Id de sesión o de usuario ajeno ------------------------------------

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "GET /sesiones/{sesion}",
            "PATCH /sesiones/{sesion}",
            "PUT /sesiones/{sesion}/completar",
            "GET /sesiones/{sesion}/volumen",
            "DELETE /sesiones/{sesion}",
            "GET /sesiones/usuario/{owner}",
            "GET /sesiones/usuario/{owner}/completadas",
            "GET /sesiones/usuario/{owner}/pendientes",
            "GET /sesiones/usuario/{owner}/fecha/{fecha}",
            "GET /sesiones/usuario/{owner}/rutina/{rutina}",
            "GET /sesiones/usuario/{owner}/ordenadas",
            "GET /sesiones/usuario/{owner}/completadas/ordenadas",
            "GET /sesiones/usuario/{owner}/volumen-muscular",
            "GET /sesiones/count/usuario/{owner}"
    })
    @DisplayName("id ajeno → 403 al atacante; el dueño no recibe 403")
    void idAjeno_403(String plantillaRuta) throws Exception {
        String ruta = rellenar(plantillaRuta, ids);
        assertThat(estado(attacker, ruta)).as("atacante en " + ruta).isEqualTo(403);
        assertThat(estado(owner, ruta)).as("dueño en " + ruta).isNotEqualTo(403);
    }

    // --- Listados globales: solo ADMIN ----------------------------------------

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "GET /sesiones",
            "GET /sesiones/completadas",
            "GET /sesiones/pendientes",
            "GET /sesiones/fecha/{fecha}",
            "GET /sesiones/rutina/{rutina}",
            "GET /sesiones/count/rutina/{rutina}"
    })
    @DisplayName("listado global → 403 a un USER; ADMIN no")
    void soloAdmin_403(String plantillaRuta) throws Exception {
        String ruta = rellenar(plantillaRuta, ids);
        assertThat(estado(attacker, ruta)).as("USER en " + ruta).isEqualTo(403);
        assertThat(estado(admin, ruta)).as("ADMIN en " + ruta).isNotEqualTo(403);
    }

    // --- Ids en el cuerpo ------------------------------------------------------

    @Test
    @DisplayName("PUT /sesiones con el id de una sesión ajena en el cuerpo → 403 y no la toca")
    void putConIdAjeno_403() throws Exception {
        String cuerpo = "{\"id\":" + sesionOwner + ",\"usuarioId\":" + attacker.getId()
                + ",\"fechaInicio\":\"2026-01-01T10:00:00\",\"notas\":\"pisada\"}";
        pedir(attacker, "PUT /sesiones", cuerpo).andExpect(status().isForbidden());

        assertThat(sesionRepository.findById(sesionOwner).orElseThrow().getNotas()).isNull();
    }

    @Test
    @DisplayName("POST /sesiones con el usuarioId de otro en el cuerpo → la sesión es del token")
    void postConUsuarioAjeno_quedaDelToken() throws Exception {
        String cuerpo = "{\"usuarioId\":" + owner.getId() + ",\"fechaInicio\":\"2026-01-01T10:00:00\"}";
        pedir(attacker, "POST /sesiones", cuerpo).andExpect(status().isOk());

        assertThat(sesionRepository.findByUsuarioId(owner.getId())).hasSize(1);
        assertThat(sesionRepository.findByUsuarioId(attacker.getId())).hasSize(1);
    }

    @Test
    @DisplayName("POST /sesiones/completa con la rutina de otro → 403 y ninguna fila")
    void completaConRutinaAjena_403() throws Exception {
        pedir(attacker, "POST /sesiones/completa", completa("gp048-ajena", rutinaOwner))
                .andExpect(status().isForbidden());

        assertThat(sesionRepository.findByUsuarioId(attacker.getId())).isEmpty();
    }

    @Test
    @DisplayName("POST /sesiones/completa con una plantilla sin dueño → 200")
    void completaConPlantilla_200() throws Exception {
        pedir(attacker, "POST /sesiones/completa", completa("gp048-plantilla", plantilla))
                .andExpect(status().isOk());

        assertThat(sesionRepository.findByUsuarioId(attacker.getId())).hasSize(1);
    }

    @Test
    @DisplayName("POST /sesiones/completa con una rutina que no existe → 404")
    void completaConRutinaInexistente_404() throws Exception {
        pedir(attacker, "POST /sesiones/completa", completa("gp048-nada", Integer.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    // --- Regla de rol ----------------------------------------------------------

    @Test
    @DisplayName("GUEST pidiendo SUS propias sesiones → 403 (lo para la regla de rol)")
    void guestConSuPropioId_403() throws Exception {
        crearSesion(guest, null);
        assertThat(estado(guest, "GET /sesiones/usuario/" + guest.getId())).isEqualTo(403);
    }

    // --- Andamiaje ---------------------------------------------------------------

    private String completa(String clave, Integer rutinaId) {
        return "{\"claveIdempotencia\":\"" + clave + "\",\"rutinaId\":" + rutinaId
                + ",\"duracionMinutos\":30,\"ejercicios\":[]}";
    }

    private Rutina crearRutina(String nombre, Usuario dueno) {
        Rutina rutina = new Rutina();
        rutina.setNombre(nombre);
        rutina.setNivel(Nivel.values()[0]);
        rutina.setUsuario(dueno);
        rutina.setActiva(true);
        return rutinaRepository.save(rutina);
    }

    private SesionEntrenamiento crearSesion(Usuario dueno, Integer rutinaId) {
        SesionEntrenamiento s = new SesionEntrenamiento();
        s.setUsuario(dueno);
        s.setFechaInicio(LocalDateTime.of(2026, 9, 24, 10, 0));
        s.setFechaFin(LocalDateTime.of(2026, 9, 24, 11, 0));
        s.setCompletada(true);
        if (rutinaId != null) s.setRutina(rutinaRepository.findById(rutinaId).orElseThrow());
        return sesionRepository.save(s);
    }
}
