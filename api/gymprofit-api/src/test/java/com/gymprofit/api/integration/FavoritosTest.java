package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.dto.entity.productooff.ProductoOffImportDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.service.externo.LimiteOpenFoodFacts;
import com.gymprofit.api.service.externo.OpenFoodFactsClient;
import com.gymprofit.api.service.productooff.ProductoOffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// FavoritosTest — los favoritos y su propuesta (lote 1.6.3, A3 y A4)
// Con el contexto levantado y JWT de verdad (DEC-014): un favorito es de la cuenta del
// token y de un alimento que esa cuenta puede ver. El id llega en la ruta y su dueño
// manda (DEC-027): el alimento propio de otro es un 403 que no dice si existe.
// ============================================================
class FavoritosTest extends AbstractOwnershipTest {

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @Autowired
    private ProductoOffService productoOffService;

    @Autowired
    private LimiteOpenFoodFacts limite;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private OpenFoodFactsClient openFoodFactsClient;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    private Alimento yogur;
    private Alimento pan;
    private Alimento avena;

    @BeforeEach
    void sembrar() {
        limite.reiniciar();
        when(openFoodFactsClient.porBarcode(anyString())).thenReturn(Optional.empty());
        yogur = catalogo("Yogur kzfav");
        pan = catalogo("Pan kzfav");
        avena = catalogo("Avena kzfav");
    }

    // --- A3 · Marcar y desmarcar --------------------------------------------

    @Test
    @DisplayName("marcar es repetible y no duplica; el alimento dice favorito en la ficha y en la lista")
    void marcar() throws Exception {
        pedir(owner, "PUT /favoritos/" + yogur.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(yogur.getId()))
                .andExpect(jsonPath("$.favorito").value(true));
        pedir(owner, "PUT /favoritos/" + yogur.getId()).andExpect(status().isOk());
        assertThat(filas("favoritos", owner)).isEqualTo(1);

        pedir(owner, "GET /alimentos/" + yogur.getId()).andExpect(jsonPath("$.favorito").value(true));
        pedir(owner, "GET /alimentos/" + pan.getId()).andExpect(jsonPath("$.favorito").value(false));
        assertThat(ids(lista(owner))).containsExactly(yogur.getId());
    }

    @Test
    @DisplayName("desmarcar es repetible: 204 las dos veces, y deja de ser favorito")
    void desmarcar() throws Exception {
        pedir(owner, "PUT /favoritos/" + yogur.getId()).andExpect(status().isOk());
        pedir(owner, "DELETE /favoritos/" + yogur.getId()).andExpect(status().isNoContent());
        pedir(owner, "DELETE /favoritos/" + yogur.getId()).andExpect(status().isNoContent());
        pedir(owner, "GET /alimentos/" + yogur.getId()).andExpect(jsonPath("$.favorito").value(false));
        assertThat(ids(lista(owner))).isEmpty();
    }

