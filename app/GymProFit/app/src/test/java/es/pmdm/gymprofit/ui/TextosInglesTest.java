package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.pmdm.gymprofit.utils.FrasesA11y;

// ============================================================
// TextosInglesTest — el inglés de la app es el de EE. UU. y las frases para TalkBack se leen
// enteras (GP-225).
//
// Convivían «programme» y «program», «litres» y «liters», «licences» y «licenses». La cifra de
// las kcal se anunciaba «Left 3,061 kcal.» y la proteína juntaba «peso., te faltan».
// ============================================================
public class TextosInglesTest {

    private static final Path RES = Paths.get("src", "main", "res");

    // Lo británico y su forma de EE. UU.; «Licence Ouverte» es el nombre de una licencia.
    private static final Pattern BRITANICO = Pattern.compile(
            "(?i)\\b(programmes?|licen[cs]es?|licence|litres?|millilitres?|centimetres?|metres?|catalogue|"
                    + "analys(e|ed|ing)|fibre|colours?|favourites?|behaviours?|organis(e|ed|ing)|"
                    + "personalis(e|ed)|realis(e|ed)|recognis(e|ed)|travelled|cancelled|grey|centre|Proteins)\\b");

    @Test
    public void inglesDeEstadosUnidos() throws IOException {
        List<String> malas = new ArrayList<>();
        for (String linea : leer(RES.resolve("values-en/strings.xml")).split("\n")) {
            if (linea.trim().startsWith("<!--")) continue;
            String sinNombre = linea.replace("Licence Ouverte", "");
            Matcher m = BRITANICO.matcher(sinNombre);
            while (m.find()) {
                String w = m.group(1);
                if (w.equalsIgnoreCase("license") || w.equalsIgnoreCase("licenses")) continue;
                malas.add(w + " → " + linea.trim());
            }
        }
        if (!malas.isEmpty()) fail("Inglés británico en values-en:\n" + String.join("\n", malas));
    }

    @Test
    public void laProteinaEnSingular() throws IOException {
        assertTrue(leer(RES.resolve("values-en/strings.xml"))
                .contains("<string name=\"nutricion_proteinas\">Protein</string>"));
    }

    @Test
    public void lasKcalQueQuedanSeDicenAlFinal() throws IOException {
        String en = leer(RES.resolve("values-en/strings.xml"));
        assertTrue(en.contains("<string name=\"nutricion_kcal_a11y_quedan\">%1$s of %2$s kilocalories. %3$s kilocalories left.</string>"));
        assertTrue(en.contains("<string name=\"nutricion_kcal_a11y_pasas\">%1$s of %2$s kilocalories. %3$s kilocalories over.</string>"));
        String codigo = leer(Paths.get("src", "main", "java", "es", "pmdm", "gymprofit", "ui", "fragments", "NutricionFragment.java"));
        assertFalse("la frase de las kcal ya no se arma con el rótulo visible", codigo.contains("R.string.nutricion_kcal_a11y,"));
    }

    @Test
    public void unaFraseNoLlevaDosPuntos() {
        assertEquals("Proteínas: 80 de 160 gramos. Te faltan 80 gramos. Calculada sobre tu peso",
                FrasesA11y.unir("Proteínas: 80 de 160 gramos", "Te faltan 80 gramos", "Calculada sobre tu peso."));
        assertEquals("Uno. Dos", FrasesA11y.unir("Uno.", null, "", "  ", "Dos"));
        assertEquals("", FrasesA11y.unir());
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
