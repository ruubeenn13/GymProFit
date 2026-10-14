package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import es.pmdm.gymprofit.model.comida.DiaDiario;

// ============================================================
// DiaCompletoTest — el día completo (lote 1.6.8, decisión 10 del lienzo, tablero 3)
// Completo es que las cinco comidas tengan algo apuntado o estén marcadas «Hoy no», sin
// mirar las calorías. El punto de cada día de la semana, cuántos completos lleva la semana
// y qué frase sale debajo. Sin rachas: un día a medias no rompe nada.
// ============================================================
public class DiaCompletoTest {

    private static DiaDiario dia(String fecha, List<String> apuntadas, List<String> hoyNo, int entrenos) {
        DiaDiario d = new DiaDiario();
        d.setFecha(fecha);
        d.setComidasApuntadas(apuntadas);
        d.setHoyNo(hoyNo);
        d.setEntrenos(entrenos);
        return d;
    }

    private static List<String> l(String... s) {
        return new ArrayList<>(Arrays.asList(s));
    }

    private static final List<String> NADA = Collections.emptyList();

    // ── Completo ────────────────────────────────────────────────────────────

    @Test
    public void las_cinco_apuntadas_es_completo() {
        assertTrue(DiaCompleto.completo(l("DESAYUNO", "ALMUERZO", "COMIDA", "MERIENDA", "CENA"), NADA));
    }

    @Test
    public void hoy_no_cuenta_como_apuntada() {
        assertTrue(DiaCompleto.completo(l("DESAYUNO", "COMIDA", "CENA"), l("ALMUERZO", "MERIENDA")));
    }

    @Test
    public void con_una_que_falta_no_es_completo() {
        assertFalse(DiaCompleto.completo(l("DESAYUNO", "ALMUERZO", "COMIDA", "CENA"), NADA));
        assertFalse(DiaCompleto.completo(NADA, NADA));
        assertFalse(DiaCompleto.completo(null, null));
    }

    @Test
    public void todas_hoy_no_tambien_es_completo() {
        // Un día que no se come nada apuntable (ayuno, viaje) se puede cerrar igual.
        assertTrue(DiaCompleto.completo(NADA, l("DESAYUNO", "ALMUERZO", "COMIDA", "MERIENDA", "CENA")));
    }

    @Test
    public void apuntada_y_hoy_no_a_la_vez_cuenta_una_vez() {
        assertFalse(DiaCompleto.completo(l("DESAYUNO", "ALMUERZO"), l("DESAYUNO", "ALMUERZO", "COMIDA", "CENA")));
    }

    @Test
    public void lo_que_no_es_del_diario_no_cuenta() {
        assertFalse(DiaCompleto.completo(l("DESAYUNO", "ALMUERZO", "COMIDA", "SNACK", "CENA"), NADA));
    }

    @Test
    public void cuantas_faltan() {
        assertEquals(5, DiaCompleto.faltan(NADA, NADA));
        assertEquals(2, DiaCompleto.faltan(l("DESAYUNO", "COMIDA"), l("CENA")));
        assertEquals(0, DiaCompleto.faltan(l("DESAYUNO", "ALMUERZO", "COMIDA"), l("MERIENDA", "CENA")));
    }

    // ── El punto de cada día ────────────────────────────────────────────────

    @Test
    public void el_punto_de_un_dia_completo_es_verde() {
        DiaDiario d = dia("2026-10-12", l("DESAYUNO", "ALMUERZO", "COMIDA"), l("MERIENDA", "CENA"), 0);
        assertEquals(DiaCompleto.Punto.COMPLETO, DiaCompleto.punto(d, false, false));
        assertEquals(DiaCompleto.Punto.COMPLETO, DiaCompleto.punto(d, true, false));
    }

    @Test
    public void un_dia_pasado_con_algo_esta_a_medias_y_hoy_en_curso() {
        DiaDiario d = dia("2026-10-13", l("DESAYUNO"), NADA, 0);
        assertEquals(DiaCompleto.Punto.A_MEDIAS, DiaCompleto.punto(d, false, false));
        assertEquals(DiaCompleto.Punto.EN_CURSO, DiaCompleto.punto(d, true, false));
        DiaDiario soloHoyNo = dia("2026-10-13", NADA, l("CENA"), 0);
        assertEquals(DiaCompleto.Punto.A_MEDIAS, DiaCompleto.punto(soloHoyNo, false, false));
    }

