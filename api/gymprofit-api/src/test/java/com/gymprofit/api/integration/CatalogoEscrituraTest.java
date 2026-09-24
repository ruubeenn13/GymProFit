package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// CatalogoEscrituraTest — el catálogo de ejercicios y logros solo lo escribe ADMIN (GP-048)
//
// FALLO REAL: PATCH /ejercicios/{id} no tenía regla en SecurityConfig. Caía en
// anyRequest().authenticated(), y EjercicioService.patch no comprueba nada, así que
// cualquier token —también el del invitado, que se pide sin credenciales— renombraba
// o desactivaba cualquier ejercicio del catálogo. El resto de verbos sí tenía su regla;
// faltaba el test que impidiera perderla.
// ============================================================
@DisplayName("Catálogo — ejercicios y logros solo los escribe ADMIN (GP-048)")
class CatalogoEscrituraTest extends AbstractOwnershipTest {

    private static final String NOMBRE = "Ejercicio IDOR test";

    @Autowired
    private IEjercicioRepository ejercicioRepository;

    private Usuario admin;
    private Map<String, Object> ids;

    @BeforeEach
    void sembrar() {
        admin = crearUsuario("__idor_admin__", RoleType.ADMIN);
        ids = Map.of("ejercicio", crearEjercicioCatalogo().getId());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "PATCH /ejercicios/{ejercicio}",
            "PUT /ejercicios/{ejercicio}/activar",
            "DELETE /ejercicios/{ejercicio}",
            "DELETE /ejercicios/{ejercicio}/permanente",
            "PUT /ejercicios",
            "POST /ejercicios",
            "PUT /logros/1",
            "POST /logros"
    })
    @DisplayName("escribir el catálogo → 403 a USER y a GUEST; ADMIN no")
    void soloAdmin(String plantilla) throws Exception {
        String ruta = rellenar(plantilla, ids);
        assertThat(estado(attacker, ruta)).as("USER en " + ruta).isEqualTo(403);
        assertThat(estado(guest, ruta)).as("GUEST en " + ruta).isEqualTo(403);
        assertThat(estado(admin, ruta)).as("ADMIN en " + ruta).isNotEqualTo(403);
    }

    @Test
    @DisplayName("un PATCH de invitado no cambia el ejercicio")
    void patchDeInvitado_noCambia() throws Exception {
        Integer id = (Integer) ids.get("ejercicio");
        pedir(guest, "PATCH /ejercicios/" + id, "{\"nombre\":\"pisado\",\"activo\":false}");

        Ejercicio leido = ejercicioRepository.findById(id).orElseThrow();
        assertThat(leido.getNombre()).isEqualTo(NOMBRE);
        assertThat(leido.getActivo()).isTrue();
    }
}
