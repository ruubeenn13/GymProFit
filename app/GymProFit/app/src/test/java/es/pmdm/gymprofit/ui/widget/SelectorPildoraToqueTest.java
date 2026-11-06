package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * GP-225: la pista del selector responde al toque también en sus 4 dp de los lados (antes no
 * había nada debajo y el toque se perdía). Un toque ahí cuenta como de la opción del borde.
 */
public class SelectorPildoraToqueTest {

    private static final int ANCHO = 400, RELLENO = 10;

    @Test
    public void enElRellenoIzquierdoEsLaPrimera() {
        assertEquals(RELLENO, SelectorPildora.xDentro(0, ANCHO, RELLENO), 0.01);
        assertEquals(RELLENO, SelectorPildora.xDentro(9.5f, ANCHO, RELLENO), 0.01);
    }

    @Test
    public void enElRellenoDerechoEsLaUltima() {
        float x = SelectorPildora.xDentro(ANCHO, ANCHO, RELLENO);
        assertEquals(ANCHO - RELLENO - 1, x, 0.01);
        assertEquals(ANCHO - RELLENO - 1, SelectorPildora.xDentro(395, ANCHO, RELLENO), 0.01);
    }

    @Test
    public void dentroNoSeMueve() {
        assertEquals(200, SelectorPildora.xDentro(200, ANCHO, RELLENO), 0.01);
        assertEquals(RELLENO, SelectorPildora.xDentro(RELLENO, ANCHO, RELLENO), 0.01);
    }
}
