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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
// ProveedoresPrivacidadWebTest — la tabla «Con quién se comparten» de la política de
// privacidad nombra los proveedores que de verdad reciben datos personales.
//
// La lista sale del código y la configuración: Render aloja la API, TiDB Cloud guarda la
// base (DEC-068), Brevo envía el correo y gestiona la lista de espera, Firebase manda los
// avisos y los informes de fallo de la app, ML Kit (de Play Services) lee los códigos de
// barras y manda a Google sus datos de diagnóstico, Sentry recibe los fallos de la API
// (DEC-061), Cloudflare sirve la web, el DNS y el correo entrante, Gmail recibe ese correo
// reenviado, y Open Food Facts resuelve los códigos de barras. Si cambia un proveedor, cambian a la vez este test y la página.
// ============================================================
public class ProveedoresPrivacidadWebTest {

    private static final Set<String> PROVEEDORES = new LinkedHashSet<>(Arrays.asList(
            "Render", "TiDB Cloud (PingCAP)", "Brevo", "Google (Firebase)", "Google (ML Kit)",
            "Google (Gmail)", "Sentry", "Cloudflare", "Open Food Facts"));

    @Test
    public void laTablaNombraLosProveedoresReales() throws IOException {
        assertEquals(PROVEEDORES, proveedoresDeLaTabla());
    }

    @Test
    public void aivenYaNoAparece() throws IOException {
        assertFalse("la base ya no está en Aiven", pagina().contains("Aiven"));
    }

    // La primera celda de cada fila de la tabla que sigue a «Con quién se comparten».
    private static Set<String> proveedoresDeLaTabla() throws IOException {
        String html = pagina();
        int seccion = html.indexOf("Con quién se comparten");
        assertTrue("falta la sección de proveedores", seccion >= 0);
        int inicio = html.indexOf("<tbody>", seccion);
        int fin = html.indexOf("</tbody>", inicio);
        assertTrue("falta la tabla de proveedores", inicio >= 0 && fin > inicio);
        Matcher fila = Pattern.compile("<tr><td>(.*?)</td>").matcher(html.substring(inicio, fin));
        Set<String> nombres = new LinkedHashSet<>();
        while (fila.find()) nombres.add(fila.group(1));
        return nombres;
    }

    private static String pagina() throws IOException {
        Path fichero = raiz().resolve("web").resolve("privacidad.html");
        assertTrue("no existe web/privacidad.html", Files.exists(fichero));
        return new String(Files.readAllBytes(fichero), StandardCharsets.UTF_8);
    }

    // La raíz del repositorio: la primera carpeta hacia arriba que contiene web/.
    private static Path raiz() {
        Path raiz = Paths.get("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("web"))) raiz = raiz.getParent();
        assertNotNull("no se encuentra la carpeta web", raiz);
        return raiz;
    }
}
