package com.gymprofit.api.integration;

import com.gymprofit.api.config.security.JwtTokenProvider;
import com.gymprofit.api.entity.RefreshToken;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.exceptions.CuentaDesactivadaException;
import com.gymprofit.api.repository.jpa.IRefreshTokenRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.auth.RefreshTokenService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// CuentaDesactivadaYCorreoTest — GP-083
//
// Una cuenta desactivada seguía entrando, se reactivaba sola con el PATCH del perfil,
// y el correo se cambiaba sin contraseña ni comprobaciones. Estos tests cubren los
// cuatro frentes con el contexto entero levantado.
//
// Las peticiones llevan un JWT DE VERDAD en la cabecera, no @WithUserDetails: lo que se
// prueba aquí es justo el filtro JWT, y @WithUserDetails lo salta rellenando el
// SecurityContext por su cuenta.
// ============================================================
@DisplayName("GP-083 — cuenta desactivada, PATCH del perfil y cambio de correo")
class CuentaDesactivadaYCorreoTest extends AbstractOwnershipTest {

    private static final String PASSWORD = "Test1234";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private IRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @PersistenceContext
    private EntityManager em;

    // La petición sale con el token del usuario, como la mandaría la app. Accept explícito:
    // sin él, la API negocia XML (jackson-dataformat-xml está en el classpath).
    private MockHttpServletRequestBuilder como(Usuario usuario, MockHttpServletRequestBuilder peticion) {
        return peticion.header("Authorization", "Bearer " + jwtTokenProvider.generateToken(usuario))
                .accept(MediaType.APPLICATION_JSON);
    }

    // Desactiva la cuenta directamente en la base, sin pasar por la administración:
    // así el token y el refresh siguen intactos y solo puede pararlos el control nuevo.
    private void desactivarEnBase(Usuario usuario) {
        usuario.setActivo(false);
        usuarioRepository.saveAndFlush(usuario);
    }

    private String json(Object cuerpo) throws Exception {
        return objectMapper.writeValueAsString(cuerpo);
    }

    private Usuario releer(Usuario usuario) {
        em.flush();
        em.clear();
        return usuarioRepository.findById(usuario.getId()).orElseThrow();
    }

    @Nested
    @DisplayName("Cuenta desactivada")
    class CuentaDesactivada {

