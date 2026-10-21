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
// PaginaNoEncontradaWebTest — una ruta que no existe responde 404 de verdad (GP-205).
//
// Cloudflare Pages sirve web/404.html, con estado 404, en cualquier ruta que no
// existe. Si el fichero falta, Pages trata el sitio como una aplicación de una página
// y responde con la portada y un 200, que los buscadores indexan como duplicada.
// Como se sirve en cualquier ruta (/beta/lo-que-sea incluida), todo lo que carga va
// con ruta absoluta; y no se indexa.
// ============================================================
public class PaginaNoEncontradaWebTest {

    @Test
    public void existeYNoSeIndexa() throws IOException {
        String html = pagina();
        assertTrue("404.html no lleva noindex", html.contains("<meta name=\"robots\" content=\"noindex\">"));
        assertTrue("404.html no tiene título", html.contains("<h1>"));
    }

    @Test
    public void llevaAlInicioYALaBeta() throws IOException {
        String html = pagina();
        assertTrue("404.html no lleva al inicio", html.contains("href=\"/\""));
        assertTrue("404.html no lleva a la beta", html.contains("href=\"/beta\""));
    }

    @Test
    public void todoLoQueCargaVaConRutaAbsoluta() throws IOException {
        String html = pagina();
        assertFalse("404.html lleva JavaScript", html.contains("<script"));
        Matcher carga = Pattern.compile("(?:src|srcset|href)=\"([^\"]*)\"").matcher(html);
        int recursos = 0;
        while (carga.find()) {
            String url = carga.group(1);
            if (url.startsWith("mailto:")) continue;
            recursos++;
            assertTrue("404.html carga o enlaza algo con ruta relativa: " + url, url.startsWith("/"));
        }
        assertTrue("no se ha encontrado ningún recurso", recursos > 5);
    }

    private static String pagina() throws IOException {
        Path raiz = Paths.get("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("web"))) raiz = raiz.getParent();
        assertNotNull("no se encuentra la carpeta web", raiz);
        Path fichero = raiz.resolve("web").resolve("404.html");
        assertTrue("falta web/404.html: Pages respondería 200 con la portada", Files.exists(fichero));
        return new String(Files.readAllBytes(fichero), StandardCharsets.UTF_8);
    }
}
