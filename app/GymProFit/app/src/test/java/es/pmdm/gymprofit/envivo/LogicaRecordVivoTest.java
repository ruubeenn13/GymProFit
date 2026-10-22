package es.pmdm.gymprofit.envivo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso.Marca;

// ============================================================
// LogicaRecordVivoTest — el récord en mitad de la serie (GP-210 paso 3, decisión 4 de la
// firma visual): la regla de GP-088 (igualar no es récord, la primera marca no es un
// récord, con peso y sin peso son marcas distintas) y que se celebra UNA VEZ por
// ejercicio y sesión.
// ============================================================
public class LogicaRecordVivoTest {

    private static Marca peso(String kilos, int reps) {
        return new Marca("PESO", kilos, reps, 0);
    }

    private static SesionEnCurso.Ejercicio ejercicio(int id, int series, Marca marca) {
        SesionEnCurso.Ejercicio e = new SesionEnCurso.Ejercicio();
        e.id = id;
        e.ejercicioId = id;
        e.deRutina = true;
        e.marcaCargada = true;
        e.marca = marca;
        for (int i = 0; i < series; i++) {
            SesionEnCurso.Serie s = new SesionEnCurso.Serie();
            s.id = id * 100L + i;
            e.series.add(s);
        }
        return e;
    }

    /** Escribe la serie i y la da por hecha, como hace marcar. */
    private static long hecha(SesionEnCurso.Ejercicio e, int i, String kilos, String reps) {
        SesionEnCurso.Serie s = e.series.get(i);
        s.peso = kilos;
        s.repeticiones = reps;
        s.hecha = true;
        return s.id;
    }

    // ── La regla de GP-088 ───────────────────────────────────

    @Test
    public void mas_peso_supera() {
        assertTrue(LogicaRecordVivo.supera(peso("85", 5), peso("82.5", 8)));
    }

    @Test
    public void a_igual_peso_supera_con_mas_repeticiones() {
        assertTrue(LogicaRecordVivo.supera(peso("80", 9), peso("80", 8)));
    }

    @Test
    public void igualar_no_es_record() {
        assertFalse(LogicaRecordVivo.supera(peso("80", 8), peso("80", 8)));
    }

    @Test
    public void menos_peso_con_mas_repeticiones_no_supera() {
        assertFalse(LogicaRecordVivo.supera(peso("77.5", 12), peso("80", 8)));
    }

    @Test
    public void sin_peso_manda_el_mayor_numero_de_repeticiones() {
        Marca a = new Marca("REPETICIONES", null, 12, 0), b = new Marca("REPETICIONES", null, 11, 0);
        assertTrue(LogicaRecordVivo.supera(a, b));
        assertFalse(LogicaRecordVivo.supera(b, a));
        assertFalse(LogicaRecordVivo.supera(a, a));
    }

    @Test
    public void por_tiempo_manda_la_serie_mas_larga() {
        Marca larga = new Marca("TIEMPO", null, 0, 75), corta = new Marca("TIEMPO", null, 0, 60);
        assertTrue(LogicaRecordVivo.supera(larga, corta));
        assertFalse(LogicaRecordVivo.supera(corta, larga));
    }

    @Test
    public void con_peso_y_sin_peso_son_marcas_distintas() {
        Marca lastrada = peso("10", 5), libre = new Marca("REPETICIONES", null, 20, 0);
        assertFalse(LogicaRecordVivo.supera(libre, lastrada));
        assertFalse(LogicaRecordVivo.supera(lastrada, libre));
    }

    @Test
    public void la_marca_sale_de_lo_escrito_con_coma_o_con_punto() {
        SesionEnCurso.Ejercicio e = ejercicio(1, 1, null);
        hecha(e, 0, "82,5", "8");
        Marca m = LogicaRecordVivo.deSerie(e, e.series.get(0));
        assertNotNull(m);
        assertEquals("PESO", m.tipo);
        assertEquals(0, new java.math.BigDecimal("82.5").compareTo(new java.math.BigDecimal(m.peso)));
        assertEquals(8, m.repeticiones);
    }

