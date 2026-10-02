package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
// LicenciasDatosTest — las fuentes de datos en Licencias de terceros (GP-153).
//
// Cada una, en los dos idiomas, con la licencia que dicen los términos de la web
// (web/terminos.html, punto 7): free-exercise-db de dominio público (Unlicense), wger
// con las licencias Creative Commons que indica cada ejercicio —no «CC BY-SA» a secas— y
// Open Food Facts con ODbL. Y con su enlace.
// ============================================================
public class LicenciasDatosTest {

    private static final Path RES = Paths.get("src", "main", "res");

    @Test
    public void lasTresFuentesConSuLicenciaEnLosDosIdiomas() throws IOException {
        for (String carpeta : new String[]{"values", "values-en"}) {
            assertTrue(carpeta, cadena(carpeta, "licencias_fed").contains("Unlicense"));
            assertTrue(carpeta, cadena(carpeta, "licencias_off").contains("ODbL"));
            String wger = cadena(carpeta, "licencias_wger") + " " + cadena(carpeta, "licencias_wger_uso");
            assertTrue(carpeta, wger.contains("Creative Commons"));
            assertFalse(carpeta + ": wger no es CC BY-SA a secas", wger.contains("BY-SA"));
            for (String uso : new String[]{"licencias_fed_uso", "licencias_wger_uso", "licencias_off_uso"}) {
                assertNotNull(carpeta + ": falta " + uso, cadena(carpeta, uso));
            }
        }
    }

    // Lote 1.6.1: los básicos salen de Ciqual 2025 (ANSES, Licence Ouverte, que pide
    // citarla) y de USDA FoodData Central (dominio público), como dicen los términos.
    @Test
    public void losBasicosCitanCiqualYUsda() throws IOException {
        for (String carpeta : new String[]{"values", "values-en"}) {
            String ciqual = cadena(carpeta, "licencias_ciqual");
            assertNotNull(carpeta + ": falta Ciqual", ciqual);
            assertTrue(carpeta, ciqual.contains("Ciqual 2025") && ciqual.contains("ANSES"));
            assertTrue(carpeta, ciqual.contains("Licence Ouverte"));
            String usda = cadena(carpeta, "licencias_usda");
            assertNotNull(carpeta + ": falta USDA", usda);
            assertTrue(carpeta, usda.contains("USDA FoodData Central"));
            assertNotNull(carpeta, cadena(carpeta, "licencias_ciqual_uso"));
            assertNotNull(carpeta, cadena(carpeta, "licencias_usda_uso"));
        }
        assertTrue(cadena("values", "url_ciqual").startsWith("https://ciqual.anses.fr"));
        assertTrue(cadena("values", "url_usda").startsWith("https://fdc.nal.usda.gov"));
    }

    @Test
    public void cadaFuenteTieneSuEnlace() throws IOException {
        assertTrue(cadena("values", "url_fed").startsWith("https://github.com/yuhonas/free-exercise-db"));
        assertTrue(cadena("values", "url_wger").startsWith("https://wger.de"));
        assertTrue(cadena("values", "url_off").startsWith("https://world.openfoodfacts.org"));
    }

    @Test
    public void losTerminosDeLaWebNombranLasMismas() throws IOException {
        Path raiz = Paths.get("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("web"))) raiz = raiz.getParent();
        assertNotNull(raiz);
        String terminos = new String(Files.readAllBytes(raiz.resolve("web").resolve("terminos.html")),
                StandardCharsets.UTF_8);
        for (String s : new String[]{"free-exercise-db", "wger", "Creative Commons", "Open Food Facts", "ODbL"}) {
            assertTrue("los términos no nombran " + s, terminos.contains(s));
        }
    }

    private static String cadena(String carpeta, String nombre) throws IOException {
        String xml = new String(Files.readAllBytes(RES.resolve(carpeta).resolve("strings.xml")), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("<string name=\"" + nombre + "\"[^>]*>(.*?)</string>").matcher(xml);
        return m.find() ? m.group(1) : null;
    }
}
