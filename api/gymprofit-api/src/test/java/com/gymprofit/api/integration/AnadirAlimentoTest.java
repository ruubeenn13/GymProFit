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

    // --- Andamiaje ----------------------------------------------------------

    private int comidasDe(com.gymprofit.api.entity.Usuario u) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM comidas WHERE usuario_id = ?", Integer.class, u.getId());
    }

    private Integer comidaId(org.springframework.test.web.servlet.ResultActions r) throws Exception {
        JsonNode json = objectMapper.readTree(r.andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
        return json.get("comida").get("id").asInt();
    }
}
