package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.record.Record;
import es.pmdm.gymprofit.ui.adapters.RecordAdapter;

// ============================================================
// ZonasTest — GP-088: de músculo a zona, en un solo sitio.
//
// Fija que cada clave de músculo que devuelve la API (Musculos.normalizar) cae en
// su zona, que la zona olvidada sigue el orden de gravedad de antes, que la
// pantalla de Récords agrupa en ese orden con «Otros» al final, y que el nombre
// del ejercicio sale en el idioma de la app cuando existe.
// ============================================================
public class ZonasTest {

    // Las claves que puede devolver Musculos.normalizar en la API.
    private static final List<String> CLAVES_API = Arrays.asList(
            "abdominales", "aductores", "biceps", "gemelos", "pecho", "antebrazos", "gluteos",
            "isquiotibiales", "dorsales", "lumbares", "cuello", "cuadriceps", "hombros",
            "trapecios", "triceps");

    @Test
    public void cada_musculo_de_la_api_cae_en_su_zona() {
        for (String clave : CLAVES_API) {
            if (clave.equals("cuello")) {
                assertNull("el cuello no es una zona que se entrene aparte", Zonas.deMusculo(clave));
            } else {
                assertEquals("sin zona: " + clave, true, Zonas.deMusculo(clave) != null);
            }
        }
        assertSame(Zonas.PIERNA, Zonas.deMusculo("gluteos"));
        assertSame(Zonas.ESPALDA, Zonas.deMusculo("lumbares"));
        assertSame(Zonas.BRAZOS, Zonas.deMusculo("antebrazos"));
        assertNull(Zonas.deMusculo(null));
    }

    @Test
    public void la_zona_olvidada_sigue_el_orden_de_gravedad() {
        Map<String, Integer> series = new HashMap<>();
        series.put("pecho", 6);
        assertSame("con solo pecho, falta pierna", Zonas.PIERNA, Zonas.sinTrabajar(series));

        series.put("cuadriceps", 4);
        assertSame(Zonas.ESPALDA, Zonas.sinTrabajar(series));

        for (String m : new String[]{"dorsales", "hombros", "biceps", "abdominales"}) series.put(m, 1);
        assertNull("todo tocado", Zonas.sinTrabajar(series));

        series.put("biceps", 0);
        assertSame("cero series no es tocar", Zonas.BRAZOS, Zonas.sinTrabajar(series));
    }

    @Test
    public void los_records_se_agrupan_por_zona_con_otros_al_final() {
        Record curl = record("biceps");
        Record sentadilla = record("cuadriceps");
        Record cuello = record("cuello");
        Record press = record("pecho");

        List<Object> filas = RecordAdapter.agrupar(Arrays.asList(curl, sentadilla, cuello, press));

        assertEquals(Arrays.asList(
                R.string.zona_titulo_pierna, sentadilla,
                R.string.zona_titulo_pecho, press,
                R.string.zona_titulo_brazos, curl,
                R.string.zona_titulo_otros, cuello), filas);
    }

    @Test
    public void el_nombre_sale_en_el_idioma_de_la_app_si_existe() {
        Record conIngles = new Record(1, "Sentadilla", "Squat", "cuadriceps", Record.TIPO_PESO, 100.0, 5, null);
        Record sinIngles = new Record(2, "Remo Gironda", null, "dorsales", Record.TIPO_PESO, 60.0, 8, null);

        assertEquals("Squat", conIngles.nombre(Locale.ENGLISH));
        assertEquals("Sentadilla", conIngles.nombre(new Locale("es")));
        assertEquals("sin traducción, el español", "Remo Gironda", sinIngles.nombre(Locale.ENGLISH));
    }

    private static Record record(String musculo) {
        return new Record(musculo.hashCode(), musculo, null, musculo, Record.TIPO_PESO, 50.0, 5, null);
    }
}
