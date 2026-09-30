package es.pmdm.gymprofit.envivo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.envivo.UltimaVez;

// ============================================================
// LogicaSesionTest — las reglas de la sesión en vivo (GP-012, GP-014): el reloj y la
// duración, «Anterior» y las pistas desde /sesiones/ultima-vez, marcar como confirmar
// y qué se manda al guardar (solo lo marcado, renumerado, sin ejercicios vacíos).
// ============================================================
public class LogicaSesionTest {

    private static final Locale ES = new Locale("es", "ES");
    private static final TimeZone MADRID = TimeZone.getTimeZone("Europe/Madrid");

    /** Textos de prueba: los de verdad salen de strings.xml. */
    private static final LogicaSesion.Textos T = new LogicaSesion.Textos() {
        @Override public String pesoPorReps(String kilos, int reps) { return kilos + " × " + reps; }
        @Override public String reps(int reps) { return reps + " reps"; }
        @Override public String rango(int min, int max) { return min + "–" + max; }
        @Override public String segundos(String numero) { return numero + " s"; }
        @Override public String nada() { return "—"; }
    };

    private static SesionEnCurso.Ejercicio ejercicio(int id, int series) {
        SesionEnCurso.Ejercicio e = new SesionEnCurso.Ejercicio();
        e.id = id;
        e.ejercicioId = id;
        e.deRutina = true;
        for (int i = 0; i < series; i++) {
            SesionEnCurso.Serie s = new SesionEnCurso.Serie();
            s.id = id * 100L + i;
            e.series.add(s);
        }
        return e;
    }

    private static UltimaVez ultima(int ejercicioId, UltimaVez.Serie... series) {
        UltimaVez u = new UltimaVez();
        u.ejercicioId = ejercicioId;
        u.fecha = "2026-09-20T18:00:00";
        u.series = Arrays.asList(series);
        return u;
    }

    private static UltimaVez.Serie serie(int numero, String peso, Integer reps, Integer segundos) {
        UltimaVez.Serie s = new UltimaVez.Serie();
        s.numero = numero;
        s.peso = peso != null ? new BigDecimal(peso) : null;
        s.repeticiones = reps;
        s.segundos = segundos;
        return s;
    }

    // ── Reloj ────────────────────────────────────────────────

    @Test
    public void el_reloj_cuenta_desde_el_inicio() {
        long inicio = 1_000_000L;
        assertEquals("0:00", LogicaSesion.reloj(inicio, inicio));
        assertEquals("23:14", LogicaSesion.reloj(inicio, inicio + (23 * 60 + 14) * 1000L));
        assertEquals("1:02:03", LogicaSesion.reloj(inicio, inicio + (3600 + 2 * 60 + 3) * 1000L));
    }

    @Test
    public void la_duracion_son_los_minutos_enteros_entre_1_y_600() {
        long inicio = 0;
        assertEquals("nada más empezar, 1", 1, LogicaSesion.minutosReloj(inicio, 20_000));
        assertEquals(43, LogicaSesion.minutosReloj(inicio, 43 * 60_000L + 59_000));
        assertEquals(600, LogicaSesion.minutosReloj(inicio, 11 * 3_600_000L));
    }

    @Test
    public void los_segundos_se_teclean_en_segundos_o_en_minutos_y_segundos() {
        assertEquals(Integer.valueOf(40), LogicaSesion.segundosDe("40"));
        assertEquals(Integer.valueOf(40), LogicaSesion.segundosDe("0:40"));
        assertEquals(Integer.valueOf(65), LogicaSesion.segundosDe("1:05"));
        assertNull(LogicaSesion.segundosDe("1:5"));
        assertNull(LogicaSesion.segundosDe("0"));
        assertNull(LogicaSesion.segundosDe("3601"));
        assertNull(LogicaSesion.segundosDe(""));
    }

    // ── Anterior y pistas (GP-014) ───────────────────────────

