package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

// ============================================================
// PortadaWebTest — la portada de gymprofit.app explica la app y lleva a la beta (GP-203).
//
// Es la página que más gente ve, así que no pide nada a terceros al cargar. Desde GP-205
// lleva JavaScript propio como mejora (DEC-053): solo scripts del dominio, sin manejadores
// en línea y dentro del presupuesto de 60 KB comprimidos. Las secciones van en el
// orden aprobado, con un solo h1. Las capturas son de la app real, con su texto
// alternativo y su tamaño fijo para que la página no salte al cargar; solo la del hero
// carga sin esperar. Y no lleva lo que se dejó fuera a propósito: ni redes todavía, ni
// el enlace de la prueba cerrada, ni el del grupo de probadores.
// ============================================================
public class PortadaWebTest {

    /** DEC-053: el JavaScript de la portada, comprimido, no pasa de 60 KB. */
    private static final int PRESUPUESTO_JS_GZIP = 60 * 1024;

    @Test
    public void soloScriptsPropiosYCeroRecursosDeFuera() throws IOException {
        String html = portada();
        assertFalse("la portada lleva manejadores de eventos", Pattern.compile("\\son[a-z]+=").matcher(html).find());
        Matcher carga = Pattern.compile("(?:src|srcset)=\"([^\"]*)\"|<link\\b[^>]*href=\"([^\"]*)\"").matcher(html);
        int recursos = 0;
        while (carga.find()) {
            String valor = carga.group(1) != null ? carga.group(1) : carga.group(2);
            // Un srcset lleva varias URL separadas por comas
            for (String url : valor.split(",")) {
                recursos++;
                assertTrue("la portada carga algo de fuera: " + url.trim(), url.trim().startsWith("/"));
            }
        }
        assertTrue("no se ha encontrado ningún recurso", recursos > 5);
        // Ningún script en línea: cada <script> es un fichero propio
        Matcher script = Pattern.compile("<script\\b([^>]*)>").matcher(html);
        while (script.find()) {
            Matcher src = Pattern.compile(" src=\"([^\"]*)\"").matcher(script.group(1));
            assertTrue("un script en línea: " + script.group(), src.find());
            assertTrue("un script de fuera: " + src.group(1), src.group(1).startsWith("/js/"));
        }
    }

    @Test
    public void elJavaScriptCabeEnElPresupuesto() throws IOException {
        int total = 0;
        List<String> rutas = scripts(portada());
        for (String ruta : rutas) {
            Path fichero = web().resolve(ruta.substring(1));
            assertTrue("no existe web" + ruta, Files.exists(fichero));
            total += gzip(Files.readAllBytes(fichero));
        }
        assertEquals("la portada tiene que cargar su script y sus tres librerías", 4, rutas.size());
        assertTrue("el JavaScript de la portada pesa " + total + " B comprimido, más de " + PRESUPUESTO_JS_GZIP,
                total <= PRESUPUESTO_JS_GZIP);
    }

    @Test
    public void lasLibreriasLlevanSuLicencia() throws IOException {
        int librerias = 0;
        for (String ruta : scripts(portada())) {
            if (!ruta.startsWith("/js/vendor/")) continue;
            librerias++;
            String js = new String(Files.readAllBytes(web().resolve(ruta.substring(1))), StandardCharsets.UTF_8);
            String cabecera = js.substring(0, Math.min(js.length(), 800)).toLowerCase();
            assertTrue(ruta + " no lleva su aviso de licencia", cabecera.contains("license") || cabecera.contains("mit"));
        }
        assertEquals("tiene que haber tres librerías", 3, librerias);
    }

    @Test
    public void lasSeccionesVanEnElOrdenAprobado() throws IOException {
        String html = portada();
        assertEquals("tiene que haber un solo h1", 1, ocurrencias(html, "<h1"));
        int anterior = -1;
        for (String id : new String[]{"titular", "problema", "entreno", "nutricion", "progreso", "beta", "preguntas"}) {
            int donde = html.indexOf("id=\"" + id + "\"");
            assertTrue("falta la sección " + id, donde >= 0);
            assertTrue("la sección " + id + " está fuera de orden", donde > anterior);
            anterior = donde;
        }
        assertTrue("el pie no va después de las preguntas", html.indexOf("<footer") > anterior);
    }

