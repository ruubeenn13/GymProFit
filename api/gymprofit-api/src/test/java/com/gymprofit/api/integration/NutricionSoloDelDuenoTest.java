package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaCreateDTO;
import com.gymprofit.api.dto.entity.comida.ComidaCreateDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.service.alimentocomida.IAlimentoComidaService;
import com.gymprofit.api.service.comida.IComidaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// NutricionSoloDelDuenoTest — DEC-042: los alimentos, los platos y las líneas del diario
// de un usuario son solo suyos, también para un ADMIN.
// Un ADMIN ve y mantiene el catálogo como siempre; lo que tiene dueño no sale en sus
// listados ni en sus contadores, y un id de otro le da el mismo 403 que a un USER (DEC-014).
// Con el contexto levantado y JWT de verdad.
// ============================================================
@DisplayName("DEC-042 — la nutrición de un usuario es solo suya, también para un ADMIN")
class NutricionSoloDelDuenoTest extends AbstractOwnershipTest {

    private static final String CATEGORIA = "Kzdueno";

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @Autowired
    private IComidaService comidaService;

    @Autowired
    private IAlimentoComidaService alimentoComidaService;

    private Usuario admin;
    private Integer catalogo;
    private Integer propio;
    private Integer plato;
    private Integer comidaOwner;
    private Integer lineaCatalogo;
    private Integer lineaPropia;
    private Integer comidaAdmin;
    private Integer lineaAdmin;

    @BeforeEach
    void sembrar() {
        admin = crearUsuario("__dueno_admin__", RoleType.ADMIN);
        catalogo = alimento("Kzdueno catálogo", null, null).getId();
        propio = alimento("Kzdueno propio", owner, null).getId();
        plato = alimento("Kzdueno plato", owner, 2).getId();

        runAs(owner, () -> {
            comidaOwner = comida(owner, "DESAYUNO");
            lineaCatalogo = linea(comidaOwner, catalogo);
            lineaPropia = linea(comidaOwner, propio);
        });
        runAs(admin, () -> {
            comidaAdmin = comida(admin, "CENA");
            lineaAdmin = linea(comidaAdmin, catalogo);
        });
    }

    @Test
    @DisplayName("los listados de alimentos de un ADMIN: el catálogo y lo suyo")
    void listados_admin() throws Exception {
        for (String ruta : List.of("/alimentos", "/alimentos/nombre/kzdueno", "/alimentos/categoria/" + CATEGORIA,
                "/alimentos/activos", "/alimentos/calorias?min=0&max=1000")) {
            assertThat(ids(admin, ruta)).as(ruta).contains(catalogo).doesNotContain(propio, plato);
        }
        // Los contadores, como los de cualquier otra cuenta sin alimentos propios.
        for (String ruta : List.of("/alimentos/count/activos", "/alimentos/count/categoria/" + CATEGORIA)) {
            assertThat(cuerpo(admin, ruta).get("count").asLong()).as(ruta).isEqualTo(cuerpo(attacker, ruta).get("count").asLong());
        }
    }

    @Test
    @DisplayName("un id de alimento o de plato de otro: 403 también a un ADMIN")
    void ids_admin() throws Exception {
        for (Integer id : List.of(propio, plato)) {
            for (String ruta : List.of("GET /alimentos/" + id, "PATCH /alimentos/" + id, "DELETE /alimentos/" + id,
                    "PUT /alimentos/" + id + "/activar", "DELETE /alimentos/" + id + "/permanente",
                    "GET /alimentos-comida/alimento/" + id, "GET /alimentos-comida/count/alimento/" + id)) {
                assertThat(estado(admin, ruta)).as(ruta).isEqualTo(403);
            }
        }
        assertThat(estado(admin, "GET /alimentos/usuario/" + owner.getId())).isEqualTo(403);
        // Meterlo en una comida propia tampoco: la respuesta devolvería su nombre y sus macros.
        String cuerpo = "{\"comidaId\":" + comidaAdmin + ",\"alimentoId\":" + propio + ",\"cantidadGramos\":100}";
        assertThat(pedir(admin, "POST /alimentos-comida", cuerpo).andReturn().getResponse().getStatus())
                .isEqualTo(403);
        // El catálogo, como siempre.
        assertThat(estado(admin, "GET /alimentos/" + catalogo)).isEqualTo(200);
    }

