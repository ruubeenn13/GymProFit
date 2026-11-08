package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import es.pmdm.gymprofit.model.programa.VistaPrevia;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;

// ============================================================
// TiempoRutinasTest — la hoja «¿Cuánto tiempo tienes por sesión?» (GP-243, puntos 2 y 3)
//
// La frase de arriba y la línea de cada rutina, con las cadenas de verdad en español y en
// inglés, en los cuatro casos del recorte: sin recorte, solo extras, extras y series, y 75+
// (los básicos ganan una serie). Y los bloques: los que se quedan con sus series, en su
// orden, y luego los que salen.
// ============================================================
public class TiempoRutinasTest {

    private static final Cadenas ES = Cadenas.es();
    private static final Cadenas EN = Cadenas.en();

    private static TiempoRutinas.Textos textos(Cadenas c) {
        return new TiempoRutinas.Textos() {
            @Override public String quedan(int quedan, int total) { return c.p("tiempo_linea_quedan", total, quedan, total); }
            @Override public String enteros(int total) { return c.p("tiempo_linea_enteros", total, total); }
            @Override public String todos(int total) { return c.p("tiempo_linea_todos", total, total); }
            @Override public String basicosA(int series) { return c.p("tiempo_linea_basicos_a", series, series); }
            @Override public String basicosCon(int series) { return c.p("tiempo_linea_basicos_con", series, series); }
            @Override public String basicosConTodas() { return c.s("tiempo_linea_basicos_todas"); }
            @Override public String minutos(int minutos) {
                return minutos >= 75 ? c.s("tiempo_minutos_75") : c.p("tiempo_minutos", minutos, minutos);
            }
            @Override public String fraseIgual(String minutos) { return c.s("tiempo_frase_igual", minutos); }
            @Override public String fraseBasicos(String minutos, int basicos) {
                return c.p("tiempo_frase_basicos", basicos, minutos, basicos);
            }
            @Override public String fraseSusBasicos(String minutos) { return c.s("tiempo_frase_sus_basicos", minutos); }
            @Override public String fraseSeries(String minutos, int series) {
                return c.p("tiempo_frase_series", series, minutos, series);
            }
            @Override public String fraseSalen(String minutos, int salen) {
                return c.p("tiempo_frase_salen", salen, minutos, salen);
            }
        };
    }

    private static RutinaEjercicio ej(String tipo, int series) {
        RutinaEjercicio e = new RutinaEjercicio();
        e.setTipo(tipo);
        e.setSeries(series);
        return e;
    }

    private static List<String> fuera(int n) {
        List<String> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add("Extra " + i);
        return l;
    }

    // Cuerpo completo A: 3 básicos y 4 extras en la plantilla, como el tablero.
    private static VistaPrevia.Rutina sinRecorte() {
        return new VistaPrevia.Rutina("Cuerpo completo A", 55, Arrays.asList(ej("BASICO", 3), ej("BASICO", 3),
                ej("BASICO", 3), ej("EXTRA", 3), ej("EXTRA", 3), ej("EXTRA", 2), ej("EXTRA", 2)),
                Collections.emptyList(), null);
    }

    private static VistaPrevia.Rutina soloExtras() {
        return new VistaPrevia.Rutina("Cuerpo completo B", 30,
                Arrays.asList(ej("BASICO", 3), ej("BASICO", 3), ej("BASICO", 3)), fuera(4), null);
    }

    private static VistaPrevia.Rutina extrasYSeries() {
        return new VistaPrevia.Rutina("Cuerpo completo C", 25,
                Arrays.asList(ej("BASICO", 2), ej("BASICO", 2), ej("BASICO", 2)), fuera(4), 2);
    }

    private static VistaPrevia.Rutina conMas() {
        return new VistaPrevia.Rutina("Cuerpo completo A", 70, Arrays.asList(ej("BASICO", 4), ej("BASICO", 4),
                ej("BASICO", 4), ej("EXTRA", 3), ej("EXTRA", 3), ej("EXTRA", 2), ej("EXTRA", 2)),
                Collections.emptyList(), 4);
    }

    @Test
    public void la_linea_sin_recorte() {
        assertEquals("Los 7 ejercicios, enteros.", TiempoRutinas.linea(textos(ES), sinRecorte()));
        assertEquals("All 7 exercises, in full.", TiempoRutinas.linea(textos(EN), sinRecorte()));
    }

    @Test
    public void la_linea_con_solo_extras_fuera() {
        assertEquals("3 de 7 ejercicios. Los básicos, con sus 3 series.", TiempoRutinas.linea(textos(ES), soloExtras()));
        assertEquals("3 of 7 exercises. Main lifts keep their 3 sets.", TiempoRutinas.linea(textos(EN), soloExtras()));
    }

