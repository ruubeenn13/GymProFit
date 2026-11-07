package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import es.pmdm.gymprofit.R;

// ============================================================
// CarasTest — «¿Qué tal fue?» (GP-241, punto 3): cinco caras que valen de 1 a 5, con su
// nombre en español y en inglés; sin tocar, NULL (GP-077).
// ============================================================
public class CarasTest {

    private static final String[] NOMBRES = {"cara_fatal", "cara_flojo", "cara_normal", "cara_bien", "cara_genial"};
    private static final String[] ES = {"Fatal", "Flojo", "Normal", "Bien", "Genial"};
    private static final String[] EN = {"Awful", "Poor", "Okay", "Good", "Great"};
    private static final int[] IDS = {R.string.cara_fatal, R.string.cara_flojo, R.string.cara_normal,
            R.string.cara_bien, R.string.cara_genial};

    @Test
    public void de_1_a_5_cada_cara_tiene_su_nombre_en_los_dos_idiomas() {
        Cadenas es = Cadenas.es(), en = Cadenas.en();
        for (int valor = 1; valor <= 5; valor++) {
            assertTrue(Caras.valida(valor));
            assertEquals("la cara " + valor, IDS[valor - 1], Caras.nombre(valor));
            assertEquals(ES[valor - 1], es.s(NOMBRES[valor - 1]));
            assertEquals(EN[valor - 1], en.s(NOMBRES[valor - 1]));
        }
    }

    @Test
    public void sin_tocar_es_null_y_no_tiene_nombre() {
        assertFalse(Caras.valida(null));
        assertEquals(0, Caras.nombre(null));
        assertEquals("0 no es una cara", 0, Caras.nombre(0));
        assertEquals(0, Caras.nombre(6));
    }

    @Test
    public void tocar_una_la_marca_y_tocar_la_marcada_la_quita() {
        assertEquals(Integer.valueOf(4), Caras.alTocar(null, 4));
        assertEquals(Integer.valueOf(2), Caras.alTocar(4, 2));
        assertNull("la marcada otra vez vuelve a NULL", Caras.alTocar(4, 4));
        assertEquals(Integer.valueOf(1), Caras.alTocar(null, 1));
        assertEquals(Integer.valueOf(5), Caras.alTocar(3, 5));
    }

    @Test
    public void lo_que_se_manda_es_el_valor_o_nada() {
        // Lo que viaja a la API es la valoración tal cual: de 1 a 5, o el campo sin mandar.
        for (int valor = 1; valor <= 5; valor++) {
            assertEquals(Integer.valueOf(valor), Caras.alTocar(null, valor));
        }
        assertNull(Caras.alTocar(null, 0));
    }
}
