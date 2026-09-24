package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

// ============================================================
// UsuarioOwnershipTest — cobertura IDOR de /usuarios/** y del panel /admin/** (GP-048)
//
//   · rutas del propio perfil con el id de otro → 403; el dueño no lo recibe;
//   · rutas de administración pedidas por un USER → 403; ADMIN no (entre ellas
//     /admin/usuarios/{id}/rol y /toggle-activo, que no tenían test);
//   · la regla de rol del perfil, con el propio id del invitado.
// El username y el correo también identifican a un usuario: cuentan como id.
// ============================================================
@DisplayName("IDOR /usuarios y /admin — cobertura de las rutas con id (GP-048)")
class UsuarioOwnershipTest extends AbstractOwnershipTest {

    private Usuario admin;
    private Map<String, Object> ids;

    @BeforeEach
    void sembrar() {
        admin = crearUsuario("__idor_admin__", RoleType.ADMIN);
        ids = Map.of("owner", owner.getId(), "username", owner.getUsername(), "email", owner.getEmail(),
                "guest", guest.getId());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "GET /usuarios/{owner}",
            "PATCH /usuarios/{owner}",
            "GET /usuarios/username/{username}",
            "GET /usuarios/{owner}/estadisticas",
            "GET /usuarios/{owner}/foto"
    })
    @DisplayName("perfil ajeno → 403 al atacante; el dueño no recibe 403")
    void idAjeno_403(String plantillaRuta) throws Exception {
        String ruta = rellenar(plantillaRuta, ids);
        assertThat(estado(attacker, ruta)).as("atacante en " + ruta).isEqualTo(403);
        assertThat(estado(owner, ruta)).as("dueño en " + ruta).isNotEqualTo(403);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "PATCH /admin/usuarios/{owner}/rol?nuevoRol=ADMIN",
            "PATCH /admin/usuarios/{owner}/toggle-activo",
            "GET /admin/usuarios",
            "GET /admin/estadisticas-globales",
            "GET /usuarios",
            "GET /usuarios/activos",
            "GET /usuarios/email/{email}",
            "GET /usuarios/exists/username/{username}",
            "GET /usuarios/exists/email/{email}",
            "PUT /usuarios/{owner}/activar",
            "DELETE /usuarios/{owner}",
            "DELETE /usuarios/{owner}/permanente",
            "GET /jooq/usuarios/nivel/PRINCIPIANTE"
    })
    @DisplayName("ruta de administración → 403 a un USER; ADMIN no")
    void soloAdmin_403(String plantillaRuta) throws Exception {
        String ruta = rellenar(plantillaRuta, ids);
        assertThat(estado(attacker, ruta)).as("USER en " + ruta).isEqualTo(403);
        assertThat(estado(admin, ruta)).as("ADMIN en " + ruta).isNotEqualTo(403);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "GET /usuarios/{guest}",
            "PATCH /usuarios/{guest}",
            "GET /usuarios/{guest}/estadisticas",
            "GET /usuarios/{guest}/foto"
    })
    @DisplayName("GUEST pidiendo SU propio perfil → 403 (lo para la regla de rol)")
    void guestConSuPropioId_403(String plantillaRuta) throws Exception {
        assertThat(estado(guest, rellenar(plantillaRuta, ids))).isEqualTo(403);
    }

    @Test
    @DisplayName("POST /usuarios/{id}/foto a la cuenta de otro → 403")
    void subirFotoAjena_403() throws Exception {
        // JPEG mínimo: la cabecera basta para pasar la validación por magic bytes.
        MockMultipartFile foto = new MockMultipartFile("foto", "f.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});
        int estado = mockMvc.perform(multipart("/usuarios/" + owner.getId() + "/foto").file(foto)
                        .header("Authorization", "Bearer " + token(attacker)))
                .andReturn().getResponse().getStatus();
        assertThat(estado).isEqualTo(403);
    }
}
