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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// LogoMarcaTest — el logo único sale de los trazados de la G del arranque (GP-231, DEC-057).
//
// La G no se redibuja ni se vuelve a trazar: logo_marca.xml (Acerca de) y web/img/logo.svg
// llevan los dos trazados de g_arranque.xml tal cual, con sus colores de marca. La baldosa
// es #0B0C0E en claro y transparente en oscuro, así que en oscuro la G va sola en el mismo
// sitio. Y en res/ no queda ningún logo en PNG: la fuente de la marca vive fuera del APK.
// ============================================================
public class LogoMarcaTest {

    private static final Pattern PATH_DATA = Pattern.compile(
            "<path[^>]*android:name=\"%s\"[^>]*android:pathData=\"([^\"]+)\"", Pattern.DOTALL);

    @Test
    public void elLogoDeLaAppLlevaLosTrazadosDeLaGDelArranque() throws IOException {
        String arranque = leer(res("drawable/g_arranque.xml"));
        String logo = leer(res("drawable/logo_marca.xml"));
        for (String pieza : new String[]{"cuerpo", "trazo"}) {
            assertEquals("el " + pieza + " del logo no es el de g_arranque", trazado(arranque, pieza), trazado(logo, pieza));
        }
        assertTrue("el cuerpo no va en logo_g_cuerpo", logo.contains("android:fillColor=\"@color/logo_g_cuerpo\""));
        assertTrue("el trazo no va en logo_g_trazo", logo.contains("android:fillColor=\"@color/logo_g_trazo\""));
        assertTrue("la baldosa no va en logo_baldosa", logo.contains("android:fillColor=\"@color/logo_baldosa\""));
    }

    @Test
    public void elLogoDeLaWebLlevaLosMismosTrazados() throws IOException {
        String arranque = leer(res("drawable/g_arranque.xml"));
        String svg = leer(raiz().resolve("web/img/logo.svg"));
        assertTrue("logo.svg no lleva el cuerpo de g_arranque", svg.contains("d=\"" + trazado(arranque, "cuerpo") + "\""));
        assertTrue("logo.svg no lleva el trazo de g_arranque", svg.contains("d=\"" + trazado(arranque, "trazo") + "\""));
        assertTrue("logo.svg no pinta el cuerpo en su dorado", svg.contains("fill=\"" + color("values", "logo_g_cuerpo") + "\""));
        assertTrue("logo.svg no pinta el trazo en su naranja", svg.contains("fill=\"" + color("values", "logo_g_trazo") + "\""));
    }

    @Test
    public void laBaldosaSoloSeVeEnClaro() throws IOException {
        assertEquals("#0B0C0E", color("values", "logo_baldosa"));
        String noche = color("values-night", "logo_baldosa");
        assertTrue("en oscuro la baldosa no es transparente: " + noche, noche.matches("#00[0-9A-Fa-f]{6}"));
    }

    @Test
    public void acercaDeUsaElLogoUnicoYNoQuedanLogosEnRes() throws IOException {
        assertTrue("Acerca de no usa logo_marca",
                leer(res("layout/activity_acerca_de.xml")).contains("android:src=\"@drawable/logo_marca\""));
        assertFalse("sigue drawable/logo.png", Files.exists(res("drawable/logo.png")));
        assertFalse("sigue drawable-night/logo.png", Files.exists(res("drawable-night/logo.png")));
        assertFalse("hay un logo_marca por tema", Files.exists(res("drawable-night/logo_marca.xml")));
    }

    private static String trazado(String xml, String pieza) {
        Matcher m = Pattern.compile(String.format(PATH_DATA.pattern(), pieza), Pattern.DOTALL).matcher(xml);
        assertTrue("no está el trazado «" + pieza + "»", m.find());
        return m.group(1);
    }

    private static String color(String carpeta, String nombre) throws IOException {
        Matcher m = Pattern.compile("<color name=\"" + nombre + "\">([^<]+)</color>")
                .matcher(leer(res(carpeta + "/colors.xml")));
        assertTrue("no está el color " + nombre + " en " + carpeta, m.find());
        return m.group(1).trim();
    }

    private static Path res(String ruta) {
        return Paths.get("src/main/res").resolve(ruta);
    }

    private static Path raiz() {
        Path raiz = Paths.get("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("web"))) raiz = raiz.getParent();
        assertNotNull("no se encuentra la raíz del repositorio", raiz);
        return raiz;
    }

    private static String leer(Path p) throws IOException {
        assertTrue("falta " + p, Files.exists(p));
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
