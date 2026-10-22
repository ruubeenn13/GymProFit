package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

// ============================================================
// SemanaNutricionTest — «Tu semana de nutrición» (tablero 12, decisión 13 del lienzo de
// nutrición, GP-210 paso 1): la semana de lunes a domingo, agregada en el cliente con lo que
// ya dan /comidas/.../resumen y /diario. La misma semana del lienzo (Lucía): proteína 140,
// 138, 142, 137, 139, 101 y 107 de 136; entrenos lunes, miércoles y viernes.
// ============================================================
public class SemanaNutricionTest {

    private static final String[] FECHAS = {"2026-09-21", "2026-09-22", "2026-09-23", "2026-09-24",
            "2026-09-25", "2026-09-26", "2026-09-27"};

    private static List<SemanaNutricion.Entrada> lucia() {
        double[] p = {140, 138, 142, 137, 139, 101, 107};
        int[] k = {2405, 2350, 2420, 2290, 2380, 2150, 2175};
        boolean[] entreno = {true, false, true, false, true, false, false};
        boolean[] completo = {true, true, true, true, true, false, false};
        int[] agua = {2250, 1500, 2250, 1750, 2250, 1500, 1750};
        List<SemanaNutricion.Entrada> l = new ArrayList<>();
        for (int i = 0; i < 7; i++) l.add(new SemanaNutricion.Entrada(FECHAS[i], true, p[i], k[i], completo[i], entreno[i], agua[i]));
        return l;
    }

    private static SemanaNutricion semana(List<SemanaNutricion.Entrada> e) {
        return SemanaNutricion.de(e, "2026-10-04", 136, 2397, 2200);
    }

    @Test
    public void la_proteina_de_la_semana_del_lienzo() {
        SemanaNutricion s = semana(lucia());
        assertEquals(7, s.diasQueHanLlegado());
        assertEquals(5, s.proteinaConseguida());
        assertEquals(129, s.mediaProteina().intValue());
    }

    @Test
    public void dias_completos_calorias_y_agua_de_la_semana_del_lienzo() {
        SemanaNutricion s = semana(lucia());
        assertEquals(5, s.diasCompletos());
        assertEquals(2310, s.mediaKcal().intValue());
        assertEquals(96, s.porcentajeKcal());
        assertEquals(1893, s.mediaAguaMl().intValue());
        assertEquals(3, s.diasConAguaCumplida());
    }

    @Test
    public void las_dos_observaciones_del_lienzo() {
        List<SemanaNutricion.Observacion> o = semana(lucia()).observaciones();
        assertEquals(2, o.size());
        assertEquals(SemanaNutricion.Observacion.Tipo.ENTRENO_Y_PROTEINA, o.get(0).tipo);
        assertEquals(3, o.get(0).valor);
        assertEquals(SemanaNutricion.Observacion.Tipo.ENTRE_SEMANA_Y_FIN_DE_SEMANA, o.get(1).tipo);
        // El fin de semana: (101 + 107) / 2 = 104 g.
        assertEquals(104, o.get(1).valor);
    }

    @Test
    public void una_observacion_que_no_pasa_no_se_dice() {
        List<SemanaNutricion.Entrada> e = lucia();
        // El viernes entrenó pero no llegó: ya no «los días que entrenaste, llegaste».
        e.set(4, new SemanaNutricion.Entrada(FECHAS[4], true, 120, 2380, true, true, 2250));
        List<SemanaNutricion.Observacion> o = semana(e).observaciones();
        for (SemanaNutricion.Observacion x : o) {
            assertFalse(x.tipo == SemanaNutricion.Observacion.Tipo.ENTRENO_Y_PROTEINA);
            assertFalse(x.tipo == SemanaNutricion.Observacion.Tipo.ENTRE_SEMANA_Y_FIN_DE_SEMANA);
        }
    }

    @Test
    public void sin_entrenos_no_hay_observacion_de_entreno() {
        List<SemanaNutricion.Entrada> e = new ArrayList<>();
        for (SemanaNutricion.Entrada x : lucia()) {
            e.add(new SemanaNutricion.Entrada(x.fecha, true, x.proteina, x.kcal, x.completo, false, x.aguaMl));
        }
        for (SemanaNutricion.Observacion x : semana(e).observaciones()) {
            assertFalse(x.tipo == SemanaNutricion.Observacion.Tipo.ENTRENO_Y_PROTEINA);
        }
    }

    @Test
    public void una_semana_en_curso_solo_cuenta_los_dias_que_han_llegado() {
        // Hoy es el miércoles: jueves a domingo no han llegado y no restan ni suman.
        SemanaNutricion s = SemanaNutricion.de(lucia(), "2026-09-23", 136, 2397, 2200);
        assertEquals(3, s.diasQueHanLlegado());
        assertEquals(3, s.proteinaConseguida());
        assertEquals(140, s.mediaProteina().intValue());
        // El fin de semana no ha llegado: ni se compara ni se dice nada de él.
        for (SemanaNutricion.Observacion x : s.observaciones()) {
            assertFalse(x.tipo == SemanaNutricion.Observacion.Tipo.ENTRE_SEMANA_Y_FIN_DE_SEMANA);
        }
    }

    @Test
    public void un_dia_sin_nada_apuntado_no_baja_la_media() {
        List<SemanaNutricion.Entrada> e = lucia();
        e.set(1, new SemanaNutricion.Entrada(FECHAS[1], false, 0, 0, false, false, 0));
        SemanaNutricion s = semana(e);
        // La media es de los días con comida: no se cuenta un cero que no se apuntó.
        assertEquals(6, s.diasConDatos());
        assertEquals(128, s.mediaProteina().intValue());
        assertEquals(2303, s.mediaKcal().intValue());
    }

    @Test
    public void una_semana_vacia_no_tiene_medias_ni_porcentajes() {
        List<SemanaNutricion.Entrada> e = new ArrayList<>();
        for (String f : FECHAS) e.add(new SemanaNutricion.Entrada(f, false, 0, 0, false, false, 0));
        SemanaNutricion s = semana(e);
        assertTrue(s.vacia());
        assertNull(s.mediaProteina());
        assertNull(s.mediaKcal());
        assertNull(s.mediaAguaMl());
        assertEquals(0, s.porcentajeKcal());
        assertEquals(0, s.proteinaConseguida());
    }

    @Test
    public void sin_objetivo_hay_medias_pero_nada_que_comparar() {
        // «Prefiero no decirlo» (GP-103): sin peso ni altura no hay objetivo.
        SemanaNutricion s = SemanaNutricion.de(lucia(), "2026-10-04", 0, 0, 0);
        assertEquals(129, s.mediaProteina().intValue());
        assertEquals(0, s.proteinaConseguida());
        assertEquals(0, s.porcentajeKcal());
        assertEquals(0, s.diasConAguaCumplida());
        assertFalse(s.hayObjetivoProteina());
        assertTrue(s.observaciones().isEmpty());
    }
}
