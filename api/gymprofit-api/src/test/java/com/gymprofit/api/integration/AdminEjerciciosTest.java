package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.Equipamiento;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// AdminEjerciciosTest — GP-085, el catálogo de ejercicios en la web
//
// Un ejercicio es catálogo público (DEC-027): no hay id ajeno. Lo que se protege es la
// escritura y la vista de administración, que solo son de ADMIN: USER e invitado, 403.
// Lo demás: la búsqueda en los dos idiomas, los filtros, «sin revisar», en cuántas
// rutinas se usa, y la regla de guardado —un nombre en español distinto del inglés
// cuenta como revisado; igual, solo si se marca—.
// ============================================================
@DisplayName("GP-085 — /admin/ejercicios: lista, editor y revisión del nombre")
class AdminEjerciciosTest extends AbstractOwnershipTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private IEjercicioRepository ejercicios;

    private Usuario admin;
    private Ejercicio sinTraducir;

    @BeforeEach
    void sembrar() {
        admin = crearUsuario("__gp085_admin_ej__", RoleType.ADMIN);
        sinTraducir = crearEjercicioCatalogo();
        sinTraducir.setNombre("Zgp085 air bike");
        sinTraducir.setNombreEn("Zgp085 air bike");
        sinTraducir.setEquipamiento(Equipamiento.MAQUINA);
        sinTraducir = ejercicios.saveAndFlush(sinTraducir);
    }

    private String cuerpo(String nombre, String nombreEn, boolean revisado) {
        return """
                {"nombre":"%s","nombreEn":"%s","grupoMuscular":"CARDIO","equipamiento":"MAQUINA",
                 "dificultad":"PRINCIPIANTE","activo":true,"nombreRevisado":%s,
                 "descripcionEn":"Pedal while pushing and pulling the handles."}""".formatted(nombre, nombreEn, revisado);
    }

    private Ejercicio releer() {
        em.flush();
        em.clear();
        return ejercicios.findById(sinTraducir.getId()).orElseThrow();
    }

    @Test
    @DisplayName("USER e invitado → 403 en lista, resumen, editor y guardado")
    void solo_admin() throws Exception {
        for (Usuario quien : List.of(attacker, guest)) {
            pedir(quien, "GET /admin/ejercicios").andExpect(status().isForbidden());
            pedir(quien, "GET /admin/ejercicios/resumen").andExpect(status().isForbidden());
            pedir(quien, "GET /admin/ejercicios/" + sinTraducir.getId()).andExpect(status().isForbidden());
            pedir(quien, "PUT /admin/ejercicios/" + sinTraducir.getId(), cuerpo("Pirateado", "x", true))
                    .andExpect(status().isForbidden());
        }
        assertThat(releer().getNombre()).isEqualTo("Zgp085 air bike");
    }

    @Test
    @DisplayName("busca en los dos idiomas y filtra por grupo, equipamiento y sin revisar")
    void listar() throws Exception {
        sinTraducir.setNombre("Zgp085 bicicleta de aire");
        ejercicios.saveAndFlush(sinTraducir);

        pedir(admin, "GET /admin/ejercicios?q=zgp085 AIR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(sinTraducir.getId()))
                .andExpect(jsonPath("$.content[0].nombreEn").value("Zgp085 air bike"))
                .andExpect(jsonPath("$.totalElements").value(1));
        pedir(admin, "GET /admin/ejercicios?q=bicicleta de aire&equipamiento=maquina")
                .andExpect(jsonPath("$.content[*].id", hasItem(sinTraducir.getId())));
        pedir(admin, "GET /admin/ejercicios?q=zgp085&equipamiento=BARRA")
                .andExpect(jsonPath("$.totalElements").value(0));
        pedir(admin, "GET /admin/ejercicios?sinRevisar=true&size=100&q=zgp085")
                .andExpect(jsonPath("$.content[*].nombreRevisado", everyItem(org.hamcrest.Matchers.is(false))))
                .andExpect(jsonPath("$.content[*].id", hasItem(sinTraducir.getId())));
        pedir(admin, "GET /admin/ejercicios?equipamiento=TRAMPOLIN").andExpect(status().isBadRequest());
        pedir(admin, "GET /admin/ejercicios?grupo=CUELLO").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("el resumen cuenta activos y sin revisar, y trae las ocho opciones de equipamiento")
    void resumen() throws Exception {
        long sinRevisarAntes = ejercicios.countByActivoTrueAndNombreRevisadoFalse();
        pedir(admin, "GET /admin/ejercicios/resumen")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sinRevisar").value(sinRevisarAntes))
                .andExpect(jsonPath("$.equipamientos.length()").value(8))
                .andExpect(jsonPath("$.equipamientos[*].etiqueta", hasItem("Peso corporal")))
                .andExpect(jsonPath("$.equipamientos[*].etiquetaEn", hasItem("Bodyweight")));
    }

    @Test
    @DisplayName("el editor dice en cuántas rutinas se usa, contando cada rutina una vez")
    void detalle_con_rutinas() throws Exception {
        em.createNativeQuery("INSERT INTO rutinas (nombre, nivel, usuario_id) VALUES ('R gp085', 'PRINCIPIANTE', %d)"
                .formatted(owner.getId())).executeUpdate();
        Integer rutina = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).intValue();
        for (int i = 0; i < 2; i++) {
            em.createNativeQuery("INSERT INTO rutina_ejercicio (rutina_id, ejercicio_id) VALUES (%d, %d)"
                    .formatted(rutina, sinTraducir.getId())).executeUpdate();
        }
        pedir(admin, "GET /admin/ejercicios/" + sinTraducir.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rutinas").value(1))
                .andExpect(jsonPath("$.equipamiento").value("MAQUINA"))
                .andExpect(jsonPath("$.origen").value("MANUAL"))
                .andExpect(jsonPath("$.nombreRevisado").value(false));
        pedir(admin, "GET /admin/ejercicios/999999999").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("guardar un nombre en español distinto del inglés lo marca como revisado")
    void traducir_lo_revisa() throws Exception {
        pedir(admin, "PUT /admin/ejercicios/" + sinTraducir.getId(),
                cuerpo("Bicicleta de aire", "Air bike", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreRevisado").value(true));
        Ejercicio e = releer();
        assertThat(e.getNombre()).isEqualTo("Bicicleta de aire");
        assertThat(e.getNombreEn()).isEqualTo("Air bike");
        assertThat(e.getDescripcionEn()).startsWith("Pedal while");
        assertThat(e.getNombreRevisado()).isTrue();
    }

    @Test
    @DisplayName("mismo nombre: sin marcar sigue sin revisar; con «se dice igual» queda revisado")
    void se_dice_igual() throws Exception {
        pedir(admin, "PUT /admin/ejercicios/" + sinTraducir.getId(), cuerpo("Hip thrust", "Hip thrust", false))
                .andExpect(jsonPath("$.nombreRevisado").value(false));
        pedir(admin, "PUT /admin/ejercicios/" + sinTraducir.getId(), cuerpo("Hip thrust", "Hip thrust", true))
                .andExpect(jsonPath("$.nombreRevisado").value(true));
        assertThat(releer().getNombreRevisado()).isTrue();
    }

    @Test
    @DisplayName("sin nombre o con un equipamiento que no existe → 400 y no se guarda")
    void guardar_invalido() throws Exception {
        pedir(admin, "PUT /admin/ejercicios/" + sinTraducir.getId(), cuerpo("", "Air bike", true))
                .andExpect(status().isBadRequest());
        pedir(admin, "PUT /admin/ejercicios/" + sinTraducir.getId(),
                cuerpo("Bici", "Air bike", true).replace("\"MAQUINA\"", "\"TRAMPOLIN\""))
                .andExpect(status().isBadRequest());
        assertThat(releer().getNombre()).isEqualTo("Zgp085 air bike");
        pedir(admin, "GET /admin/ejercicios?q=" + sinTraducir.getNombre())
                .andExpect(jsonPath("$.content[*].nombre", not(hasItem("Bici"))));
    }
}
