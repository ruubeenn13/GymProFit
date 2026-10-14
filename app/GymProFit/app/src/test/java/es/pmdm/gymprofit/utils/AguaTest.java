package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Locale;

// ============================================================
// AguaTest — el agua, a vasos (lote 1.6.8, GP-166, decisión 11 del lienzo)
// Cuántos vasos se pintan y cuántos van llenos, qué hace tocar cada uno (el último lleno lo
// quita), los topes, el tamaño del vaso y cómo se escriben los litros.
// ============================================================
public class AguaTest {

    @Test
    public void vasos_hacia_el_objetivo_del_plan_redondeando_hacia_arriba() {
        // 2,2 L con vasos de 250 ml son 8,8: nueve vasos, como en el tablero 1.
        assertEquals(9, Agua.vasosObjetivo(2.2, 250));
        assertEquals(8, Agua.vasosObjetivo(2.0, 250));
        assertEquals(7, Agua.vasosObjetivo(2.2, 330));
        assertEquals(0, Agua.vasosObjetivo(0, 250));
    }

    @Test
    public void llenos_redondeando_al_vaso_mas_cercano() {
        assertEquals(0, Agua.vasosLlenos(0, 250));
        assertEquals(5, Agua.vasosLlenos(1250, 250));
        // Cambiar el vaso a media tarde: 750 ml con vasos de 330 son algo más de dos.
        assertEquals(2, Agua.vasosLlenos(750, 330));
        assertEquals(1, Agua.vasosLlenos(125, 250));
        assertEquals(0, Agua.vasosLlenos(124, 250));
    }

    @Test
    public void se_pintan_los_del_objetivo_o_los_bebidos_si_son_mas() {
        assertEquals(9, Agua.vasosAPintar(5, 9));
        assertEquals(11, Agua.vasosAPintar(11, 9));
        // Sin objetivo (sin peso en el perfil): los bebidos y uno vacío para el siguiente.
        assertEquals(1, Agua.vasosAPintar(0, 0));
        assertEquals(4, Agua.vasosAPintar(3, 0));
        // Con tope, para que la fila no crezca sin fin.
        assertEquals(Agua.MAXIMO_VASOS, Agua.vasosAPintar(200, 9));
    }

    @Test
    public void tocar_el_ultimo_lleno_lo_quita_y_uno_vacio_anade() {
        assertEquals(Agua.Toque.QUITAR, Agua.toque(4, 5));
        assertEquals(Agua.Toque.ANADIR, Agua.toque(5, 5));
        assertEquals(Agua.Toque.ANADIR, Agua.toque(8, 5));
        // Uno lleno que no es el último no hace nada: no se vacía media fila de un toque.
        assertEquals(Agua.Toque.NADA, Agua.toque(2, 5));
        assertEquals(Agua.Toque.ANADIR, Agua.toque(0, 0));
    }

    @Test
    public void sumar_y_quitar_un_vaso_con_sus_topes() {
        assertEquals(1500, Agua.mas(1250, 250));
        assertEquals(1000, Agua.menos(1250, 250));
        assertEquals(0, Agua.menos(100, 250));
        assertEquals(0, Agua.menos(0, 250));
        assertEquals(Agua.MAXIMO_ML, Agua.mas(Agua.MAXIMO_ML - 100, 250));
    }

    @Test
    public void el_vaso_es_de_una_lista_cerrada_y_250_por_defecto() {
        assertEquals(250, Agua.VASO_POR_DEFECTO);
        for (int ml : Agua.VASOS) assertTrue(Agua.vasoValido(ml));
        assertFalse(Agua.vasoValido(0));
        assertFalse(Agua.vasoValido(251));
        assertEquals(250, Agua.vasoOPorDefecto(0));
        assertEquals(330, Agua.vasoOPorDefecto(330));
        assertEquals(250, Agua.vasoOPorDefecto(9999));
    }

    @Test
    public void los_litros_con_coma_en_espanol_y_punto_en_ingles_sin_ceros_de_mas() {
        Locale es = new Locale("es", "ES");
        assertEquals("1,25", Agua.litros(1250, es));
        assertEquals("1,5", Agua.litros(1500, es));
        assertEquals("2", Agua.litros(2000, es));
        assertEquals("0", Agua.litros(0, es));
        assertEquals("2,2", Agua.litrosObjetivo(2.2, es));
        assertEquals("1.25", Agua.litros(1250, Locale.ENGLISH));
    }
}
