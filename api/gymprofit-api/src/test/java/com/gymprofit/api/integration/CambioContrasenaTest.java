package com.gymprofit.api.integration;

import com.gymprofit.api.exceptions.ContrasenaActualIncorrectaException;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// CambioContrasenaTest — POST /auth/change-password con la contraseña actual mal
//
// Respondía 401, y la app trata todo 401 como sesión caducada: renueva el token y, si
// no puede, echa al usuario a Login. Equivocarse al teclear la contraseña actual no es
// perder la sesión. Ahora responde 403 con PASSWORD_ACTUAL_INCORRECTA en "cause", el
// mismo 403 que el borrado de cuenta (GP-008) cuando la contraseña no es la buena.
//
// Contexto entero con JWT real: el código lo pone el manejador global, no el servicio.
// ============================================================
@DisplayName("POST /auth/change-password — contraseña actual incorrecta")
class CambioContrasenaTest extends AbstractOwnershipTest {

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("→ 403 con el código en cause, y la contraseña no cambia")
    void actual_incorrecta_da_403_con_codigo() throws Exception {
        pedir(owner, "POST /auth/change-password",
                "{\"currentPassword\":\"NoEsLaSuya1\",\"newPassword\":\"tren de lavar\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.cause").value(ContrasenaActualIncorrectaException.CODIGO));

        String hash = usuarioRepository.findById(owner.getId()).orElseThrow().getPassword();
        assertThat(passwordEncoder.matches("Test1234", hash)).isTrue();
    }

    @Test
    @DisplayName("con la actual buena → 200")
    void actual_correcta_da_200() throws Exception {
        pedir(owner, "POST /auth/change-password",
                "{\"currentPassword\":\"Test1234\",\"newPassword\":\"tren de lavar\"}")
                .andExpect(status().isOk());
    }
}
