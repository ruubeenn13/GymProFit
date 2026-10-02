package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.dto.entity.productooff.ProductoOffImportDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AlimentoRacion;
import com.gymprofit.api.repository.jpa.IAlimentoRacionRepository;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.service.externo.LimiteOpenFoodFacts;
import com.gymprofit.api.service.externo.OpenFoodFactsClient;
import com.gymprofit.api.service.productooff.ProductoOffService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// AnadirAlimentoTest — POST /comidas/anadir, añadir en un viaje (lote 1.6.1)
// Antes eran hasta cuatro peticiones: importar, buscar la comida del día, crearla y
// añadir. Ahora una: encuentra o crea la comida del usuario del TOKEN (DEC-013) y
// devuelve la comida con sus totales y la línea. El alimento llega en el cuerpo, así
// que se comprueba como un id de recurso (DEC-014, DEC-027): uno de otro usuario, 403;
// del catálogo, sí; por código, solo lo tuyo y el catálogo.
// ============================================================
class AnadirAlimentoTest extends AbstractOwnershipTest {

    private static final String FECHA = "2026-10-02";

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @Autowired
    private IAlimentoRacionRepository racionRepository;

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
    private Integer envaseId;

    @BeforeEach
    void sembrar() {
        limite.reiniciar();
        when(openFoodFactsClient.porBarcode(anyString())).thenReturn(Optional.empty());
        yogur = crearAlimentoCatalogo();
        yogur.setCalorias(60);
        yogur.setProteinas(new BigDecimal("10.00"));
        alimentoRepository.save(yogur);
        envaseId = racion(yogur, "1 envase", "1 pack", "200.0", 1).getId();
    }

    @AfterEach
    void sinRastro() {
        limite.reiniciar();
    }

    private AlimentoRacion racion(Alimento a, String nombre, String nombreEn, String gramos, int orden) {
        AlimentoRacion r = new AlimentoRacion();
        r.setAlimento(a);
        r.setNombre(nombre);
        r.setNombreEn(nombreEn);
        r.setGramos(new BigDecimal(gramos));
        r.setFuente("prueba");
        r.setOrden(orden);
        return racionRepository.save(r);
    }

    private static String cuerpo(String tipo, String alimento, String cantidad) {
        return "{\"fecha\":\"" + FECHA + "\",\"tipoComida\":\"" + tipo + "\"," + alimento + "," + cantidad + "}";
    }

    @Test
    @DisplayName("crea la comida del día si no existe y devuelve la comida con sus totales y la línea")
    void crea_la_comida() throws Exception {
        pedir(owner, "POST /comidas/anadir",
                cuerpo("MERIENDA", "\"alimentoId\":" + yogur.getId(), "\"cantidadGramos\":150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comida.tipoComida").value("MERIENDA"))
                .andExpect(jsonPath("$.comida.usuarioId").value(owner.getId()))
                .andExpect(jsonPath("$.comida.totalCalorias").value(90))
                .andExpect(jsonPath("$.comida.totalProteinas").value(15.0))
                .andExpect(jsonPath("$.linea.cantidadGramos").value(150.0))
                .andExpect(jsonPath("$.linea.racionId").doesNotExist());
        assertThat(comidasDe(owner)).isEqualTo(1);
    }

    @Test
    @DisplayName("la segunda vez usa la misma comida; otro tipo u otro día, otra")
    void reutiliza_la_comida() throws Exception {
        Alimento pan = crearAlimentoCatalogo();
        Integer primera = comidaId(pedir(owner, "POST /comidas/anadir",
                cuerpo("MERIENDA", "\"alimentoId\":" + yogur.getId(), "\"cantidadGramos\":100")));
        Integer segunda = comidaId(pedir(owner, "POST /comidas/anadir",
                cuerpo("MERIENDA", "\"alimentoId\":" + pan.getId(), "\"cantidadGramos\":50")));
        assertThat(segunda).isEqualTo(primera);

        Integer cena = comidaId(pedir(owner, "POST /comidas/anadir",
                cuerpo("CENA", "\"alimentoId\":" + pan.getId(), "\"cantidadGramos\":50")));
        assertThat(cena).isNotEqualTo(primera);
        Integer otroDia = comidaId(pedir(owner, "POST /comidas/anadir",
                "{\"fecha\":\"2026-10-03\",\"tipoComida\":\"MERIENDA\",\"alimentoId\":" + pan.getId()
                        + ",\"cantidadGramos\":50}"));
        assertThat(otroDia).isNotEqualTo(primera);
        assertThat(comidasDe(owner)).isEqualTo(3);
    }

