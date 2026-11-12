package com.gymprofit.api.integration;

import io.sentry.Sentry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// SentryConDsnTest — con DSN, un 500 llega a Sentry sin token ni datos personales, y un
// 4xx no llega (GP-221).
//
// El DSN apunta a ServidorSentryFalso, que guarda lo que manda el SDK. La petición lleva lo
// peor que podría escaparse: el JWT en Authorization, y correo y contraseña en el cuerpo; y
// la excepción repite el correo en su mensaje. Nada de eso puede estar en lo que llega.
//
// Sentry es estático: al acabar se cierra y el contexto se tira, para que ningún otro test
// de la JVM herede un SDK encendido.
// ============================================================
@DisplayName("Sentry con DSN — los 500 llegan limpios, los 4xx no llegan")
@Import(SentryConDsnTest.ErroresDePrueba.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SentryConDsnTest extends AbstractOwnershipTest {

    static final String CORREO = "persona.real@ejemplo.com";
    static final String CONTRASENA = "Secreta1234.";

    @DynamicPropertySource
    static void dsnDePrueba(DynamicPropertyRegistry registro) {
        registro.add("sentry.dsn", ServidorSentryFalso::dsn);
    }

    @AfterAll
    static void apagarSentry() {
        Sentry.close();
    }

    @BeforeEach
    void vaciar() {
        Sentry.flush(5000);
        ServidorSentryFalso.vaciar();
    }

    @Test
    @DisplayName("un 500 llega como aviso, sin el token, sin el cuerpo y sin el correo")
    void un_500_llega_sin_datos_personales() throws Exception {
        String jwt = token(owner);

        mockMvc.perform(post("/prueba-sentry/500")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + CORREO + "\",\"password\":\"" + CONTRASENA + "\"}"))
                .andExpect(status().isInternalServerError());
        Sentry.flush(5000);

        List<String> llegado = ServidorSentryFalso.recibidos();
        assertThat(llegado).as("el 500 tenía que llegar a Sentry").isNotEmpty();
        String todo = String.join("\n", llegado);
        assertThat(todo).contains("FalloDePrueba").contains("/prueba-sentry/500");
        assertThat(todo)
                .doesNotContain(jwt)
                .doesNotContain(jwt.substring(0, 24))
                .doesNotContain("\"headers\"")
                .doesNotContain("Bearer ")
                .doesNotContain(CORREO)
                .doesNotContain(CONTRASENA)
                .doesNotContain(OWNER)
                .doesNotContain("\"ip_address\"")
                .doesNotContain("\"cookies\"");
    }

    @Test
    @DisplayName("un 4xx no llega")
    void un_4xx_no_llega() throws Exception {
        mockMvc.perform(post("/prueba-sentry/404")
                        .header("Authorization", "Bearer " + token(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
        Sentry.flush(5000);

        assertThat(ServidorSentryFalso.recibidos()).isEmpty();
    }

    /** Excepción con nombre propio, para reconocerla en lo que llega. */
    static class FalloDePrueba extends IllegalStateException {
        FalloDePrueba(String mensaje) {
            super(mensaje);
        }
    }

    /** Rutas que fallan a propósito. Anidada en un test: el escaneo de los tests no la recoge. */
    @RestController
    static class ErroresDePrueba {
        @PostMapping("/prueba-sentry/500")
        public void quinientos(@RequestBody Map<String, Object> cuerpo) {
            throw new FalloDePrueba("No se pudo guardar a " + cuerpo.get("email"));
        }

        @PostMapping("/prueba-sentry/404")
        public void cuatrocientosCuatro() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
