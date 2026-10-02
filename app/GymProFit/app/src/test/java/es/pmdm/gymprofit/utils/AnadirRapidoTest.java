package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.alimento.Racion;
import es.pmdm.gymprofit.model.alimento.UltimaCantidad;

// ============================================================
// AnadirRapidoTest — qué añade el «+» de una fila (lote 1.6.3, B2)
// La última cantidad si la hay; si no, lo que propondría la ficha: la primera ración
// por una, o 100 g.
// ============================================================
public class AnadirRapidoTest {

    private static final Cantidades.Formatos F = new Cantidades.Formatos(
            "%1$s (%2$s g)", "%1$s × %2$s (%3$s g)", "%1$s %2$s (%3$s g)", "%1$s g");
    private static final Locale ES = new Locale("es", "ES");

    private static Alimento pan() {
        Alimento a = new Alimento();
        a.setId(7);
        a.setCalorias(250);
        a.setRaciones(new ArrayList<>(Arrays.asList(new Racion(31, "1 rebanada", 28), new Racion(32, "1 barra", 250))));
        return a;
    }

    @Test
    public void con_la_ultima_por_raciones_anade_esas_raciones() {
        Alimento a = pan();
        a.setUltima(new UltimaCantidad(56, 31, "1 rebanada", 2.0, "rebanada", "rebanadas"));
        CantidadFicha c = AnadirRapido.cantidad(a);
        assertEquals(Integer.valueOf(31), c.unidad().racionId);
        assertEquals(2, c.valor(), 0);
        assertEquals(56, c.gramos(), 0);
        assertEquals(140, AnadirRapido.kcal(a, c));
        assertTrue(AnadirRapido.tieneUltima(a));
    }

    @Test
    public void con_la_ultima_en_gramos_anade_esos_gramos() {
        Alimento a = pan();
        a.setUltima(new UltimaCantidad(150, null, null, null, null, null));
        CantidadFicha c = AnadirRapido.cantidad(a);
        assertTrue(c.unidad().esGramos());
        assertEquals(150, c.gramos(), 0);
        assertEquals("150 g", AnadirRapido.texto(F, ES, c));
    }

    @Test
    public void sin_ultima_la_primera_racion_por_una() {
        Alimento a = pan();
        CantidadFicha c = AnadirRapido.cantidad(a);
        assertEquals(Integer.valueOf(31), c.unidad().racionId);
        assertEquals(1, c.valor(), 0);
        assertEquals("1 rebanada (28 g)", AnadirRapido.texto(F, ES, c));
        assertFalse(AnadirRapido.tieneUltima(a));
    }

    @Test
    public void sin_ultima_ni_raciones_100_g() {
        Alimento a = new Alimento();
        a.setCalorias(165);
        CantidadFicha c = AnadirRapido.cantidad(a);
        assertTrue(c.unidad().esGramos());
        assertEquals(100, c.gramos(), 0);
        assertEquals(165, AnadirRapido.kcal(a, c));
    }

    @Test
    public void una_ultima_con_una_racion_que_ya_no_esta_va_en_gramos() {
        Alimento a = pan();
        a.setUltima(new UltimaCantidad(80, 99, "1 loncha", 2.0, null, null));
        CantidadFicha c = AnadirRapido.cantidad(a);
        assertTrue(c.unidad().esGramos());
        assertEquals(80, c.gramos(), 0);
    }
}
