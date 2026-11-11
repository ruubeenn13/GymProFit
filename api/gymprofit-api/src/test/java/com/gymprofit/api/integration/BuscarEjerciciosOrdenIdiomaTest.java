package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.enums.Dificultad;
import com.gymprofit.api.enums.GrupoMuscular;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// BuscarEjerciciosOrdenIdiomaTest — el buscador del catálogo ordena por el nombre que
// enseña (GP-246, lote 12)
//
// El paso 2 de crear una rutina lista lo que da GET /ejercicios/buscar tal cual llega.
// En inglés salía en el orden del nombre en español, porque la consulta ordenaba por
// `nombre`. Ahora ordena por el nombre en el idioma de la petición: con Accept-Language
// en, el inglés (y el español si un ejercicio no lo tiene, como el mapeador); sin
// cabecera o en español, el español. El contrato no cambia: mismos campos y filtros.
// Contexto completo (DEC-014); el catálogo es público, así que el invitado ve lo mismo.
// ============================================================
@DisplayName("GP-246 — /ejercicios/buscar ordena por el nombre en el idioma de la petición")
class BuscarEjerciciosOrdenIdiomaTest extends AbstractOwnershipTest {

    // Prefijo propio para que el texto deje fuera el resto del catálogo sembrado.
    private static final String Q = "zzlote12";

    @Autowired
    private IEjercicioRepository repo;

    @BeforeEach
    void sembrar() {
        // En español: a, b, c. En inglés: z, y y, sin traducción, c (que cae en el español).
        crear(Q + " a", Q + " z");
        crear(Q + " b", Q + " y");
        crear(Q + " c", null);
    }

    private void crear(String nombre, String nombreEn) {
        Ejercicio e = new Ejercicio();
        e.setNombre(nombre);
        e.setNombreEn(nombreEn);
        e.setGrupoMuscular(GrupoMuscular.PECHO);
        e.setDificultad(Dificultad.values()[0]);
        e.setCaloriasQuemadas(0);
        e.setActivo(true);
        repo.save(e);
    }

    private MockHttpServletRequestBuilder buscar(String idioma) {
        MockHttpServletRequestBuilder p = MockMvcRequestBuilders.get("/ejercicios/buscar")
                .param("q", Q)
                .header("Authorization", "Bearer " + token(owner))
                .accept(MediaType.APPLICATION_JSON);
        if (idioma != null) p.header("Accept-Language", idioma);
        return p;
    }

    @Test
    @DisplayName("en inglés, por el nombre en inglés (y el español si no lo tiene)")
    void en_ingles_orden_ingles() throws Exception {
        mockMvc.perform(buscar("en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nombre").value(contains(Q + " c", Q + " y", Q + " z")));
    }

    @Test
    @DisplayName("en español o sin cabecera, por el nombre en español, como antes")
    void en_espanol_orden_espanol() throws Exception {
        mockMvc.perform(buscar("es"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nombre").value(contains(Q + " a", Q + " b", Q + " c")));
        mockMvc.perform(buscar(null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nombre").value(contains(Q + " a", Q + " b", Q + " c")));
    }

    @Test
    @DisplayName("el orden en inglés vale también entre páginas")
    void en_ingles_paginado() throws Exception {
        mockMvc.perform(buscar("en").param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nombre").value(contains(Q + " z")))
                .andExpect(jsonPath("$.totalElements").value(3));
    }
}