    @Test
    public void una_serie_sin_peso_es_de_repeticiones_y_una_vacia_no_es_marca() {
        SesionEnCurso.Ejercicio e = ejercicio(1, 2, null);
        hecha(e, 0, "", "12");
        assertEquals("REPETICIONES", LogicaRecordVivo.deSerie(e, e.series.get(0)).tipo);
        assertNull(LogicaRecordVivo.deSerie(e, e.series.get(1)));
    }

    // ── Una vez por ejercicio y sesión ───────────────────────

    @Test
    public void la_serie_que_bate_la_marca_se_celebra() {
        SesionEnCurso.Ejercicio e = ejercicio(1, 4, peso("82.5", 8));
        long id = hecha(e, 0, "85", "7");

        Marca nueva = LogicaRecordVivo.alMarcar(e, id);

        assertNotNull(nueva);
        assertEquals(7, nueva.repeticiones);
        assertEquals(Long.valueOf(id), e.recordSerieId);
    }

    @Test
    public void una_segunda_serie_mejor_no_se_vuelve_a_celebrar() {
        SesionEnCurso.Ejercicio e = ejercicio(1, 4, peso("82.5", 8));
        assertNotNull(LogicaRecordVivo.alMarcar(e, hecha(e, 0, "85", "7")));

        long segunda = hecha(e, 1, "87.5", "6");

        assertNull("ya se celebró este ejercicio en esta sesión", LogicaRecordVivo.alMarcar(e, segunda));
        assertEquals("la celebrada sigue siendo la primera", Long.valueOf(100L), e.recordSerieId);
    }

    @Test
    public void desmarcar_y_volver_a_marcar_no_celebra_otra_vez() {
        SesionEnCurso.Ejercicio e = ejercicio(1, 4, peso("82.5", 8));
        long id = hecha(e, 0, "85", "7");
        assertNotNull(LogicaRecordVivo.alMarcar(e, id));

        e.series.get(0).hecha = false;
        e.series.get(0).hecha = true;

        assertNull(LogicaRecordVivo.alMarcar(e, id));
    }

    @Test
    public void cada_ejercicio_celebra_el_suyo() {
        SesionEnCurso.Ejercicio a = ejercicio(1, 2, peso("80", 8));
        SesionEnCurso.Ejercicio b = ejercicio(2, 2, peso("100", 5));
        assertNotNull(LogicaRecordVivo.alMarcar(a, hecha(a, 0, "82.5", "8")));
        assertNotNull(LogicaRecordVivo.alMarcar(b, hecha(b, 0, "102.5", "5")));
    }

    @Test
    public void igualar_la_marca_no_se_celebra_y_deja_la_celebracion_para_despues() {
        SesionEnCurso.Ejercicio e = ejercicio(1, 4, peso("82.5", 8));
        assertNull(LogicaRecordVivo.alMarcar(e, hecha(e, 0, "82.5", "8")));
        assertNull(e.recordSerieId);

        assertNotNull("la que bate de verdad, después, sí", LogicaRecordVivo.alMarcar(e, hecha(e, 1, "82.5", "9")));
    }

    @Test
    public void sin_historial_la_primera_marca_no_es_record() {
        SesionEnCurso.Ejercicio e = ejercicio(1, 3, null);
        assertNull(LogicaRecordVivo.alMarcar(e, hecha(e, 0, "60", "10")));
        assertNull("ni la siguiente, que mejora la de hoy", LogicaRecordVivo.alMarcar(e, hecha(e, 1, "65", "10")));
        assertNull(e.recordSerieId);
    }

    @Test
    public void sin_saber_la_marca_no_se_celebra_nada() {
        SesionEnCurso.Ejercicio e = ejercicio(1, 2, peso("50", 5));
        e.marcaCargada = false;
        assertNull(LogicaRecordVivo.alMarcar(e, hecha(e, 0, "200", "5")));
    }

    @Test
    public void una_serie_de_otro_tipo_no_bate_la_marca() {
        SesionEnCurso.Ejercicio e = ejercicio(1, 2, peso("10", 5));
        assertNull(LogicaRecordVivo.alMarcar(e, hecha(e, 0, "", "30")));
    }

