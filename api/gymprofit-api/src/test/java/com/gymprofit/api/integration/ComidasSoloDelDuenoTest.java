package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.dto.entity.comida.ComidaCreateDTO;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.service.comida.IComidaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// ComidasSoloDelDuenoTest — GP-197: las comidas de un usuario son solo suyas, también
// para un ADMIN (DEC-042, DEC-027), y lo que se crea es siempre de quien llama (DEC-013).
// Las rutas de /comidas que siguen siendo de ADMIN devuelven lo de quien llama; un id
// de otro da el mismo 403 que a un USER (DEC-014). Con el contexto levantado y JWT de verdad.
// ============================================================
@DisplayName("GP-197 — las comidas de un usuario son solo suyas, también para un ADMIN")
class ComidasSoloDelDuenoTest extends AbstractOwnershipTest {

    @Autowired
    private IComidaService comidaService;

    @Autowired
    private IAlimentoRepository alimentoRepository;

    private Usuario admin;
    private Integer comidaOwner;
    private Integer comidaAdmin;

    @BeforeEach
    void sembrar() {
        admin = crearUsuario("__comidas_admin__", RoleType.ADMIN);
        runAs(owner, () -> comidaOwner = comida(owner, "DESAYUNO"));
        runAs(admin, () -> comidaAdmin = comida(admin, "DESAYUNO"));
    }

    @Test
    @DisplayName("los listados y el contador de ADMIN: solo sus comidas")
    void listados_admin() throws Exception {
        for (String ruta : List.of("/comidas", "/comidas/tipo/DESAYUNO", "/comidas/fecha/" + LocalDate.now())) {
            assertThat(ids(admin, ruta)).as(ruta).contains(comidaAdmin).doesNotContain(comidaOwner);
        }
        assertThat(cuerpo(admin, "/comidas/count/tipo/DESAYUNO").get("count").asLong()).isEqualTo(1);
    }

    @Test
    @DisplayName("una comida o un usuario de otro: 403 también a un ADMIN")
    void ids_admin() throws Exception {
        Integer o = owner.getId();
        String hoy = LocalDate.now().toString();
        for (String ruta : List.of("GET /comidas/" + comidaOwner, "PATCH /comidas/" + comidaOwner,
                "DELETE /comidas/" + comidaOwner,
                "GET /comidas/usuario/" + o, "GET /comidas/usuario/" + o + "/fecha/" + hoy,
                "GET /comidas/usuario/" + o + "/resumen?inicio=" + hoy + "&fin=" + hoy,
                "GET /comidas/usuario/" + o + "/tipo/DESAYUNO", "GET /comidas/count/usuario/" + o,
                "GET /comidas/count/usuario/" + o + "/tipo/DESAYUNO")) {
            assertThat(estado(admin, ruta)).as(ruta).isEqualTo(403);
        }
        String put = "{\"id\":" + comidaOwner + ",\"tipoComida\":\"CENA\"}";
        assertThat(pedir(admin, "PUT /comidas", put).andReturn().getResponse().getStatus()).isEqualTo(403);
        // La suya, sí.
        assertThat(estado(admin, "GET /comidas/" + comidaAdmin)).isEqualTo(200);
    }

    @Test
    @DisplayName("crear a nombre de otro: el dueño sale del token, también para un ADMIN")
    void crear_es_de_quien_llama() throws Exception {
        String comida = "{\"usuarioId\":" + owner.getId() + ",\"tipoComida\":\"CENA\"}";
        JsonNode c = objectMapper.readTree(pedir(admin, "POST /comidas", comida).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
        assertThat(c.get("usuarioId").asInt()).isEqualTo(admin.getId());
        assertThat(cuerpo(owner, "/comidas/count/usuario/" + owner.getId()).get("count").asLong()).isEqualTo(1);

        String alimento = "{\"nombre\":\"Kzcomidas ajeno\",\"calorias\":10,\"usuarioId\":" + owner.getId() + "}";
        JsonNode a = objectMapper.readTree(pedir(admin, "POST /alimentos", alimento).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
        assertThat(alimentoRepository.findById(a.get("id").asInt()).orElseThrow().getUsuario().getId())
                .isEqualTo(admin.getId());

        // Sin usuarioId, un ADMIN sigue creando catálogo.
        JsonNode cat = objectMapper.readTree(pedir(admin, "POST /alimentos", "{\"nombre\":\"Kzcomidas cat\",\"calorias\":10}")
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(alimentoRepository.findById(cat.get("id").asInt()).orElseThrow().getUsuario()).isNull();
    }

    @Test
    @DisplayName("el dueño sigue viendo sus comidas")
    void el_dueno_si() throws Exception {
        assertThat(estado(owner, "GET /comidas/" + comidaOwner)).isEqualTo(200);
        assertThat(ids(owner, "/comidas/usuario/" + owner.getId())).contains(comidaOwner).doesNotContain(comidaAdmin);
    }

    private Integer comida(Usuario quien, String tipo) {
        ComidaCreateDTO c = new ComidaCreateDTO();
        c.setUsuarioId(quien.getId());
        c.setTipoComida(tipo);
        return comidaService.save(c).getId();
    }

    private JsonNode cuerpo(Usuario quien, String ruta) throws Exception {
        var r = pedir(quien, "GET " + ruta).andReturn().getResponse();
        assertThat(r.getStatus()).as(ruta).isEqualTo(200);
        return objectMapper.readTree(r.getContentAsString(StandardCharsets.UTF_8));
    }

    private List<Integer> ids(Usuario quien, String ruta) throws Exception {
        List<Integer> ids = new ArrayList<>();
        cuerpo(quien, ruta).forEach(n -> ids.add(n.get("id").asInt()));
        return ids;
    }
}
