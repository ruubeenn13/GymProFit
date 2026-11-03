package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertTrue;

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
// FechasLocalizadasTest — ninguna fecha que se enseña en formato fijo (GP-228, DEC-020).
//
// Con la app en inglés, Medidas, el historial y las estadísticas de nutrición decían «30/09»:
// el patrón «dd/MM» estaba escrito a mano, o se formateaba con Locale.getDefault(), que es el
// idioma del proceso y no el de la app. Este test recorre el código y los recursos y falla si
// vuelve cualquiera de las tres cosas:
//   - un patrón de fecha numérico («dd/MM», «d.M.yyyy», «MM-dd»…) que no sea el ISO de la API;
//   - una fecha montada a trozos («substring(8, 10) + "/"»);
//   - Locale.getDefault() en una llamada que formatea.
// Las fechas se formatean con FechaUtils y FechaUtils.localeDeLaApp.
// ============================================================
public class FechasLocalizadasTest {

    private static final Path JAVA = Paths.get("src", "main", "java");
    private static final Path RES = Paths.get("src", "main", "res");

    private static final Pattern LITERAL = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
    // Día y mes en cifras unidos por «/», «.» o «-», en cualquier orden.
    private static final Pattern NUMERICO = Pattern.compile("(?<![A-Za-z])(d{1,2}[/.\\-]M{1,2}|M{1,2}[/.\\-]d{1,2})(?![A-Za-z])");
    // El ISO de la API, que no se enseña: con él se lee y se escribe lo que va y viene.
    private static final Pattern ISO = Pattern.compile("yyyy-MM-dd('T'HH:mm(:ss)?)?");
    private static final Pattern A_TROZOS = Pattern.compile("substring\\(\\s*\\d+\\s*,\\s*\\d+\\s*\\)\\s*\\+\\s*\"[/.\\-]\"");
    private static final Pattern FORMATEA = Pattern.compile("format|DateFormat|Instance\\(");
    private static final Pattern PATRON_RECURSO = Pattern.compile("<string name=\"[^\"]*patron[^\"]*\"[^>]*>([^<]*)</string>");

    @Test
    public void ningunPatronNumericoDeFechaEnElCodigo() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (Path p : ficheros(JAVA, ".java")) {
            List<String> lineas = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (int i = 0; i < lineas.size(); i++) {
                String l = lineas.get(i);
                if (l.trim().startsWith("//") || l.trim().startsWith("*")) continue;
                Matcher m = LITERAL.matcher(l);
                while (m.find()) {
                    String sinIso = ISO.matcher(m.group(1)).replaceAll("");
                    if (NUMERICO.matcher(sinIso).find()) fallos.add(p.getFileName() + ":" + (i + 1) + " " + l.trim());
                }
                if (A_TROZOS.matcher(l).find()) fallos.add(p.getFileName() + ":" + (i + 1) + " " + l.trim());
            }
        }
        assertTrue("Fechas con patrón numérico escrito a mano (usa FechaUtils):\n" + String.join("\n", fallos),
                fallos.isEmpty());
    }

    @Test
    public void ningunPatronNumericoDeFechaEnLosRecursos() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (Path p : ficheros(RES, "strings.xml")) {
            String xml = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
            Matcher m = PATRON_RECURSO.matcher(xml);
            while (m.find()) {
                if (NUMERICO.matcher(m.group(1)).find()) fallos.add(RES.relativize(p) + ": " + m.group(0));
            }
        }
        assertTrue("Patrones de fecha numéricos en los recursos:\n" + String.join("\n", fallos), fallos.isEmpty());
    }

    @Test
    public void ningunLocaleDelProcesoAlFormatear() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (Path p : ficheros(JAVA, ".java")) {
            List<String> lineas = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (int i = 0; i < lineas.size(); i++) {
                String l = lineas.get(i);
                if (l.trim().startsWith("//") || l.trim().startsWith("*")) continue;
                if (l.contains("Locale.getDefault()") && FORMATEA.matcher(l).find()) {
                    fallos.add(p.getFileName() + ":" + (i + 1) + " " + l.trim());
                }
            }
        }
        assertTrue("Locale.getDefault() al formatear (usa FechaUtils.localeDeLaApp):\n" + String.join("\n", fallos),
                fallos.isEmpty());
    }

    @Test
    public void elFormatoFijoDeFechaYHoraNoVuelve() throws IOException {
        Path utils = JAVA.resolve(Paths.get("es", "pmdm", "gymprofit", "utils", "FechaUtils.java"));
        String fuente = new String(Files.readAllBytes(utils), StandardCharsets.UTF_8);
        assertTrue("formatearFechaHora («dd/MM/yyyy HH:mm») se borró en GP-228",
                !fuente.contains("formatearFechaHora("));
    }

    private static List<Path> ficheros(Path raiz, String final_) throws IOException {
        try (Stream<Path> s = Files.walk(raiz)) {
            List<Path> r = new ArrayList<>();
            s.filter(p -> p.toString().endsWith(final_)).forEach(r::add);
            return r;
        }
    }
}
