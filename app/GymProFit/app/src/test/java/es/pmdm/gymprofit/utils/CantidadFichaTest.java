package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import es.pmdm.gymprofit.model.alimento.Racion;

// ============================================================
// CantidadFichaTest — la cantidad de la ficha (lote 1.6.1)
// Por defecto, la primera ración, o 100 g si no tiene. − y + de uno en uno en raciones
// y de 25 en 25 en gramos; tocando la cifra se escribe. Cambiar de unidad conserva los
// gramos. Al editar una línea, la ficha abre con su ración y cuántas, o con sus gramos.
// ============================================================
public class CantidadFichaTest {

    private static final double D = 0.0001;

    private static List<Racion> yogur() {
        return Arrays.asList(new Racion(11, "1 envase", 200), new Racion(12, "1 ración", 125));
    }

    @Test
    public void por_defecto_la_primera_racion() {
        CantidadFicha c = CantidadFicha.nueva(yogur());
        assertEquals(0, c.elegida());
        assertEquals(1, c.valor(), D);
        assertEquals(200, c.gramos(), D);
        assertEquals(Integer.valueOf(11), c.unidad().racionId);
    }

    @Test
    public void sin_raciones_100_gramos() {
        CantidadFicha c = CantidadFicha.nueva(Collections.emptyList());
        assertTrue(c.unidad().esGramos());
        assertEquals(100, c.gramos(), D);
        assertEquals(1, c.unidades().size());
    }

    @Test
    public void las_unidades_son_las_raciones_y_al_final_gramos() {
        CantidadFicha c = CantidadFicha.nueva(yogur());
        assertEquals(3, c.unidades().size());
        assertTrue(c.unidades().get(2).esGramos());
        assertFalse(c.unidades().get(0).esGramos());
    }

    @Test
    public void mas_y_menos_de_uno_en_uno_en_raciones() {
        CantidadFicha c = CantidadFicha.nueva(yogur());
        c.mas();
        assertEquals(2, c.valor(), D);
        assertEquals(400, c.gramos(), D);
        c.menos();
        c.menos();
        assertEquals("no baja de una ración", 1, c.valor(), D);
        assertFalse(c.puedeMenos());
    }

    @Test
    public void mas_y_menos_de_25_en_25_en_gramos() {
        CantidadFicha c = CantidadFicha.nueva(Collections.emptyList());
        c.mas();
        assertEquals(125, c.gramos(), D);
        for (int i = 0; i < 10; i++) c.menos();
        assertEquals("no baja de 25 g", 25, c.gramos(), D);
    }

    @Test
    public void menos_desde_una_cifra_escrita_baja_al_paso() {
        CantidadFicha c = CantidadFicha.nueva(Collections.emptyList());
        assertTrue(c.escribir(40));
        c.menos();
        assertEquals(25, c.gramos(), D);
        assertTrue(c.escribir(10));
        assertFalse("10 g ya está por debajo del paso: no baja más", c.puedeMenos());
    }

    @Test
    public void escribir_valida() {
        CantidadFicha c = CantidadFicha.nueva(yogur());
        assertTrue(c.escribir(1.5));
        assertEquals(300, c.gramos(), D);
        assertFalse(c.escribir(0));
        assertFalse(c.escribir(-2));
        assertFalse("más de 99 raciones no", c.escribir(100));
        assertEquals(1.5, c.valor(), D);
        c.elegir(2);
        assertFalse("más de 5 kg no", c.escribir(5001));
    }

    @Test
    public void cambiar_de_unidad_conserva_los_gramos() {
        CantidadFicha c = CantidadFicha.nueva(yogur());
        c.mas(); // 2 envases, 400 g
        c.elegir(2);
        assertTrue(c.unidad().esGramos());
        assertEquals(400, c.valor(), D);
        c.escribir(250);
        c.elegir(1); // raciones de 125 g: 2
        assertEquals(2, c.valor(), D);
        c.elegir(0); // envases de 200 g: 1,25, a la media más cercana hacia arriba
        assertEquals(1.5, c.valor(), D);
    }

    @Test
    public void a_raciones_redondea_a_la_media_y_nunca_a_cero() {
        CantidadFicha c = CantidadFicha.nueva(yogur());
        c.elegir(2);
        c.escribir(30);
        c.elegir(0);
        assertEquals(0.5, c.valor(), D);
    }

    @Test
    public void editar_abre_con_su_racion_y_cuantas() {
        CantidadFicha c = CantidadFicha.deLinea(yogur(), 12, 2.0, 250);
        assertEquals(1, c.elegida());
        assertEquals(2, c.valor(), D);
        assertEquals(250, c.gramos(), D);
    }

    @Test
    public void editar_sin_racion_abre_en_gramos() {
        CantidadFicha c = CantidadFicha.deLinea(yogur(), null, null, 180);
        assertTrue(c.unidad().esGramos());
        assertEquals(180, c.gramos(), D);
        // Y una ración que ya no existe, también en gramos.
        assertTrue(CantidadFicha.deLinea(yogur(), 99, 1.0, 200).unidad().esGramos());
    }

    @Test
    public void en_gramos_no_hay_racion_que_mandar() {
        CantidadFicha c = CantidadFicha.nueva(yogur());
        c.elegir(2);
        assertNull(c.unidad().racionId);
        assertEquals(-1, c.unidad().indice);
        c.elegir(1);
        assertEquals(1, c.unidad().indice);
    }

    @Test
    public void el_nombre_de_la_unidad_sin_el_uno() {
        // Para «2 × envase (400 g)»: las raciones llegan en singular y con su «1».
        assertEquals("envase", CantidadFicha.nombreUnidad("1 envase"));
        assertEquals("vaso (250 ml)", CantidadFicha.nombreUnidad("1 vaso (250 ml)"));
        assertEquals("media taza", CantidadFicha.nombreUnidad("Media taza"));
        assertEquals("2 sardinas", CantidadFicha.nombreUnidad("2 sardinas"));
        assertEquals("slice", CantidadFicha.nombreUnidad("1 slice"));
    }
}
