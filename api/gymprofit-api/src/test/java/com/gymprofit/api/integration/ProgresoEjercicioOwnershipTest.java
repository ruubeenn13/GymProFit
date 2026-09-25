package com.gymprofit.api.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// ProgresoEjercicioOwnershipTest — 403 IDOR sobre las rutas heredadas de /progreso-ejercicios.
//
// Desde GP-088 los récords se calculan de las series y la tabla progreso_ejercicios
// ya no se lee ni se escribe. Quedan dos rutas, para las builds ya repartidas, y las
// dos llevan el id del USUARIO en la ruta: el dueño del id es el propio id, así que
// el criterio es 403 (DEC-027) y se compara contra el token sin tocar la base. Las
// rutas nuevas, sin id de usuario, tienen su test de aislamiento en RecordsTest.
// ============================================================
@DisplayName("IDOR /progreso-ejercicios (rutas heredadas) — un usuario no accede al progreso de otro")
class ProgresoEjercicioOwnershipTest extends AbstractOwnershipTest {

    private static final String[] RUTAS = {
            "GET /progreso-ejercicios/usuario/{owner}/ejercicio/{ejercicio}/historial",
            "GET /progreso-ejercicios/usuario/{owner}/record-destacado"
    };

    // Ejercicio del catálogo (CI no lo trae por Flyway).
    private Integer ejercicio;

    @BeforeEach
    void catalogo() {
        ejercicio = crearEjercicioCatalogo().getId();
    }

    private String ruta(String plantilla, Integer usuarioId) {
        return rellenar(plantilla, Map.of("owner", usuarioId, "ejercicio", ejercicio));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "GET /progreso-ejercicios/usuario/{owner}/ejercicio/{ejercicio}/historial",
            "GET /progreso-ejercicios/usuario/{owner}/record-destacado"
    })
    @DisplayName("Id de otro usuario → 403 al atacante; el dueño no")
    void idAjeno(String plantilla) throws Exception {
        String ruta = ruta(plantilla, owner.getId());
        assertThat(estado(attacker, ruta)).as("atacante en " + ruta).isEqualTo(403);
        assertThat(estado(owner, ruta)).as("dueño en " + ruta).isNotEqualTo(403);
    }

    @Test
    @DisplayName("GUEST pidiendo SU propio progreso → 403 (regla de rol)")
    void guestConSuPropioId() throws Exception {
        for (String plantilla : RUTAS) {
            String ruta = ruta(plantilla, guest.getId());
            assertThat(estado(guest, ruta)).as("invitado en " + ruta).isEqualTo(403);
        }
    }

    @Test
    @DisplayName("El CRUD de la tabla retirada ya no responde")
    void crudRetirado() throws Exception {
        // Si volviera a mapearse una ruta sobre la tabla, este test lo dice. Una ruta que
        // no existe da hoy 500 y no 404 (el manejador global no distingue ese caso): lo
        // que se afirma es que ya no sirve nada.
        for (String ruta : new String[]{
                "GET /progreso-ejercicios",
                "GET /progreso-ejercicios/usuario/" + owner.getId(),
                "DELETE /progreso-ejercicios/usuario/" + owner.getId()}) {
            assertThat(estado(owner, ruta)).as(ruta).isGreaterThanOrEqualTo(400);
        }
    }
}
