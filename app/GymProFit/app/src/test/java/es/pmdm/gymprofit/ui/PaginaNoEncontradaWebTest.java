package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// PaginaNoEncontradaWebTest — una ruta que no existe responde 404 de verdad (GP-205), y desde
// la fase 4 de «Un día» la 404 es «la hora que no existe».
//
// Cloudflare Pages sirve dist/404.html, con estado 404, en cualquier ruta que no existe. Si el
// fichero falta, Pages trata el sitio como una aplicación de una página y responde con la
// portada y un 200, que los buscadores indexan como duplicada. Como se sirve en cualquier ruta
// (/beta/lo-que-sea incluida), todo lo que carga va con ruta absoluta; y no se indexa.
//
// En el mundo de «Un día» cada página marca su hora en el reloj; la 404 marca una que no puede
// ser (más de 23 horas o más de 59 minutos). Sin el JavaScript de la portada, ni ningún otro.
// ============================================================
public class PaginaNoEncontradaWebTest {

    @Test
    public void existeYNoSeIndexa() throws IOException {
        String html = pagina();
        assertTrue("404.html no lleva noindex", html.contains("<meta name=\"robots\" content=\"noindex\">"));
        assertTrue("404.html no tiene título", Pattern.compile("<h1\\b").matcher(html).find());
    }

    @Test
    public void llevaAlInicioYALaBeta() throws IOException {
        String html = pagina();
        assertTrue("404.html no lleva al inicio", html.contains("href=\"/\""));
        assertTrue("404.html no lleva a la beta", html.contains("href=\"/beta\""));
    }

    @Test
    public void marcaUnaHoraQueNoExiste() throws IOException {
        String html = pagina();
        int relojes = 0;
        Matcher reloj = Pattern.compile("<span class=\"(reloj [^\"]*|perdido-hora[^\"]*)\"[^>]*>(.*?)</span>\\s*(?:</h|<h|</div|</header)",
                Pattern.DOTALL).matcher(html);
        while (reloj.find()) {
            String cifras = reloj.group(2).replaceAll("<[^>]+>", "").replaceAll("\\s+", "");
            Matcher hora = Pattern.compile("^(\\d\\d):(\\d\\d)$").matcher(cifras);
            assertTrue("el reloj de la 404 no marca una hora: «" + cifras + "»", hora.matches());
            int h = Integer.parseInt(hora.group(1));
            int m = Integer.parseInt(hora.group(2));
            assertTrue("la 404 marca una hora que existe: " + cifras, h > 23 || m > 59);
            relojes++;
        }
        assertTrue("la 404 no tiene el reloj de cabecera y el grande", relojes >= 2);
    }

    @Test
    public void esDelMundoNuevoSinElJavaScriptDeLaPortada() throws IOException {
        String html = pagina();
        assertFalse("404.html sigue con la hoja del dorsal", html.contains("portada.css"));
        assertFalse("404.html sigue siendo un dorsal", html.contains("class=\"dorsal"));
        assertFalse("404.html carga el JavaScript de la portada", html.contains("portada.js"));
        assertFalse("404.html carga los módulos de «Un día»", html.contains("/_astro/") && html.contains(".js"));
    }

    // La 404 va siempre en carbón, como la portada: con el móvil en claro tampoco cambia. Un
    // solo theme-color, sin media, solo color-scheme oscuro, y ningún bloque claro que toque
    // algo más que la baldosa del logo (que en claro es del mismo carbón, DEC-057).
    @Test
    public void vaSiempreEnCarbon() throws IOException {
        String html = pagina();
        Matcher color = Pattern.compile("<meta name=\"theme-color\"[^>]*>").matcher(html);
        int colores = 0;
        while (color.find()) {
            colores++;
            assertEquals("el theme-color de la 404 no es el carbón",
                    "<meta name=\"theme-color\" content=\"#0B0C0E\">", color.group());
        }
        assertEquals("la 404 lleva más de un theme-color", 1, colores);
        assertTrue("la 404 no se declara solo oscura", html.contains("<meta name=\"color-scheme\" content=\"dark\">"));
        Matcher claro = Pattern.compile("prefers-color-scheme:\\s*light\\)\\s*\\{(.*?\\})\\s*\\}", Pattern.DOTALL).matcher(html);
        while (claro.find()) {
            assertEquals("la 404 cambia en tema claro: " + claro.group(1),
                    ".logo{background:#0b0c0e}", claro.group(1).replaceAll("\\s+", "").toLowerCase());
        }
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
        // Y lo que pide la hoja: las fuentes, con ruta absoluta también
        Matcher url = Pattern.compile("url\\(\"?([^\")]*)\"?\\)").matcher(html);
        while (url.find()) {
            recursos++;
            assertTrue("404.html pide desde la hoja algo con ruta relativa: " + url.group(1),
                    url.group(1).startsWith("/") || url.group(1).startsWith("data:"));
        }
        assertTrue("no se ha encontrado ningún recurso", recursos > 5);
    }

    private static String pagina() throws IOException {
        Path fichero = WebDist.dist().resolve("404.html");
        assertTrue("falta web/dist/404.html: Pages respondería 200 con la portada", Files.exists(fichero));
        return new String(Files.readAllBytes(fichero), StandardCharsets.UTF_8);
    }
}
