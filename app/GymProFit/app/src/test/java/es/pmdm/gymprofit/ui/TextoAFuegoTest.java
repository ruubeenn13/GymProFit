package es.pmdm.gymprofit.ui;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

// ============================================================
// TextoAFuegoTest — GP-071: ni texto ni unidades a fuego en lo que se ve.
//
// El barrido de GP-071 encontró «4 ejercicios», «g carbos», «kcal/100g», «min»,
// «USER»/«ADMIN» o «Ejercicio N» escritos en el código: en inglés salían en
// español. Este test lee el código y falla si vuelve:
//   · un setText, setTitle o setHint con un literal que lleva letras (sin contar
//     los especificadores de formato ni la clave de un extra);
//   · un literal que es una cifra con unidad pegada («%d kcal», « min», «%.0fg»).
// Los formatos van en strings.xml (ES y EN), con plurales donde se cuenta.
// ============================================================
public class TextoAFuegoTest {

    private static final Path JAVA = Paths.get("src", "main", "java");

    // Una llamada que pone texto visible: setText(…), setTitle(…), setHint(…)
    private static final Pattern LLAMADA_VISIBLE = Pattern.compile("\\.(setText|setTitle|setHint)\\((.*)");
    private static final Pattern LITERAL = Pattern.compile("\"([^\"]*)\"");
    // Especificador de formato: %d, %.1f, %1$s, %,d…
    private static final Pattern FORMATO = Pattern.compile("%[-,0-9.$]*[a-zA-Z]");
    private static final Pattern LETRA = Pattern.compile("\\p{L}");

    // Una unidad dentro de un literal: "%d kcal", " min", "%.0fg", "kcal/100g", "%.1f kg"
    private static final Pattern UNIDAD = Pattern.compile(
            "\"[^\"]*(%[-0-9.]*[dfs]\\s*|\\s)(kcal|kg|cm|min|g|L)\\b[^\"]*\"");

    @Test
    public void ningun_texto_visible_a_fuego() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String[] l : lineas()) {
            Matcher m = LLAMADA_VISIBLE.matcher(l[1]);
            if (!m.find()) continue;
            String argumentos = m.group(2);
            Matcher lit = LITERAL.matcher(argumentos);
            while (lit.find()) {
                // La clave de un extra no se ve: getStringExtra("nombre").
                if (argumentos.substring(0, lit.start()).endsWith("Extra(")) continue;
                String sinFormatos = FORMATO.matcher(lit.group(1)).replaceAll("");
                if (LETRA.matcher(sinFormatos).find()) {
                    fallos.add(l[0] + "  " + l[1]);
                    break;
                }
            }
        }
        if (!fallos.isEmpty()) fail("Texto visible escrito en el código:\n" + String.join("\n", fallos));
    }

    @Test
    public void ninguna_unidad_a_fuego() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (String[] l : lineas()) {
            if (UNIDAD.matcher(l[1]).find()) fallos.add(l[0] + "  " + l[1]);
        }
        if (!fallos.isEmpty()) fail("Unidad escrita en el código (va en strings.xml):\n" + String.join("\n", fallos));
    }

    // Cada línea de código, sin comentarios ni logs, con su posición: {fichero:línea, texto}.
    private static List<String[]> lineas() throws IOException {
        List<Path> ficheros;
        try (Stream<Path> s = Files.walk(JAVA)) {
            ficheros = s.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
        List<String[]> todas = new ArrayList<>();
        for (Path f : ficheros) {
            List<String> lineas = Files.readAllLines(f, StandardCharsets.UTF_8);
            for (int i = 0; i < lineas.size(); i++) {
                String l = lineas.get(i).trim();
                if (l.startsWith("//") || l.startsWith("*") || l.contains("Log.")) continue;
                todas.add(new String[]{JAVA.relativize(f) + ":" + (i + 1), l});
            }
        }
        return todas;
    }
}