    @Test
    public void sin_nada_no_hay_punto_ni_hoy_ni_antes() {
        DiaDiario d = dia("2026-10-13", NADA, NADA, 1);
        assertEquals(DiaCompleto.Punto.NINGUNO, DiaCompleto.punto(d, false, false));
        assertEquals(DiaCompleto.Punto.NINGUNO, DiaCompleto.punto(d, true, false));
        assertEquals(DiaCompleto.Punto.NINGUNO, DiaCompleto.punto(null, false, false));
    }

    @Test
    public void un_dia_que_no_ha_llegado_no_tiene_punto() {
        DiaDiario d = dia("2026-10-20", l("DESAYUNO", "ALMUERZO", "COMIDA", "MERIENDA", "CENA"), NADA, 0);
        assertEquals(DiaCompleto.Punto.NINGUNO, DiaCompleto.punto(d, false, true));
    }

    // ── La semana ───────────────────────────────────────────────────────────

    @Test
    public void cuenta_los_completos_de_la_semana_sin_rachas() {
        List<DiaDiario> semana = new ArrayList<>();
        semana.add(dia("2026-10-12", l("DESAYUNO", "ALMUERZO", "COMIDA", "MERIENDA", "CENA"), NADA, 1));
        semana.add(dia("2026-10-13", l("DESAYUNO"), NADA, 0));
        semana.add(dia("2026-10-14", l("DESAYUNO", "ALMUERZO", "COMIDA"), l("MERIENDA", "CENA"), 1));
        semana.add(null);
        assertEquals(2, DiaCompleto.completosEnSemana(semana));
        assertEquals(0, DiaCompleto.completosEnSemana(null));
    }

    @Test
    public void la_frase_dice_lo_que_de_verdad_hubo_ese_dia() {
        assertEquals(DiaCompleto.Frase.HOY_ENTRENO_Y_PROTEINA, DiaCompleto.frase(true, true, true));
        assertEquals(DiaCompleto.Frase.HOY_ENTRENO, DiaCompleto.frase(true, true, false));
        assertEquals(DiaCompleto.Frase.HOY_PROTEINA, DiaCompleto.frase(true, false, true));
        assertEquals(DiaCompleto.Frase.HOY, DiaCompleto.frase(true, false, false));
        // Otro día no es «hoy»: la frase no lo dice.
        assertEquals(DiaCompleto.Frase.ESE_DIA_ENTRENO_Y_PROTEINA, DiaCompleto.frase(false, true, true));
        assertEquals(DiaCompleto.Frase.ESE_DIA_ENTRENO, DiaCompleto.frase(false, true, false));
        assertEquals(DiaCompleto.Frase.ESE_DIA_PROTEINA, DiaCompleto.frase(false, false, true));
        assertEquals(DiaCompleto.Frase.ESE_DIA, DiaCompleto.frase(false, false, false));
    }

    // ── Celebrar una vez ────────────────────────────────────────────────────

    @Test
    public void se_celebra_al_pasar_a_completo_y_una_vez_al_dia() {
        assertTrue(DiaCompleto.celebrar(Boolean.FALSE, true, "2026-10-14", null));
        assertTrue(DiaCompleto.celebrar(Boolean.FALSE, true, "2026-10-14", "2026-10-13"));
        // Ya celebrado ese día: no se repite aunque se quite y vuelva a completarse.
        assertFalse(DiaCompleto.celebrar(Boolean.FALSE, true, "2026-10-14", "2026-10-14"));
        // Abrir un día que ya estaba completo no es completarlo.
        assertFalse(DiaCompleto.celebrar(null, true, "2026-10-14", null));
        assertFalse(DiaCompleto.celebrar(Boolean.TRUE, true, "2026-10-14", null));
        assertFalse(DiaCompleto.celebrar(Boolean.FALSE, false, "2026-10-14", null));
    }
}
