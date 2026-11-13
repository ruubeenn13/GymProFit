package es.pmdm.gymprofit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// ============================================================
// LogoUnicoWebTest — la web usa un solo logo y un solo favicon (GP-231, DEC-057).
//
// El logo es la G dorada con el trazo naranja, la del icono de la app. No tiene
// variante por tema: sobre oscuro va sola y sobre claro la hoja le pone su baldosa.
// Antes cada página cambiaba entre dos logos (y dos favicons) con <picture> según
// prefers-color-scheme, y la G naranja del tema claro no era la de la app.
//
// Comprueba, en todas las páginas de web/: ningún <picture> ni <source> por tema,
// toda imagen de marca es /img/logo.svg, un solo favicon sin media y el
// apple-touch-icon; y que en img/ no queda otro fichero de logo.
//
// La única excepción es logo-correo.png (GP-266): la misma G sobre transparente, para
// la cabecera de los correos, que la cargan de gymprofit.app. Ninguna página la usa
// —todaImagenDeMarcaEsElLogoUnico falla si alguna la pone en un <img>—.
// ============================================================
public class LogoUnicoWebTest {

    private static final String LOGO = "/img/logo.svg";
    private static final String FAVICON = "/img/favicon.png";
    private static final String TOQUE = "/img/apple-touch-icon.png";
    /** El logo de los correos (GP-266): vive en la web solo para que los correos lo enlacen. */
    private static final String CORREO = "/img/logo-correo.png";

    /** Las imágenes de web/img que no son de marca: fondos y capturas. */
    private static final Pattern NO_MARCA = Pattern.compile("/img/(capturas/.*|tela\\.webp|tyvek\\.webp)");

    @Test
    public void ningunaPaginaCambiaDeLogoConElTema() throws IOException {
        for (Path p : paginas()) {
            String html = leer(p);
            assertFalse(nombre(p) + " usa <picture>", html.contains("<picture"));
            assertFalse(nombre(p) + " elige una imagen por tema con <source>", html.contains("<source"));
        }
    }

    @Test
    public void todaImagenDeMarcaEsElLogoUnico() throws IOException {
        int logos = 0;
        for (Path p : paginas()) {
            Matcher img = Pattern.compile("<img\\b[^>]*>").matcher(leer(p));
            while (img.find()) {
                String etiqueta = img.group();
                Matcher src = Pattern.compile("\\ssrc=\"([^\"]+)\"").matcher(etiqueta);
                assertTrue(nombre(p) + ": <img> sin src: " + etiqueta, src.find());
                String url = src.group(1);
                if (url.startsWith("/img/") && NO_MARCA.matcher(url).matches()) continue;
                assertEquals(nombre(p) + " usa otro logo", LOGO, url);
                assertTrue(nombre(p) + ": el logo no lleva class=\"logo\", que es lo que le pone la baldosa",
                        etiqueta.contains("class=\"logo\""));
                logos++;
            }
        }
        assertTrue("no se ha encontrado ningún logo", logos >= 9);
    }

    @Test
    public void unSoloFaviconParaLosDosTemas() throws IOException {
        for (Path p : paginas()) {
            String html = leer(p);
            List<String> iconos = new ArrayList<>();
            Matcher link = Pattern.compile("<link\\b[^>]*rel=\"(?:icon|shortcut icon)\"[^>]*>").matcher(html);
            while (link.find()) iconos.add(link.group());
            assertEquals(nombre(p) + " no tiene exactamente un favicon: " + iconos, 1, iconos.size());
            assertTrue(nombre(p) + " usa otro favicon: " + iconos.get(0), iconos.get(0).contains("href=\"" + FAVICON + "\""));
            assertFalse(nombre(p) + " elige el favicon por tema: " + iconos.get(0), iconos.get(0).contains("media="));
            assertTrue(nombre(p) + " no enlaza el apple-touch-icon",
                    html.contains("<link rel=\"apple-touch-icon\" href=\"" + TOQUE + "\">"));
        }
    }

    @Test
    public void enImgNoQuedaOtroLogo() throws IOException {
        Path img = web().resolve("img");
        TreeSet<String> marca;
        try (Stream<Path> s = Files.list(img)) {
            marca = s.filter(Files::isRegularFile)
                    .map(f -> "/img/" + f.getFileName())
                    .filter(u -> !NO_MARCA.matcher(u).matches())
                    .collect(Collectors.toCollection(TreeSet::new));
        }
        assertEquals("web/img tiene logos o favicons de más",
                new TreeSet<>(Arrays.asList(TOQUE, FAVICON, LOGO, CORREO)), marca);
    }

    private static List<Path> paginas() throws IOException {
        List<Path> lista;
        try (Stream<Path> s = Files.walk(web())) {
            lista = s.filter(f -> f.toString().endsWith(".html"))
                    .filter(f -> !f.toString().contains(".impeccable"))
                    .sorted()
                    .collect(Collectors.toList());
        }
        assertTrue("faltan páginas en web/: " + lista, lista.size() >= 9);
        return lista;
    }

    private static Path web() {
        Path raiz = Paths.get("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("web"))) raiz = raiz.getParent();
        assertNotNull("no se encuentra la carpeta web", raiz);
        return raiz.resolve("web");
    }

    private static String nombre(Path p) {
        return web().relativize(p).toString().replace('\\', '/');
    }

    private static String leer(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
