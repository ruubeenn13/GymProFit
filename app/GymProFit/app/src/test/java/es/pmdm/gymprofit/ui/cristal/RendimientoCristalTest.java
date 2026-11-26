package es.pmdm.gymprofit.ui.cristal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// RendimientoCristalTest — GP-285, DEC-070: el interruptor de rendimiento del cristal vivo.
//
// Baja de nivel solo ante un tirón sostenido: varias ventanas seguidas de fotogramas con una
// parte clara de ellos perdidos. Un tirón suelto (una carga, el recolector) no cuenta.
// ============================================================
public class RendimientoCristalTest {

    private static final long BIEN = 12_000_000L;   // 12 ms
    private static final long MAL = 50_000_000L;    // 50 ms: más de dos fotogramas de 60 Hz

    // Una ventana entera con esa parte de fotogramas perdidos, repartidos. Devuelve si pidió bajar.
    private static boolean ventana(RendimientoCristal r, double parteMala) {
        boolean bajar = false;
        int malos = (int) Math.round(RendimientoCristal.VENTANA * parteMala);
        for (int i = 0; i < RendimientoCristal.VENTANA; i++) {
            bajar |= r.fotograma(i < malos ? MAL : BIEN);
        }
        return bajar;
    }

    @Test
    public void un_scroll_fluido_nunca_pide_bajar() {
        RendimientoCristal r = new RendimientoCristal();
        for (int v = 0; v < 50; v++) assertFalse(ventana(r, 0.0));
    }

    @Test
    public void algun_fotograma_perdido_no_pide_bajar() {
        RendimientoCristal r = new RendimientoCristal();
        // Por debajo del umbral, por muchas ventanas que pasen.
        for (int v = 0; v < 50; v++) assertFalse(ventana(r, RendimientoCristal.UMBRAL - 0.05));
    }

    @Test
    public void un_tiron_suelto_no_pide_bajar() {
        RendimientoCristal r = new RendimientoCristal();
        for (int v = 0; v < RendimientoCristal.VENTANAS_MALAS - 1; v++) assertFalse(ventana(r, 0.9));
        // Una ventana buena en medio corta la racha.
        assertFalse(ventana(r, 0.0));
        for (int v = 0; v < RendimientoCristal.VENTANAS_MALAS - 1; v++) assertFalse(ventana(r, 0.9));
    }

    @Test
    public void un_tiron_sostenido_pide_bajar_una_vez() {
        RendimientoCristal r = new RendimientoCristal();
        for (int v = 0; v < RendimientoCristal.VENTANAS_MALAS - 1; v++) assertFalse(ventana(r, 0.5));
        assertTrue(ventana(r, 0.5));
        // Tras pedirlo empieza de cero: el nivel nuevo se mide desde el principio.
        for (int v = 0; v < RendimientoCristal.VENTANAS_MALAS - 1; v++) assertFalse(ventana(r, 0.5));
        assertTrue(ventana(r, 0.5));
    }

    @Test
    public void el_limite_de_fotograma_perdido_son_dos_de_60_hz() {
        RendimientoCristal r = new RendimientoCristal();
        // Justo en el límite no es un fotograma perdido.
        for (int v = 0; v < 10; v++) {
            for (int i = 0; i < RendimientoCristal.VENTANA; i++) {
                assertFalse(r.fotograma(RendimientoCristal.PERDIDO_NS));
            }
        }
    }
}
