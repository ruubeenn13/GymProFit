package es.pmdm.gymprofit.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// Cadenas — las cadenas de verdad de res/values*/strings.xml para los tests de JVM (GP-241):
// una frase se prueba con el texto que va a ver el usuario, en español y en inglés, no con
// uno puesto a mano en el test. Lee como aapt: comillas que guardan espacios,   y \'.
// ============================================================
public final class Cadenas {

    private static final Path RES = Paths.get("src", "main", "res");

    /** «values» o «values-en». */
    private final String carpeta;
    private final String xml;

    private Cadenas(String carpeta) {
        this.carpeta = carpeta;
        try {
            this.xml = new String(Files.readAllBytes(RES.resolve(carpeta).resolve("strings.xml")), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Cadenas es() { return new Cadenas("values"); }

    public static Cadenas en() { return new Cadenas("values-en"); }

    /** El idioma de la carpeta. */
    public Locale idioma() {
        return carpeta.endsWith("-en") ? Locale.ENGLISH : new Locale("es", "ES");
    }

    /** Una cadena, con sus argumentos ya puestos si los lleva. */
    public String s(String nombre, Object... args) {
        Matcher m = Pattern.compile("<string name=\"" + nombre + "\"[^>]*>(.*?)</string>", Pattern.DOTALL).matcher(xml);
        if (!m.find()) throw new AssertionError(carpeta + ": falta " + nombre);
        return String.format(Locale.ROOT, android(m.group(1)), args);
    }

    /** Un plural de español o inglés: «one» con 1 y «other» con lo demás. */
    public String p(String nombre, int n, Object... args) {
        Matcher m = Pattern.compile("<plurals name=\"" + nombre + "\">(.*?)</plurals>", Pattern.DOTALL).matcher(xml);
        if (!m.find()) throw new AssertionError(carpeta + ": falta el plural " + nombre);
        Matcher item = Pattern.compile("<item quantity=\"" + (n == 1 ? "one" : "other") + "\">(.*?)</item>",
                Pattern.DOTALL).matcher(m.group(1));
        if (!item.find()) throw new AssertionError(carpeta + ": " + nombre + " sin su forma para " + n);
        return String.format(Locale.ROOT, android(item.group(1)), args);
    }

    private static String android(String s) {
        s = s.replace("\\u00A0", " ").replace("\\'", "'").replace("\\n", "\n");
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
        return s;
    }
}
