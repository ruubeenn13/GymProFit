package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// BusquedaAdminCatalogoTest — GP-195: la búsqueda de alimentos del panel es del catálogo
// GET /admin/alimentos/busqueda trae solo los alimentos sin dueño, activos o no, como
// el listado del catálogo de la web de administración (buscarCatalogoAdmin).
// Con el contexto levantado y JWT de verdad.
// ============================================================
@DisplayName("GP-195 — la búsqueda de alimentos del panel es del catálogo")
class BusquedaAdminCatalogoTest extends AbstractOwnershipTest {

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    @Test
    @DisplayName("solo el catálogo, activo o no")
    void solo_catalogo() throws Exception {
        Alimento activo = alimento("Kzpanel catálogo activo", null, true, null);
        Alimento inactivo = alimento("Kzpanel catálogo inactivo", null, false, null);
        Alimento propio = alimento("Kzpanel propio", owner, true, null);
        Alimento plato = alimento("Kzpanel plato", owner, true, 4);
        em.flush();
        em.clear();

        Usuario admin = crearUsuario("__panel_admin__", RoleType.ADMIN);
        List<Integer> todos = ids(admin, "/admin/alimentos/busqueda?nombre=kzpanel");
        assertThat(todos).contains(activo.getId(), inactivo.getId())
                .doesNotContain(propio.getId(), plato.getId());
        assertThat(ids(admin, "/admin/alimentos/busqueda?nombre=kzpanel&activo=true"))
                .contains(activo.getId()).doesNotContain(inactivo.getId(), propio.getId(), plato.getId());
        assertThat(ids(admin, "/admin/alimentos/busqueda?nombre=kzpanel&activo=false"))
                .containsExactly(inactivo.getId());
        // Sin filtros, tampoco.
        assertThat(ids(admin, "/admin/alimentos/busqueda")).doesNotContain(propio.getId(), plato.getId());
    }

    @Test
    @DisplayName("sigue siendo solo de ADMIN")
    void solo_admin() throws Exception {
        assertThat(estado(owner, "GET /admin/alimentos/busqueda")).isEqualTo(403);
        assertThat(estado(guest, "GET /admin/alimentos/busqueda")).isEqualTo(403);
    }

    private Alimento alimento(String nombre, Usuario dueno, boolean activo, Integer raciones) {
        Alimento a = new Alimento();
        a.setNombre(nombre);
        a.setCalorias(100);
        a.setActivo(activo);
        a.setUsuario(dueno);
        a.setPlatoRaciones(raciones);
        return alimentoRepository.save(a);
    }

    private List<Integer> ids(Usuario quien, String ruta) throws Exception {
        JsonNode lista = objectMapper.readTree(pedir(quien, "GET " + ruta).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        List<Integer> ids = new ArrayList<>();
        lista.forEach(n -> ids.add(n.get("id").asInt()));
        return ids;
    }
}
