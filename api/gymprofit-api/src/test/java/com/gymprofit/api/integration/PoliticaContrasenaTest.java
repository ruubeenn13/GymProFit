package com.gymprofit.api.integration;

import com.gymprofit.api.entity.PasswordResetCodigo;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.exceptions.ContrasenaRechazadaException;
import com.gymprofit.api.repository.jpa.IPasswordResetCodigoRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// PoliticaContrasenaTest — GP-101: contraseñas según NIST SP 800-63B-4 (DEC-034).
//
// Sustituye a la regla de mezcla de GP-095. Mínimo 8 caracteres contados como
// caracteres, máximo 72 bytes en UTF-8 (BCrypt), sin reglas de composición, y una lista
// de bloqueo dentro de la API más el nombre del servicio y el del usuario. Solo al
// poner una contraseña nueva: alta, recuperar y cambiar. Entrar no la comprueba.
//
// Contexto entero: lo que se prueba es la cadena real, del @Valid del DTO al servicio
// y al código en "cause" del manejador global. Todo se revierte al acabar cada test.
// ============================================================
@DisplayName("GP-101 — contraseñas nuevas según NIST, con mínimo de 8")
class PoliticaContrasenaTest extends AbstractOwnershipTest {

    private static final String USUARIO = "libre-gp101";

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private IPasswordResetCodigoRepository codigoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ResultActions registrar(String password) throws Exception {
        String cuerpo = objectMapper.writeValueAsString(Map.of(
                "username", USUARIO,
                "email", USUARIO + "@test.local",
                "password", password));
        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private static String repetir(String s, int veces) {
        return s.repeat(veces);
    }

    @Nested
    @DisplayName("Alta")
    class Alta {

        @Test
        @DisplayName("7 caracteres → 400 que dice el mínimo, sin código")
        void siete_caracteres() throws Exception {
            registrar("zqxmplo")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("8 caracteres")))
                    .andExpect(jsonPath("$.cause").value(nullValue()));
        }

        @Test
        @DisplayName("8 minúsculas, sin mayúsculas, números ni símbolos → 201")
        void ocho_minusculas() throws Exception {
            registrar("zqxmplok").andExpect(status().isCreated());
        }

        @Test
        @DisplayName("una frase con espacios → 201")
        void frase_con_espacios() throws Exception {
            registrar("tren de lavar").andExpect(status().isCreated());
        }

        @Test
        @DisplayName("se cuentan caracteres, no unidades UTF-16: 7 emojis (14 unidades) → 400")
        void siete_emojis() throws Exception {
            registrar(repetir("🏋", 7))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("8 caracteres")));
        }

        @Test
        @DisplayName("Unicode: 8 caracteres con eñes → 201")
        void ocho_con_enes() throws Exception {
            registrar("ñañañaña").andExpect(status().isCreated());
        }

        @Test
        @DisplayName("exactamente 72 bytes (24 «€») → 201")
        void setenta_y_dos_bytes() throws Exception {
            registrar(repetir("€", 24)).andExpect(status().isCreated());
        }

        @Test
        @DisplayName("más de 72 bytes → 400 de longitud, nunca el genérico")
        void mas_de_72_bytes() throws Exception {
            registrar(repetir("€", 25))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("demasiado larga")))
                    .andExpect(jsonPath("$.message", not(containsString("Parámetro con valor inválido"))));
            registrar(repetir("a", 73))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("demasiado larga")));
        }

        @Test
        @DisplayName("un carácter de control → 400")
        void caracter_de_control() throws Exception {
            registrar("abcd\u0007efgh").andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("de la lista (password1, también en mayúsculas) → 400 PASSWORD_COMUN")
        void de_la_lista() throws Exception {
            registrar("password1")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.cause").value(ContrasenaRechazadaException.PASSWORD_COMUN));
            registrar("PASSWORD1")
                    .andExpect(jsonPath("$.cause").value(ContrasenaRechazadaException.PASSWORD_COMUN));
        }

        @Test
        @DisplayName("con el nombre del servicio dentro → 400 PASSWORD_CONTIENE_NOMBRE")
        void con_el_servicio() throws Exception {
            registrar("miGymProFit2026")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.cause").value(ContrasenaRechazadaException.PASSWORD_CONTIENE_NOMBRE));
        }

        @Test
        @DisplayName("con el nombre de usuario dentro, sin distinguir mayúsculas → 400 PASSWORD_CONTIENE_NOMBRE")
        void con_el_usuario() throws Exception {
            registrar("xx" + USUARIO.toUpperCase() + "yy")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.cause").value(ContrasenaRechazadaException.PASSWORD_CONTIENE_NOMBRE));
        }
    }

    @Nested
    @DisplayName("Cambiar y recuperar")
    class CambiarYRecuperar {

        @Test
        @DisplayName("cambiar a una de la lista → 400 PASSWORD_COMUN; a una válida → 200")
        void cambiar() throws Exception {
            pedir(owner, "POST /auth/change-password",
                    "{\"currentPassword\":\"Test1234\",\"newPassword\":\"password1\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.cause").value(ContrasenaRechazadaException.PASSWORD_COMUN));
            pedir(owner, "POST /auth/change-password",
                    "{\"currentPassword\":\"Test1234\",\"newPassword\":\"corta\"}")
                    .andExpect(status().isBadRequest());
            pedir(owner, "POST /auth/change-password",
                    "{\"currentPassword\":\"Test1234\",\"newPassword\":\"tren de lavar\"}")
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("recuperar con una de la lista → 400 PASSWORD_COMUN y el código NO se gasta")
        void recuperar() throws Exception {
            PasswordResetCodigo registro = new PasswordResetCodigo();
            registro.setUsuario(owner);
            registro.setCodigoHash(passwordEncoder.encode("123456"));
            registro.setFechaCreacion(LocalDateTime.now());
            registro.setFechaExpiracion(LocalDateTime.now().plusMinutes(15));
            registro = codigoRepository.save(registro);

            String comun = objectMapper.writeValueAsString(Map.of(
                    "identificador", owner.getUsername(), "codigo", "123456", "newPassword", "password1"));
            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON).content(comun))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.cause").value(ContrasenaRechazadaException.PASSWORD_COMUN));

            PasswordResetCodigo despues = codigoRepository.findById(registro.getId()).orElseThrow();
            assertThat(despues.isUsado()).isFalse();
            assertThat(despues.getIntentos()).isZero();

            String valida = objectMapper.writeValueAsString(Map.of(
                    "identificador", owner.getUsername(), "codigo", "123456", "newPassword", "tren de lavar"));
            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON).content(valida))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("una cuenta con contraseña de la regla vieja (o de ninguna) sigue entrando")
    void cuenta_existente_sigue_entrando() throws Exception {
        Usuario vieja = crearUsuario("vieja-gp101", RoleType.USER);
        vieja.setPassword(passwordEncoder.encode("abc"));
        usuarioRepository.save(vieja);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"vieja-gp101\",\"password\":\"abc\"}"))
                .andExpect(status().isOk());
    }
}
