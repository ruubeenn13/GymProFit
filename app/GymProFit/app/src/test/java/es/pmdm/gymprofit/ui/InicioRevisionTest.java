package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// InicioRevisionTest — GP-212: lo que dejó la revisión de Inicio.
//
//   · la nota de «Esta semana» dice lo mismo que enseña la tarjeta, en los dos idiomas;
//   · ningún tamaño de texto de fragment_inicio.xml está escrito a mano en sp;
//   · la tarjeta de nutrición, que es pulsable, tiene nombre accesible: el título al
//     arrancar y, con datos, el resumen de kcal (con y sin objetivo).
// ============================================================
public class InicioRevisionTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Path INICIO = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit",
            "ui", "fragments", "InicioFragment.java");

    @Test
    public void la_nota_de_la_semana_explica_el_numero_de_cada_zona_en_los_dos_idiomas() throws IOException {
        assertEquals("Junto a cada zona, las series que llevas esta semana.",
                cadena("values", "semana_nota"));
        assertEquals("Next to each zone, the sets you have done this week.",
                cadena("values-en", "semana_nota"));
    }

    @Test
    public void inicio_no_escribe_tamanos_de_texto_en_sp() throws IOException {
        String layout = leer(RES.resolve("layout").resolve("fragment_inicio.xml"));
        Matcher m = Pattern.compile("android:textSize=\"([^\"]*)\"").matcher(layout);
        while (m.find()) {
            assertTrue("textSize a mano en fragment_inicio.xml: " + m.group(1),
                    m.group(1).startsWith("@dimen/"));
        }
    }

    @Test
    public void la_tarjeta_de_nutricion_tiene_nombre_accesible() throws IOException {
        String java = leer(INICIO);
        assertTrue("falta el nombre de la tarjeta",
                java.contains("findViewById(R.id.cardNutricionHoy).setContentDescription("));
        // Al arrancar (sin datos) y en las dos formas de pintar el día: con y sin objetivo.
        int llamadas = java.split("nombrarTarjetaNutricion\\(", -1).length - 1;
        assertTrue("nombrarTarjetaNutricion se llama al arrancar y al pintar con y sin objetivo",
                llamadas >= 4); // la definición y tres llamadas
        assertFalse("sin título, la tarjeta no tiene nombre", cadena("values", "nutricion_de_hoy").isEmpty());
    }

    private static String cadena(String carpeta, String nombre) throws IOException {
        String xml = leer(RES.resolve(carpeta).resolve("strings.xml"));
        Matcher m = Pattern.compile("<string name=\"" + Pattern.quote(nombre) + "\"[^>]*>([^<]*)</string>").matcher(xml);
        assertTrue("falta " + nombre + " en " + carpeta, m.find());
        return m.group(1).replace("\\'", "'");
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
