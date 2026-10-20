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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// ============================================================
// HojasVersionadasWebTest — cada página de web/ pide sus hojas con la versión de su
// contenido, y _headers no deja que el navegador se quede con una vieja (GP-201).
//
// Un navegador que ya había visitado /beta mezcló la página nueva con el beta.css de
// su caché, y la etiqueta del correo quedó debajo del campo. Las hojas se piden como
// /beta.css?v=<hash>, con los 8 primeros caracteres del SHA-256 de su contenido: si la
// hoja cambia y la versión del HTML no, este test falla. La versión se regenera con
// scripts/versionar-hojas-web.py (ver web/README.md). El hash se calcula con los saltos
// de línea normalizados a \n, para que salga igual en Windows y en el CI.
// ============================================================
public class HojasVersionadasWebTest {

    private static final Pattern HOJA = Pattern.compile("<link rel=\"stylesheet\" href=\"([^\"]+)\">");

    @Test
    public void cadaPaginaPideSusHojasConLaVersionDeSuContenido() throws Exception {
        Path web = web();
        List<Path> paginas = paginas(web);
        assertTrue("no hay páginas en web/", paginas.size() >= 7);
        for (Path pagina : paginas) {
            String html = leer(pagina);
            Matcher m = HOJA.matcher(html);
            int hojas = 0;
            while (m.find()) {
                hojas++;
                String href = m.group(1);
                Matcher v = Pattern.compile("^(/[a-z-]+\\.css)\\?v=([0-9a-f]{8})$").matcher(href);
                assertTrue(web.relativize(pagina) + " pide una hoja sin versión: " + href, v.matches());
                String esperada = version(web.resolve(v.group(1).substring(1)));
                assertEquals(web.relativize(pagina) + ": " + v.group(1) + " ha cambiado y su versión no; "
                        + "regenera con scripts/versionar-hojas-web.py", esperada, v.group(2));
            }
            assertTrue(web.relativize(pagina) + " no pide ninguna hoja", hojas > 0);
        }
    }

    @Test
    public void elHtmlSeRevalidaSiempreYLasHojasSeGuardanUnAno() throws IOException {
        String cabeceras = leer(web().resolve("_headers"));
        // Cada página, con una regla que la cubra y sin caché
        for (String ruta : new String[]{"/", "/beta", "/beta/*", "/privacidad", "/terminos", "/eliminar-cuenta",
                "/*.html"}) {
            assertEquals("la regla de " + ruta + " no pide revalidar", "no-cache", regla(cabeceras, ruta));
        }
        for (String hoja : new String[]{"/estilo.css", "/beta.css"}) {
            assertEquals(hoja + " no lleva caché larga", "public, max-age=31536000, immutable", regla(cabeceras, hoja));
        }
        // Las fuentes y las imágenes no van versionadas: no pueden ser immutable
        assertFalse("hay algo immutable que no va versionado",
                cabeceras.replaceAll("(?m)^/(estilo|beta)\\.css\\R[^\\n]*\\R?", "").contains("immutable"));
        // Ninguna regla con comodín para todo: Pages uniría su Cache-Control con el de las demás
        assertFalse("una regla /* se sumaría a las demás", Pattern.compile("(?m)^/\\*\\s*$").matcher(cabeceras).find());
    }

    // El Cache-Control de la regla de esa ruta exacta en _headers.
    private static String regla(String cabeceras, String ruta) {
        Matcher m = Pattern.compile("(?m)^" + Pattern.quote(ruta) + "\\s*\\R\\s+Cache-Control:\\s*(.+?)\\s*$")
                .matcher(cabeceras);
        assertTrue("_headers no tiene regla para " + ruta, m.find());
        return m.group(1);
    }

    /** Los 8 primeros caracteres hexadecimales del SHA-256 del fichero, con los saltos de línea en \n. */
    static String version(Path hoja) throws IOException, NoSuchAlgorithmException {
        byte[] texto = leer(hoja).replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8);
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto);
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < 4; i++) hex.append(String.format("%02x", hash[i]));
        return hex.toString();
    }

    private static List<Path> paginas(Path web) throws IOException {
        try (Stream<Path> todo = Files.walk(web)) {
            return todo.filter(p -> p.toString().endsWith(".html")).sorted().collect(Collectors.toCollection(ArrayList::new));
        }
    }

    private static String leer(Path fichero) throws IOException {
        assertTrue("no existe " + fichero, Files.exists(fichero));
        return new String(Files.readAllBytes(fichero), StandardCharsets.UTF_8);
    }

    private static Path web() {
        Path raiz = Paths.get("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("web"))) raiz = raiz.getParent();
        assertNotNull("no se encuentra la carpeta web", raiz);
        return raiz.resolve("web");
    }
}
