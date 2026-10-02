package com.gymprofit.api.integration;

import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaCreateDTO;
import com.gymprofit.api.dto.entity.comida.ComidaCreateDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AlimentoRacion;
import com.gymprofit.api.repository.jpa.IAlimentoRacionRepository;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.service.alimentocomida.IAlimentoComidaService;
import com.gymprofit.api.service.comida.IComidaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// RacionEnComidaTest — la ración elegida se guarda en la línea (lote 1.6.1)
// Los gramos siguen mandando en las cuentas; la ración y cuántas solo dicen cómo lo
// eligió el usuario, para que la app enseñe «2 rebanadas (56 g)». La ración tiene que
// ser de ese alimento, y cambiar los gramos a mano la quita.
// ============================================================
class RacionEnComidaTest extends AbstractOwnershipTest {

    @Autowired
    private IComidaService comidaService;

    @Autowired
    private IAlimentoComidaService alimentoComidaService;

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @Autowired
    private IAlimentoRacionRepository racionRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private Integer lineaId;
    private Integer panId;
    private Integer rebanadaId;
    private Integer racionDeOtroId;

    @BeforeEach
    void sembrar() {
        Alimento pan = crearAlimentoCatalogo();
        pan.setProteinas(new BigDecimal("9.00"));
        alimentoRepository.save(pan);
        panId = pan.getId();
        rebanadaId = racion(pan, "1 rebanada", "1 slice", "28.0", 1).getId();
        racion(pan, "1 barra", "1 loaf", "250.0", 2);
        racionDeOtroId = racion(crearAlimentoCatalogo(), "1 vaso", "1 glass", "200.0", 1).getId();

        runAs(owner, () -> {
            ComidaCreateDTO comida = new ComidaCreateDTO();
            comida.setTipoComida("MERIENDA");
            Integer comidaId = comidaService.save(comida).getId();
            lineaId = alimentoComidaService.save(
                    new AlimentoComidaCreateDTO(comidaId, pan.getId(), new BigDecimal("100"))).getId();
        });
    }

    private AlimentoRacion racion(Alimento alimento, String nombre, String nombreEn, String gramos, int orden) {
        AlimentoRacion r = new AlimentoRacion();
        r.setAlimento(alimento);
        r.setNombre(nombre);
        r.setNombreEn(nombreEn);
        r.setGramos(new BigDecimal(gramos));
        r.setFuente("prueba");
        r.setOrden(orden);
        return racionRepository.save(r);
    }

    @Test
    @DisplayName("PATCH con ración y cuántas: los gramos salen de la ración y la línea la devuelve")
    void patch_con_racion() throws Exception {
        pedir(owner, "PATCH /alimentos-comida/" + lineaId, "{\"racionId\":" + rebanadaId + ",\"raciones\":2}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadGramos").value(56.0))
                .andExpect(jsonPath("$.caloriasTotales").value(56))
                .andExpect(jsonPath("$.racionId").value(rebanadaId))
                .andExpect(jsonPath("$.racionNombre").value("1 rebanada"))
                .andExpect(jsonPath("$.racionGramos").value(28.0))
                .andExpect(jsonPath("$.raciones").value(2));

        pedir(owner, "GET /alimentos-comida/" + lineaId)
                .andExpect(jsonPath("$.racionId").value(rebanadaId))
                .andExpect(jsonPath("$.raciones").value(2));
    }

    @Test
    @DisplayName("si llegan también los gramos, mandan los gramos; si no cuadran con la ración, sale en gramos (GP-177)")
    void mandan_los_gramos() throws Exception {
        // 2 rebanadas de 28 son 56 g, no 60: se guarda lo pedido, pero la línea ya no es
        // «2 rebanadas» (lote 1.6.3). Hasta la 1.6.2 salía con la ración.
        pedir(owner, "PATCH /alimentos-comida/" + lineaId,
                "{\"racionId\":" + rebanadaId + ",\"raciones\":2,\"cantidadGramos\":60}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadGramos").value(60.0))
                .andExpect(jsonPath("$.racionId").doesNotExist());
        // Con los gramos que le tocan, sí.
        pedir(owner, "PATCH /alimentos-comida/" + lineaId,
                "{\"racionId\":" + rebanadaId + ",\"raciones\":2,\"cantidadGramos\":56}")
                .andExpect(jsonPath("$.racionId").value(rebanadaId));
    }

    @Test
    @DisplayName("una ración de otro alimento es un 400")
    void racion_de_otro_alimento_400() throws Exception {
        pedir(owner, "PATCH /alimentos-comida/" + lineaId, "{\"racionId\":" + racionDeOtroId + ",\"raciones\":1}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("ración sin cuántas, o cuántas sin ración, es un 400")
    void racion_incompleta_400() throws Exception {
        pedir(owner, "PATCH /alimentos-comida/" + lineaId, "{\"racionId\":" + rebanadaId + "}")
                .andExpect(status().isBadRequest());
        pedir(owner, "PATCH /alimentos-comida/" + lineaId, "{\"raciones\":2}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("cambiar solo los gramos quita la ración: ya no son «2 rebanadas»")
    void gramos_a_mano_quitan_la_racion() throws Exception {
        pedir(owner, "PATCH /alimentos-comida/" + lineaId, "{\"racionId\":" + rebanadaId + ",\"raciones\":2}")
                .andExpect(status().isOk());
        pedir(owner, "PATCH /alimentos-comida/" + lineaId, "{\"cantidadGramos\":75}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadGramos").value(75.0))
                .andExpect(jsonPath("$.racionId").doesNotExist())
                .andExpect(jsonPath("$.raciones").doesNotExist());
    }

    @Test
    @DisplayName("el PATCH de una línea ajena sigue siendo 403, también con ración")
    void ajena_403() throws Exception {
        pedir(attacker, "PATCH /alimentos-comida/" + lineaId, "{\"racionId\":" + rebanadaId + ",\"raciones\":1}")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("las raciones del alimento llevan su id, para poder elegirlas")
    void racion_con_id() throws Exception {
        // Como una petición de verdad: el pan se lee de la base, con sus raciones.
        entityManager.flush();
        entityManager.clear();
        pedir(owner, "GET /alimentos/" + panId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raciones[0].id").value(rebanadaId))
                .andExpect(jsonPath("$.raciones[0].nombre").value("1 rebanada"));
    }
}