    @Test
    @DisplayName("lo propio sí se marca")
    void lo_propio() throws Exception {
        Alimento mio = propio(owner, "Tortilla kzfav");
        pedir(owner, "PUT /favoritos/" + mio.getId()).andExpect(status().isOk());
        assertThat(ids(lista(owner))).containsExactly(mio.getId());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"PUT /favoritos/{id}", "DELETE /favoritos/{id}", "PUT /favoritos/rechazados/{id}"})
    @DisplayName("DEC-014: el alimento propio de otro es un 403, igual que uno que no existe")
    void ajeno_403_sin_decir_si_existe(String ruta) throws Exception {
        Alimento ajeno = propio(owner, "Dieta secreta kzfav");
        pedir(owner, "PUT /favoritos/" + ajeno.getId()).andExpect(status().isOk());

        String deOtro = pedir(attacker, ruta.replace("{id}", String.valueOf(ajeno.getId())))
                .andExpect(status().isForbidden()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String queNoExiste = pedir(attacker, ruta.replace("{id}", "2147483000"))
                .andExpect(status().isForbidden()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(deOtro).isEqualTo(queNoExiste).doesNotContain("Dieta secreta");
        assertThat(filas("favoritos", attacker)).isZero();
        // El favorito de su dueño sigue ahí.
        assertThat(ids(lista(owner))).containsExactly(ajeno.getId());
    }

    @Test
    @DisplayName("DEC-027: la lista de uno nunca trae lo de otro")
    void aislamiento() throws Exception {
        Alimento mio = propio(owner, "Tortilla kzfav");
        pedir(owner, "PUT /favoritos/" + mio.getId()).andExpect(status().isOk());
        pedir(owner, "PUT /favoritos/" + yogur.getId()).andExpect(status().isOk());
        pedir(attacker, "PUT /favoritos/" + pan.getId()).andExpect(status().isOk());

        assertThat(ids(lista(owner))).containsExactlyInAnyOrder(mio.getId(), yogur.getId());
        assertThat(ids(lista(attacker))).containsExactly(pan.getId());
        pedir(attacker, "GET /alimentos/" + yogur.getId()).andExpect(jsonPath("$.favorito").value(false));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GET /favoritos", "PUT /favoritos/{id}", "DELETE /favoritos/{id}",
            "PUT /favoritos/codigo/8400000730013", "PUT /favoritos/rechazados/{id}"})
    @DisplayName("el invitado no tiene favoritos, como no añade: 403")
    void invitado_403(String ruta) throws Exception {
        pedir(guest, ruta.replace("{id}", String.valueOf(yogur.getId()))).andExpect(status().isForbidden());
        assertThat(filas("favoritos", guest)).isZero();
    }

    @Test
    @DisplayName("un producto que aún no está en el catálogo se marca por su código y se materializa")
    void por_codigo() throws Exception {
        productoOffService.importarLote(List.of(new ProductoOffImportDTO("8400000730013", "Batido kzfav",
                "Marca", 60.0, 10.0, 4.0, 0.2, null, null, null, "200 ml", 50, null, null)));
        JsonNode alimento = json(pedir(owner, "PUT /favoritos/codigo/8400000730013"));
        assertThat(alimento.get("id").isNull()).isFalse();
        assertThat(alimento.get("favorito").asBoolean()).isTrue();
        assertThat(alimento.get("nombre").asText()).isEqualTo("Batido kzfav");
        // Repetir no lo materializa dos veces ni duplica el favorito.
        pedir(owner, "PUT /favoritos/codigo/8400000730013").andExpect(jsonPath("$.id").value(alimento.get("id").asInt()));
        assertThat(filas("favoritos", owner)).isEqualTo(1);
        pedir(owner, "GET /alimentos/codigo/8400000730013").andExpect(jsonPath("$.favorito").value(true));
        assertThat(ids(lista(owner))).containsExactly(alimento.get("id").asInt());
    }

    @Test
    @DisplayName("un código que no existe es un 404 y no marca nada")
    void codigo_404() throws Exception {
        pedir(owner, "PUT /favoritos/codigo/8400000730020").andExpect(status().isNotFound());
        assertThat(filas("favoritos", owner)).isZero();
    }

    @Test
    @DisplayName("los desactivados no salen en la lista")
    void desactivados() throws Exception {
        pedir(owner, "PUT /favoritos/" + yogur.getId()).andExpect(status().isOk());
        yogur.setActivo(false);
        alimentoRepository.saveAndFlush(yogur);
        assertThat(ids(lista(owner))).isEmpty();
    }

    @Test
    @DisplayName("borrar el alimento propio se lleva su favorito")
    void borrar_el_alimento() throws Exception {
        Alimento mio = propio(owner, "Tortilla kzfav");
        pedir(owner, "PUT /favoritos/" + mio.getId()).andExpect(status().isOk());
        pedir(owner, "DELETE /alimentos/" + mio.getId() + "/permanente").andExpect(status().isOk());
        assertThat(filas("favoritos", owner)).isZero();
    }

    @Test
    @DisplayName("la lista va por uso en 60 días; a igualdad, el usado más reciente y luego el nombre")
    void orden_por_uso() throws Exception {
        Alimento zanahoria = catalogo("Zanahoria kzfav");
        for (Alimento a : List.of(yogur, pan, avena, zanahoria)) {
            pedir(owner, "PUT /favoritos/" + a.getId()).andExpect(status().isOk());
        }
        LocalDate hoy = LocalDate.now();
        // Pan: 3 comidas. Yogur: 2, la última hace 1 día. Avena: 2, la última hace 5. Zanahoria: 0.
        apuntar(owner, pan, hoy, "50", null, null);
        apuntar(owner, pan, hoy.minusDays(10), "50", null, null);
        apuntar(owner, pan, hoy.minusDays(20), "50", null, null);
        apuntar(owner, yogur, hoy.minusDays(1), "125", null, null);
        apuntar(owner, yogur, hoy.minusDays(30), "125", null, null);
        apuntar(owner, avena, hoy.minusDays(5), "40", null, null);
        apuntar(owner, avena, hoy.minusDays(6), "40", null, null);
        // Fuera de los 60 días no cuenta: si contara, la avena iría primera.
        for (int i = 0; i < 4; i++) apuntar(owner, avena, hoy.minusDays(61 + i), "40", null, null);

        assertThat(ids(lista(owner))).containsExactly(pan.getId(), yogur.getId(), avena.getId(), zanahoria.getId());
    }

    @Test
    @DisplayName("cada favorito trae sus raciones y su última cantidad")
    void con_raciones_y_ultima() throws Exception {
        Integer envase = racion(yogur, "1 envase", "1 pot", "125.0");
        pedir(owner, "PUT /favoritos/" + yogur.getId()).andExpect(status().isOk());
        apuntar(owner, yogur, LocalDate.now().minusDays(2), "250", envase, "2");
        JsonNode f = lista(owner).get("favoritos").get(0);
        assertThat(f.get("favorito").asBoolean()).isTrue();
        assertThat(f.get("raciones").get(0).get("id").asInt()).isEqualTo(envase);
        assertThat(f.get("ultima").get("cantidadGramos").decimalValue()).isEqualByComparingTo("250");
        assertThat(f.get("ultima").get("racionId").asInt()).isEqualTo(envase);
        assertThat(f.get("ultima").get("raciones").decimalValue()).isEqualByComparingTo("2");
    }

    // --- A4 · La propuesta ----------------------------------------------------

    @Test
    @DisplayName("3 comidas en 14 días no proponen; 4 sí, con las veces")
    void propuesta_de_4() throws Exception {
        LocalDate hoy = LocalDate.now();
        for (int i = 0; i < 3; i++) apuntar(owner, pan, hoy.minusDays(i), "56", null, null);
        assertThat(lista(owner).get("propuesta").isNull()).isTrue();

        apuntar(owner, pan, hoy.minusDays(13), "56", null, null);
        JsonNode propuesta = lista(owner).get("propuesta");
        assertThat(propuesta.get("alimento").get("id").asInt()).isEqualTo(pan.getId());
        assertThat(propuesta.get("alimento").get("favorito").asBoolean()).isFalse();
        assertThat(propuesta.get("veces").asInt()).isEqualTo(4);
    }

    @Test
    @DisplayName("una comida de hace 14 días ya no cuenta")
    void fuera_de_14_dias() throws Exception {
        LocalDate hoy = LocalDate.now();
        for (int i = 0; i < 3; i++) apuntar(owner, pan, hoy.minusDays(i), "56", null, null);
        apuntar(owner, pan, hoy.minusDays(14), "56", null, null);
        assertThat(lista(owner).get("propuesta").isNull()).isTrue();
    }

    @Test
    @DisplayName("si hay varios, el más usado")
    void el_mas_usado() throws Exception {
        LocalDate hoy = LocalDate.now();
        for (int i = 0; i < 4; i++) apuntar(owner, pan, hoy.minusDays(i), "56", null, null);
        for (int i = 0; i < 5; i++) apuntar(owner, avena, hoy.minusDays(i), "40", null, null);
        JsonNode propuesta = lista(owner).get("propuesta");
        assertThat(propuesta.get("alimento").get("id").asInt()).isEqualTo(avena.getId());
        assertThat(propuesta.get("veces").asInt()).isEqualTo(5);
    }

    @Test
    @DisplayName("aceptada (marcarlo favorito) no vuelve, tampoco si después se quita")
    void aceptada_no_vuelve() throws Exception {
        LocalDate hoy = LocalDate.now();
        for (int i = 0; i < 4; i++) apuntar(owner, pan, hoy.minusDays(i), "56", null, null);
        pedir(owner, "PUT /favoritos/" + pan.getId()).andExpect(status().isOk());
        assertThat(lista(owner).get("propuesta").isNull()).isTrue();
        pedir(owner, "DELETE /favoritos/" + pan.getId()).andExpect(status().isNoContent());
        assertThat(lista(owner).get("propuesta").isNull()).isTrue();
    }

    @Test
    @DisplayName("rechazada no vuelve, y rechazar es repetible; la siguiente sí sale")
    void rechazada_no_vuelve() throws Exception {
        LocalDate hoy = LocalDate.now();
        for (int i = 0; i < 5; i++) apuntar(owner, pan, hoy.minusDays(i), "56", null, null);
        for (int i = 0; i < 4; i++) apuntar(owner, avena, hoy.minusDays(i), "40", null, null);
        assertThat(lista(owner).get("propuesta").get("alimento").get("id").asInt()).isEqualTo(pan.getId());

        pedir(owner, "PUT /favoritos/rechazados/" + pan.getId()).andExpect(status().isNoContent());
        pedir(owner, "PUT /favoritos/rechazados/" + pan.getId()).andExpect(status().isNoContent());
        assertThat(lista(owner).get("propuesta").get("alimento").get("id").asInt()).isEqualTo(avena.getId());
        // Ni aunque un día fuera favorito y se quitara.
        pedir(owner, "PUT /favoritos/" + pan.getId()).andExpect(status().isOk());
        pedir(owner, "DELETE /favoritos/" + pan.getId()).andExpect(status().isNoContent());
        assertThat(lista(owner).get("propuesta").get("alimento").get("id").asInt()).isEqualTo(avena.getId());
    }

    @Test
    @DisplayName("lo que apunta otro no propone nada aquí, ni su propuesta se ve")
    void propuesta_ajena() throws Exception {
        Alimento suyo = propio(attacker, "Batido secreto kzfav");
        LocalDate hoy = LocalDate.now();
        for (int i = 0; i < 4; i++) apuntar(attacker, suyo, hoy.minusDays(i), "300", null, null);
        assertThat(lista(owner).get("propuesta").isNull()).isTrue();
        assertThat(lista(attacker).get("propuesta").get("alimento").get("id").asInt()).isEqualTo(suyo.getId());
    }

    // --- A6 · La clave de la categoría ----------------------------------------

    @Test
    @DisplayName("A6: categoriaClave es la canónica; categoria se sigue traduciendo para las builds repartidas")
    void categoria_clave() throws Exception {
        yogur.setCategoria("Lácteos");
        yogur.setCategoriaEn("Dairy");
        alimentoRepository.saveAndFlush(yogur);
        String ruta = "/alimentos/" + yogur.getId();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(ruta)
                        .header("Authorization", "Bearer " + token(owner)).header("Accept-Language", "en"))
                .andExpect(jsonPath("$.categoria").value("Dairy"))
                .andExpect(jsonPath("$.categoriaClave").value("Lácteos"));
        pedir(owner, "GET " + ruta).andExpect(jsonPath("$.categoriaClave").value("Lácteos"));
        pedir(owner, "GET /alimentos/" + pan.getId()).andExpect(jsonPath("$.categoriaClave").doesNotExist());
    }

    // --- Andamiaje ----------------------------------------------------------

    private Alimento catalogo(String nombre) {
        Alimento a = crearAlimentoCatalogo();
        a.setNombre(nombre);
        return alimentoRepository.saveAndFlush(a);
    }

    private Alimento propio(Usuario dueno, String nombre) {
        Alimento a = crearAlimentoCatalogo();
        a.setNombre(nombre);
        a.setUsuario(dueno);
        return alimentoRepository.saveAndFlush(a);
    }

    private Integer racion(Alimento a, String nombre, String nombreEn, String gramos) {
        jdbc.update("INSERT INTO alimento_raciones (alimento_id, nombre, nombre_en, gramos, fuente, orden) "
                + "VALUES (?, ?, ?, ?, 'prueba', 1)", a.getId(), nombre, nombreEn, new BigDecimal(gramos));
        // Fuera de la sesión de JPA del test: que la búsqueda cargue el alimento con ella.
        em.flush();
        em.clear();
        return jdbc.queryForObject("SELECT MAX(id) FROM alimento_raciones WHERE alimento_id = ?", Integer.class, a.getId());
    }

    // Una comida nueva ese día, con una línea de ese alimento.
    private void apuntar(Usuario quien, Alimento a, LocalDate dia, String gramos, Integer racionId, String raciones) {
        jdbc.update("INSERT INTO comidas (usuario_id, fecha, tipo_comida) VALUES (?, ?, 'COMIDA')",
                quien.getId(), Timestamp.valueOf(dia.atStartOfDay()));
        Integer comida = jdbc.queryForObject("SELECT MAX(id) FROM comidas WHERE usuario_id = ?", Integer.class, quien.getId());
        jdbc.update("INSERT INTO alimentos_comida (comida_id, alimento_id, cantidad_gramos, calorias_totales, racion_id, raciones) "
                        + "VALUES (?, ?, ?, 90, ?, ?)", comida, a.getId(), new BigDecimal(gramos), racionId,
                raciones == null ? null : new BigDecimal(raciones));
    }

    private int filas(String tabla, Usuario u) {
        em.flush();
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla + " WHERE usuario_id = ?", Integer.class, u.getId());
    }

    private JsonNode lista(Usuario quien) throws Exception {
        return json(pedir(quien, "GET /favoritos"));
    }

    private static List<Integer> ids(JsonNode lista) {
        List<Integer> ids = new ArrayList<>();
        lista.get("favoritos").forEach(n -> ids.add(n.get("id").asInt()));
        return ids;
    }

    private JsonNode json(ResultActions r) throws Exception {
        return objectMapper.readTree(r.andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
    }
}