    // ── Lo que enseña «Tu récord» ────────────────────────────

    @Test
    public void tu_record_es_el_de_siempre_hasta_que_hoy_lo_bate() {
        SesionEnCurso.Ejercicio e = ejercicio(1, 4, peso("82.5", 8));
        assertEquals("82.5", LogicaRecordVivo.mostrada(e).peso);
        assertFalse(LogicaRecordVivo.esDeHoy(e));

        hecha(e, 0, "80", "8");
        assertEquals("una serie peor no cambia nada", "82.5", LogicaRecordVivo.mostrada(e).peso);

        LogicaRecordVivo.alMarcar(e, hecha(e, 1, "85", "7"));
        assertEquals("85", LogicaRecordVivo.mostrada(e).peso);
        assertTrue(LogicaRecordVivo.esDeHoy(e));
    }

    @Test
    public void tras_el_record_manda_la_mejor_serie_de_hoy() {
        SesionEnCurso.Ejercicio e = ejercicio(1, 4, peso("82.5", 8));
        LogicaRecordVivo.alMarcar(e, hecha(e, 0, "85", "7"));
        hecha(e, 1, "87.5", "6");
        assertEquals("87.5", LogicaRecordVivo.mostrada(e).peso);
    }

    @Test
    public void sin_historial_no_hay_record_que_enseñar() {
        SesionEnCurso.Ejercicio e = ejercicio(1, 2, null);
        hecha(e, 0, "60", "10");
        assertNull("«Primera vez: es tu punto de partida»", LogicaRecordVivo.mostrada(e));
    }

    // ── Pedir las marcas ─────────────────────────────────────

    @Test
    public void solo_se_piden_las_marcas_de_quien_tiene_historial() {
        SesionEnCurso s = new SesionEnCurso();
        SesionEnCurso.Ejercicio conHistorial = ejercicio(1, 1, null);
        conHistorial.marcaCargada = false;
        conHistorial.anteriorCargado = true;
        conHistorial.anterior = Collections.singletonList(new SesionEnCurso.SerieAnterior(1, "80", 8, null));
        SesionEnCurso.Ejercicio nuevo = ejercicio(2, 1, null);
        nuevo.marcaCargada = false;
        nuevo.anteriorCargado = true;
        SesionEnCurso.Ejercicio sinSaber = ejercicio(3, 1, null);
        sinSaber.marcaCargada = false;
        s.ejercicios = Arrays.asList(conHistorial, nuevo, sinSaber);

        assertEquals(Collections.singletonList(1), LogicaRecordVivo.sinMarca(s));
        assertTrue("el que no tiene historial ya se sabe: no hay marca", nuevo.marcaCargada);
        assertFalse("sin la última vez no se sabe", sinSaber.marcaCargada);
    }

    @Test
    public void la_respuesta_rellena_las_marcas_y_los_que_faltan_quedan_sin_marca() {
        SesionEnCurso s = new SesionEnCurso();
        SesionEnCurso.Ejercicio a = ejercicio(1, 1, null), b = ejercicio(2, 1, null);
        a.marcaCargada = false;
        b.marcaCargada = false;
        s.ejercicios = Arrays.asList(a, b);
        Map<Integer, Marca> marcas = new HashMap<>();
        marcas.put(1, peso("90", 5));

        LogicaRecordVivo.aplicarMarcas(s, Arrays.asList(1, 2), marcas);

        assertTrue(a.marcaCargada);
        assertEquals("90", a.marca.peso);
        assertTrue(b.marcaCargada);
        assertNull(b.marca);
    }

    @Test
    public void una_respuesta_nula_no_cuenta_como_saber() {
        SesionEnCurso s = new SesionEnCurso();
        SesionEnCurso.Ejercicio a = ejercicio(1, 1, null);
        a.marcaCargada = false;
        s.ejercicios = Collections.singletonList(a);

        LogicaRecordVivo.aplicarMarcas(s, Collections.singletonList(1), null);

        assertFalse("sin respuesta se vuelve a preguntar", a.marcaCargada);
    }
}
