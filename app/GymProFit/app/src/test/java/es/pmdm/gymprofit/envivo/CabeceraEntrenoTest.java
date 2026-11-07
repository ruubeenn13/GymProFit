package es.pmdm.gymprofit.envivo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.record.Record;
import es.pmdm.gymprofit.model.sesion.VezRutina;
import es.pmdm.gymprofit.utils.Cadenas;

// ============================================================
// CabeceraEntrenoTest — las sumas de la cabecera de Apuntar entreno (GP-241, puntos 1 y 4):
// con kilos, a 0 kg (peso corporal) y en un entreno libre; la frase del pie, la última vez en
// cascada y el récord contado como en la sesión en vivo. Con las cadenas de verdad.
// ============================================================
public class CabeceraEntrenoTest {

    private static long ids = 1;

    // ── Con kilos ─────────────────────────────────────────────

    @Test
    public void con_kilos_suma_solo_lo_marcado_y_cuenta_las_series_de_las_previstas() {
        SesionEnCurso.Ejercicio banca = ejercicio("Press de banca con barra",
                serie("70", "8", true), serie("72,5", "8", true), serie("70", "8", false));
        SesionEnCurso.Ejercicio remo = ejercicio("Remo con barra",
                serie("65", "8", true), serie("", "", false));
        CabeceraEntreno c = CabeceraEntreno.de(Arrays.asList(banca, remo), true, vez(8410, 21));

        // 70×8 + 72,5×8 + 65×8 = 560 + 580 + 520; la serie sin marcar no suma aunque tenga peso.
        assertEquals(1660, c.kilos, 0.001);
        assertEquals(3, c.marcadas);
        assertEquals(5, c.previstas);
        assertTrue(c.conRutina);
        assertTrue(c.cuadroKilos);
        // Los miles de cuatro cifras: Android no los agrupa en español y la JVM sí, así que en
        // español se prueban con cinco (abajo) y aquí en inglés, que los agrupa en los dos.
        assertEquals("3 sets logged and 1,660 kg. Anything you don't check isn't saved.", d(c.pie(en(), Cadenas.en().idioma())));
    }

    @Test
    public void una_sola_serie_va_en_singular_y_las_cifras_grandes_agrupan() {
        SesionEnCurso.Ejercicio sentadilla = ejercicio("Sentadilla", serie("100", "100", true));
        CabeceraEntreno c = CabeceraEntreno.de(Arrays.asList(sentadilla, ejercicio("Zancada", serie("40", "100", true))), true, null);
        assertEquals("2 series apuntadas y 14.000 kg. Lo que no marques no se guarda.", d(c.pie(es(), Cadenas.es().idioma())));

        CabeceraEntreno una = CabeceraEntreno.de(Arrays.asList(ejercicio("Curl", serie("12,5", "10", true))), true, null);
        assertEquals("1 serie apuntada y 125 kg. Lo que no marques no se guarda.", d(una.pie(es(), Cadenas.es().idioma())));
        assertEquals("1 set logged and 125 kg. Anything you don't check isn't saved.", d(una.pie(en(), Cadenas.en().idioma())));
    }

    // ── A 0 kg ────────────────────────────────────────────────

    @Test
    public void a_peso_corporal_no_lleva_cuadro_de_kilos_y_el_pie_cuenta_series() {
        SesionEnCurso.Ejercicio dominadas = ejercicio("Dominadas", serie("", "10", true), serie("", "8", true));
        SesionEnCurso.Ejercicio fondos = ejercicio("Fondos", serie("", "12", true), serie("", "", false));
        CabeceraEntreno c = CabeceraEntreno.de(Arrays.asList(dominadas, fondos), true, vez(0, 12));

        assertEquals(0, c.kilos, 0);
        assertEquals(3, c.marcadas);
        assertEquals(4, c.previstas);
        assertFalse("ni la última vez ni lo marcado tienen peso", c.cuadroKilos);
        assertEquals("3 series apuntadas. Lo que no marques no se guarda.", d(c.pie(es(), Cadenas.es().idioma())));
        assertEquals("3 sets logged. Anything you don't check isn't saved.", d(c.pie(en(), Cadenas.en().idioma())));
    }

    @Test
    public void con_peso_en_la_ultima_vez_de_un_ejercicio_lleva_el_cuadro_aunque_aun_no_sume() {
        SesionEnCurso.Ejercicio banca = ejercicio("Press de banca", serie("", "", false));
        banca.anterior.add(new SesionEnCurso.SerieAnterior(1, "67.5", 8, null));
        CabeceraEntreno c = CabeceraEntreno.de(Arrays.asList(banca), true, null);
        assertTrue(c.cuadroKilos);
        assertEquals(0, c.kilos, 0);
        // Con kilos en la última vez de la rutina, también.
        assertTrue(CabeceraEntreno.de(Arrays.asList(ejercicio("Dominadas", serie("", "", false))), true, vez(500, 3)).cuadroKilos);
    }

