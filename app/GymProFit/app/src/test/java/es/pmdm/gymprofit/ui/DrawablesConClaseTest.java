package es.pmdm.gymprofit.ui;

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
import java.util.stream.Stream;

// ============================================================
// DrawablesConClaseTest — un drawable que nombra su clase en el XML (<drawable class=…>) necesita
// que R8 la conserve con su constructor vacío: nadie la nombra en el código y en la release la
// quitaba. Visto en la comprobación en producción de GP-225: con BurbujaProfi la release se
// cerraba al abrir Inicio («Didn't find class es.pmdm.gymprofit.ui.widget.BurbujaProfi»).
// ============================================================
public class DrawablesConClaseTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Path REGLAS = Paths.get("proguard-rules.pro");

    @Test
    public void r8ConservaCadaDrawableConClase() throws IOException {
        List<String> clases = new ArrayList<>();
        try (Stream<Path> s = Files.walk(RES)) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".xml"))::iterator) {
                if (!p.getParent().getFileName().toString().startsWith("drawable")) continue;
                Matcher m = Pattern.compile("<drawable[^>]*\\sclass=\"([^\"]+)\"").matcher(leer(p));
                while (m.find()) clases.add(m.group(1));
            }
        }
        assertTrue("se esperaba al menos el bocadillo de Profi", !clases.isEmpty());
        String reglas = leer(REGLAS);
        List<String> sinRegla = new ArrayList<>();
        for (String c : clases) {
            if (!reglas.contains("-keep class " + c + " {") || !reglas.contains("<init>();")) sinRegla.add(c);
        }
        if (!sinRegla.isEmpty()) fail("Drawables con clase que R8 quitaría: " + sinRegla);
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
