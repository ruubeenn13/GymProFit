package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.enums.NivelExperiencia;
import com.gymprofit.api.enums.TipoObjetivo;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.sql.DataSource;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// ProgramasSinCuentaTest — recomendado y vista previa sin cuenta (GP-103, lote 1.5.0)
//
// El alta nueva enseña «Tu plan» antes de crear la cuenta, así que esas dos rutas se
// piden sin token y con el nivel y el objetivo del cuestionario en la consulta. Lo que
// se comprueba: que sin token dan 200 y no dicen nada de ninguna cuenta; que con los
// parámetros no se mira el perfil, ni siquiera con token; que el resto de /programas
// sigue pidiendo cuenta; y que un token caducado sigue siendo 401 en estas dos rutas,
// porque la 1.4.0 renueva con ese 401 y, si no llegara, recibiría la recomendación de
// un perfil vacío sin enterarse.
// ============================================================
@DisplayName("GP-103 — recomendado y vista previa sin cuenta")
class ProgramasSinCuentaTest extends AbstractOwnershipTest {

    private static final String RECOMENDADO = "/programas/recomendado?equipamiento=GIMNASIO&dias=4";
    private static final String VISTA = "/programas/GIM-TP/vista-previa?minutos=30";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Value("${jwt.secret}")
    private String secreto;

    @BeforeEach
    void sembrar() {
        CatalogoPlantillasDePrueba.sembrarCompleto(jdbc, dataSource);
        owner.setNivelExperiencia(NivelExperiencia.AVANZADO);
        owner.setObjetivo(TipoObjetivo.MEJORAR_FUERZA);
    }

    @Test
    @DisplayName("sin token y sin nivel: 200, como un perfil sin nivel")
    void sin_token_sin_nivel() throws Exception {
        JsonNode r = json(sinToken("GET " + RECOMENDADO));
        assertThat(r.get("programa").get("codigo").asText()).isEqualTo("GIM-PE");
        assertThat(r.get("nivel").asText()).isEqualTo("PRINCIPIANTE");
        assertThat(r.get("nivelEnPerfil").asBoolean()).isFalse();

        JsonNode v = json(sinToken("GET " + VISTA));
        assertThat(v.get("ajustes")).isEmpty();
    }

    @Test
    @DisplayName("sin token con nivel y objetivo: los del cuestionario, y nivelEnPerfil a false")
    void sin_token_con_parametros() throws Exception {
        JsonNode r = json(sinToken("GET " + RECOMENDADO + "&nivel=INTERMEDIO"));
        assertThat(r.get("programa").get("codigo").asText()).isEqualTo("GIM-TP");
        assertThat(r.get("nivel").asText()).isEqualTo("INTERMEDIO");
        assertThat(r.get("nivelEnPerfil").asBoolean()).isFalse();

        JsonNode v = json(sinToken("GET " + VISTA + "&nivel=AVANZADO&objetivo=MEJORAR_FUERZA"));
        assertThat(v.get("ajustes")).extracting(JsonNode::asText).containsExactly("AVANZADO", "FUERZA");
        // Mismas reglas que con cuenta: igual a la vista previa de un perfil con esos datos.
        assertThat(pedir(owner, "GET " + VISTA).andReturn().getResponse().getContentAsString())
                .isEqualTo(sinToken("GET " + VISTA + "&nivel=AVANZADO&objetivo=MEJORAR_FUERZA").getContentAsString());
    }

    @Test
    @DisplayName("con token y parámetros no se mira el perfil, ni para los ajustes")
    void con_token_manda_el_parametro() throws Exception {
        JsonNode r = json(pedir(owner, "GET " + RECOMENDADO + "&nivel=PRINCIPIANTE").andReturn().getResponse());
        assertThat(r.get("programa").get("codigo").asText()).isEqualTo("GIM-PE");
        assertThat(r.get("nivelEnPerfil").asBoolean()).isFalse();

        // Solo el nivel: el objetivo del perfil (fuerza) no se cuela en los ajustes.
        JsonNode v = json(pedir(owner, "GET " + VISTA + "&nivel=AVANZADO").andReturn().getResponse());
        assertThat(v.get("ajustes")).extracting(JsonNode::asText).containsExactly("AVANZADO");
        v = json(pedir(owner, "GET " + VISTA + "&objetivo=GANAR_MASA_MUSCULAR").andReturn().getResponse());
        assertThat(v.get("ajustes")).isEmpty();

        // Sin parámetros, el perfil, como hasta ahora.
        r = json(pedir(owner, "GET " + RECOMENDADO).andReturn().getResponse());
        assertThat(r.get("nivelEnPerfil").asBoolean()).isTrue();
        assertThat(r.get("nivel").asText()).isEqualTo("AVANZADO");
    }