    @Test
    public void la_linea_con_extras_fuera_y_series_menos() {
        assertEquals("3 de 7 ejercicios. Los básicos, a 2 series.", TiempoRutinas.linea(textos(ES), extrasYSeries()));
        assertEquals("3 of 7 exercises. Main lifts at 2 sets.", TiempoRutinas.linea(textos(EN), extrasYSeries()));
    }

    @Test
    public void la_linea_con_75_o_mas() {
        assertEquals("Los 7 ejercicios. Los básicos, a 4 series.", TiempoRutinas.linea(textos(ES), conMas()));
        assertEquals("All 7 exercises. Main lifts at 4 sets.", TiempoRutinas.linea(textos(EN), conMas()));
    }

    @Test
    public void basicos_con_series_distintas_no_inventan_un_numero() {
        VistaPrevia.Rutina r = new VistaPrevia.Rutina("X", 30, Arrays.asList(ej("BASICO", 3), ej("BASICO", 4)),
                fuera(2), null);
        assertEquals("2 de 4 ejercicios. Los básicos, con todas sus series.", TiempoRutinas.linea(textos(ES), r));
    }

    @Test
    public void la_frase_sin_recorte() {
        List<VistaPrevia.Rutina> r = Arrays.asList(sinRecorte(), sinRecorte());
        assertEquals("Con 60 minutos, tus rutinas se quedan enteras.", TiempoRutinas.frase(textos(ES), 60, r));
        assertEquals("With 60 minutes, your routines stay whole.", TiempoRutinas.frase(textos(EN), 60, r));
    }

    @Test
    public void la_frase_cuando_cada_rutina_se_queda_en_sus_basicos() {
        // El tablero: con 30 minutos, las tres rutinas se quedan en sus 3 básicos.
        List<VistaPrevia.Rutina> r = Arrays.asList(extrasYSeries(), soloExtras(), extrasYSeries());
        assertEquals("Con 30 minutos, cada rutina se queda en sus 3 básicos.", TiempoRutinas.frase(textos(ES), 30, r));
        assertEquals("With 30 minutes, each routine keeps its 3 main lifts.", TiempoRutinas.frase(textos(EN), 30, r));
        // Si no tienen los mismos básicos, sin número.
        List<VistaPrevia.Rutina> distintas = Arrays.asList(soloExtras(),
                new VistaPrevia.Rutina("Y", 30, Arrays.asList(ej("BASICO", 3), ej("BASICO", 3)), fuera(3), null));
        assertEquals("Con 30 minutos, cada rutina se queda en sus básicos.", TiempoRutinas.frase(textos(ES), 30, distintas));
    }

    @Test
    public void la_frase_con_solo_extras_fuera_en_algunas() {
        List<VistaPrevia.Rutina> r = Arrays.asList(sinRecorte(),
                new VistaPrevia.Rutina("Z", 45, Arrays.asList(ej("BASICO", 3), ej("BASICO", 3), ej("BASICO", 3),
                        ej("EXTRA", 3), ej("EXTRA", 3)), fuera(2), null));
        assertEquals("Con 45 minutos, salen 2 ejercicios de tus rutinas.", TiempoRutinas.frase(textos(ES), 45, r));
        assertEquals("With 45 minutes, your routines drop 2 exercises.", TiempoRutinas.frase(textos(EN), 45, r));
    }

    @Test
    public void la_frase_con_75_o_mas() {
        List<VistaPrevia.Rutina> r = Arrays.asList(conMas(), conMas());
        assertEquals("Con 75 minutos o más, los básicos pasan a 4 series.", TiempoRutinas.frase(textos(ES), 75, r));
        assertEquals("With 75 minutes or more, main lifts go to 4 sets.", TiempoRutinas.frase(textos(EN), 75, r));
    }

    @Test
    public void los_bloques_primero_los_que_se_quedan_y_luego_los_que_salen() {
        List<TiempoRutinas.Bloque> b = TiempoRutinas.bloques(extrasYSeries());
        assertEquals(7, b.size());
        for (int i = 0; i < 3; i++) {
            assertTrue(b.get(i).queda);
            assertEquals(2, b.get(i).series);
        }
        for (int i = 3; i < 7; i++) {
            assertFalse(b.get(i).queda);
            assertEquals(0, b.get(i).series);
        }
        assertEquals(7, TiempoRutinas.bloques(sinRecorte()).size());
    }

    @Test
    public void sin_datos_no_rompe() {
        VistaPrevia.Rutina vacia = new VistaPrevia.Rutina("V", 10, null, null, null);
        assertEquals("Los 0 ejercicios, enteros.", TiempoRutinas.linea(textos(ES), vacia));
        assertTrue(TiempoRutinas.bloques(vacia).isEmpty());
        assertEquals("Con 30 minutos, tus rutinas se quedan enteras.",
                TiempoRutinas.frase(textos(ES), 30, Collections.emptyList()));
    }
}
