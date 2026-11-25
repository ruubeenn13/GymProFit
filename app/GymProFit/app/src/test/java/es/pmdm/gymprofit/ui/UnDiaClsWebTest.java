package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// ============================================================
// UnDiaClsWebTest — lo que movía la página de «Un día» sin que nadie la tocara (CLS), medido
// con scripts/medir-un-dia.py el 25-11-2026 y corregido. Lee web/dist y web/src:
//
// · Las cifras que cambian (la cuenta atrás del descanso, la marca del récord) son tabulares en
//   lo que se sirve. El minificador ponía font-variant-numeric delante del atajo font, que la
//   anula: cada segundo de la cuenta atrás cambiaba de ancho.
// · El aviso del récord no se pliega en el layout: ni height ni márgenes ni rellenos animados;
//   las filas suben con transform.
// · El volumen de la cabecera reserva el ancho de su cifra final.
// ============================================================
public class UnDiaClsWebTest {

    @Test
    public void lasCifrasTabularesSobrevivenAlMinificador() throws IOException {
        List<String> anuladas = new ArrayList<>();
        int tabulares = 0;
        for (Path hoja : hojas()) {
            // Cada bloque de declaraciones sin llaves dentro: con anidado, el más interior.
            Matcher m = Pattern.compile("([^{}]*)\\{([^{}]*)\\}").matcher(leer(hoja));
            while (m.find()) {
                String bloque = m.group(2);
                int tab = bloque.indexOf("tabular-nums");
                if (tab < 0) continue;
                tabulares++;
                // Un atajo font: después de tabular-nums la devuelve a normal.
                if (Pattern.compile("(^|;)\\s*font\\s*:").matcher(bloque.substring(tab)).find()) {
                    anuladas.add(m.group(1).trim() + " → " + bloque);
                }
            }
        }
        assertTrue("no hay ninguna regla con tabular-nums en dist", tabulares >= 3);
        assertTrue("font: anula tabular-nums en " + anuladas, anuladas.isEmpty());
    }

    @Test
    public void elAvisoDelRecordNoCambiaElLayout() throws IOException {
        String js = leer(WebDist.web().resolve("src/movimiento/escena-entreno.js"));
        Matcher m = Pattern.compile("gsap\\.(?:to|set|fromTo|from)\\(aviso,([^;]*)").matcher(js);
        int llamadas = 0;
        while (m.find()) {
            llamadas++;
            assertFalse("el aviso se anima con propiedades de layout: " + m.group(1),
                    Pattern.compile("\\b(height|marginTop|paddingTop|paddingBottom|borderWidth)\\s*:").matcher(m.group(1)).find());
        }
        assertTrue("no se encuentra el aviso en la escena", llamadas > 0);
        assertTrue("las filas no suben con transform", js.contains("gsap.set(filasEj, { y: -pliegue })"));
    }

    @Test
    public void elVolumenReservaElAnchoDeSuCifraFinal() throws IOException {
        String html = leer(WebDist.dist().resolve("un-dia.html"));
        Matcher m = Pattern.compile("class=\"number cab-vol\"[^>]*style=\"--cifras: ?(\\d+)\"[^>]*>([^<]+)<").matcher(html);
        assertTrue("el volumen no lleva --cifras", m.find());
        assertTrue("--cifras no es el largo de la cifra final", Integer.parseInt(m.group(1)) == m.group(2).length());
        String css = hojas().stream().map(UnDiaClsWebTest::leerSinFallo).collect(Collectors.joining());
        assertTrue("el volumen no reserva su ancho",
                Pattern.compile("\\.cab-vol[^{]*\\{[^}]*min-width:\\s*calc\\(var\\(--cifras\\)").matcher(css).find());
    }

    private static List<Path> hojas() throws IOException {
        try (Stream<Path> s = Files.list(WebDist.dist().resolve("_astro"))) {
            return s.filter(p -> p.toString().endsWith(".css")).sorted().collect(Collectors.toList());
        }
    }

    private static String leerSinFallo(Path p) {
        try {
            return leer(p);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
