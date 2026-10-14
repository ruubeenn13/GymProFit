package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.alimento.Racion;

// ============================================================
// FilaPlatoTest — la fila de un plato en «Mis platos» (lote 1.6.6, B1)
// ============================================================
public class FilaPlatoTest {

    private static final Locale ES = new Locale("es", "ES");
    private static final Cantidades.Formatos F = new Cantidades.Formatos("%1$s (%2$s g)", "%1$s × %2$s (%3$s g)",
            "%1$s %2$s (%3$s g)", "%1$s g", "%1$s %2$s", "%1$s × %2$s");

    private static Alimento plato(int raciones, int kcal, double prot, Double peso, double gramosRacion) {
        Alimento a = new Alimento();
        a.setNombre("Tortilla de patatas");
        a.setCalorias(kcal);
        a.setProteinas(prot);
        a.setPlatoRaciones(raciones);
        a.setPlatoPesoGramos(peso);
        a.setRaciones(List.of(new RacionConUnidad(gramosRacion)));
        return a;
    }

    /** «1 ración», con su unidad como la manda la API. */
    private static final class RacionConUnidad extends Racion {
        RacionConUnidad(double gramos) {
            super(7, "1 ración", gramos);
        }

        @Override public String getUnidad() { return "ración"; }

        @Override public String getUnidadPlural() { return "raciones"; }
    }

    private static String fila(Alimento a) {
        return FilaPlato.texto(F, "%1$s (1/%2$d)", "%1$s · %2$s kcal · %3$s g prot.", ES, a).replace(' ', ' ');
    }

    @Test
    public void varias_raciones_dicen_que_parte_es() {
        assertEquals("1 ración (1/6) · 220 kcal · 10 g prot.", fila(plato(6, 220, 10, null, 100)));
    }

    @Test
    public void con_una_sola_racion_sin_parentesis() {
        assertEquals("1 ración · 474 kcal · 25 g prot.", fila(plato(1, 474, 25, null, 100)));
    }

    @Test
    public void con_peso_hecho_vale_la_racion_de_verdad_y_tampoco_hay_gramos() {
        // 105 kcal por 100 g y 450 g por ración: 473 kcal, como lo que se apunta.
        assertEquals("1 ración (1/4) · 473 kcal · 25 g prot.", fila(plato(4, 105, 5.56, 1800.0, 450)));
    }
}
