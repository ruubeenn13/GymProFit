package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.NivelActividad;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.enums.Sexo;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// ============================================================
// AltaSinUsuarioTest — POST /auth/register sin nombre de usuario (GP-103, lote 1.5.0)
//
// El alta nueva pide el correo y la contraseña, y el nombre de usuario lo propone la
// API con la parte del correo antes de la «@». Se comprueba la propuesta (limpia, de
// 3 a 30 caracteres, con número si se queda corta o ya existe), que dos correos que
// chocan dan usuarios distintos, que la cuenta nace con su perfil, que ningún nombre
// nuevo lleva «@» se cree por donde se cree, y que el alta de la 1.4.0, con usuario,
// sigue igual.
// ============================================================
@DisplayName("GP-103 — alta sin nombre de usuario")
class AltaSinUsuarioTest extends AbstractOwnershipTest {

    private static final String PASSWORD = "Frase corta de alta";

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @PersistenceContext
    private EntityManager em;

    @Test
    @DisplayName("sin usuario: se propone con la parte del correo, y dos que chocan reciben usuarios distintos")
    void dos_correos_que_chocan() throws Exception {
        String primero = altaOk(Map.of("email", "ana@a.com"));
        String segundo = altaOk(Map.of("email", "ana@b.com"));
        assertThat(primero).isEqualTo("ana");
        assertThat(segundo).isEqualTo("ana2");
        assertThat(usuarioRepository.findByUsername("ana2").orElseThrow().getEmail()).isEqualTo("ana@b.com");
    }

    @Test
    @DisplayName("la propuesta va en minúsculas, sin tildes y solo con letras, números, punto y guion bajo")
    void propuesta_limpia() throws Exception {
        assertThat(altaOk(Map.of("email", "José.Pérez_Ñú+gym@x.com"))).isEqualTo("jose.perez_nugym");
    }

    @Test
    @DisplayName("corta: se completa con número hasta 3; larga: se corta a 30, y con número sigue en 30")
    void longitudes() throws Exception {
        assertThat(altaOk(Map.of("email", "al@x.com"))).isEqualTo("al1");
        assertThat(altaOk(Map.of("email", "b@x.com"))).isEqualTo("b10");
        String largo = "a".repeat(40);
        assertThat(altaOk(Map.of("email", largo + "@x.com"))).isEqualTo("a".repeat(30));
        assertThat(altaOk(Map.of("email", largo + "@y.com"))).isEqualTo("a".repeat(29) + "2");
    }

    @Test
    @DisplayName("sin nada aprovechable en el correo, «usuario» y su número")
    void nada_aprovechable() throws Exception {
        assertThat(altaOk(Map.of("email", "+++@x.com"))).isEqualTo("usuario");
        assertThat(altaOk(Map.of("email", "---@x.com"))).isEqualTo("usuario2");
    }

    @Test
    @DisplayName("la cuenta nace con su perfil: nombre recortado, sexo y actividad")
    void nace_con_perfil() throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("email", "perfil-gp103@test.local");
        cuerpo.put("nombre", "   Ana María  ");
        cuerpo.put("sexo", "MUJER");
        cuerpo.put("nivelActividad", "MODERADO");
        cuerpo.put("nivelExperiencia", "INTERMEDIO");
        String username = altaOk(cuerpo);

        em.flush();
        em.clear();
        Usuario u = usuarioRepository.findByUsername(username).orElseThrow();
        assertThat(u.getNombre()).isEqualTo("Ana María");
        assertThat(u.getSexo()).isEqualTo(Sexo.MUJER);
        assertThat(u.getNivelActividad()).isEqualTo(NivelActividad.MODERADO);
    }

    @Test
    @DisplayName("el nombre sigue la regla del PATCH: en blanco no se guarda y con más de 40 es 400")
    void nombre_con_la_regla_del_patch() throws Exception {
        String username = altaOk(Map.of("email", "blanco-gp103@test.local", "nombre", "   "));
        assertThat(usuarioRepository.findByUsername(username).orElseThrow().getNombre()).isNull();

        MockHttpServletResponse r = alta(Map.of("email", "largo-gp103@test.local", "nombre", "a".repeat(41)));
        assertThat(r.getStatus()).isEqualTo(400);
        assertThat(usuarioRepository.findByEmail("largo-gp103@test.local")).isEmpty();
        assertThat(alta(Map.of("email", "sexo-gp103@test.local", "sexo", "OTRO")).getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("ningún usuario nuevo lleva «@»: ni en el alta ni en POST /usuarios")
    void sin_arroba() throws Exception {
        MockHttpServletResponse r = alta(Map.of("username", "ana@b.com", "email", "arroba-gp103@test.local"));
        assertThat(r.getStatus()).isEqualTo(400);
        assertThat(objectMapper.readTree(r.getContentAsString()).get("cause").asText())
                .isEqualTo(InvalidDataException.USERNAME_NO_VALIDO);
        assertThat(usuarioRepository.findByEmail("arroba-gp103@test.local")).isEmpty();

        Usuario admin = crearUsuario("__admin_gp103__", RoleType.ADMIN);
        pedir(admin, "POST /usuarios", """
                {"username":"otra@cuenta","password":"%s","email":"arroba2-gp103@test.local"}""".formatted(PASSWORD))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(400));
        assertThat(usuarioRepository.findByEmail("arroba2-gp103@test.local")).isEmpty();
    }

    @Test
    @DisplayName("la 1.4.0: con usuario se da de alta como siempre, y la respuesta lo devuelve")
    void alta_de_la_140() throws Exception {
        assertThat(altaOk(Map.of("username", "Clasico_140", "email", "clasico-gp103@test.local")))
                .isEqualTo("Clasico_140");
        assertThat(usuarioRepository.findByUsername("Clasico_140")).isPresent();
    }

    @Test
    @DisplayName("el correo en uso se dice antes de proponer nada")
    void correo_en_uso() throws Exception {
        MockHttpServletResponse r = alta(Map.of("email", owner.getEmail()));
        assertThat(r.getStatus()).isEqualTo(400);
        assertThat(objectMapper.readTree(r.getContentAsString()).get("cause").asText()).isEqualTo("EMAIL_EN_USO");
    }

    // --- Ayudas --------------------------------------------------------------

    private String altaOk(Map<String, ?> datos) throws Exception {
        MockHttpServletResponse r = alta(datos);
        assertThat(r.getStatus()).as(r.getContentAsString()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(r.getContentAsString());
        String username = json.get("username").asText();
        assertThat(username).doesNotContain("@");
        return username;
    }

    private MockHttpServletResponse alta(Map<String, ?> datos) throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>(datos);
        cuerpo.putIfAbsent("password", PASSWORD);
        return mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andReturn().getResponse();
    }
}
