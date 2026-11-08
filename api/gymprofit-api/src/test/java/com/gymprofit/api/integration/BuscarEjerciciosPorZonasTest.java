package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.enums.Dificultad;
import com.gymprofit.api.enums.GrupoMuscular;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// BuscarEjerciciosPorZonasTest — el buscador del catálogo con varias zonas (GP-244)
//
// Crear una rutina elige una o varias zonas y el paso 2 busca en todas a la vez:
// GET /ejercicios/buscar acepta grupoMuscular repetido (o separado por comas), y con
// uno solo responde igual que antes. Contexto completo, como pide DEC-014: el id que
// viaja es de catálogo público, así que lo que se comprueba es el aislamiento (DEC-027):
// la respuesta no depende de quién pregunta y no lleva nada del otro usuario.
// ============================================================
@DisplayName("GP-244 — buscar ejercicios en varias zonas")
class BuscarEjerciciosPorZonasTest extends AbstractOwnershipTest {

    // Prefijo propio para que el texto deje fuera el resto del catálogo sembrado.
    private static final String Q = "zzlote11";

    @Autowired
    private IEjercicioRepository repo;

    @BeforeEach
    void sembrar() {
        crear(Q + " pecho", GrupoMuscular.PECHO, true);
        crear(Q + " hombros", GrupoMuscular.HOMBROS, true);
        crear(Q + " pierna", GrupoMuscular.PIERNAS, true);
        crear(Q + " pecho apagado", GrupoMuscular.PECHO, false);
    }

    private void crear(String nombre, GrupoMuscular grupo, boolean activo) {
        Ejercicio e = new Ejercicio();
        e.setNombre(nombre);
        e.setGrupoMuscular(grupo);
        e.setDificultad(Dificultad.values()[0]);
        e.setCaloriasQuemadas(0);
        e.setActivo(activo);
        repo.save(e);
    }

    @Test
    @DisplayName("con una zona, como siempre: solo esa y solo los activos")
    void una_zona_como_hoy() throws Exception {
        pedir(owner, "GET /ejercicios/buscar?q=" + Q + "&grupoMuscular=PECHO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nombre").value(containsInAnyOrder(Q + " pecho")));
    }

    @Test
    @DisplayName("con dos zonas repetidas en la consulta, las dos")
    void dos_zonas_repetidas() throws Exception {
        pedir(owner, "GET /ejercicios/buscar?q=" + Q + "&grupoMuscular=PECHO&grupoMuscular=HOMBROS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nombre").value(containsInAnyOrder(Q + " pecho", Q + " hombros")));
    }

    @Test
    @DisplayName("con dos zonas separadas por comas, las dos")
    void dos_zonas_con_comas() throws Exception {
        pedir(owner, "GET /ejercicios/buscar?q=" + Q + "&grupoMuscular=pecho,HOMBROS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nombre").value(containsInAnyOrder(Q + " pecho", Q + " hombros")));
    }

    @Test
    @DisplayName("sin zona, todas; una zona que no existe entre varias, 400")
    void sin_zona_y_zona_invalida() throws Exception {
        pedir(owner, "GET /ejercicios/buscar?q=" + Q)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nombre")
                        .value(containsInAnyOrder(Q + " pecho", Q + " hombros", Q + " pierna")));
        pedir(owner, "GET /ejercicios/buscar?q=" + Q + "&grupoMuscular=PECHO&grupoMuscular=CUELLO")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("aislamiento (DEC-027): otro usuario y el invitado reciben lo mismo, sin nada de nadie")
    void aislamiento_del_catalogo() throws Exception {
        String ruta = "GET /ejercicios/buscar?q=" + Q + "&grupoMuscular=PECHO&grupoMuscular=HOMBROS";
        String delDueno = pedir(owner, ruta).andReturn().getResponse().getContentAsString();
        String delOtro = pedir(attacker, ruta).andReturn().getResponse().getContentAsString();
        String delInvitado = pedir(guest, ruta).andReturn().getResponse().getContentAsString();
        assertThat(delOtro).isEqualTo(delDueno);
        assertThat(delInvitado).isEqualTo(delDueno);
        assertThat(delDueno).doesNotContain(OWNER).doesNotContain(ATTACKER).doesNotContain("@test.local");
    }
}