    @Test
    public void anterior_es_la_serie_del_mismo_numero_de_la_ultima_vez() {
        SesionEnCurso s = new SesionEnCurso();
        SesionEnCurso.Ejercicio e = ejercicio(12, 3);
        s.ejercicios.add(e);

        LogicaSesion.aplicarUltimaVez(s, Collections.singletonList(12), Collections.singletonList(
                ultima(12, serie(1, "57.50", 12, null), serie(2, "60.00", 10, null))));

        assertEquals("57,5 × 12", LogicaSesion.textoAnterior(e, 1, T, ES));
        assertEquals("60 × 10", LogicaSesion.textoAnterior(e, 2, T, ES));
        assertEquals("sin tercera la última vez", "—", LogicaSesion.textoAnterior(e, 3, T, ES));
        assertEquals("57.5 × 12", LogicaSesion.textoAnterior(e, 1, T, Locale.US));
    }

    @Test
    public void anterior_por_tiempo_y_sin_peso() {
        SesionEnCurso s = new SesionEnCurso();
        SesionEnCurso.Ejercicio plancha = ejercicio(7, 1);
        SesionEnCurso.Ejercicio dominadas = ejercicio(8, 1);
        s.ejercicios.add(plancha);
        s.ejercicios.add(dominadas);

        LogicaSesion.aplicarUltimaVez(s, Arrays.asList(7, 8), Arrays.asList(
                ultima(7, serie(1, null, 0, 40)), ultima(8, serie(1, null, 8, null))));

        assertEquals("0:40", LogicaSesion.textoAnterior(plancha, 1, T, ES));
        assertEquals("8 reps", LogicaSesion.textoAnterior(dominadas, 1, T, ES));
        assertTrue("sin pauta, lo dice la última vez", LogicaSesion.porTiempo(plancha));
        assertFalse(LogicaSesion.porTiempo(dominadas));
    }

    @Test
    public void los_pistas_son_lo_de_la_ultima_vez() {
        SesionEnCurso s = new SesionEnCurso();
        SesionEnCurso.Ejercicio e = ejercicio(12, 2);
        e.minimo = 8;
        e.maximo = 12;
        s.ejercicios.add(e);
        LogicaSesion.aplicarUltimaVez(s, Collections.singletonList(12), Collections.singletonList(
                ultima(12, serie(1, "57.50", 12, null))));

        assertEquals("57,5", LogicaSesion.pistaPeso(e, 1, ES));
        assertEquals("12", LogicaSesion.pistaReps(e, 1, T));
        assertEquals("sin anterior, sin pista de kilos", "", LogicaSesion.pistaPeso(e, 2, ES));
        assertEquals("sin anterior, el rango de la pauta", "8–12", LogicaSesion.pistaReps(e, 2, T));
    }

    @Test
    public void sin_ninguna_vez_es_la_primera_y_nunca_ceros() {
        SesionEnCurso s = new SesionEnCurso();
        SesionEnCurso.Ejercicio e = ejercicio(12, 1);
        e.repeticionesPauta = 10;
        s.ejercicios.add(e);
        assertFalse("sin preguntar todavía, no se sabe", LogicaSesion.primeraVez(e));

        LogicaSesion.aplicarUltimaVez(s, Collections.singletonList(12), Collections.emptyList());

        assertTrue(LogicaSesion.primeraVez(e));
        assertEquals("—", LogicaSesion.textoAnterior(e, 1, T, ES));
        assertEquals("", LogicaSesion.pistaPeso(e, 1, ES));
        assertEquals("10", LogicaSesion.pistaReps(e, 1, T));
        assertTrue(LogicaSesion.sinAnterior(s).isEmpty());
    }

    @Test
    public void pista_de_segundos_por_tiempo() {
        SesionEnCurso.Ejercicio e = ejercicio(7, 2);
        e.medida = "SEGUNDOS";
        e.minimo = 30;
        e.maximo = 60;
        e.anteriorCargado = true;
        e.anterior.add(new SesionEnCurso.SerieAnterior(1, null, 0, 40));

        assertEquals("0:40", LogicaSesion.pistaSegundos(e, 1, T));
        assertEquals("30–60 s", LogicaSesion.pistaSegundos(e, 2, T));
    }

    // ── Marcar es confirmar ──────────────────────────────────