    @Test
    @DisplayName("con ración: los gramos salen de ella y la línea la guarda")
    void con_racion() throws Exception {
        pedir(owner, "POST /comidas/anadir",
                cuerpo("MERIENDA", "\"alimentoId\":" + yogur.getId(), "\"racionId\":" + envaseId + ",\"raciones\":1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linea.cantidadGramos").value(200.0))
                .andExpect(jsonPath("$.linea.racionId").value(envaseId))
                .andExpect(jsonPath("$.linea.racionNombre").value("1 envase"))
                .andExpect(jsonPath("$.comida.totalCalorias").value(120));
    }

    @Test
    @DisplayName("si el alimento ya está en la comida, se suma: misma ración, más raciones")
    void suma() throws Exception {
        String conRacion = cuerpo("MERIENDA", "\"alimentoId\":" + yogur.getId(),
                "\"racionId\":" + envaseId + ",\"raciones\":1");
        pedir(owner, "POST /comidas/anadir", conRacion).andExpect(status().isOk());
        pedir(owner, "POST /comidas/anadir", conRacion)
                .andExpect(jsonPath("$.linea.cantidadGramos").value(400.0))
                .andExpect(jsonPath("$.linea.raciones").value(2))
                .andExpect(jsonPath("$.linea.racionId").value(envaseId));
        // Y en gramos, ya no son «3 envases».
        pedir(owner, "POST /comidas/anadir", cuerpo("MERIENDA", "\"alimentoId\":" + yogur.getId(),
                "\"cantidadGramos\":50"))
                .andExpect(jsonPath("$.linea.cantidadGramos").value(450.0))
                .andExpect(jsonPath("$.linea.racionId").doesNotExist());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alimentos_comida ac JOIN comidas c ON c.id = ac.comida_id "
                + "WHERE c.usuario_id = ?", Integer.class, owner.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("por código: se materializa desde productos_off, y la ración se elige por su posición")
    void por_codigo() throws Exception {
        productoOffService.importarLote(List.of(new ProductoOffImportDTO("8400000720014", "Yogur kzanadir",
                "Marca", 60.0, 10.0, 4.0, 0.2, null, null, null, "4 x 125 g", 50, null, null)));
        pedir(owner, "POST /comidas/anadir",
                cuerpo("MERIENDA", "\"barcode\":\"8400000720014\"", "\"racionIndice\":0,\"raciones\":2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linea.nombreAlimento").value("Yogur kzanadir"))
                .andExpect(jsonPath("$.linea.racionNombre").value("1 unidad"))
                .andExpect(jsonPath("$.linea.cantidadGramos").value(250.0));
    }

    @Test
    @DisplayName("un código que no existe es un 404 y no deja una comida vacía")
    void codigo_404() throws Exception {
        pedir(owner, "POST /comidas/anadir", cuerpo("MERIENDA", "\"barcode\":\"8400000720021\"", "\"cantidadGramos\":100"))
                .andExpect(status().isNotFound());
        assertThat(comidasDe(owner)).isZero();
    }

    @Test
    @DisplayName("DEC-014: el alimento propio de otro usuario es un 403, y no se crea nada")
    void alimento_ajeno_403() throws Exception {
        Alimento privado = crearAlimentoCatalogo();
        privado.setUsuario(owner);
        alimentoRepository.save(privado);
        String cuerpo = pedir(attacker, "POST /comidas/anadir",
                cuerpo("MERIENDA", "\"alimentoId\":" + privado.getId(), "\"cantidadGramos\":100"))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(cuerpo).doesNotContain("Alimento IDOR test");
        assertThat(comidasDe(attacker)).isZero();

        // Su dueño sí.
        pedir(owner, "POST /comidas/anadir",
                cuerpo("MERIENDA", "\"alimentoId\":" + privado.getId(), "\"cantidadGramos\":100"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DEC-027: por código, el alimento propio de otro no sale (aislamiento: 404)")
    void codigo_ajeno_aislado() throws Exception {
        Alimento privado = crearAlimentoCatalogo();
        privado.setUsuario(owner);
        privado.setBarcode("8400000720038");
        alimentoRepository.save(privado);
        pedir(attacker, "POST /comidas/anadir", cuerpo("MERIENDA", "\"barcode\":\"8400000720038\"", "\"cantidadGramos\":100"))
                .andExpect(status().isNotFound());
        pedir(owner, "POST /comidas/anadir", cuerpo("MERIENDA", "\"barcode\":\"8400000720038\"", "\"cantidadGramos\":100"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("la comida es siempre la del token: un usuarioId en el cuerpo no cambia nada")
    void usuario_del_token() throws Exception {
        pedir(attacker, "POST /comidas/anadir", "{\"fecha\":\"" + FECHA + "\",\"tipoComida\":\"MERIENDA\",\"usuarioId\":"
                + owner.getId() + ",\"alimentoId\":" + yogur.getId() + ",\"cantidadGramos\":100}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comida.usuarioId").value(attacker.getId()));
        assertThat(comidasDe(owner)).isZero();
    }

    @Test
    @DisplayName("una ración de otro alimento, 400")
    void racion_de_otro_400() throws Exception {
        Alimento pan = crearAlimentoCatalogo();
        pedir(owner, "POST /comidas/anadir",
                cuerpo("MERIENDA", "\"alimentoId\":" + pan.getId(), "\"racionId\":" + envaseId + ",\"raciones\":1"))
                .andExpect(status().isBadRequest());
        assertThat(comidasDe(owner)).isZero();
    }

    @Test
    @DisplayName("datos que no cuadran, 400: sin alimento, con los dos, sin cantidad, tipo que no existe")
    void datos_400() throws Exception {
        pedir(owner, "POST /comidas/anadir", cuerpo("MERIENDA", "\"x\":1", "\"cantidadGramos\":100"))
                .andExpect(status().isBadRequest());
        pedir(owner, "POST /comidas/anadir", cuerpo("MERIENDA",
                "\"alimentoId\":" + yogur.getId() + ",\"barcode\":\"8400000720014\"", "\"cantidadGramos\":100"))
                .andExpect(status().isBadRequest());
        pedir(owner, "POST /comidas/anadir", cuerpo("MERIENDA", "\"alimentoId\":" + yogur.getId(), "\"x\":1"))
                .andExpect(status().isBadRequest());
        pedir(owner, "POST /comidas/anadir", cuerpo("RECENA", "\"alimentoId\":" + yogur.getId(), "\"cantidadGramos\":100"))
                .andExpect(status().isBadRequest());
        pedir(owner, "POST /comidas/anadir", cuerpo("MERIENDA", "\"alimentoId\":" + yogur.getId(), "\"cantidadGramos\":0"))
                .andExpect(status().isBadRequest());
        assertThat(comidasDe(owner)).isZero();
    }

    @Test
    @DisplayName("el invitado no añade")
    void invitado_403() throws Exception {
        pedir(guest, "POST /comidas/anadir",
                cuerpo("MERIENDA", "\"alimentoId\":" + yogur.getId(), "\"cantidadGramos\":100"))
                .andExpect(status().isForbidden());
    }

    // --- Lote 1.6.3 (A2): «anterior», para deshacer exacto ----------------------

    @Test
    @DisplayName("A2: una línea nueva trae anterior null, y deshacerla (borrarla) deja la comida como estaba")
    void anterior_nueva_y_deshacer() throws Exception {
        Integer pan = crearAlimentoCatalogo().getId();
        pedir(owner, "POST /comidas/anadir", cuerpo("MERIENDA", "\"alimentoId\":" + pan, "\"cantidadGramos\":50"))
                .andExpect(status().isOk());
        String antes = estadoComida();

        JsonNode r = json(pedir(owner, "POST /comidas/anadir",
                cuerpo("MERIENDA", "\"alimentoId\":" + yogur.getId(), "\"racionId\":" + envaseId + ",\"raciones\":1")));
        assertThat(r.has("anterior")).isTrue();
        assertThat(r.get("anterior").isNull()).isTrue();

        pedir(owner, "DELETE /alimentos-comida/" + r.get("linea").get("id").asInt()).andExpect(status().isOk());
        assertThat(estadoComida()).isEqualTo(antes);
    }

    @Test
    @DisplayName("A2: si se suma, anterior trae la cantidad de antes, y el PATCH con ella deja la comida como estaba")
    void anterior_suma_y_deshacer() throws Exception {
        pedir(owner, "POST /comidas/anadir",
                cuerpo("MERIENDA", "\"alimentoId\":" + yogur.getId(), "\"racionId\":" + envaseId + ",\"raciones\":1.5"))
                .andExpect(status().isOk());
        String antes = estadoComida();

        // Se suma en gramos: la línea deja de ir por raciones.
        JsonNode r = json(pedir(owner, "POST /comidas/anadir",
                cuerpo("MERIENDA", "\"alimentoId\":" + yogur.getId(), "\"cantidadGramos\":30")));
        JsonNode anterior = r.get("anterior");
        assertThat(anterior.get("cantidadGramos").decimalValue()).isEqualByComparingTo("300");
        assertThat(anterior.get("racionId").asInt()).isEqualTo(envaseId);
        assertThat(anterior.get("raciones").decimalValue()).isEqualByComparingTo("1.5");
        assertThat(r.get("linea").get("racionId").isNull()).isTrue();

        pedir(owner, "PATCH /alimentos-comida/" + r.get("linea").get("id").asInt(), objectMapper.writeValueAsString(anterior))
                .andExpect(status().isOk());
        assertThat(estadoComida()).isEqualTo(antes);
    }

    @Test
    @DisplayName("A2: sumada en gramos a una línea en gramos, anterior trae solo los gramos")
    void anterior_en_gramos() throws Exception {
        String g = cuerpo("MERIENDA", "\"alimentoId\":" + yogur.getId(), "\"cantidadGramos\":80");
        pedir(owner, "POST /comidas/anadir", g).andExpect(status().isOk());
        String antes = estadoComida();
        JsonNode r = json(pedir(owner, "POST /comidas/anadir", g));
        assertThat(r.get("anterior").get("cantidadGramos").decimalValue()).isEqualByComparingTo("80");
        assertThat(r.get("anterior").get("racionId").isNull()).isTrue();
        pedir(owner, "PATCH /alimentos-comida/" + r.get("linea").get("id").asInt(),
                objectMapper.writeValueAsString(r.get("anterior"))).andExpect(status().isOk());
        assertThat(estadoComida()).isEqualTo(antes);
    }

    // --- Lote 1.6.3 (A5, GP-177): una línea va por raciones solo si sus gramos cuadran ---

    @Test
    @DisplayName("A5: si la ración ya no pesa lo mismo, la línea sale en gramos; si vuelve a pesarlo, con su ración")
    void racion_que_no_cuadra() throws Exception {
        Alimento pan = crearAlimentoCatalogo();
        pan.setUsuario(owner);
        alimentoRepository.save(pan);
        AlimentoRacion rebanada = racion(pan, "1 rebanada", "1 slice", "40.0", 1);
        Integer comida = comidaId(pedir(owner, "POST /comidas/anadir",
                cuerpo("MERIENDA", "\"alimentoId\":" + pan.getId(), "\"racionId\":" + rebanada.getId() + ",\"raciones\":1")));
        String lineas = "GET /alimentos-comida/comida/" + comida;
        pedir(owner, lineas).andExpect(jsonPath("$[0].racionId").value(rebanada.getId()))
                .andExpect(jsonPath("$[0].raciones").value(1));

        // Medio gramo de margen: 40,4 sigue siendo «1 rebanada».
        rebanada.setGramos(new BigDecimal("40.4"));
        racionRepository.saveAndFlush(rebanada);
        pedir(owner, lineas).andExpect(jsonPath("$[0].racionId").value(rebanada.getId()));

        rebanada.setGramos(new BigDecimal("50.0"));
        racionRepository.saveAndFlush(rebanada);
        pedir(owner, lineas)
                .andExpect(jsonPath("$[0].cantidadGramos").value(40.0))
                .andExpect(jsonPath("$[0].racionId").doesNotExist())
                .andExpect(jsonPath("$[0].racionNombre").doesNotExist())
                .andExpect(jsonPath("$[0].racionGramos").doesNotExist())
                .andExpect(jsonPath("$[0].racionUnidad").doesNotExist())
                .andExpect(jsonPath("$[0].raciones").doesNotExist());
        // No se ha reescrito nada.
        assertThat(jdbc.queryForObject("SELECT racion_id FROM alimentos_comida WHERE comida_id = ?", Integer.class, comida))
                .isEqualTo(rebanada.getId());

        rebanada.setGramos(new BigDecimal("40.0"));
        racionRepository.saveAndFlush(rebanada);
        pedir(owner, lineas).andExpect(jsonPath("$[0].racionId").value(rebanada.getId()))
                .andExpect(jsonPath("$[0].racionUnidad").value("rebanada"));
    }

    @Test
    @DisplayName("A5: sumar la misma ración a una línea que ya no cuadra no la cuenta como raciones")
    void sumar_a_racion_que_no_cuadra() throws Exception {
        String una = cuerpo("MERIENDA", "\"alimentoId\":" + yogur.getId(), "\"racionId\":" + envaseId + ",\"raciones\":1");
        pedir(owner, "POST /comidas/anadir", una).andExpect(status().isOk());
        AlimentoRacion envase = racionRepository.findById(envaseId).orElseThrow();
        envase.setGramos(new BigDecimal("250.0"));
        racionRepository.saveAndFlush(envase);
        // 200 g de antes + 250 g de ahora no son «2 envases» de 250.
        pedir(owner, "POST /comidas/anadir", una)
                .andExpect(jsonPath("$.linea.cantidadGramos").value(450.0))
                .andExpect(jsonPath("$.linea.racionId").doesNotExist());
    }

    // --- Andamiaje ----------------------------------------------------------

    private int comidasDe(com.gymprofit.api.entity.Usuario u) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM comidas WHERE usuario_id = ?", Integer.class, u.getId());
    }

    private JsonNode json(org.springframework.test.web.servlet.ResultActions r) throws Exception {
        return objectMapper.readTree(r.andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
    }

    // Lo que se ve de las comidas del dueño: totales y líneas, para comparar antes y después.
    private String estadoComida() {
        em.flush();
        em.clear();
        return jdbc.queryForList("""
                SELECT c.id, c.total_calorias, c.total_proteinas, c.total_carbohidratos, c.total_grasas,
                       ac.alimento_id, ac.cantidad_gramos, ac.racion_id, ac.raciones, ac.calorias_totales
                FROM comidas c LEFT JOIN alimentos_comida ac ON ac.comida_id = c.id
                WHERE c.usuario_id = ? ORDER BY c.id, ac.alimento_id""", owner.getId()).toString();
    }

    private Integer comidaId(org.springframework.test.web.servlet.ResultActions r) throws Exception {
        JsonNode json = objectMapper.readTree(r.andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
        return json.get("comida").get("id").asInt();
    }
}
