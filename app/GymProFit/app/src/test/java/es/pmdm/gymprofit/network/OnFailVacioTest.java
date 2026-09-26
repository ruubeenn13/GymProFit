package es.pmdm.gymprofit.network;

import static org.junit.Assert.assertEquals;
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
// OnFailVacioTest — GP-017: ningún fallo mudo.
//
// Un onFail vacío es un error de red que nadie ve: la pantalla se queda a medias y
// no dice por qué. Cada fallo se muestra, se reintenta, o se ignora con un comentario
// DENTRO del cuerpo que explique por qué es seguro. Este test lee el código y falla
// si un onFail no hace nada más que, como mucho, escribir en el log, y no lleva ese
// comentario. Solo el log no cuenta: el log no lo lee el usuario.
// ============================================================
public class OnFailVacioTest {

    private static final Path JAVA = Paths.get("src", "main", "java");
    private static final Pattern FIRMA = Pattern.compile("void\\s+onFail\\s*\\([^)]*\\)\\s*\\{");
    private static final Pattern COMENTARIO = Pattern.compile("//[^\\n]*|/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LOG = Pattern.compile("Log\\.[a-z]+\\s*\\((?:[^;]*)\\)\\s*;");

    @Test
    public void ningun_onFail_mudo_sin_comentario() throws IOException {
        List<Path> ficheros;
        try (Stream<Path> s = Files.walk(JAVA)) {
            ficheros = s.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
        List<String> fallos = new ArrayList<>();
        for (Path f : ficheros) {
            String codigo = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
            for (String cuerpo : cuerposMudos(codigo)) {
                fallos.add(JAVA.relativize(f) + "  onFail {" + cuerpo.trim() + "}");
            }
        }
        if (!fallos.isEmpty()) {
            fail("onFail que no avisa, no reintenta y no dice por qué se calla:\n" + String.join("\n", fallos));
        }
    }

    // Comprueba que el propio detector reconoce los casos (si no, el test de arriba
    // pasaría siempre).
    @Test
    public void el_detector_distingue_los_casos() {
        assertEquals(1, cuerposMudos("void onFail(int code, String message) { }").size());
        assertEquals(1, cuerposMudos("void onFail(int c, String m) {\n Log.w(\"x\", \"y\" + c);\n}").size());
        assertEquals(0, cuerposMudos("void onFail(int c, String m) {\n // seguro porque...\n}").size());
        assertEquals(0, cuerposMudos("void onFail(int c, String m) { UiFeedback.toastError(a, c, m); }").size());
    }

    // Cuerpos de onFail sin comentario y sin nada más que log.
    static List<String> cuerposMudos(String codigo) {
        List<String> mudos = new ArrayList<>();
        Matcher m = FIRMA.matcher(codigo);
        while (m.find()) {
            int inicio = m.end();
            int nivel = 1;
            int i = inicio;
            while (i < codigo.length() && nivel > 0) {
                char c = codigo.charAt(i);
                if (c == '{') nivel++;
                else if (c == '}') nivel--;
                i++;
            }
            String cuerpo = codigo.substring(inicio, Math.max(inicio, i - 1));
            boolean comentado = COMENTARIO.matcher(cuerpo).find();
            String sinLog = LOG.matcher(COMENTARIO.matcher(cuerpo).replaceAll("")).replaceAll("");
            if (!comentado && sinLog.trim().isEmpty()) mudos.add(cuerpo);
        }
        return mudos;
    }
}
