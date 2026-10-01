package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// AdminAlimentosTest — GP-085, el catálogo de alimentos en la web
//
// Lo que más importa: un alimento con dueño es la dieta de alguien y NO sale por
// /admin/alimentos, ni buscándolo por su nombre, ni con ningún filtro. Además: solo
// ADMIN, los filtros «sin inglés» y origen, la búsqueda por código de barras, y la
// edición por la ruta de siempre con los tres campos nuevos.
// ============================================================
@DisplayName("GP-085 — /admin/alimentos: solo el catálogo, filtros y edición")
class AdminAlimentosTest extends AbstractOwnershipTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private IAlimentoRepository alimentos;

    private Usuario admin;
    private Alimento escaneado;
    private Alimento aMano;
    private Alimento personal;

    @BeforeEach
    void sembrar() {
        admin = crearUsuario("__gp085_admin_al__", RoleType.ADMIN);
        escaneado = alimento("Zgp085 yogur natural", null, "8410000000017", null);
        aMano = alimento("Zgp085 arroz blanco cocido", "Zgp085 cooked white rice", null, null);
        personal = alimento("Zgp085 tortilla de la abuela", null, null, owner);
    }

    private Alimento alimento(String nombre, String nombreEn, String barcode, Usuario dueno) {
        Alimento a = new Alimento();
        a.setNombre(nombre);
        a.setNombreEn(nombreEn);
        a.setBarcode(barcode);
        a.setCategoria("Lácteos");
        a.setCalorias(61);
        a.setProteinas(new BigDecimal("3.5"));
        a.setActivo(true);
        a.setUsuario(dueno);
        return alimentos.saveAndFlush(a);
    }

    @Test
    @DisplayName("USER e invitado → 403 en la lista y el resumen")
    void solo_admin() throws Exception {
        for (Usuario quien : List.of(attacker, guest, owner)) {
            pedir(quien, "GET /admin/alimentos").andExpect(status().isForbidden());
            pedir(quien, "GET /admin/alimentos/resumen").andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("un alimento con dueño no sale nunca, ni buscándolo por su nombre")
    void nunca_los_personales() throws Exception {
        for (String consulta : List.of("?q=tortilla de la abuela", "?q=zgp085&size=100",
                "?q=zgp085&sinIngles=true&size=100", "?q=zgp085&origen=MANUAL&size=100",
                "?q=zgp085&categoria=Lácteos&size=100")) {
            pedir(admin, "GET /admin/alimentos" + consulta)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id", not(hasItem(personal.getId()))));
        }
    }

    @Test
    @DisplayName("filtra por «sin inglés» y por origen, y busca por código de barras")
    void filtros() throws Exception {
        pedir(admin, "GET /admin/alimentos?q=zgp085&sinIngles=true")
                .andExpect(jsonPath("$.content[*].id", hasItem(escaneado.getId())))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(aMano.getId()))));
        pedir(admin, "GET /admin/alimentos?q=zgp085&origen=OPEN_FOOD_FACTS")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].origen").value("OPEN_FOOD_FACTS"));
        pedir(admin, "GET /admin/alimentos?q=zgp085&origen=MANUAL")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].origen").value("MANUAL"));
        pedir(admin, "GET /admin/alimentos?q=8410000000017")
                .andExpect(jsonPath("$.content[0].id").value(escaneado.getId()));
        pedir(admin, "GET /admin/alimentos?q=cooked white")
                .andExpect(jsonPath("$.content[*].id", hasItem(aMano.getId())));
        pedir(admin, "GET /admin/alimentos?origen=SATELITE").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("el resumen cuenta solo el catálogo")
    void resumen() throws Exception {
        long catalogo = alimentos.countByUsuarioIsNull();
        pedir(admin, "GET /admin/alimentos/resumen")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.catalogo").value(catalogo))
                .andExpect(jsonPath("$.sinIngles").value(alimentos.contarCatalogoSinIngles()))
                .andExpect(jsonPath("$.categorias", hasItem("Lácteos")));
        assertThat(alimentos.count()).isGreaterThan(catalogo);
    }

    @Test
    @DisplayName("filtra por fuente: los básicos de Ciqual y de USDA, Open Food Facts y lo hecho a mano (GP-127)")
    void filtro_fuente() throws Exception {
        pedir(admin, "GET /admin/alimentos?fuente=CIQUAL&size=100")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].fuente", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("CIQUAL"))))
                .andExpect(jsonPath("$.content[*].revisado", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(true))))
                .andExpect(jsonPath("$.totalElements", org.hamcrest.Matchers.greaterThan(300)));
        pedir(admin, "GET /admin/alimentos?fuente=usda&size=100")
                .andExpect(jsonPath("$.content[*].fuente", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("USDA"))));
        pedir(admin, "GET /admin/alimentos?q=zgp085&fuente=MANUAL")
                .andExpect(jsonPath("$.content[*].fuente", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.nullValue())));
        pedir(admin, "GET /admin/alimentos?fuente=SATELITE").andExpect(status().isBadRequest());
        pedir(admin, "GET /admin/alimentos/resumen")
                .andExpect(jsonPath("$.porFuente.CIQUAL", org.hamcrest.Matchers.greaterThan(300)))
                .andExpect(jsonPath("$.porFuente.USDA", org.hamcrest.Matchers.greaterThan(5)))
                .andExpect(jsonPath("$.productos").isNumber());
    }

    @Test
    @DisplayName("la ruta de siempre edita el nombre en inglés, la marca y el código de barras")
    void editar() throws Exception {
        pedir(admin, "PATCH /alimentos/" + escaneado.getId(),
                "{\"nombreEn\":\"Plain yogurt\",\"marca\":\"Hacendado\",\"barcode\":\"8410000000024\"}")
                .andExpect(status().isOk());
        em.flush();
        em.clear();
        Alimento a = alimentos.findById(escaneado.getId()).orElseThrow();
        assertThat(a.getNombreEn()).isEqualTo("Plain yogurt");
        assertThat(a.getMarca()).isEqualTo("Hacendado");
        assertThat(a.getBarcode()).isEqualTo("8410000000024");
        assertThat(a.getNombre()).isEqualTo("Zgp085 yogur natural");

        pedir(admin, "PATCH /alimentos/" + escaneado.getId(), "{\"marca\":\"  \"}").andExpect(status().isOk());
        em.flush();
        em.clear();
        assertThat(alimentos.findById(escaneado.getId()).orElseThrow().getMarca()).isNull();
    }

    @Test
    @DisplayName("un código de barras que ya es de otro alimento → 409")
    void barcode_repetido() throws Exception {
        pedir(admin, "PATCH /alimentos/" + aMano.getId(), "{\"barcode\":\"8410000000017\"}")
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("un USER no edita el catálogo con los campos nuevos")
    void user_no_edita_catalogo() throws Exception {
        pedir(attacker, "PATCH /alimentos/" + escaneado.getId(), "{\"nombreEn\":\"Hacked\"}")
                .andExpect(status().isForbidden());
    }
}
