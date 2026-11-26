package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// ============================================================
// TransicionesWebTest — las páginas secundarias se cambian con View Transitions entre
// documentos, solo con CSS (fase 4 de «Un día», web/PLAN-UN-DIA.md §3).
//
// · Las dos páginas de una navegación tienen que pedirla con @view-transition, y solo con
//   movimiento permitido: con prefers-reduced-motion: reduce no hay transición.
// · Sin JavaScript: ni el ClientRouter de Astro ni ningún script.
// · El reloj de cabecera es el elemento compartido: un solo view-transition-name «reloj» por
//   página (con dos, el navegador se salta la transición entera).
// · La portada del dorsal (public/index.html) no la pide: entre ella y estas páginas se navega
//   como siempre. Cambia en la fase 3, cuando / pase a «Un día».
// ============================================================
public class TransicionesWebTest {

    private static final String[] PAGINAS = {
            "privacidad.html", "terminos.html", "eliminar-cuenta.html",
            "beta.html", "beta/revisa.html", "beta/dentro.html", "beta/entrar.html", "404.html",
    };

    private static final Pattern BLOQUE = Pattern.compile("@media\\s*\\(prefers-reduced-motion:\\s*no-preference\\)\\s*\\{");

    @Test
    public void cadaPaginaLaPideSoloConMovimientoPermitido() throws IOException {
        for (String pagina : PAGINAS) {
            String css = cssDe(pagina);
            String fuera = sinBloquesDeMovimiento(css);
            assertTrue(pagina + " no pide la transición entre documentos", css.contains("@view-transition"));
            assertFalse(pagina + " pide la transición también con movimiento reducido",
                    fuera.contains("@view-transition"));
            assertFalse(pagina + " anima la transición fuera del bloque de movimiento",
                    fuera.contains("::view-transition-old") || fuera.contains("::view-transition-new"));
        }
    }

    @Test
    public void sinJavaScriptNiClientRouter() throws IOException {
        for (String pagina : PAGINAS) {
            String html = leer(WebDist.dist().resolve(pagina));
            assertFalse(pagina + " lleva un script", html.contains("<script"));
            assertFalse(pagina + " usa el ClientRouter de Astro", html.contains("astro-view-transitions"));
        }
        try (Stream<Path> s = Files.walk(WebDist.web().resolve("src"))) {
            List<Path> astro = s.filter(p -> p.toString().endsWith(".astro")).collect(Collectors.toList());
            for (Path p : astro) {
                assertFalse(p.getFileName() + " importa astro:transitions", leer(p).contains("astro:transitions"));
            }
        }
    }

    @Test
    public void elRelojEsElUnicoElementoCompartido() throws IOException {
        for (String pagina : PAGINAS) {
            String html = leer(WebDist.dist().resolve(pagina));
            assertEquals(pagina + ": tiene que haber un reloj de cabecera y uno solo",
                    1, html.split("class=\"reloj reloj-cabecera\"", -1).length - 1);
            Matcher nombres = Pattern.compile("view-transition-name:\\s*([a-z0-9_-]+)").matcher(cssDe(pagina));
            while (nombres.find()) {
                assertEquals(pagina + ": otro elemento compartido además del reloj", "reloj", nombres.group(1));
            }
        }
    }

    @Test
    public void laPortadaDelDorsalNoLaPide() throws IOException {
        assertFalse("la portada del dorsal pide la transición: comprobar antes que convive",
                leer(WebDist.dist().resolve("portada.css")).contains("@view-transition"));
    }

    /** El CSS que aplica a la página: el de sus <style> y el de las hojas que enlaza. */
    private static String cssDe(String pagina) throws IOException {
        String html = leer(WebDist.dist().resolve(pagina));
        StringBuilder css = new StringBuilder();
        Matcher estilo = Pattern.compile("(?s)<style>(.*?)</style>").matcher(html);
        while (estilo.find()) css.append(estilo.group(1)).append('\n');
        Matcher hoja = Pattern.compile("<link rel=\"stylesheet\" href=\"(/[^\"?]+)[^\"]*\">").matcher(html);
        while (hoja.find()) css.append(leer(WebDist.dist().resolve(hoja.group(1).substring(1)))).append('\n');
        return css.toString();
    }

    private static String sinBloquesDeMovimiento(String css) {
        StringBuilder fuera = new StringBuilder();
        int desde = 0;
        Matcher m = BLOQUE.matcher(css);
        while (m.find(desde)) {
            fuera.append(css, desde, m.start());
            int nivel = 0;
            int i = m.end() - 1;
            for (; i < css.length(); i++) {
                if (css.charAt(i) == '{') nivel++;
                if (css.charAt(i) == '}' && --nivel == 0) break;
            }
            desde = i + 1;
        }
        fuera.append(css.substring(Math.min(desde, css.length())));
        return fuera.toString();
    }

    private static String leer(Path p) throws IOException {
        assertTrue("no existe " + p, Files.isRegularFile(p));
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
