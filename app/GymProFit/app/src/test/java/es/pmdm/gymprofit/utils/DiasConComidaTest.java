package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import es.pmdm.gymprofit.model.comida.ResumenDiarioNutricion;

// Estadísticas de nutrición (revisor, lote 12): la API devuelve un día por cada comida que exista,
// también una vacía (la que queda al quitar su último alimento o al marcar «Hoy no»). La pantalla
// los contaba como días registrados y decía «2 días registrados», media 0 kcal y, a la vez,
// «No hay comidas apuntadas en estas fechas». Solo cuenta un día con algo apuntado.
public class DiasConComidaTest {

    private static ResumenDiarioNutricion dia(String json) {
        return new Gson().fromJson(json, ResumenDiarioNutricion.class);
    }

    @Test
    public void un_dia_sin_nada_apuntado_no_es_un_dia_registrado() {
        List<ResumenDiarioNutricion> l = Arrays.asList(
                dia("{\"fecha\":\"2026-11-06\",\"calorias\":0,\"proteinas\":0,\"carbohidratos\":0,\"grasas\":0}"),
                dia("{\"fecha\":\"2026-11-07\",\"calorias\":540,\"proteinas\":30,\"carbohidratos\":60,\"grasas\":12}"),
                dia("{\"fecha\":\"2026-11-08\",\"calorias\":0,\"proteinas\":0,\"carbohidratos\":0,\"grasas\":0}"));
        List<ResumenDiarioNutricion> d = DiasConComida.de(l);
        assertEquals(1, d.size());
        assertEquals("2026-11-07", d.get(0).getFecha());
    }

    @Test
    public void un_dia_con_algo_que_no_llega_a_una_kcal_cuenta() {
        assertEquals(1, DiasConComida.de(Arrays.asList(
                dia("{\"fecha\":\"2026-11-06\",\"calorias\":0,\"proteinas\":0.2,\"carbohidratos\":0,\"grasas\":0}"))).size());
    }

    @Test
    public void sin_lista_no_hay_dias() {
        assertTrue(DiasConComida.de(null).isEmpty());
    }
}