    @Test
    public void marcar_con_campos_vacios_guarda_lo_de_la_pista() {
        SesionEnCurso.Ejercicio e = ejercicio(12, 2);
        e.minimo = 8;
        e.maximo = 12;
        e.anteriorCargado = true;
        e.anterior.add(new SesionEnCurso.SerieAnterior(1, "57.50", 12, null));

        assertEquals(LogicaSesion.Marcado.HECHA, LogicaSesion.marcar(e, 0, ES));
        assertEquals("57,5", e.series.get(0).peso);
        assertEquals("12", e.series.get(0).repeticiones);

        assertEquals("sin anterior, el rango no se confirma", LogicaSesion.Marcado.FALTAN_REPS,
                LogicaSesion.marcar(e, 1, ES));
        assertFalse(e.series.get(1).hecha);
        assertEquals("sin anterior no se inventan repeticiones", "", e.series.get(1).repeticiones);
        assertEquals("sin anterior no se inventa peso", "", e.series.get(1).peso);
    }

    @Test
    public void la_pauta_no_se_confirma_como_si_fuera_la_ultima_vez() {
        SesionEnCurso.Ejercicio fija = ejercicio(12, 1);
        fija.repeticionesPauta = 10;
        fija.anteriorCargado = true;
        assertEquals(LogicaSesion.Marcado.FALTAN_REPS, LogicaSesion.marcar(fija, 0, ES));
        assertFalse(fija.series.get(0).hecha);

        SesionEnCurso.Ejercicio plancha = ejercicio(7, 2);
        plancha.medida = "SEGUNDOS";
        plancha.minimo = 30;
        plancha.maximo = 60;
        plancha.anteriorCargado = true;
        plancha.anterior.add(new SesionEnCurso.SerieAnterior(1, null, 0, 40));

        assertEquals(LogicaSesion.Marcado.HECHA, LogicaSesion.marcar(plancha, 0, ES));
        assertEquals("con última vez se confirma", "0:40", plancha.series.get(0).segundos);
        assertEquals(LogicaSesion.Marcado.FALTAN_SEGUNDOS, LogicaSesion.marcar(plancha, 1, ES));
        assertFalse(plancha.series.get(1).hecha);
        assertEquals("", plancha.series.get(1).segundos);
    }

    @Test
    public void lo_escrito_manda_sobre_la_pista() {
        SesionEnCurso.Ejercicio e = ejercicio(12, 1);
        e.anteriorCargado = true;
        e.anterior.add(new SesionEnCurso.SerieAnterior(1, "57.50", 12, null));
        e.series.get(0).peso = "60";

        LogicaSesion.marcar(e, 0, ES);

        assertEquals("60", e.series.get(0).peso);
        assertEquals("12", e.series.get(0).repeticiones);
    }

    @Test
    public void sin_pista_ni_repeticiones_no_se_marca() {
        SesionEnCurso.Ejercicio e = ejercicio(12, 1);
        e.deRutina = false;
        e.anteriorCargado = true;

        assertEquals(LogicaSesion.Marcado.FALTAN_REPS, LogicaSesion.marcar(e, 0, ES));
        assertFalse(e.series.get(0).hecha);

        e.series.get(0).repeticiones = "abc";
        assertEquals(LogicaSesion.Marcado.NO_VALIDO, LogicaSesion.marcar(e, 0, ES));
        assertFalse(e.series.get(0).hecha);
    }

    @Test
    public void marcar_otra_vez_desmarca_sin_borrar_lo_escrito() {
        SesionEnCurso.Ejercicio e = ejercicio(12, 1);
        e.series.get(0).repeticiones = "10";
        LogicaSesion.marcar(e, 0, ES);

        assertEquals(LogicaSesion.Marcado.DESMARCADA, LogicaSesion.marcar(e, 0, ES));
        assertFalse(e.series.get(0).hecha);
        assertEquals("10", e.series.get(0).repeticiones);
    }

    // ── Qué se guarda ────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    public void solo_las_marcadas_renumeradas_y_sin_ejercicios_vacios() {
        SesionEnCurso s = new SesionEnCurso();
        s.rutinaId = 5;
        s.inicioMs = 1_790_000_000_000L; // 2026-09-21 16:13:20 en Madrid
        s.valoracion = 4;
        s.notas = "  bien  ";

        SesionEnCurso.Ejercicio press = ejercicio(12, 3);
        press.series.get(0).peso = "60";
        press.series.get(0).repeticiones = "10";
        press.series.get(2).peso = "62,5";
        press.series.get(2).repeticiones = "8";
        LogicaSesion.marcar(press, 0, ES);
        LogicaSesion.marcar(press, 2, ES);

