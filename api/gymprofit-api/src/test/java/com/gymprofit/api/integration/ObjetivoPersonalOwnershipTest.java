package com.gymprofit.api.integration;

import com.gymprofit.api.entity.ObjetivoPersonal;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.enums.TipoObjetivo;
import com.gymprofit.api.repository.jpa.IObjetivoPersonalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// ObjetivoPersonalOwnershipTest — cobertura IDOR de /objetivos-personales/** (GP-048)
//
// Mismo esquema que SesionOwnershipTest: id ajeno en la ruta → 403 (y el dueño no lo
// recibe), listados globales solo para ADMIN, ids en el cuerpo de las escrituras y la
// regla de rol probada con el propio id del invitado.
// ============================================================
@DisplayName("IDOR /objetivos-personales — cobertura de todas las rutas con id (GP-048)")
class ObjetivoPersonalOwnershipTest extends AbstractOwnershipTest {

    @Autowired
    private IObjetivoPersonalRepository objetivoRepository;

    private Usuario admin;
    private Integer objetivoOwner;
    private Map<String, Object> ids;

    @BeforeEach
    void sembrar() {
        admin = crearUsuario("__idor_admin__", RoleType.ADMIN);
        objetivoOwner = crearObjetivo(owner).getId();
        ids = Map.of("objetivo", objetivoOwner, "owner", owner.getId(), "tipo", TipoObjetivo.PERDER_PESO);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "GET /objetivos-personales/{objetivo}",
            "PATCH /objetivos-personales/{objetivo}",
            "PUT /objetivos-personales/{objetivo}/completar",
            "DELETE /objetivos-personales/{objetivo}",
            "GET /objetivos-personales/usuario/{owner}",
            "GET /objetivos-personales/usuario/{owner}/ordenados",
            "GET /objetivos-personales/usuario/{owner}/pendientes",
            "GET /objetivos-personales/usuario/{owner}/completados",
            "GET /objetivos-personales/count/usuario/{owner}",
            "GET /objetivos-personales/count/usuario/{owner}/completados",
            "GET /objetivos-personales/count/usuario/{owner}/pendientes"
    })
    @DisplayName("id ajeno → 403 al atacante; el dueño no recibe 403")
    void idAjeno_403(String plantillaRuta) throws Exception {
        String ruta = rellenar(plantillaRuta, ids);
        assertThat(estado(attacker, ruta)).as("atacante en " + ruta).isEqualTo(403);
        assertThat(estado(owner, ruta)).as("dueño en " + ruta).isNotEqualTo(403);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "GET /objetivos-personales",
            "GET /objetivos-personales/tipo/{tipo}"
    })
    @DisplayName("listado global → 403 a un USER; ADMIN no")
    void soloAdmin_403(String plantillaRuta) throws Exception {
        String ruta = rellenar(plantillaRuta, ids);
        assertThat(estado(attacker, ruta)).as("USER en " + ruta).isEqualTo(403);
        assertThat(estado(admin, ruta)).as("ADMIN en " + ruta).isNotEqualTo(403);
    }

    @Test
    @DisplayName("PUT /objetivos-personales con el id de un objetivo ajeno en el cuerpo → 403 y no lo toca")
    void putConIdAjeno_403() throws Exception {
        pedir(attacker, "PUT /objetivos-personales",
                "{\"id\":" + objetivoOwner + ",\"valorObjetivo\":1,\"completado\":true}")
                .andExpect(status().isForbidden());

        ObjetivoPersonal leido = objetivoRepository.findById(objetivoOwner).orElseThrow();
        assertThat(leido.getValorObjetivo()).isEqualByComparingTo("70");
    }

    @Test
    @DisplayName("POST /objetivos-personales con el usuarioId de otro → el objetivo es del token")
    void postConUsuarioAjeno_quedaDelToken() throws Exception {
        pedir(attacker, "POST /objetivos-personales",
                "{\"usuarioId\":" + owner.getId() + ",\"tipoObjetivo\":\"PERDER_PESO\","
                        + "\"descripcion\":\"colado\",\"valorObjetivo\":60}")
                .andExpect(status().is2xxSuccessful());

        assertThat(objetivoRepository.findByUsuarioId(owner.getId())).hasSize(1);
        assertThat(objetivoRepository.findByUsuarioId(attacker.getId())).hasSize(1);
    }

    @Test
    @DisplayName("GUEST pidiendo SUS propios objetivos → 403 (lo para la regla de rol)")
    void guestConSuPropioId_403() throws Exception {
        crearObjetivo(guest);
        assertThat(estado(guest, "GET /objetivos-personales/usuario/" + guest.getId())).isEqualTo(403);
    }

    private ObjetivoPersonal crearObjetivo(Usuario dueno) {
        ObjetivoPersonal o = new ObjetivoPersonal();
        o.setUsuario(dueno);
        o.setTipoObjetivo(TipoObjetivo.PERDER_PESO);
        o.setDescripcion("Objetivo de prueba");
        o.setValorObjetivo(new BigDecimal("70"));
        o.setFechaInicio(LocalDate.of(2026, 9, 1));
        o.setCompletado(false);
        return objetivoRepository.save(o);
    }
}