    @Test
    public void elHeroLlevaALaBeta() throws IOException {
        String hero = entre(portada(), "<section class=\"marco hero\"", "</section>");
        assertTrue("el botón del hero no lleva a /beta",
                hero.contains("<a class=\"boton\" href=\"/beta\">Entra en la beta</a>"));
        assertTrue("el hero no enseña la captura de inicio", hero.contains("/img/capturas/inicio-"));
    }

    @Test
    public void elProblemaEsElAprobado() throws IOException {
        String texto = texto(entre(portada(), "id=\"problema\"", "</section>"));
        assertTrue("el problema no dice «Entrenas en serio. Comes a ojo.»",
                texto.contains("Entrenas en serio. Comes a ojo."));
    }

    @Test
    public void cadaPilarTieneSuCapturaYDosOTresFunciones() throws IOException {
        String html = portada();
        for (String pilar : new String[]{"entreno", "nutricion", "progreso"}) {
            String seccion = entre(html, "id=\"" + pilar + "\"", "</section>");
            assertTrue(pilar + " no lleva su captura", seccion.contains("/img/capturas/" + pilar + "-"));
            int funciones = ocurrencias(entre(seccion, "<ul class=\"funciones\">", "</ul>"), "<li>");
            assertTrue(pilar + " tiene " + funciones + " funciones", funciones >= 2 && funciones <= 3);
        }
    }

    @Test
    public void lasCapturasNoSaltanYSoloLaDelHeroCargaYa() throws IOException {
        Matcher img = Pattern.compile("<img\\b[^>]*>").matcher(portada());
        int capturas = 0;
        while (img.find()) {
            String etiqueta = img.group();
            assertTrue("una imagen sin medidas: " + etiqueta,
                    etiqueta.contains(" width=\"") && etiqueta.contains(" height=\""));
            if (!etiqueta.contains("/img/capturas/")) continue;
            capturas++;
            Matcher alt = Pattern.compile(" alt=\"([^\"]*)\"").matcher(etiqueta);
            assertTrue("una captura sin texto alternativo: " + etiqueta, alt.find() && alt.group(1).length() > 20);
            boolean hero = etiqueta.contains("/img/capturas/inicio-");
            assertEquals("la captura del hero tiene que cargar ya y las demás, al llegar: " + etiqueta,
                    !hero, etiqueta.contains("loading=\"lazy\""));
            // Cada captura que pide la página existe, en webp
            Matcher src = Pattern.compile("/img/capturas/[a-z]+-\\d+\\.webp").matcher(etiqueta);
            while (src.find()) {
                assertTrue("no existe web" + src.group(), Files.exists(web().resolve(src.group().substring(1))));
            }
        }
        assertEquals("tiene que haber cuatro capturas", 4, capturas);
    }

    @Test
    public void cadaCapturaTieneSuReservaYSePrecarga() throws IOException {
        // GP-205: con un scroll rápido en un 4G, los móviles llegaban como rectángulos
        // negros. Cada captura lleva debajo su reserva borrosa (portada.css, regenerada con
        // scripts/reservas-capturas-web.py) y portada.js la pide un viewport antes.
        String css = new String(Files.readAllBytes(web().resolve("portada.css")), StandardCharsets.UTF_8);
        for (String captura : new String[]{"inicio", "entreno", "nutricion", "progreso"}) {
            Matcher reserva = Pattern.compile("\\.movil img\\[src\\*=\"/" + captura + "-\"\\] \\{ background: #[0-9A-F]{6} url\\(\"data:image/webp;base64,[A-Za-z0-9+/=]+\"\\)").matcher(css);
            assertTrue("la captura " + captura + " no tiene reserva", reserva.find());
            assertFalse("la reserva de " + captura + " es negra pura", reserva.group().contains("#000000"));
        }
        String js = new String(Files.readAllBytes(web().resolve("js/portada.js")), StandardCharsets.UTF_8);
        assertTrue("portada.js no precarga con un viewport de margen", js.contains("rootMargin: \"100% 0px\""));
        assertTrue("portada.js no decodifica antes de mostrar", js.contains(".decode()"));
        assertTrue("la precarga no corre antes de mirar el movimiento reducido",
                js.indexOf("precargarCapturas();") < js.indexOf("matchMedia(\"(prefers-reduced-motion: reduce)\")"));
    }

