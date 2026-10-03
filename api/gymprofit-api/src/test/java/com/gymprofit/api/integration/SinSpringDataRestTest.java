package com.gymprofit.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// SinSpringDataRestTest — la API no publica repositorios (GP-186, DEC-046)
//
// La API se sirve solo con controladores propios, cada uno con sus reglas de acceso.
// Spring Data REST publicaría rutas por repositorio que no pasan por ningún servicio,
// así que no está en el classpath. Si alguien la vuelve a añadir, este test falla: ni
// la librería, ni su raíz que enumera recursos, ni su /profile.
// ============================================================
@DisplayName("GP-186 — Spring Data REST no está y la API no publica repositorios")
class SinSpringDataRestTest extends AbstractOwnershipTest {

    @Test
    @DisplayName("la librería no está en el classpath")
    void sin_libreria() {
        assertThat(ClassUtils.isPresent("org.springframework.data.rest.core.config.RepositoryRestConfiguration",
                getClass().getClassLoader())).isFalse();
    }

    @Test
    @DisplayName("la raíz y /profile dan 404 a un USER y a un invitado")
    void raiz_y_profile_404() throws Exception {
        List<String> malas = new ArrayList<>();
        for (String ruta : new String[]{"GET /", "GET /profile"}) {
            for (var quien : List.of(owner, guest)) {
                int estado = estado(quien, ruta);
                if (estado != 404) malas.add(ruta + " como " + quien.getUsername() + ": " + estado);
            }
        }
        assertThat(malas).isEmpty();
    }
}
