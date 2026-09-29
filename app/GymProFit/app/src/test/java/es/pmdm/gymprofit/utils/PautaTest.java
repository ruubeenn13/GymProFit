package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ============================================================
// PautaTest — cómo se escribe la pauta de un ejercicio (GP-074 / GP-125, lote 1.2.1).
// Con los formatos de values/strings.xml; el espacio de «s» es duro.
// ============================================================
public class PautaTest {

    private static final Pauta.Formatos ES = new Pauta.Formatos("%1$d–%2$d", "%1$s s", "%1$d × %2$s",
            "%1$s por pierna", "%1$s por brazo", "%1$s por lado");

    @Test
    public void con_rango() {
        assertEquals("3 × 8–12", Pauta.texto(ES, 3, 8, 12, 12, "REPETICIONES", null));
    }

    @Test
    public void sin_rango_como_siempre() {
        assertEquals("3 × 10", Pauta.texto(ES, 3, null, null, 10, null, null));
        // Un rango de un solo número es un número.
        assertEquals("4 × 6", Pauta.texto(ES, 4, 6, 6, 6, "REPETICIONES", null));
    }

    @Test
    public void por_tiempo() {
        assertEquals("3 × 30–60 s", Pauta.texto(ES, 3, 30, 60, 60, "SEGUNDOS", null));
        assertEquals("2 × 45 s", Pauta.texto(ES, 2, null, 45, 45, "SEGUNDOS", null));
    }

    @Test
    public void por_lado() {
        assertEquals("3 × 10–12 por pierna", Pauta.texto(ES, 3, 10, 12, 12, "REPETICIONES", "PIERNA"));
        assertEquals("3 × 10–12 por brazo", Pauta.texto(ES, 3, 10, 12, 12, "REPETICIONES", "BRAZO"));
        assertEquals("2 × 20–40 s por lado", Pauta.texto(ES, 2, 20, 40, 40, "SEGUNDOS", "LADO"));
    }

    @Test
    public void la_pista_del_registro_es_solo_la_cantidad() {
        assertEquals("8–12", Pauta.cantidad(ES, 8, 12, 12, "REPETICIONES"));
        assertEquals("30–60 s", Pauta.cantidad(ES, 30, 60, 60, "SEGUNDOS"));
        assertEquals("10", Pauta.cantidad(ES, null, null, 10, null));
    }
}