        @Test
        @DisplayName("su token recibe 401 en una ruta protegida, con el código de cuenta desactivada")
        void token_ruta_protegida_401() throws Exception {
            desactivarEnBase(owner);

            mockMvc.perform(como(owner, get("/usuarios/{id}", owner.getId())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.cause").value(CuentaDesactivadaException.CODIGO));
        }

        @Test
        @DisplayName("su token recibe 401 también en una ruta pública")
        void token_ruta_publica_401() throws Exception {
            desactivarEnBase(owner);

            mockMvc.perform(como(owner, get("/actuator/health")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("el mismo token funciona mientras la cuenta está activa (control del test)")
        void token_cuenta_activa_200() throws Exception {
            mockMvc.perform(como(owner, get("/usuarios/{id}", owner.getId())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("su refresh vigente recibe 401 y no emite tokens")
        void refresh_401() throws Exception {
            RefreshToken refresh = refreshTokenService.crear(owner);
            desactivarEnBase(owner);

            mockMvc.perform(post("/auth/refresh")
                            .accept(MediaType.APPLICATION_JSON)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("refreshToken", refresh.getToken()))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.cause").value(CuentaDesactivadaException.CODIGO));
        }

        @Test
        @DisplayName("desactivar desde administración revoca sus sesiones, aunque luego se reactive")
        void toggle_revoca_refresh() throws Exception {
            Usuario admin = crearUsuario("__gp083_admin__", RoleType.ADMIN);
            RefreshToken refresh = refreshTokenService.crear(owner);

            // Desactivar y volver a activar: si el refresh sigue sin servir con la cuenta
            // activa, lo que lo para es la revocación y no la comprobación de activo.
            mockMvc.perform(como(admin, patch("/admin/usuarios/{id}/toggle-activo", owner.getId())))
                    .andExpect(status().isOk());
            mockMvc.perform(como(admin, patch("/admin/usuarios/{id}/toggle-activo", owner.getId())))
                    .andExpect(status().isOk());

            em.flush();
            em.clear();
            assertThat(refreshTokenRepository.findByToken(refresh.getToken()).orElseThrow().isRevocado())
                    .as("el refresh de la cuenta desactivada tiene que quedar revocado")
                    .isTrue();

            mockMvc.perform(post("/auth/refresh")
                            .accept(MediaType.APPLICATION_JSON)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("refreshToken", refresh.getToken()))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PATCH /usuarios/{id}")
    class PatchPerfil {

        @Test
        @DisplayName("con activo = false no desactiva la cuenta")
        void activo_false_no_cambia() throws Exception {
            mockMvc.perform(como(owner, patch("/usuarios/{id}", owner.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("activo", false, "edad", 30))))
                    .andExpect(status().isOk());

            Usuario leido = releer(owner);
            assertThat(leido.getActivo()).isTrue();
            assertThat(leido.getEdad()).as("el resto del PATCH sí se aplica").isEqualTo(30);
        }

        @Test
        @DisplayName("una cuenta desactivada no se reactiva con activo = true")
        void desactivada_no_se_reactiva() throws Exception {
            desactivarEnBase(owner);

            mockMvc.perform(como(owner, patch("/usuarios/{id}", owner.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("activo", true))))
                    .andExpect(status().isUnauthorized());

            assertThat(releer(owner).getActivo()).isFalse();
        }

        @Test
        @DisplayName("con el mismo email que ya tiene responde 200, como mandan las builds repartidas")
        void mismo_email_200() throws Exception {
            mockMvc.perform(como(owner, patch("/usuarios/{id}", owner.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", owner.getEmail(), "edad", 31))))
                    .andExpect(status().isOk());

            assertThat(releer(owner).getEdad()).isEqualTo(31);
        }

        @Test
        @DisplayName("con el mismo email en otras mayúsculas responde 200 y no lo reescribe")
        void mismo_email_mayusculas_200() throws Exception {
            mockMvc.perform(como(owner, patch("/usuarios/{id}", owner.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", owner.getEmail().toUpperCase()))))
                    .andExpect(status().isOk());

            assertThat(releer(owner).getEmail()).isEqualTo(OWNER + "@test.local");
        }

        @Test
        @DisplayName("con un email distinto responde 400 y no cambia nada")
        void email_distinto_400() throws Exception {
            mockMvc.perform(como(owner, patch("/usuarios/{id}", owner.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", "otro@test.local", "edad", 44))))
                    .andExpect(status().isBadRequest());

            Usuario leido = releer(owner);
            assertThat(leido.getEmail()).isEqualTo(OWNER + "@test.local");
            assertThat(leido.getEdad()).as("un PATCH rechazado no aplica nada").isNull();
        }
    }

    @Nested
    @DisplayName("PUT /usuarios/me/email")
    class CambioCorreo {

        private MockHttpServletRequestBuilder cambio(Usuario usuario, String email, String password) throws Exception {
            return como(usuario, put("/usuarios/me/email"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("email", email, "password", password)));
        }

        @Test
        @DisplayName("con la contraseña correcta cambia el correo del usuario del token")
        void cambia_200() throws Exception {
            mockMvc.perform(cambio(owner, "nuevo-gp083@test.local", PASSWORD))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("nuevo-gp083@test.local"));

            assertThat(releer(owner).getEmail()).isEqualTo("nuevo-gp083@test.local");
            assertThat(releer(attacker).getEmail()).isEqualTo(ATTACKER + "@test.local");
        }

        @Test
        @DisplayName("con la contraseña incorrecta responde 403 y no cambia nada")
        void password_incorrecta_403() throws Exception {
            mockMvc.perform(cambio(owner, "nuevo-gp083@test.local", "NoEsLaMia1"))
                    .andExpect(status().isForbidden());

            assertThat(releer(owner).getEmail()).isEqualTo(OWNER + "@test.local");
        }

        @Test
        @DisplayName("sin formato de correo responde 400")
        void formato_invalido_400() throws Exception {
            mockMvc.perform(cambio(owner, "esto-no-es-un-correo", PASSWORD))
                    .andExpect(status().isBadRequest());

            assertThat(releer(owner).getEmail()).isEqualTo(OWNER + "@test.local");
        }

        @Test
        @DisplayName("con el correo de otra cuenta responde 409")
        void en_uso_409() throws Exception {
            mockMvc.perform(cambio(owner, attacker.getEmail(), PASSWORD))
                    .andExpect(status().isConflict());

            assertThat(releer(owner).getEmail()).isEqualTo(OWNER + "@test.local");
        }

        @Test
        @DisplayName("con el correo que ya tiene responde 200 sin cambios")
        void mismo_correo_200() throws Exception {
            mockMvc.perform(cambio(owner, owner.getEmail(), PASSWORD))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("el invitado no puede usarla (403)")
        void guest_403() throws Exception {
            mockMvc.perform(cambio(guest, "guest-gp083@test.local", PASSWORD))
                    .andExpect(status().isForbidden());

            assertThat(releer(guest).getEmail()).isEqualTo(GUEST + "@test.local");
        }

        @Test
        @DisplayName("sin token responde 401")
        void sin_token_401() throws Exception {
            mockMvc.perform(put("/usuarios/me/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", "x@test.local", "password", PASSWORD))))
                    .andExpect(status().isUnauthorized());
        }
    }
}