        SesionEnCurso.Ejercicio vacio = ejercicio(13, 2);
        vacio.series.get(0).repeticiones = "10"; // escrita pero sin marcar

        SesionEnCurso.Ejercicio plancha = ejercicio(7, 1);
        plancha.medida = "SEGUNDOS";
        plancha.series.get(0).segundos = "0:45";
        LogicaSesion.marcar(plancha, 0, ES);

        s.ejercicios.addAll(Arrays.asList(press, vacio, plancha));

        Map<String, Object> cuerpo = LogicaSesion.cuerpo(s, "clave-1", 43, MADRID);

        assertEquals("clave-1", cuerpo.get("claveIdempotencia"));
        assertEquals(5, cuerpo.get("rutinaId"));
        assertEquals("fechaInicio, la del reloj", "2026-09-21T16:13:20", cuerpo.get("fechaInicio"));
        assertEquals(43, cuerpo.get("duracionMinutos"));
        assertEquals(4, cuerpo.get("valoracion"));
        assertEquals("bien", cuerpo.get("notas"));

        List<Map<String, Object>> ejercicios = (List<Map<String, Object>>) cuerpo.get("ejercicios");
        assertEquals("el ejercicio sin marcar no va", 2, ejercicios.size());

        List<Map<String, Object>> seriesPress = (List<Map<String, Object>>) ejercicios.get(0).get("series");
        assertEquals(2, seriesPress.size());
        assertEquals(1, seriesPress.get(0).get("numero"));
        assertEquals("la tercera pasa a ser la segunda", 2, seriesPress.get(1).get("numero"));
        assertEquals(0, new BigDecimal("62.5").compareTo((BigDecimal) seriesPress.get(1).get("peso")));
        assertEquals(8, seriesPress.get(1).get("repeticiones"));

        Map<String, Object> seriePlancha = ((List<Map<String, Object>>) ejercicios.get(1).get("series")).get(0);
        assertEquals(45, seriePlancha.get("segundos"));
        assertEquals(0, seriePlancha.get("repeticiones"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void las_pistas_confirmadas_se_guardan() {
        SesionEnCurso s = new SesionEnCurso();
        SesionEnCurso.Ejercicio e = ejercicio(12, 1);
        e.anteriorCargado = true;
        e.anterior.add(new SesionEnCurso.SerieAnterior(1, "57.50", 12, null));
        s.ejercicios.add(e);

        LogicaSesion.marcar(e, 0, ES);
        Map<String, Object> serie = ((List<Map<String, Object>>) ((List<Map<String, Object>>)
                LogicaSesion.cuerpo(s, "c", 30, MADRID).get("ejercicios")).get(0).get("series")).get(0);

        assertEquals(12, serie.get("repeticiones"));
        assertEquals(0, new BigDecimal("57.5").compareTo((BigDecimal) serie.get("peso")));
    }

    @Test
    public void sin_rutina_ni_valoracion_no_se_mandan() {
        SesionEnCurso s = new SesionEnCurso();
        Map<String, Object> cuerpo = LogicaSesion.cuerpo(s, "c", 30, MADRID);
        assertFalse(cuerpo.containsKey("rutinaId"));
        assertFalse(cuerpo.containsKey("valoracion"));
        assertFalse(cuerpo.containsKey("notas"));
    }

    @Test
    public void totales_de_series_y_ejercicios() {
        SesionEnCurso s = new SesionEnCurso();
        SesionEnCurso.Ejercicio a = ejercicio(1, 3);
        SesionEnCurso.Ejercicio b = ejercicio(2, 2);
        a.series.get(0).hecha = true;
        a.series.get(1).hecha = true;
        s.ejercicios.addAll(Arrays.asList(a, b));

        LogicaSesion.Totales t = LogicaSesion.totales(s);
        assertEquals(2, t.seriesHechas);
        assertEquals(5, t.seriesTotales);
        assertEquals(1, t.ejerciciosHechos);
        assertEquals(2, t.ejerciciosTotales);
        assertEquals(3, t.sinMarcar());
    }
}