    @Test
    public void sin_nada_marcado_se_guardan_el_dia_y_la_duracion() {
        CabeceraEntreno c = CabeceraEntreno.de(Arrays.asList(ejercicio("Sentadilla", serie("100", "5", false))), true, null);
        assertEquals(0, c.marcadas);
        assertEquals(1, c.previstas);
        assertEquals("Sin series apuntadas: se guardan el día y la duración.", d(c.pie(es(), Cadenas.es().idioma())));
        assertEquals("No sets logged: the day and the duration are saved.", d(c.pie(en(), Cadenas.en().idioma())));
    }

    // ── Entreno libre ─────────────────────────────────────────

    @Test
    public void en_un_entreno_libre_solo_cuentan_las_marcadas() {
        CabeceraEntreno vacio = CabeceraEntreno.de(new ArrayList<>(), false, null);
        assertFalse(vacio.conRutina);
        assertEquals(0, vacio.marcadas);
        assertFalse("sin ejercicios no hay kilos que enseñar", vacio.cuadroKilos);

        SesionEnCurso.Ejercicio curl = ejercicio("Curl de bíceps", serie("15", "10", true), serie("15", "9", false));
        CabeceraEntreno libre = CabeceraEntreno.de(Arrays.asList(curl), false, null);
        assertFalse(libre.conRutina);
        assertEquals(1, libre.marcadas);
        assertEquals(150, libre.kilos, 0);
        assertTrue(libre.cuadroKilos);
        assertNull("libre no tiene última vez de rutina", CabeceraEntreno.ultimaVez(null, es(), Cadenas.es().idioma()));
    }

    // ── La última vez ─────────────────────────────────────────

    @Test
    public void la_ultima_vez_en_cascada() {
        Locale idioma = Cadenas.es().idioma();
        assertEquals("La última vez moviste 12.050 kg.", d(CabeceraEntreno.ultimaVez(vez(12050, 21), es(), idioma)));
        assertEquals("La última vez hiciste 12 series.", d(CabeceraEntreno.ultimaVez(vez(0, 12), es(), idioma)));
        assertEquals("La última vez hiciste 1 serie.", d(CabeceraEntreno.ultimaVez(vez(0, 1), es(), idioma)));
        assertEquals("La última vez, sin series hechas.", d(CabeceraEntreno.ultimaVez(vez(0, 0), es(), idioma)));
        assertEquals("Last time you moved 8,410 kg.", d(CabeceraEntreno.ultimaVez(vez(8410, 21), en(), Cadenas.en().idioma())));
        assertEquals("Last time you did 12 sets.", d(CabeceraEntreno.ultimaVez(vez(0, 12), en(), Cadenas.en().idioma())));
        assertEquals("Last time, no sets done.", d(CabeceraEntreno.ultimaVez(vez(0, 0), en(), Cadenas.en().idioma())));
        assertNull("la primera vez, nada", CabeceraEntreno.ultimaVez(null, es(), idioma));
    }

    // ── El récord ─────────────────────────────────────────────

    @Test
    public void el_record_es_la_mejor_marcada_que_supera_la_marca_una_por_ejercicio() {
        SesionEnCurso.Serie s1 = serie("70", "8", true), s2 = serie("72,5", "8", true), s3 = serie("75", "8", false);
        SesionEnCurso.Ejercicio banca = ejercicio("Press de banca", s1, s2, s3);
        banca.marca = new SesionEnCurso.Marca(Record.TIPO_PESO, "70", 8, 0);
        banca.marcaCargada = true;

        List<CabeceraEntreno.RecordHoy> r = CabeceraEntreno.records(Arrays.asList(banca), es());
        assertEquals(1, r.size());
        assertEquals("la mejor marcada, no la de 75 sin marcar", s2.id, r.get(0).serieId);
        assertEquals("Récord en press de banca: 72,5 kg × 8", d(CabeceraEntreno.frase(r, es(), Cadenas.es().idioma())));
        assertEquals("Record in press de banca: 72,5 kg × 8", d(CabeceraEntreno.frase(r, en(), Cadenas.en().idioma())));
    }

    @Test
    public void igualar_la_marca_no_es_record_y_sin_marca_tampoco() {
        SesionEnCurso.Ejercicio banca = ejercicio("Press de banca", serie("70", "8", true));
        banca.marca = new SesionEnCurso.Marca(Record.TIPO_PESO, "70", 8, 0);
        banca.marcaCargada = true;
        SesionEnCurso.Ejercicio nuevo = ejercicio("Remo", serie("100", "10", true));   // primera vez: sin marca
        nuevo.marcaCargada = true;
        SesionEnCurso.Ejercicio sinSaber = ejercicio("Curl", serie("100", "10", true)); // aún no se sabe su marca

        assertTrue(CabeceraEntreno.records(Arrays.asList(banca, nuevo, sinSaber), es()).isEmpty());
        assertNull(CabeceraEntreno.frase(new ArrayList<>(), es(), Cadenas.es().idioma()));
    }