    @Test
    @DisplayName("sin token no sale nada de ninguna cuenta: la respuesta no cambia con lo que haga el dueño")
    void aislamiento() throws Exception {
        String recomendadoAntes = sinToken("GET " + RECOMENDADO).getContentAsString();
        String vistaAntes = sinToken("GET " + VISTA).getContentAsString();

        assertThat(pedir(owner, "POST /programas/GIM-TP/seguir", "{\"minutos\":30}").andReturn().getResponse()
                .getStatus()).isEqualTo(201);

        assertThat(sinToken("GET " + RECOMENDADO).getContentAsString()).isEqualTo(recomendadoAntes);
        assertThat(sinToken("GET " + VISTA).getContentAsString()).isEqualTo(vistaAntes);
        assertThat(recomendadoAntes).doesNotContain(owner.getUsername()).doesNotContain(String.valueOf(owner.getId()));
    }

    @Test
    @DisplayName("el resto de /programas sigue pidiendo cuenta: 401 sin token")
    void resto_con_cuenta() throws Exception {
        assertThat(sinToken("GET /programas/seguido").getStatus()).isEqualTo(401);
        assertThat(sinToken("DELETE /programas/seguido").getStatus()).isEqualTo(401);
        assertThat(sinToken("POST /programas/GIM-TP/seguir").getStatus()).isEqualTo(401);
        assertThat(sinToken("GET /programas").getStatus()).isEqualTo(401);
        assertThat(sinToken("GET /programas/GIM-TP").getStatus()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM programas_usuario", Integer.class)).isZero();
    }

    @Test
    @DisplayName("un token caducado sigue siendo 401, para que la 1.4.0 renueve y no reciba un perfil vacío")
    void token_caducado() throws Exception {
        String caducado = Jwts.builder().subject(owner.getUsername())
                .issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .expiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto)))
                .compact();
        for (String ruta : new String[]{RECOMENDADO, VISTA}) {
            int estado = mockMvc.perform(MockMvcRequestBuilders.get(ruta)
                    .header("Authorization", "Bearer " + caducado)).andReturn().getResponse().getStatus();
            assertThat(estado).as(ruta).isEqualTo(401);
        }
    }

    @Test
    @DisplayName("un nivel o un objetivo que no existe es 400")
    void parametros_no_validos() throws Exception {
        assertThat(sinToken("GET " + RECOMENDADO + "&nivel=LEYENDA").getStatus()).isEqualTo(400);
        assertThat(sinToken("GET " + VISTA + "&nivel=LEYENDA").getStatus()).isEqualTo(400);
        assertThat(sinToken("GET " + VISTA + "&objetivo=VOLAR").getStatus()).isEqualTo(400);
    }

    // --- Ayudas --------------------------------------------------------------

    private MockHttpServletResponse sinToken(String verboYRuta) throws Exception {
        String[] partes = verboYRuta.split(" ", 2);
        MockHttpServletRequestBuilder peticion = MockMvcRequestBuilders
                .request(HttpMethod.valueOf(partes[0]), partes[1])
                .header("Accept-Language", "es")
                .accept(MediaType.APPLICATION_JSON);
        return mockMvc.perform(peticion).andReturn().getResponse();
    }

    private JsonNode json(MockHttpServletResponse r) throws Exception {
        assertThat(r.getStatus()).as(r.getContentAsString()).isEqualTo(200);
        return objectMapper.readTree(r.getContentAsString());
    }
}
