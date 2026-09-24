package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// AlimentoVisibilidadTest — los alimentos personales no son catálogo (GP-048)
//
// FALLO REAL encontrado al buscar hermanos: /alimentos/usuario/{id} se cerró el
// 2026-09-19, pero GET /alimentos/{id}, /alimentos, /nombre, /categoria, /activos y
// /calorias devolvían también los alimentos personales de TODOS los usuarios, a
// cualquiera, también al invitado, que entra sin credenciales. Y los contadores los
// contaban. Ahora:
//   · por id: el alimento personal ajeno → 403 (tiene dueño, DEC-027);
//   · listados y contadores: sin dueño en la ruta → aislamiento, el ajeno no sale.
// Y la escritura de lo que tiene dueño (el ajeno) o es de todos (el catálogo), que
// solo tenía test por PUT: PATCH, DELETE, activar y borrado permanente.
// ============================================================
@DisplayName("Alimentos — lo personal no sale en el catálogo ni se escribe desde fuera (GP-048)")
class AlimentoVisibilidadTest extends AbstractOwnershipTest {

    private static final String NOMBRE_PRIVADO = "Dieta privada gp048";
    private static final String CATEGORIA = "CATEGORIA_GP048";

    @Autowired
    private IAlimentoRepository alimentoRepository;

    private Usuario admin;
    private Integer privadoOwner;
    private Integer catalogo;
    private Map<String, Object> ids;

    @BeforeEach
    void sembrar() {
        admin = crearUsuario("__idor_admin__", RoleType.ADMIN);
        privadoOwner = crearAlimento(NOMBRE_PRIVADO, owner).getId();
        catalogo = crearAlimento("Catalogo gp048", null).getId();
        ids = Map.of("privado", privadoOwner, "catalogo", catalogo, "categoria", CATEGORIA);
    }

    @Test
    @DisplayName("GET /alimentos/{id} del alimento personal ajeno → 403; al dueño no")
    void porId_ajeno_403() throws Exception {
        assertThat(estado(attacker, "GET /alimentos/" + privadoOwner)).isEqualTo(403);
        assertThat(estado(guest, "GET /alimentos/" + privadoOwner)).isEqualTo(403);
        assertThat(estado(owner, "GET /alimentos/" + privadoOwner)).isEqualTo(200);
        assertThat(estado(attacker, "GET /alimentos/" + catalogo)).as("el catálogo sí").isEqualTo(200);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "GET /alimentos",
            "GET /alimentos/activos",
            "GET /alimentos/nombre/gp048",
            "GET /alimentos/categoria/{categoria}",
            "GET /alimentos/calorias?min=770&max=779"
    })
    @DisplayName("listado del catálogo → el personal ajeno no sale; al dueño sí")
    void listado_aislado(String plantilla) throws Exception {
        String ruta = rellenar(plantilla, ids);
        assertThat(pedir(attacker, ruta).andReturn().getResponse().getContentAsString())
                .as("atacante en " + ruta).doesNotContain(NOMBRE_PRIVADO);
        assertThat(pedir(guest, ruta).andReturn().getResponse().getContentAsString())
                .as("invitado en " + ruta).doesNotContain(NOMBRE_PRIVADO);
        assertThat(pedir(owner, ruta).andReturn().getResponse().getContentAsString())
                .as("dueño en " + ruta).contains(NOMBRE_PRIVADO);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "GET /alimentos/count/activos",
            "GET /alimentos/count/categoria/{categoria}"
    })
    @DisplayName("contador del catálogo → no cuenta el personal ajeno")
    void contador_aislado(String plantilla) throws Exception {
        String ruta = rellenar(plantilla, ids);
        long alAtacante = contar(attacker, ruta);
        long alDueno = contar(owner, ruta);
        assertThat(alDueno - alAtacante).as("el dueño ve uno más: el suyo, en " + ruta).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "PATCH /alimentos/{privado}",
            "PUT /alimentos/{privado}/activar",
            "DELETE /alimentos/{privado}",
            "DELETE /alimentos/{privado}/permanente"
    })
    @DisplayName("escribir el alimento personal ajeno → 403; al dueño no")
    void escritura_ajena_403(String plantilla) throws Exception {
        String ruta = rellenar(plantilla, ids);
        assertThat(estado(attacker, ruta)).as("atacante en " + ruta).isEqualTo(403);
        assertThat(estado(owner, ruta)).as("dueño en " + ruta).isNotEqualTo(403);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "PATCH /alimentos/{catalogo}",
            "PUT /alimentos/{catalogo}/activar",
            "DELETE /alimentos/{catalogo}",
            "DELETE /alimentos/{catalogo}/permanente"
    })
    @DisplayName("escribir el catálogo → 403 a un USER; ADMIN no")
    void escritura_catalogo_soloAdmin(String plantilla) throws Exception {
        String ruta = rellenar(plantilla, ids);
        assertThat(estado(attacker, ruta)).as("USER en " + ruta).isEqualTo(403);
        assertThat(estado(admin, ruta)).as("ADMIN en " + ruta).isNotEqualTo(403);
    }

    private long contar(Usuario quien, String ruta) throws Exception {
        String json = pedir(quien, ruta).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("count").asLong();
    }

    private Alimento crearAlimento(String nombre, Usuario dueno) {
        Alimento a = new Alimento();
        a.setNombre(nombre);
        a.setCategoria(CATEGORIA);
        a.setCalorias(775);
        a.setActivo(true);
        a.setUsuario(dueno);
        return alimentoRepository.save(a);
    }
}
