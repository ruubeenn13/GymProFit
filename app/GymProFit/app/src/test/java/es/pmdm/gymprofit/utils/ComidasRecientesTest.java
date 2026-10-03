package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.model.comida.ComidaReciente;

// ============================================================
// ComidasRecientesTest — copiar una comida (lote 1.6.4, B1 y B2), sin vistas
// Cómo se llama cada comida reciente, cuál es «la de ayer» y lo descartado con la ✗.
// ============================================================
public class ComidasRecientesTest {

    // Jueves, 1 de octubre de 2026.
    private static Calendar hoy() {
        return ComidasRecientes.dia("2026-10-01");
    }

    @Test
    public void hoy_ayer_la_semana_y_la_fecha() {
        assertEquals(ComidasRecientes.Cuando.HOY, ComidasRecientes.cuando("2026-10-01", hoy()));
        assertEquals(ComidasRecientes.Cuando.AYER, ComidasRecientes.cuando("2026-09-30", hoy()));
        assertEquals(ComidasRecientes.Cuando.SEMANA, ComidasRecientes.cuando("2026-09-29", hoy()));
        assertEquals(ComidasRecientes.Cuando.SEMANA, ComidasRecientes.cuando("2026-09-25", hoy()));
        // Hace una semana ya no es «del jueves»: sería el mismo nombre que hoy.
        assertEquals(ComidasRecientes.Cuando.FECHA, ComidasRecientes.cuando("2026-09-24", hoy()));
        assertEquals(ComidasRecientes.Cuando.FECHA, ComidasRecientes.cuando("2026-10-02", hoy()));
        assertEquals(ComidasRecientes.Cuando.FECHA, ComidasRecientes.cuando("basura", hoy()));
    }

    @Test
    public void ayer_cruza_meses_y_el_cambio_de_hora() {
        assertEquals("2026-09-30", ComidasRecientes.diaAnterior("2026-10-01"));
        assertEquals("2025-12-31", ComidasRecientes.diaAnterior("2026-01-01"));
        // El 25 de octubre de 2026 tiene 25 horas en España.
        assertEquals("2026-10-25", ComidasRecientes.diaAnterior("2026-10-26"));
        Calendar tras = ComidasRecientes.dia("2026-10-26");
        tras.set(Calendar.HOUR_OF_DAY, 0);
        assertEquals(ComidasRecientes.Cuando.AYER, ComidasRecientes.cuando("2026-10-25", tras));
        assertNull(ComidasRecientes.diaAnterior(null));
    }

    @Test
    public void la_de_ayer_es_la_del_mismo_tipo_del_dia_anterior() {
        ComidaReciente ayer = reciente("MERIENDA", "2026-09-30", "Pan");
        List<ComidaReciente> r = Arrays.asList(ayer, reciente("COMIDA", "2026-10-01", "Arroz"));
        assertSame(ayer, ComidasRecientes.deAyer(r, "MERIENDA", "2026-10-01"));
        // La de hace dos días no es la de ayer.
        assertNull(ComidasRecientes.deAyer(Collections.singletonList(reciente("MERIENDA", "2026-09-29", "Pan")),
                "MERIENDA", "2026-10-01"));
        // Ni otra comida de ayer.
        assertNull(ComidasRecientes.deAyer(Collections.singletonList(reciente("CENA", "2026-09-30", "Pan")),
                "MERIENDA", "2026-10-01"));
        assertNull(ComidasRecientes.deAyer(null, "MERIENDA", "2026-10-01"));
    }

    @Test
    public void la_de_ayer_cuenta_desde_el_dia_de_la_comida_no_desde_hoy() {
        ComidaReciente anteayer = reciente("CENA", "2026-09-27", "Pollo");
        assertSame(anteayer, ComidasRecientes.deAyer(Collections.singletonList(anteayer), "CENA", "2026-09-28"));
    }

    @Test
    public void sin_alimentos_no_hay_nada_que_copiar() {
        ComidaReciente vacia = new ComidaReciente(1, "MERIENDA", "2026-09-30", 0, new ArrayList<>());
        assertNull(ComidasRecientes.deAyer(Collections.singletonList(vacia), "MERIENDA", "2026-10-01"));
    }

    @Test
    public void los_nombres_van_en_su_orden() {
        assertEquals("Pan integral, Pechuga de pavo",
                ComidasRecientes.nombres(reciente("MERIENDA", "2026-09-30", "Pan integral", "Pechuga de pavo").getLineas()));
    }

    @Test
    public void lo_descartado_se_guarda_por_dia_y_comida_y_se_poda() {
        Set<String> s = ComidasRecientes.descartar(null, "2026-10-01", "MERIENDA");
        assertTrue(s.contains(ComidasRecientes.claveDescarte("2026-10-01", "MERIENDA")));
        assertFalse(s.contains(ComidasRecientes.claveDescarte("2026-10-01", "CENA")));
        assertFalse(s.contains(ComidasRecientes.claveDescarte("2026-10-02", "MERIENDA")));
        // Descartar dos veces no duplica.
        assertEquals(1, ComidasRecientes.descartar(s, "2026-10-01", "MERIENDA").size());

        Set<String> muchos = new HashSet<>();
        for (int i = 1; i <= ComidasRecientes.MAXIMO_DESCARTES; i++) {
            muchos.add(ComidasRecientes.claveDescarte(String.format("2026-%02d-%02d", 1 + i / 28, 1 + i % 28), "CENA"));
        }
        Set<String> podado = ComidasRecientes.descartar(muchos, "2026-10-01", "CENA");
        assertEquals(ComidasRecientes.MAXIMO_DESCARTES, podado.size());
        assertTrue(podado.contains(ComidasRecientes.claveDescarte("2026-10-01", "CENA")));
        // Se va el más viejo.
        assertFalse(podado.contains(ComidasRecientes.claveDescarte("2026-01-02", "CENA")));
    }

    private static ComidaReciente reciente(String tipo, String fecha, String... nombres) {
        List<AlimentoComida> lineas = new ArrayList<>();
        for (String n : nombres) {
            AlimentoComida l = new AlimentoComida();
            l.setNombreAlimento(n);
            lineas.add(l);
        }
        return new ComidaReciente(1, tipo, fecha, 100, lineas);
    }
}
