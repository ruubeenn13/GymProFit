package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// ============================================================
// LoginConCorreoTest — POST /auth/login con el correo o el usuario (GP-103, lote 1.5.0)
//
// El alta nueva no pide usuario, así que se entra con el correo. El campo sigue siendo
// «username» (la 1.4.0 lo manda así): con «@» se busca primero por correo, sin
// distinguir mayúsculas, y si no hay, por usuario, por si alguna cuenta antigua lo
// lleva. El token sale con el username de la cuenta, como hoy.
// ============================================================
@DisplayName("GP-103 — entrar con el correo o el usuario")
class LoginConCorreoTest extends AbstractOwnershipTest {

    private static final String PASSWORD = "Frase corta de entrar";

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario cuenta;

    @BeforeEach
    void cuenta() {
        cuenta = crearUsuario("correo-gp103", RoleType.USER);
        cuenta.setEmail("Buzon.Gp103@Test.Local");
        cuenta.setPassword(passwordEncoder.encode(PASSWORD));
        usuarioRepository.save(cuenta);
    }

    @Test
    @DisplayName("con el correo, con otras mayúsculas y con el usuario: 200 y el token con el username de la cuenta")
    void tres_formas() throws Exception {
        for (String identificador : new String[]{"Buzon.Gp103@Test.Local", "buzon.gp103@test.local",
                "BUZON.GP103@TEST.LOCAL", "correo-gp103"}) {
            JsonNode r = entrarOk(identificador, PASSWORD);
            assertThat(r.get("username").asText()).as(identificador).isEqualTo("correo-gp103");
            assertThat(r.get("token").asText()).isNotBlank();
        }
    }

    @Test
    @DisplayName("la contraseña mala o el correo que no existe dan el mismo 401 que siempre")
    void fallos() throws Exception {
        MockHttpServletResponse mala = entrar("buzon.gp103@test.local", "otra cosa distinta");
        MockHttpServletResponse sinCuenta = entrar("nadie-gp103@test.local", PASSWORD);
        MockHttpServletResponse usuarioMalo = entrar("correo-gp103", "otra cosa distinta");
        assertThat(mala.getStatus()).isEqualTo(401);
        assertThat(sinCuenta.getStatus()).isEqualTo(401);
        assertThat(sinCuenta.getContentAsString()).isEqualTo(mala.getContentAsString())
                .isEqualTo(usuarioMalo.getContentAsString());
    }

    @Test
    @DisplayName("una cuenta antigua con «@» en el usuario sigue entrando con él")
    void usuario_antiguo_con_arroba() throws Exception {
        Usuario vieja = crearUsuario("vieja@gp103", RoleType.USER);
        vieja.setEmail("otro-buzon-gp103@test.local");
        vieja.setPassword(passwordEncoder.encode(PASSWORD));
        usuarioRepository.save(vieja);

        assertThat(entrarOk("vieja@gp103", PASSWORD).get("username").asText()).isEqualTo("vieja@gp103");
        assertThat(entrarOk("otro-buzon-gp103@test.local", PASSWORD).get("username").asText())
                .isEqualTo("vieja@gp103");
    }

    @Test
    @DisplayName("la 1.4.0: alta con usuario y entrar con usuario, como siempre")
    void flujo_140() throws Exception {
        String alta = objectMapper.writeValueAsString(Map.of(
                "username", "flujo140-gp103", "email", "flujo140-gp103@test.local", "password", PASSWORD));
        assertThat(mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(alta))
                .andReturn().getResponse().getStatus()).isEqualTo(201);
        assertThat(entrarOk("flujo140-gp103", PASSWORD).get("username").asText()).isEqualTo("flujo140-gp103");
    }

    @Test
    @DisplayName("el alta nueva: sin usuario, y se entra con el correo")
    void flujo_nuevo() throws Exception {
        String alta = objectMapper.writeValueAsString(Map.of("email", "Nueva.GP103@test.local", "password", PASSWORD));
        MockHttpServletResponse r = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(alta)).andReturn().getResponse();
        assertThat(r.getStatus()).isEqualTo(201);
        String username = objectMapper.readTree(r.getContentAsString()).get("username").asText();
        assertThat(entrarOk("nueva.gp103@test.local", PASSWORD).get("username").asText()).isEqualTo(username);
    }

    // --- Ayudas --------------------------------------------------------------

    private JsonNode entrarOk(String identificador, String password) throws Exception {
        MockHttpServletResponse r = entrar(identificador, password);
        assertThat(r.getStatus()).as(identificador + " → " + r.getContentAsString()).isEqualTo(200);
        return objectMapper.readTree(r.getContentAsString());
    }

    private MockHttpServletResponse entrar(String identificador, String password) throws Exception {
        String cuerpo = objectMapper.writeValueAsString(Map.of("username", identificador, "password", password));
        return mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andReturn().getResponse();
    }
}
