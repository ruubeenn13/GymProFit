package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
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

import es.pmdm.gymprofit.utils.ReglasEdad;

// ============================================================
// LegalTest — los términos de uso, a la vista (GP-087).
//
// La frase de «Guarda tu plan» confirma la edad mínima, que tiene que ser la de la API
// (ReglasEdad.MINIMA, DEC-038), y enlaza términos y privacidad. Ajustes y Acerca de
// abren los términos, que dejaron de estar ocultos cuando existió la página.
// ============================================================
public class LegalTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Path UI = Paths.get("src", "main", "java", "es", "pmdm", "gymprofit", "ui", "activities");

    @Test
    public void laFraseDelAltaConfirmaLaEdadMinimaYEnlazaLosDos() throws IOException {
        for (String carpeta : new String[]{"values", "values-en"}) {
            String frase = cadena(carpeta, "guarda_legal");
            assertNotNull(carpeta + ": falta guarda_legal", frase);
            assertTrue(carpeta + ": «" + frase + "» no dice la edad mínima",
                    frase.contains(String.valueOf(ReglasEdad.MINIMA)));
            assertTrue(carpeta + ": sin los términos", frase.contains("%1$s"));
            assertTrue(carpeta + ": sin la privacidad", frase.contains("%2$s"));
        }
    }

    @Test
    public void ajustesYAcercaDeAbrenLosTerminos() throws IOException {
        String ajustes = java("AjustesActivity.java");
        assertFalse("la fila de los términos sigue oculta",
                ajustes.contains("filaTerminos).setVisibility(View.GONE)"));
        assertTrue(ajustes.contains("R.string.url_terminos"));
        assertTrue("Acerca de no enlaza los términos", java("AcercaDeActivity.java").contains("R.string.url_terminos"));
    }

    @Test
    public void laDireccionEsLaDeLaWeb() throws IOException {
        assertEquals("https://gymprofit.app/terminos", cadena("values", "url_terminos"));
    }

    private static String java(String nombre) throws IOException {
        return new String(Files.readAllBytes(UI.resolve(nombre)), StandardCharsets.UTF_8);
    }

    private static String cadena(String carpeta, String nombre) throws IOException {
        String xml = new String(Files.readAllBytes(RES.resolve(carpeta).resolve("strings.xml")), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("<string name=\"" + nombre + "\"[^>]*>(.*?)</string>").matcher(xml);
        return m.find() ? m.group(1) : null;
    }
}