    @Test
    @DisplayName("las líneas del diario de otro: ni en los listados ni por id, también para un ADMIN")
    void diario_admin() throws Exception {
        assertThat(ids(admin, "/alimentos-comida")).contains(lineaAdmin).doesNotContain(lineaCatalogo, lineaPropia);
        assertThat(ids(admin, "/alimentos-comida/alimento/" + catalogo))
                .contains(lineaAdmin).doesNotContain(lineaCatalogo);
        assertThat(cuerpo(admin, "/alimentos-comida/count/alimento/" + catalogo).get("count").asLong()).isEqualTo(1);

        for (String ruta : List.of("GET /alimentos-comida/" + lineaCatalogo,
                "PATCH /alimentos-comida/" + lineaCatalogo,
                "DELETE /alimentos-comida/" + lineaCatalogo,
                "GET /alimentos-comida/comida/" + comidaOwner,
                "GET /alimentos-comida/comida/" + comidaOwner + "/alimento/" + catalogo,
                "GET /alimentos-comida/exists/comida/" + comidaOwner + "/alimento/" + catalogo,
                "GET /alimentos-comida/count/comida/" + comidaOwner,
                "DELETE /alimentos-comida/comida/" + comidaOwner + "/alimento/" + catalogo,
                "DELETE /alimentos-comida/comida/" + comidaOwner)) {
            assertThat(estado(admin, ruta)).as(ruta).isEqualTo(403);
        }
        String cuerpo = "{\"comidaId\":" + comidaOwner + ",\"alimentoId\":" + catalogo + ",\"cantidadGramos\":10}";
        assertThat(pedir(admin, "POST /alimentos-comida", cuerpo).andReturn().getResponse().getStatus())
                .isEqualTo(403);
    }

    @Test
    @DisplayName("el dueño sigue viendo lo suyo")
    void el_dueno_si() throws Exception {
        assertThat(ids(owner, "/alimentos/nombre/kzdueno")).contains(catalogo, propio, plato);
        assertThat(ids(owner, "/alimentos/usuario/" + owner.getId())).contains(propio, plato);
        assertThat(estado(owner, "GET /alimentos/" + propio)).isEqualTo(200);
        assertThat(estado(owner, "GET /alimentos/" + plato)).isEqualTo(200);
        assertThat(estado(owner, "GET /alimentos-comida/" + lineaPropia)).isEqualTo(200);
        assertThat(ids(owner, "/alimentos-comida/comida/" + comidaOwner)).contains(lineaCatalogo, lineaPropia);
    }

    private Alimento alimento(String nombre, Usuario dueno, Integer raciones) {
        Alimento a = new Alimento();
        a.setNombre(nombre);
        a.setCategoria(CATEGORIA);
        a.setCalorias(100);
        a.setActivo(true);
        a.setUsuario(dueno);
        a.setPlatoRaciones(raciones);
        return alimentoRepository.save(a);
    }

    private Integer comida(Usuario quien, String tipo) {
        ComidaCreateDTO c = new ComidaCreateDTO();
        c.setUsuarioId(quien.getId());
        c.setTipoComida(tipo);
        return comidaService.save(c).getId();
    }

    private Integer linea(Integer comidaId, Integer alimentoId) {
        AlimentoComidaCreateDTO l = new AlimentoComidaCreateDTO();
        l.setComidaId(comidaId);
        l.setAlimentoId(alimentoId);
        l.setCantidadGramos(new BigDecimal("100"));
        return alimentoComidaService.save(l).getId();
    }

    private JsonNode cuerpo(Usuario quien, String ruta) throws Exception {
        var r = pedir(quien, "GET " + ruta).andReturn().getResponse();
        assertThat(r.getStatus()).as(ruta).isEqualTo(200);
        return objectMapper.readTree(r.getContentAsString(StandardCharsets.UTF_8));
    }

    private List<Integer> ids(Usuario quien, String ruta) throws Exception {
        JsonNode lista = cuerpo(quien, ruta);
        List<Integer> ids = new ArrayList<>();
        lista.forEach(n -> ids.add(n.get("id").asInt()));
        return ids;
    }
}