    @Test
    public void con_peso_y_sin_peso_no_compiten() {
        SesionEnCurso.Ejercicio dominadas = ejercicio("Dominadas", serie("", "15", true));
        dominadas.marca = new SesionEnCurso.Marca(Record.TIPO_PESO, "10", 5, 0);
        dominadas.marcaCargada = true;
        assertTrue(CabeceraEntreno.records(Arrays.asList(dominadas), es()).isEmpty());

        dominadas.marca = new SesionEnCurso.Marca(Record.TIPO_REPETICIONES, null, 12, 0);
        assertEquals("Récord en dominadas: 15 reps",
                d(CabeceraEntreno.frase(CabeceraEntreno.records(Arrays.asList(dominadas), es()), es(), Cadenas.es().idioma())));
    }

    @Test
    public void varios_records_se_cuentan_y_se_nombran() {
        SesionEnCurso.Ejercicio banca = conRecord("Press de banca"), remo = conRecord("Remo con barra"),
                curl = conRecord("Curl");
        assertEquals("2 récords: press de banca y remo con barra", d(CabeceraEntreno.frase(
                CabeceraEntreno.records(Arrays.asList(banca, remo), es()), es(), Cadenas.es().idioma())));
        assertEquals("3 records: press de banca, remo con barra, and curl", d(CabeceraEntreno.frase(
                CabeceraEntreno.records(Arrays.asList(banca, remo, curl), en()), en(), Cadenas.en().idioma())));
    }

    // ── Apoyo ─────────────────────────────────────────────────

    // Los espacios duros de las cadenas («1660 kg», «15 reps»), como espacios normales.
    private static String d(String s) { return s == null ? null : s.replace(' ', ' '); }

    private static SesionEnCurso.Ejercicio conRecord(String nombre) {
        SesionEnCurso.Ejercicio e = ejercicio(nombre, serie("50", "10", true));
        e.marca = new SesionEnCurso.Marca(Record.TIPO_PESO, "40", 10, 0);
        e.marcaCargada = true;
        return e;
    }

    private static SesionEnCurso.Ejercicio ejercicio(String nombre, SesionEnCurso.Serie... series) {
        SesionEnCurso.Ejercicio e = new SesionEnCurso.Ejercicio();
        e.id = ids++;
        e.nombre = nombre;
        e.anteriorCargado = true;
        e.series = new ArrayList<>(Arrays.asList(series));
        return e;
    }

    private static SesionEnCurso.Serie serie(String peso, String reps, boolean hecha) {
        SesionEnCurso.Serie s = new SesionEnCurso.Serie();
        s.id = ids++;
        s.peso = peso;
        s.repeticiones = reps;
        s.hecha = hecha;
        return s;
    }

    private static VezRutina vez(double kilos, int series) {
        return new VezRutina(1, "2026-11-01T18:00:00", kilos, false, series);
    }

    private static CabeceraEntreno.Textos es() { return textos(Cadenas.es()); }

    private static CabeceraEntreno.Textos en() { return textos(Cadenas.en()); }

    // Los textos de la pantalla, con las cadenas de verdad.
    static CabeceraEntreno.Textos textos(Cadenas c) {
        return new CabeceraEntreno.Textos() {
            @NonNull @Override public String pieConKilos(int series, @NonNull String kilos) {
                return c.p("apuntar_pie_con_kilos", series, series, kilos);
            }
            @NonNull @Override public String pieSinKilos(int series) { return c.p("apuntar_pie_sin_kilos", series, series); }
            @NonNull @Override public String pieVacio() { return c.s("apuntar_pie_vacio"); }
            @NonNull @Override public String ultimaKilos(@NonNull String kilos) { return c.s("apuntar_ultima_kilos", kilos); }
            @NonNull @Override public String ultimaSeries(int series) { return c.p("apuntar_ultima_series", series, series); }
            @NonNull @Override public String ultimaSinSeries() { return c.s("apuntar_ultima_sin_series"); }
            @NonNull @Override public String recordUno(@NonNull String ejercicio, @NonNull String marca) {
                return c.s("apuntar_record_uno", ejercicio, marca);
            }
            @NonNull @Override public String recordVarios(int n, @NonNull String ejercicios) {
                return c.p("apuntar_records", n, n, ejercicios);
            }
            @NonNull @Override public String y() { return c.s("enumeracion_y"); }
            @NonNull @Override public String ySerie() { return c.s("tarjeta_y_serie"); }
            @NonNull @Override public String marca(@NonNull SesionEnCurso.Marca m) {
                // Como Marcas en la app, con los kilos en el formato de español del tablero.
                if (Record.TIPO_REPETICIONES.equals(m.tipo)) return c.p("record_repeticiones", m.repeticiones, m.repeticiones);
                return c.s("record_peso_por_reps", m.peso.replace('.', ',') + " kg", m.repeticiones);
            }
            @NonNull @Override public String sinNombre(int posicion) { return c.s("ejercicio_sin_nombre", posicion); }
        };
    }
}
