package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

// ============================================================
// FocoVisibleTest — el recorte del anillo de foco a lo que se ve de sus listas (GP-227).
//
// Recortar en los dos ejes dejaba sin arriba ni abajo las tarjetas de los carruseles (revisor,
// quinta pasada); sin recortar, alrededor de una fila medio escondida se pintaba sobre la cabecera
// (cuarta pasada). Cada contenedor recorta solo en el eje en que se desplaza.
// ============================================================
public class FocoVisibleTest {

    // La pantalla entera: 1080 × 2400.
    private static final int[] VENTANA = { 0, 0, 1080, 2400 };

    @Test
    public void unCarruselRecortaSoloLosLados() {
        // Un carrusel de 400 px de alto: el anillo de su tarjeta sale por arriba y por abajo, y se ve.
        int[] carrusel = { 0, 1500, 1080, 1900 };
        assertArrayEquals(new int[]{ 0, 0, 1080, 2400 },
                FocoVisible.recortar(VENTANA, carrusel, FocoVisible.EJE_X));
        int[] estrecho = { 40, 1500, 1040, 1900 };
        assertArrayEquals(new int[]{ 40, 0, 1040, 2400 },
                FocoVisible.recortar(VENTANA, estrecho, FocoVisible.EJE_X));
    }

    @Test
    public void unaListaVerticalRecortaSoloArribaYAbajo() {
        // La lista del historial empieza bajo la cabecera (231) y acaba sobre la barra (2274).
        int[] lista = { 42, 231, 1038, 2274 };
        assertArrayEquals(new int[]{ 0, 231, 1080, 2274 },
                FocoVisible.recortar(VENTANA, lista, FocoVisible.EJE_Y));
    }

    @Test
    public void unCarruselDentroDeUnaListaRecortaLosDosPeroCadaUnoEnSuEje() {
        int[] lista = { 0, 231, 1080, 2274 };
        int[] carrusel = { 0, 1500, 1080, 1900 };
        int[] z = FocoVisible.recortar(FocoVisible.recortar(VENTANA, lista, FocoVisible.EJE_Y),
                carrusel, FocoVisible.EJE_X);
        assertArrayEquals(new int[]{ 0, 231, 1080, 2274 }, z);
    }

    @Test
    public void loQueNoSeDesplazaNoRecorta() {
        assertArrayEquals(VENTANA, FocoVisible.recortar(VENTANA, new int[]{ 10, 10, 20, 20 }, 0));
    }

    @Test
    public void sinSolaparseNoQuedaNada() {
        assertArrayEquals(new int[4], FocoVisible.recortar(new int[]{ 0, 0, 100, 100 },
                new int[]{ 0, 200, 100, 300 }, FocoVisible.EJE_Y));
    }
}