    @Test
    public void lasPreguntasSonLasAprobadas() throws IOException {
        String preguntas = entre(portada(), "id=\"preguntas\"", "</section>");
        assertEquals("tiene que haber tres preguntas", 3, ocurrencias(preguntas, "<details>"));
        String texto = texto(preguntas);
        assertTrue(texto.contains("¿Es gratis la beta?"));
        assertTrue(texto.contains("¿Hay versión para iPhone?"));
        assertTrue("no dice que es solo Android", texto.contains("solo para Android"));
        assertTrue(texto.contains("¿Qué hacéis con mis datos?"));
        assertTrue("la de los datos no enlaza la política", preguntas.contains("href=\"/privacidad\""));
    }

    @Test
    public void elPieLlevaElContactoYLosEnlacesLegales() throws IOException {
        String pie = entre(portada(), "<footer", "</footer>");
        for (String enlace : new String[]{"mailto:soporte@gymprofit.app", "mailto:privacidad@gymprofit.app",
                "/privacidad", "/terminos", "/eliminar-cuenta"}) {
            assertTrue("el pie no enlaza " + enlace, pie.contains("href=\"" + enlace + "\""));
        }
    }

    @Test
    public void noLlevaLoQueSeDejoFuera() throws IOException {
        String html = portada();
        assertFalse("la portada enlaza a TikTok", html.contains("tiktok.com"));
        assertFalse("la portada enlaza a Instagram", html.contains("instagram.com"));
        assertFalse("la portada lleva el enlace de la prueba cerrada", html.contains("play.google.com"));
        assertFalse("la portada lleva el enlace del grupo", html.contains("groups.google.com"));
    }

    // Las rutas, sin versión, de los scripts que pide la portada: los src y la lista data-libs.
    private static List<String> scripts(String html) {
        List<String> rutas = new ArrayList<>();
        Matcher m = Pattern.compile("(?:src|data-libs)=\"([^\"]*\\.js[^\"]*)\"").matcher(html);
        while (m.find()) {
            for (String ruta : m.group(1).trim().split("\\s+")) rutas.add(ruta.replaceAll("\\?v=.*$", ""));
        }
        return rutas;
    }

    private static int gzip(byte[] datos) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(salida)) {
            gz.write(datos);
        }
        return salida.size();
    }

    private static String entre(String html, String desde, String hasta) {
        int i = html.indexOf(desde);
        assertTrue("no se encuentra " + desde, i >= 0);
        int j = html.indexOf(hasta, i);
        assertTrue("no se encuentra " + hasta + " después de " + desde, j > i);
        return html.substring(i, j);
    }

    private static int ocurrencias(String texto, String buscado) {
        return texto.split(Pattern.quote(buscado), -1).length - 1;
    }

    // El texto sin etiquetas ni saltos de más.
    private static String texto(String html) {
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").replaceAll(" ([.,:;])", "$1");
    }

    private static String portada() throws IOException {
        return new String(Files.readAllBytes(web().resolve("index.html")), StandardCharsets.UTF_8);
    }

    private static Path web() {
        Path raiz = Paths.get("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("web"))) raiz = raiz.getParent();
        assertNotNull("no se encuentra la carpeta web", raiz);
        return raiz.resolve("web");
    }
}
