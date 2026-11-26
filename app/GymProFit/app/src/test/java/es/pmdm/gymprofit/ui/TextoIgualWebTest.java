package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// TextoIgualWebTest — las páginas secundarias dicen lo mismo, palabra a palabra, después de
// pasar a Astro (fase 4 de «Un día», web/PLAN-UN-DIA.md).
//
// Privacidad, términos y eliminar cuenta son textos legales que Google Play tiene declarados:
// rehacer su página no puede cambiar ni una palabra. Las de la beta y la 404, tampoco: se
// rehace el diseño, no lo que dicen. La referencia son las páginas tal como estaban en
// web/public antes de pasarlas, copiadas sin tocar a web/pruebas/antes/ (fuera de dist, fuera
// del zip). Cambiar un texto a propósito es cambiar también su copia de referencia, en el
// mismo commit, y que se vea en el diff.
//
// Lo que se compara es lo que lee una persona: el título, la descripción y el texto del body,
// sin etiquetas, sin comentarios y sin lo que va oculto al lector de pantalla (aria-hidden: el
// reloj, los adornos), con los espacios normalizados; y, aparte, los destinos de los enlaces,
// en orden. El diseño puede cambiar las etiquetas; el texto y a dónde lleva cada enlace, no.
// ============================================================
public class TextoIgualWebTest {

    /** Las páginas que pasan a Astro en la fase 4, por su ruta en dist. */
    private static final String[] PAGINAS = {
            "privacidad.html", "terminos.html", "eliminar-cuenta.html",
            "beta.html", "beta/revisa.html", "beta/dentro.html", "beta/entrar.html", "404.html",
    };

    /** Las etiquetas que no tienen cierre: un aria-hidden en ellas no oculta nada más. */
    private static final Set<String> VACIAS = new HashSet<>(Arrays.asList(
            "img", "input", "br", "hr", "meta", "link", "source", "wbr"));

    @Test
    public void cadaPaginaDiceLoMismoQueAntes() throws IOException {
        for (String pagina : PAGINAS) {
            String antes = texto(leer(referencia(pagina)));
            String ahora = texto(leer(WebDist.dist().resolve(pagina)));
            if (!antes.equals(ahora)) fail(pagina + ": el texto ha cambiado. " + diferencia(antes, ahora));
        }
    }

    @Test
    public void cadaEnlaceLlevaAlMismoSitio() throws IOException {
        for (String pagina : PAGINAS) {
            assertEquals(pagina + ": los enlaces no son los de antes",
                    enlaces(leer(referencia(pagina))), enlaces(leer(WebDist.dist().resolve(pagina))));
        }
    }

    @Test
    public void laNormalizacionVeUnaPalabraCambiada() {
        // Sin esto, un normalizador que lo borrase todo daría siempre verde.
        String html = "<html><head><title>T</title></head><body><p>Se borra <strong>todo</strong>.</p>"
                + "<span aria-hidden=\"true\"><span>07</span>:00</span></body></html>";
        assertEquals("T |  | Se borra todo.", texto(html));
        assertTrue(!texto(html).equals(texto(html.replace("todo", "casi todo"))));
    }

    // ── Normalización ─────────────────────────────────────────────────

    /** Título, descripción y texto visible del body, en una línea. */
    static String texto(String html) {
        String sin = html.replaceAll("(?s)<!--.*?-->", "");
        String titulo = primero(sin, "(?s)<title>(.*?)</title>");
        String descripcion = primero(sin, "<meta name=\"description\" content=\"([^\"]*)\"");
        String cuerpo = primero(sin, "(?s)<body[^>]*>(.*)</body>");
        cuerpo = cuerpo.replaceAll("(?s)<(script|style|svg)\\b.*?</\\1>", " ");
        cuerpo = sinOcultos(cuerpo);
        return normalizar(titulo) + " | " + normalizar(descripcion) + " | " + normalizar(cuerpo.replaceAll("<[^>]+>", " "));
    }

    /** Los href de los <a> que se ven, en orden. */
    static List<String> enlaces(String html) {
        String cuerpo = sinOcultos(primero(html.replaceAll("(?s)<!--.*?-->", ""), "(?s)<body[^>]*>(.*)</body>"));
        List<String> lista = new ArrayList<>();
        Matcher a = Pattern.compile("<a\\b[^>]*\\shref=\"([^\"]*)\"").matcher(cuerpo);
        while (a.find()) lista.add(a.group(1).replace("&amp;", "&"));
        return lista;
    }

    /** El HTML sin los elementos marcados aria-hidden="true" ni lo que llevan dentro. */
    private static String sinOcultos(String html) {
        Pattern abre = Pattern.compile("<([a-zA-Z][a-zA-Z0-9-]*)\\b[^>]*\\saria-hidden=\"true\"[^>]*>");
        StringBuilder salida = new StringBuilder(html);
        Matcher m = abre.matcher(salida);
        while (m.find()) {
            String etiqueta = m.group(1).toLowerCase();
            int fin = VACIAS.contains(etiqueta) ? m.end() : cierre(salida, m.end(), etiqueta);
            salida.replace(m.start(), fin, " ");
            m = abre.matcher(salida);
        }
        return salida.toString();
    }

    /** El final del cierre que empareja con una etiqueta abierta justo antes de «desde». */
    private static int cierre(CharSequence html, int desde, String etiqueta) {
        Matcher m = Pattern.compile("<(/?)" + etiqueta + "\\b[^>]*>", Pattern.CASE_INSENSITIVE).matcher(html);
        int nivel = 1;
        m.region(desde, html.length());
        while (m.find()) {
            nivel += m.group(1).isEmpty() ? 1 : -1;
            if (nivel == 0) return m.end();
        }
        throw new AssertionError("<" + etiqueta + " aria-hidden> sin cerrar");
    }

    private static String normalizar(String texto) {
        String t = texto.replace("&nbsp;", " ").replace(" ", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'");
        Matcher num = Pattern.compile("&#(x?)([0-9a-fA-F]+);").matcher(t);
        StringBuffer sb = new StringBuffer();
        while (num.find()) {
            int cp = Integer.parseInt(num.group(2), num.group(1).isEmpty() ? 10 : 16);
            num.appendReplacement(sb, Matcher.quoteReplacement(new String(Character.toChars(cp))));
        }
        num.appendTail(sb);
        return sb.toString().replaceAll("\\s+", " ").replaceAll(" ([.,:;)»])", "$1")
                .replaceAll("([(«]) ", "$1").trim();
    }

    private static String primero(String texto, String patron) {
        Matcher m = Pattern.compile(patron).matcher(texto);
        return m.find() ? m.group(1) : "";
    }

    /** Dónde empiezan a ser distintos, con un poco de contexto a cada lado. */
    private static String diferencia(String antes, String ahora) {
        int i = 0;
        while (i < antes.length() && i < ahora.length() && antes.charAt(i) == ahora.charAt(i)) i++;
        int desde = Math.max(0, i - 60);
        return "Antes: «…" + antes.substring(desde, Math.min(antes.length(), i + 60)) + "…» · ahora: «…"
                + ahora.substring(desde, Math.min(ahora.length(), i + 60)) + "…»";
    }

    private static Path referencia(String pagina) {
        Path p = WebDist.web().resolve("pruebas").resolve("antes").resolve(pagina);
        assertTrue("falta la copia de referencia web/pruebas/antes/" + pagina, Files.isRegularFile(p));
        return p;
    }

    private static String leer(Path p) throws IOException {
        assertTrue("no existe " + p, Files.isRegularFile(p));
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
